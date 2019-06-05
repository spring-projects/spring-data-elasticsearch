/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchTypeMapper;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Elasticsearch specific {@link EntityReader} & {@link EntityWriter} implementation based on domain type
 * {@link ElasticsearchPersistentEntity metadata}.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public class ElasticsearchEntityMapper implements
		EntityConverter<ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty, Object, Map<String, Object>>,
		EntityWriter<Object, Map<String, Object>>, EntityReader<Object, Map<String, Object>>, InitializingBean,
		EntityMapper {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;
	private final ObjectReader objectReader;
	private final ObjectWriter objectWriter;

	private CustomConversions conversions = new ElasticsearchCustomConversions(Collections.emptyList());
	private EntityInstantiators instantiators = new EntityInstantiators();

	private ElasticsearchTypeMapper typeMapper;

	public ElasticsearchEntityMapper(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
			@Nullable GenericConversionService conversionService) {

		this.mappingContext = mappingContext;
		this.conversionService = conversionService != null ? conversionService : new DefaultConversionService();
		this.typeMapper = ElasticsearchTypeMapper.create(mappingContext);

		ObjectMapper objectMapper = new ObjectMapper();
		objectReader = objectMapper.readerFor(HashMap.class);
		objectWriter = objectMapper.writer();
	}

	// --> GETTERS / SETTERS

	@Override
	public MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public ConversionService getConversionService() {
		return conversionService;
	}

	/**
	 * Set the {@link CustomConversions} to be applied during the mapping process. <br />
	 * Conversions are registered after {@link #afterPropertiesSet() bean initialization}.
	 *
	 * @param conversions must not be {@literal null}.
	 */
	public void setConversions(CustomConversions conversions) {
		this.conversions = conversions;
	}

	/**
	 * Set the {@link ElasticsearchTypeMapper} to use for reading / writing type hints.
	 *
	 * @param typeMapper must not be {@literal null}.
	 */
	public void setTypeMapper(ElasticsearchTypeMapper typeMapper) {
		this.typeMapper = typeMapper;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		DateFormatterRegistrar.addDateConverters(conversionService);
		conversions.registerConvertersIn(conversionService);
	}

	// --> READ

	@Override
	public <T> T readObject(Map<String, Object> source, Class<T> targetType) {
		return read(targetType, source);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <R> R read(Class<R> type, Map<String, Object> source) {
		return doRead(source, ClassTypeInformation.from((Class<R>) ClassUtils.getUserClass(type)));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected <R> R doRead(Map<String, Object> source, TypeInformation<R> typeHint) {

		if (source == null) {
			return null;
		}

		typeHint = (TypeInformation<R>) typeMapper.readType(source, typeHint);

		if (conversions.hasCustomReadTarget(Map.class, typeHint.getType())) {
			return conversionService.convert(source, typeHint.getType());
		}

		if (typeHint.isMap() || ClassTypeInformation.OBJECT.equals(typeHint)) {
			return (R) source;
		}

		ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(typeHint);
		return readEntity(entity, source);
	}

	@SuppressWarnings("unchecked")
	protected <R> R readEntity(ElasticsearchPersistentEntity<?> entity, Map<String, Object> source) {

		ElasticsearchPersistentEntity<?> targetEntity = computeClosestEntity(entity, source);

		ElasticsearchPropertyValueProvider propertyValueProvider = new ElasticsearchPropertyValueProvider(
				new MapValueAccessor(source));

		EntityInstantiator instantiator = instantiators.getInstantiatorFor(targetEntity);

		R instance = (R) instantiator.createInstance(targetEntity,
				new PersistentEntityParameterValueProvider<>(targetEntity, propertyValueProvider, null));

		return targetEntity.requiresPropertyPopulation() ? readProperties(targetEntity, instance, propertyValueProvider)
				: instance;
	}

	protected <R> R readProperties(ElasticsearchPersistentEntity<?> entity, R instance,
			ElasticsearchPropertyValueProvider valueProvider) {

		PersistentPropertyAccessor<R> accessor = new ConvertingPropertyAccessor<>(entity.getPropertyAccessor(instance),
				conversionService);

		for (ElasticsearchPersistentProperty prop : entity) {

			if (entity.isConstructorArgument(prop) || prop.isScoreProperty()) {
				continue;
			}

			Object value = valueProvider.getPropertyValue(prop);
			if (value != null) {
				accessor.setProperty(prop, valueProvider.getPropertyValue(prop));
			}
		}

		return accessor.getBean();
	}

	@SuppressWarnings("unchecked")
	protected <R> R readValue(@Nullable Object source, ElasticsearchPersistentProperty property,
			TypeInformation<R> targetType) {

		if (source == null) {
			return null;
		}

		Class<R> rawType = targetType.getType();
		if (conversions.hasCustomReadTarget(source.getClass(), rawType)) {
			return rawType.cast(conversionService.convert(source, rawType));
		} else if (source instanceof List) {
			return readCollectionValue((List) source, property, targetType);
		} else if (source instanceof Map) {
			return readMapValue((Map<String, Object>) source, property, targetType);
		}

		return (R) readSimpleValue(source, targetType);
	}

	@SuppressWarnings("unchecked")
	private <R> R readMapValue(@Nullable Map<String, Object> source, ElasticsearchPersistentProperty property,
			TypeInformation<R> targetType) {

		TypeInformation information = typeMapper.readType(source);
		if (property.isEntity() && !property.isMap() || information != null) {

			ElasticsearchPersistentEntity<?> targetEntity = information != null
					? mappingContext.getRequiredPersistentEntity(information)
					: mappingContext.getRequiredPersistentEntity(property);
			return readEntity(targetEntity, source);
		}

		Map<String, Object> target = new LinkedHashMap<>();
		for (Entry<String, Object> entry : source.entrySet()) {

			if (isSimpleType(entry.getValue())) {
				target.put(entry.getKey(),
						readSimpleValue(entry.getValue(), targetType.isMap() ? targetType.getComponentType() : targetType));
			} else {

				ElasticsearchPersistentEntity<?> targetEntity = computeGenericValueTypeForRead(property, entry.getValue());

				if (targetEntity.getTypeInformation().isMap()) {

					Map<String, Object> valueMap = (Map) entry.getValue();
					if (typeMapper.containsTypeInformation(valueMap)) {
						target.put(entry.getKey(), readEntity(targetEntity, (Map) entry.getValue()));
					} else {
						target.put(entry.getKey(), readValue(valueMap, property, targetEntity.getTypeInformation()));
					}

				} else if (targetEntity.getTypeInformation().isCollectionLike()) {
					target.put(entry.getKey(),
							readValue(entry.getValue(), property, targetEntity.getTypeInformation().getActualType()));
				} else {
					target.put(entry.getKey(), readEntity(targetEntity, (Map) entry.getValue()));
				}
			}
		}

		return (R) target;
	}

	@SuppressWarnings("unchecked")
	private <R> R readCollectionValue(@Nullable List<?> source, ElasticsearchPersistentProperty property,
			TypeInformation<R> targetType) {

		if (source == null) {
			return null;
		}

		Collection<Object> target = createCollectionForValue(targetType, source.size());

		for (Object value : source) {

			if (isSimpleType(value)) {
				target.add(
						readSimpleValue(value, targetType.getComponentType() != null ? targetType.getComponentType() : targetType));
			} else {

				if (value instanceof List) {
					target.add(readValue(value, property, property.getTypeInformation().getActualType()));
				} else {
					target.add(readEntity(computeGenericValueTypeForRead(property, value), (Map) value));
				}
			}
		}

		return (R) target;
	}

	@SuppressWarnings("unchecked")
	private Object readSimpleValue(@Nullable Object value, TypeInformation<?> targetType) {

		Class<?> target = targetType.getType();

		if (value == null || target == null || ClassUtils.isAssignableValue(target, value)) {
			return value;
		}

		if (conversions.hasCustomReadTarget(value.getClass(), target)) {
			return conversionService.convert(value, target);
		}

		if (Enum.class.isAssignableFrom(target)) {
			return Enum.valueOf((Class<Enum>) target, value.toString());
		}

		return conversionService.convert(value, target);
	}

	// --> WRITE

	@Override
	public Map<String, Object> mapObject(Object source) {

		LinkedHashMap<String, Object> target = new LinkedHashMap<>();
		write(source, target);
		return target;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(@Nullable Object source, Map<String, Object> sink) {

		if (source == null) {
			return;
		}

		if (source instanceof Map) {

			sink.putAll((Map<String, Object>) source);
			return;
		}

		Class<?> entityType = ClassUtils.getUserClass(source.getClass());
		TypeInformation<?> type = ClassTypeInformation.from(entityType);

		if (requiresTypeHint(type, source.getClass(), null)) {
			typeMapper.writeType(source.getClass(), sink);
		}

		doWrite(source, sink, type);
	}

	protected void doWrite(@Nullable Object source, Map<String, Object> sink,
			@Nullable TypeInformation<? extends Object> typeHint) {

		if (source == null) {
			return;
		}

		Class<?> entityType = source.getClass();
		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(entityType, Map.class);

		if (customTarget.isPresent()) {

			sink.putAll(conversionService.convert(source, Map.class));
			return;
		}

		if (typeHint != null) {

			ElasticsearchPersistentEntity<?> entity = typeHint.getType().equals(entityType)
					? mappingContext.getRequiredPersistentEntity(typeHint)
					: mappingContext.getRequiredPersistentEntity(entityType);

			writeEntity(entity, source, sink, null);
			return;
		}

		// write Entity
		ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(entityType);
		writeEntity(entity, source, sink, null);
	}

	protected void writeEntity(ElasticsearchPersistentEntity<?> entity, Object source, Map<String, Object> sink,
			@Nullable TypeInformation containingStructure) {

		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);

		if (requiresTypeHint(entity.getTypeInformation(), source.getClass(), containingStructure)) {
			typeMapper.writeType(source.getClass(), sink);
		}

		writeProperties(entity, accessor, sink);
	}

	protected void writeProperties(ElasticsearchPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor,
			Map<String, Object> sink) {

		for (ElasticsearchPersistentProperty property : entity) {

			if (!property.isWritable()) {
				continue;
			}

			Object value = accessor.getProperty(property);

			if (value == null) {
				continue;
			}

			if (!isSimpleType(value)) {
				writeProperty(property, value, sink);
			} else {
				sink.put(property.getFieldName(), getWriteSimpleValue(value));
			}
		}
	}

	protected void writeProperty(ElasticsearchPersistentProperty property, Object value, Map<String, Object> sink) {

		Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(value.getClass());

		if (customWriteTarget.isPresent()) {

			Class<?> writeTarget = customWriteTarget.get();
			sink.put(property.getFieldName(), conversionService.convert(value, writeTarget));
			return;
		}

		TypeInformation<?> typeHint = property.getTypeInformation();
		if (typeHint.equals(ClassTypeInformation.OBJECT)) {

			if (value instanceof List) {
				typeHint = ClassTypeInformation.LIST;
			} else if (value instanceof Map) {
				typeHint = ClassTypeInformation.MAP;
			} else if (value instanceof Set) {
				typeHint = ClassTypeInformation.SET;
			} else if (value instanceof Collection) {
				typeHint = ClassTypeInformation.COLLECTION;
			}
		}

		sink.put(property.getFieldName(), getWriteComplexValue(property, typeHint, value));
	}

	protected Object getWriteSimpleValue(Object value) {

		if (value == null) {
			return null;
		}

		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(value.getClass());

		if (customTarget.isPresent()) {
			return conversionService.convert(value, customTarget.get());
		}

		return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
	}

	@SuppressWarnings("unchecked")
	protected Object getWriteComplexValue(ElasticsearchPersistentProperty property, TypeInformation<?> typeHint,
			Object value) {

		if (typeHint.isCollectionLike() || value instanceof Iterable) {
			return writeCollectionValue(value, property, typeHint);
		}
		if (typeHint.isMap()) {
			return writeMapValue((Map<String, Object>) value, property, typeHint);
		}

		if (property.isEntity() || !isSimpleType(value)) {
			return writeEntity(value, property, typeHint);
		}

		return value;
	}

	private Object writeEntity(Object value, ElasticsearchPersistentProperty property, TypeInformation<?> typeHint) {
		Map<String, Object> target = new LinkedHashMap<>();
		writeEntity(mappingContext.getRequiredPersistentEntity(value.getClass()), value, target,
				property.getTypeInformation());
		return target;
	}

	private Object writeMapValue(Map<String, Object> value, ElasticsearchPersistentProperty property,
			TypeInformation<?> typeHint) {

		Map<Object, Object> target = new LinkedHashMap<>();
		Streamable<Entry<String, Object>> mapSource = Streamable.of(value.entrySet());

		if (!typeHint.getActualType().getType().equals(Object.class)
				&& isSimpleType(typeHint.getMapValueType().getType())) {
			mapSource.forEach(it -> target.put(it.getKey(), getWriteSimpleValue(it.getValue())));
		} else {

			mapSource.forEach(it -> {

				Object converted = null;
				if (it.getValue() != null) {

					if (isSimpleType(it.getValue())) {
						converted = getWriteSimpleValue(it.getValue());
					} else {
						converted = getWriteComplexValue(property, ClassTypeInformation.from(it.getValue().getClass()),
								it.getValue());
					}
				}

				target.put(it.getKey(), converted);
			});
		}

		return target;
	}

	private Object writeCollectionValue(Object value, ElasticsearchPersistentProperty property,
			TypeInformation<?> typeHint) {

		Streamable<?> collectionSource = value instanceof Iterable ? Streamable.of((Iterable<?>) value)
				: Streamable.of(ObjectUtils.toObjectArray(value));

		List<Object> target = new ArrayList<>();
		if (!typeHint.getActualType().getType().equals(Object.class) && isSimpleType(typeHint.getActualType().getType())) {
			collectionSource.map(this::getWriteSimpleValue).forEach(target::add);
		} else {

			collectionSource.map(it -> {

				if (it == null) {
					return null;
				}

				if (isSimpleType(it)) {
					return getWriteSimpleValue(it);
				}

				return getWriteComplexValue(property, ClassTypeInformation.from(it.getClass()), it);
			}).forEach(target::add);

		}
		return target;
	}

	// --> LEGACY

	@Override
	public String mapToString(Object source) throws IOException {

		Map<String, Object> sink = new LinkedHashMap<>();
		write(source, sink);

		return objectWriter.writeValueAsString(sink);
	}

	@Override
	public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
		return read(clazz, objectReader.readValue(source));
	}

	// --> PRIVATE HELPERS

	private boolean requiresTypeHint(TypeInformation<?> type, Class<?> actualType,
			@Nullable TypeInformation<?> container) {

		if (container != null) {

			if (container.isCollectionLike()) {
				if (type.equals(container.getActualType()) && type.getType().equals(actualType)) {
					return false;
				}
			}

			if (container.isMap()) {
				if (type.equals(container.getMapValueType()) && type.getType().equals(actualType)) {
					return false;
				}
			}

			if (container.equals(type) && type.getType().equals(actualType)) {
				return false;
			}
		}

		return !conversions.isSimpleType(type.getType()) && !type.isCollectionLike()
				&& !conversions.hasCustomWriteTarget(type.getType());
	}

	/**
	 * Compute the type to use by checking the given entity against the store type;
	 *
	 * @param entity
	 * @param source
	 * @return
	 */
	private ElasticsearchPersistentEntity<?> computeClosestEntity(ElasticsearchPersistentEntity<?> entity,
			Map<String, Object> source) {

		TypeInformation<?> typeToUse = typeMapper.readType(source);

		if (typeToUse == null) {
			return entity;
		}

		if (!entity.getTypeInformation().getType().isInterface() && !entity.getTypeInformation().isCollectionLike()
				&& !entity.getTypeInformation().isMap()
				&& !ClassUtils.isAssignableValue(entity.getType(), typeToUse.getType())) {
			return entity;
		}

		return mappingContext.getRequiredPersistentEntity(typeToUse);
	}

	private ElasticsearchPersistentEntity<?> computeGenericValueTypeForRead(ElasticsearchPersistentProperty property,
			Object value) {

		return ClassTypeInformation.OBJECT.equals(property.getTypeInformation().getActualType())
				? mappingContext.getRequiredPersistentEntity(value.getClass())
				: mappingContext.getRequiredPersistentEntity(property.getTypeInformation().getActualType());
	}

	private Collection<Object> createCollectionForValue(TypeInformation<?> collectionTypeInformation, int size) {

		Class<?> collectionType = collectionTypeInformation.isSubTypeOf(Collection.class) //
				? collectionTypeInformation.getType() //
				: List.class;

		TypeInformation<?> componentType = collectionTypeInformation.getComponentType() != null //
				? collectionTypeInformation.getComponentType() //
				: ClassTypeInformation.OBJECT;

		return collectionTypeInformation.getType().isArray() //
				? new ArrayList<>(size) //
				: CollectionFactory.createCollection(collectionType, componentType.getType(), size);
	}

	private boolean isSimpleType(Object value) {
		return isSimpleType(value.getClass());
	}

	private boolean isSimpleType(Class<?> type) {
		return conversions.isSimpleType(type);
	}

	// --> OHTER STUFF

	@RequiredArgsConstructor
	class ElasticsearchPropertyValueProvider implements PropertyValueProvider<ElasticsearchPersistentProperty> {

		final MapValueAccessor mapValueAccessor;

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyValue(ElasticsearchPersistentProperty property) {
			return (T) readValue(mapValueAccessor.get(property), property, property.getTypeInformation());
		}

	}

	static class MapValueAccessor {

		final Map<String, Object> target;

		MapValueAccessor(Map<String, Object> target) {
			this.target = target;
		}

		public Object get(ElasticsearchPersistentProperty property) {

			String fieldName = property.getFieldName();

			if (!fieldName.contains(".")) {
				return target.get(fieldName);
			}

			Iterator<String> parts = Arrays.asList(fieldName.split("\\.")).iterator();
			Map<String, Object> source = target;
			Object result = null;

			while (source != null && parts.hasNext()) {

				result = source.get(parts.next());

				if (parts.hasNext()) {
					source = getAsMap(result);
				}
			}

			return result;
		}

		@SuppressWarnings("unchecked")
		private Map<String, Object> getAsMap(Object result) {

			if (result instanceof Map) {
				return (Map) result;
			}

			throw new IllegalArgumentException(String.format("%s is not a Map.", result));
		}
	}

}
