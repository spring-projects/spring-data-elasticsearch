/*
 * Copyright 2013-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentPropertyConverter;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Elasticsearch specific {@link org.springframework.data.convert.EntityConverter} implementation based on domain type
 * {@link ElasticsearchPersistentEntity metadata}.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 * @author Roman Puchkovskiy
 * @author Konrad Kurdej
 * @since 3.2
 */
public class MappingElasticsearchConverter
		implements ElasticsearchConverter, ApplicationContextAware, InitializingBean {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;

	private CustomConversions conversions = new ElasticsearchCustomConversions(Collections.emptyList());
	private EntityInstantiators instantiators = new EntityInstantiators();

	private ElasticsearchTypeMapper typeMapper;

	public MappingElasticsearchConverter(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		this(mappingContext, null);
	}

	public MappingElasticsearchConverter(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
			@Nullable GenericConversionService conversionService) {

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.mappingContext = mappingContext;
		this.conversionService = conversionService != null ? conversionService : new DefaultConversionService();
		this.typeMapper = ElasticsearchTypeMapper.create(mappingContext);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		if (mappingContext instanceof ApplicationContextAware) {
			((ApplicationContextAware) mappingContext).setApplicationContext(applicationContext);
		}
	}

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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		DateFormatterRegistrar.addDateConverters(conversionService);
		conversions.registerConvertersIn(conversionService);
	}

	// region read

	@SuppressWarnings("unchecked")
	@Override
	public <R> R read(Class<R> type, Document source) {
		TypeInformation<R> typeHint = ClassTypeInformation.from((Class<R>) ClassUtils.getUserClass(type));
		typeHint = (TypeInformation<R>) typeMapper.readType(source, typeHint);

		if (conversions.hasCustomReadTarget(Map.class, typeHint.getType())) {
			R converted = conversionService.convert(source, typeHint.getType());
			if (converted == null) {
				// EntityReader.read is defined as non nullable , so we cannot return null
				throw new ConversionException("conversion service to type " + typeHint.getType().getName() + " returned null");
			}
			return converted;
		}

		if (typeHint.isMap() || ClassTypeInformation.OBJECT.equals(typeHint)) {
			return (R) source;
		}

		ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(typeHint);
		return readEntity(entity, source);
	}

	protected <R> R readEntity(ElasticsearchPersistentEntity<?> entity, Map<String, Object> source) {

		ElasticsearchPersistentEntity<?> targetEntity = computeClosestEntity(entity, source);

		ElasticsearchPropertyValueProvider propertyValueProvider = new ElasticsearchPropertyValueProvider(
				new MapValueAccessor(source));

		EntityInstantiator instantiator = instantiators.getInstantiatorFor(targetEntity);

		@SuppressWarnings("unchecked")
		R instance = (R) instantiator.createInstance(targetEntity,
				new PersistentEntityParameterValueProvider<>(targetEntity, propertyValueProvider, null));

		if (!targetEntity.requiresPropertyPopulation()) {
			return instance;
		}

		R result = readProperties(targetEntity, instance, propertyValueProvider);

		if (source instanceof Document) {
			Document document = (Document) source;
			if (document.hasId()) {
				ElasticsearchPersistentProperty idProperty = targetEntity.getIdProperty();
				PersistentPropertyAccessor<R> accessor = new ConvertingPropertyAccessor<>(
						targetEntity.getPropertyAccessor(result), conversionService);
				// Only deal with String because ES generated Ids are strings !
				if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
					accessor.setProperty(idProperty, document.getId());
				}
			}

			if (document.hasVersion()) {
				long version = document.getVersion();
				ElasticsearchPersistentProperty versionProperty = targetEntity.getVersionProperty();
				// Only deal with Long because ES versions are longs !
				if (versionProperty != null && versionProperty.getType().isAssignableFrom(Long.class)) {
					// check that a version was actually returned in the response, -1 would indicate that
					// a search didn't request the version ids in the response, which would be an issue
					Assert.isTrue(version != -1, "Version in response is -1");
					targetEntity.getPropertyAccessor(result).setProperty(versionProperty, version);
				}
			}

			if (targetEntity.hasSeqNoPrimaryTermProperty() && document.hasSeqNo() && document.hasPrimaryTerm()) {
				if (isAssignedSeqNo(document.getSeqNo()) && isAssignedPrimaryTerm(document.getPrimaryTerm())) {
					SeqNoPrimaryTerm seqNoPrimaryTerm = new SeqNoPrimaryTerm(document.getSeqNo(), document.getPrimaryTerm());
					ElasticsearchPersistentProperty property = targetEntity.getRequiredSeqNoPrimaryTermProperty();
					targetEntity.getPropertyAccessor(result).setProperty(property, seqNoPrimaryTerm);
				}
			}
		}

		if (source instanceof SearchDocument) {
			SearchDocument searchDocument = (SearchDocument) source;
			if (targetEntity.hasScoreProperty()) {
				targetEntity.getPropertyAccessor(result) //
						.setProperty(targetEntity.getScoreProperty(), searchDocument.getScore());
			}
			populateScriptFields(result, searchDocument);
		}

		return result;

	}

	private boolean isAssignedSeqNo(long seqNo) {
		return seqNo >= 0;
	}

	private boolean isAssignedPrimaryTerm(long primaryTerm) {
		return primaryTerm > 0;
	}

	protected <R> R readProperties(ElasticsearchPersistentEntity<?> entity, R instance,
			ElasticsearchPropertyValueProvider valueProvider) {

		PersistentPropertyAccessor<R> accessor = new ConvertingPropertyAccessor<>(entity.getPropertyAccessor(instance),
				conversionService);

		for (ElasticsearchPersistentProperty prop : entity) {

			if (entity.isConstructorArgument(prop) || prop.isScoreProperty() || !prop.isReadable()) {
				continue;
			}

			Object value = valueProvider.getPropertyValue(prop);
			if (value != null) {
				accessor.setProperty(prop, value);
			}
		}

		return accessor.getBean();
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected <R> R readValue(@Nullable Object source, ElasticsearchPersistentProperty property,
			TypeInformation<R> targetType) {

		if (source == null) {
			return null;
		}

		if (property.hasPropertyConverter() && String.class.isAssignableFrom(source.getClass())) {
			source = property.getPropertyConverter().read((String) source);
		}

		Class<R> rawType = targetType.getType();
		if (conversions.hasCustomReadTarget(source.getClass(), rawType)) {
			return rawType.cast(conversionService.convert(source, rawType));
		} else if (source instanceof List) {
			return readCollectionValue((List<?>) source, property, targetType);
		} else if (source instanceof Map) {
			return readMapValue((Map<String, Object>) source, property, targetType);
		}

		return (R) readSimpleValue(source, targetType);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <R> R readCollectionValue(@Nullable List<?> source, ElasticsearchPersistentProperty property,
			TypeInformation<R> targetType) {

		if (source == null) {
			return null;
		}

		Collection<Object> target = createCollectionForValue(targetType, source.size());

		for (Object value : source) {

			if (value == null) {
				target.add(null);
			} else if (isSimpleType(value)) {
				target.add(
						readSimpleValue(value, targetType.getComponentType() != null ? targetType.getComponentType() : targetType));
			} else {

				if (value instanceof List) {
					target.add(readValue(value, property, property.getTypeInformation().getActualType()));
				} else if (value instanceof Map) {
					target
							.add(readMapValue((Map<String, Object>) value, property, property.getTypeInformation().getActualType()));
				} else {
					target.add(readEntity(computeGenericValueTypeForRead(property, value), (Map<String, Object>) value));
				}
			}
		}

		return (R) target;
	}

	@SuppressWarnings("unchecked")
	private <R> R readMapValue(@Nullable Map<String, Object> source, ElasticsearchPersistentProperty property,
			TypeInformation<R> targetType) {

		TypeInformation<?> information = typeMapper.readType(source);
		if (property.isEntity() && !property.isMap() || information != null) {

			ElasticsearchPersistentEntity<?> targetEntity = information != null
					? mappingContext.getRequiredPersistentEntity(information)
					: mappingContext.getRequiredPersistentEntity(property);
			return readEntity(targetEntity, source);
		}

		Map<String, Object> target = new LinkedHashMap<>();
		for (Entry<String, Object> entry : source.entrySet()) {

			String entryKey = entry.getKey();
			Object entryValue = entry.getValue();

			if (entryValue == null) {
				target.put(entryKey, null);
			} else if (isSimpleType(entryValue)) {
				target.put(entryKey,
						readSimpleValue(entryValue, targetType.isMap() ? targetType.getMapValueType() : targetType));
			} else {

				ElasticsearchPersistentEntity<?> targetEntity = computeGenericValueTypeForRead(property, entryValue);

				if (targetEntity.getTypeInformation().isMap()) {

					Map<String, Object> valueMap = (Map<String, Object>) entryValue;
					if (typeMapper.containsTypeInformation(valueMap)) {
						target.put(entryKey, readEntity(targetEntity, valueMap));
					} else {
						target.put(entryKey, readValue(valueMap, property, targetEntity.getTypeInformation()));
					}

				} else if (targetEntity.getTypeInformation().isCollectionLike()) {
					target.put(entryKey, readValue(entryValue, property, targetEntity.getTypeInformation().getActualType()));
				} else {
					target.put(entryKey, readEntity(targetEntity, (Map<String, Object>) entryValue));
				}
			}
		}

		return (R) target;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Nullable
	private Object readSimpleValue(@Nullable Object value, TypeInformation<?> targetType) {

		Class<?> target = targetType.getType();

		if (value == null || ClassUtils.isAssignableValue(target, value)) {
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

	private <T> void populateScriptFields(T result, SearchDocument searchDocument) {
		Map<String, List<Object>> fields = searchDocument.getFields();
		if (!fields.isEmpty()) {
			for (java.lang.reflect.Field field : result.getClass().getDeclaredFields()) {
				ScriptedField scriptedField = field.getAnnotation(ScriptedField.class);
				if (scriptedField != null) {
					String name = scriptedField.name().isEmpty() ? field.getName() : scriptedField.name();
					Object value = searchDocument.getFieldValue(name);
					if (value != null) {
						field.setAccessible(true);
						try {
							field.set(result, value);
						} catch (IllegalArgumentException e) {
							throw new ElasticsearchException("failed to set scripted field: " + name + " with value: " + value, e);
						} catch (IllegalAccessException e) {
							throw new ElasticsearchException("failed to access scripted field: " + name, e);
						}
					}
				}
			}
		}
	}
	// endregion

	// region write
	@Override
	public void write(Object source, Document sink) {

		Assert.notNull(source, "source to map must not be null");

		if (source instanceof Map) {
			// noinspection unchecked
			sink.putAll((Map<String, Object>) source);
			return;
		}

		Class<?> entityType = ClassUtils.getUserClass(source.getClass());
		TypeInformation<?> type = ClassTypeInformation.from(entityType);

		if (requiresTypeHint(type, source.getClass(), null)) {
			typeMapper.writeType(source.getClass(), sink);
		}

		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(entityType, Map.class);

		if (customTarget.isPresent()) {
			sink.putAll(conversionService.convert(source, Map.class));
			return;
		}

		ElasticsearchPersistentEntity<?> entity = type.getType().equals(entityType)
				? mappingContext.getRequiredPersistentEntity(type)
				: mappingContext.getRequiredPersistentEntity(entityType);

		writeEntity(entity, source, sink, null);
	}

	protected void writeEntity(ElasticsearchPersistentEntity<?> entity, Object source, Document sink,
			@Nullable TypeInformation<?> containingStructure) {

		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);

		if (requiresTypeHint(entity.getTypeInformation(), source.getClass(), containingStructure)) {
			typeMapper.writeType(source.getClass(), sink);
		}

		writeProperties(entity, accessor, new MapValueAccessor(sink));
	}

	protected void writeProperties(ElasticsearchPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor,
			MapValueAccessor sink) {

		for (ElasticsearchPersistentProperty property : entity) {

			if (!property.isWritable()) {
				continue;
			}

			Object value = accessor.getProperty(property);

			if (value == null) {
				continue;
			}

			if (property.hasPropertyConverter()) {
				ElasticsearchPersistentPropertyConverter propertyConverter = property.getPropertyConverter();
				value = propertyConverter.write(value);
			}

			if (!isSimpleType(value)) {
				writeProperty(property, value, sink);
			} else {
				Object writeSimpleValue = getWriteSimpleValue(value);
				if (writeSimpleValue != null) {
					sink.set(property, writeSimpleValue);
				}
			}
		}
	}

	protected void writeProperty(ElasticsearchPersistentProperty property, Object value, MapValueAccessor sink) {

		Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(value.getClass());

		if (customWriteTarget.isPresent()) {
			Class<?> writeTarget = customWriteTarget.get();
			sink.set(property, conversionService.convert(value, writeTarget));
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

		sink.set(property, getWriteComplexValue(property, typeHint, value));
	}

	@Nullable
	protected Object getWriteSimpleValue(Object value) {
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

		Document target = Document.create();
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
			mapSource.forEach(it -> {

				if (it.getValue() == null) {
					target.put(it.getKey(), null);
				} else {
					target.put(it.getKey(), getWriteSimpleValue(it.getValue()));
				}
			});
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
					// noinspection ReturnOfNull
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
	// endregion

	// region helper methods
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

	private ElasticsearchPersistentEntity<?> computeGenericValueTypeForRead(ElasticsearchPersistentProperty property,
			Object value) {

		return ClassTypeInformation.OBJECT.equals(property.getTypeInformation().getActualType())
				? mappingContext.getRequiredPersistentEntity(value.getClass())
				: mappingContext.getRequiredPersistentEntity(property.getTypeInformation().getActualType());
	}

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

	private boolean isSimpleType(Object value) {
		return isSimpleType(value.getClass());
	}

	private boolean isSimpleType(Class<?> type) {
		return conversions.isSimpleType(type);
	}
	// endregion

	// region queries
	@Override
	public void updateQuery(CriteriaQuery criteriaQuery, Class<?> domainClass) {
		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainClass);

		if (persistentEntity != null) {
			criteriaQuery.getCriteria().getCriteriaChain().forEach(criteria -> {
				String name = criteria.getField().getName();
				ElasticsearchPersistentProperty property = persistentEntity.getPersistentProperty(name);

				if (property != null && property.getName().equals(name)) {
					criteria.getField().setName(property.getFieldName());

					if (property.hasPropertyConverter()) {
						ElasticsearchPersistentPropertyConverter propertyConverter = property.getPropertyConverter();
						criteria.getQueryCriteriaEntries().forEach(criteriaEntry -> {
							Object value = criteriaEntry.getValue();
							if (value.getClass().isArray()) {
								Object[] objects = (Object[]) value;
								for (int i = 0; i < objects.length; i++) {
									objects[i] = propertyConverter.write(objects[i]);
								}
							} else {
								criteriaEntry.setValue(propertyConverter.write(value));
							}
						});
					}
				}
			});
		}
	}
	// endregion

	static class MapValueAccessor {

		final Map<String, Object> target;

		MapValueAccessor(Map<String, Object> target) {
			this.target = target;
		}

		@Nullable
		public Object get(ElasticsearchPersistentProperty property) {

			if (target instanceof Document) {
				// nested objects may have properties like 'id' which are recognized as isIdProperty() but they are not
				// Documents
				Document document = (Document) target;

				if (property.isIdProperty() && document.hasId()) {
					return document.getId();
				}

				if (property.isVersionProperty() && document.hasVersion()) {
					return document.getVersion();
				}

			}

			if (target instanceof SearchDocument && property.isScoreProperty()) {
				return ((SearchDocument) target).getScore();
			}

			String fieldName = property.getFieldName();

			if (!fieldName.contains(".")) {
				return target.get(fieldName);
			}

			Iterator<String> parts = Arrays.asList(fieldName.split("\\.")).iterator();
			Map<String, Object> source = target;
			Object result = null;

			while (parts.hasNext()) {

				result = source.get(parts.next());

				if (parts.hasNext()) {
					source = getAsMap(result);
				}
			}

			return result;
		}

		public void set(ElasticsearchPersistentProperty property, Object value) {

			if (property.isIdProperty()) {
				((Document) target).setId(value.toString());
			}

			if (property.isVersionProperty()) {
				((Document) target).setVersion((Long) value);
			}

			target.put(property.getFieldName(), value);
		}

		private Map<String, Object> getAsMap(Object result) {

			if (result instanceof Map) {
				// noinspection unchecked
				return (Map<String, Object>) result;
			}

			throw new IllegalArgumentException(String.format("%s is not a Map.", result));
		}
	}

	class ElasticsearchPropertyValueProvider implements PropertyValueProvider<ElasticsearchPersistentProperty> {

		final MapValueAccessor mapValueAccessor;

		ElasticsearchPropertyValueProvider(MapValueAccessor mapValueAccessor) {
			this.mapValueAccessor = mapValueAccessor;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyValue(ElasticsearchPersistentProperty property) {
			return (T) readValue(mapValueAccessor.get(property), property, property.getTypeInformation());
		}

	}

}
