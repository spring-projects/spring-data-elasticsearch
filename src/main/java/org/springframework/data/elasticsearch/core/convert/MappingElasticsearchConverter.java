/*
 * Copyright 2013-2021 the original author or authors.
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

import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentPropertyConverter;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Field;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.DefaultSpELExpressionEvaluator;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.mapping.model.SpELContext;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;
import org.springframework.data.mapping.model.SpELExpressionParameterValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
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
 * @author Subhobrata Dey
 * @author Marc Vanbrabant
 * @since 3.2
 */
public class MappingElasticsearchConverter
		implements ElasticsearchConverter, ApplicationContextAware, InitializingBean {

	private static final String INCOMPATIBLE_TYPES = "Cannot convert %1$s of type %2$s into an instance of %3$s! Implement a custom Converter<%2$s, %3$s> and register it with the CustomConversions.";
	private static final String INVALID_TYPE_TO_READ = "Expected to read Document %s into type %s but didn't find a PersistentEntity for the latter!";

	private static final Logger LOGGER = LoggerFactory.getLogger(MappingElasticsearchConverter.class);

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;

	// don't access directly, use getConversions(). to prevent null access
	private CustomConversions conversions = new ElasticsearchCustomConversions(Collections.emptyList());
	private final EntityInstantiators instantiators = new EntityInstantiators();

	private final ElasticsearchTypeMapper typeMapper;

	private final ConcurrentHashMap<String, Integer> propertyWarnings = new ConcurrentHashMap<>();
	private final SpELContext spELContext;

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
		this.spELContext = new SpELContext(new MapAccessor());
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

		Assert.notNull(conversions, "CustomConversions must not be null");

		this.conversions = conversions;
	}

	private CustomConversions getConversions() {
		return conversions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		DateFormatterRegistrar.addDateConverters(conversionService);
		getConversions().registerConvertersIn(conversionService);
	}

	// region read

	@SuppressWarnings("unchecked")
	@Override
	public <R> R read(Class<R> type, Document source) {
		TypeInformation<R> typeHint = ClassTypeInformation.from((Class<R>) ClassUtils.getUserClass(type));
		return read(typeHint, source);
	}

	protected <R> R readEntity(ElasticsearchPersistentEntity<?> entity, Map<String, Object> source) {

		ElasticsearchPersistentEntity<?> targetEntity = computeClosestEntity(entity, source);

		SpELExpressionEvaluator evaluator = new DefaultSpELExpressionEvaluator(source, spELContext);
		MapValueAccessor accessor = new MapValueAccessor(source);

		PreferredConstructor<?, ElasticsearchPersistentProperty> persistenceConstructor = entity
				.getPersistenceConstructor();

		ParameterValueProvider<ElasticsearchPersistentProperty> propertyValueProvider = persistenceConstructor != null
				&& persistenceConstructor.hasParameters() ? getParameterProvider(entity, accessor, evaluator)
						: NoOpParameterValueProvider.INSTANCE;

		EntityInstantiator instantiator = instantiators.getInstantiatorFor(targetEntity);

		@SuppressWarnings({ "unchecked", "ConstantConditions" })
		R instance = (R) instantiator.createInstance(targetEntity, propertyValueProvider);

		if (!targetEntity.requiresPropertyPopulation()) {
			return instance;
		}

		ElasticsearchPropertyValueProvider valueProvider = new ElasticsearchPropertyValueProvider(accessor, evaluator);
		R result = readProperties(targetEntity, instance, valueProvider);

		if (source instanceof Document) {
			Document document = (Document) source;
			if (document.hasId()) {
				ElasticsearchPersistentProperty idProperty = targetEntity.getIdProperty();
				PersistentPropertyAccessor<R> propertyAccessor = new ConvertingPropertyAccessor<>(
						targetEntity.getPropertyAccessor(result), conversionService);
				// Only deal with String because ES generated Ids are strings !
				if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
					propertyAccessor.setProperty(idProperty, document.getId());
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
			populateScriptFields(result, searchDocument);
		}

		return result;

	}

	private ParameterValueProvider<ElasticsearchPersistentProperty> getParameterProvider(
			ElasticsearchPersistentEntity<?> entity, MapValueAccessor source, SpELExpressionEvaluator evaluator) {

		ElasticsearchPropertyValueProvider provider = new ElasticsearchPropertyValueProvider(source, evaluator);

		// TODO: Support for non-static inner classes via ObjectPath
		PersistentEntityParameterValueProvider<ElasticsearchPersistentProperty> parameterProvider = new PersistentEntityParameterValueProvider<>(
				entity, provider, null);

		return new ConverterAwareSpELExpressionParameterValueProvider(evaluator, conversionService, parameterProvider);
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

			if (entity.isConstructorArgument(prop) || !prop.isReadable()) {
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
	protected <R> R readValue(@Nullable Object value, ElasticsearchPersistentProperty property, TypeInformation<?> type) {

		if (value == null) {
			return null;
		}

		Class<?> rawType = type.getType();

		if (property.hasPropertyConverter()) {
			value = propertyConverterRead(property, value);
		} else if (TemporalAccessor.class.isAssignableFrom(property.getType())
				&& !getConversions().hasCustomReadTarget(value.getClass(), rawType)) {

			// log at most 5 times
			String propertyName = property.getOwner().getType().getSimpleName() + '.' + property.getName();
			String key = propertyName + "-read";
			int count = propertyWarnings.computeIfAbsent(key, k -> 0);
			if (count < 5) {
				LOGGER.warn(
						"Type {} of property {} is a TemporalAccessor class but has neither a @Field annotation defining the date type nor a registered converter for reading!"
								+ " It cannot be mapped from a complex object in Elasticsearch!",
						property.getType().getSimpleName(), propertyName);
				propertyWarnings.put(key, count + 1);
			}
		}

		return readValue(value, type);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T> T readValue(Object value, TypeInformation<?> type) {

		Class<?> rawType = type.getType();

		if (conversions.hasCustomReadTarget(value.getClass(), rawType)) {
			return (T) conversionService.convert(value, rawType);
		} else if (value instanceof List) {
			return (T) readCollectionOrArray(type, (List<Object>) value);
		} else if (value.getClass().isArray()) {
			return (T) readCollectionOrArray(type, Arrays.asList((Object[]) value));
		} else if (value instanceof Map) {
			return (T) read(type, (Map<String, Object>) value);
		} else {
			return (T) getPotentiallyConvertedSimpleRead(value, rawType);
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <R> R read(TypeInformation<R> type, Map<String, Object> source) {

		Assert.notNull(source, "Source must not be null!");

		TypeInformation<? extends R> typeToUse = typeMapper.readType(source, type);
		Class<? extends R> rawType = typeToUse.getType();

		if (conversions.hasCustomReadTarget(source.getClass(), rawType)) {
			return conversionService.convert(source, rawType);
		}

		if (Document.class.isAssignableFrom(rawType)) {
			return (R) source;
		}

		if (typeToUse.isMap()) {
			return (R) readMap(typeToUse, source);
		}

		if (typeToUse.equals(ClassTypeInformation.OBJECT)) {
			return (R) source;
		}
		// Retrieve persistent entity info

		ElasticsearchPersistentEntity<?> entity = mappingContext.getPersistentEntity(typeToUse);

		if (entity == null) {
			throw new MappingException(String.format(INVALID_TYPE_TO_READ, source, typeToUse.getType()));
		}

		return readEntity(entity, source);
	}

	private Object propertyConverterRead(ElasticsearchPersistentProperty property, Object source) {
		ElasticsearchPersistentPropertyConverter propertyConverter = Objects
				.requireNonNull(property.getPropertyConverter());

		if (source instanceof String[]) {
			// convert to a List
			source = Arrays.asList((String[]) source);
		}

		if (source instanceof List) {
			source = ((List<?>) source).stream().map(it -> convertOnRead(propertyConverter, it)).collect(Collectors.toList());
		} else if (source instanceof Set) {
			source = ((Set<?>) source).stream().map(it -> convertOnRead(propertyConverter, it)).collect(Collectors.toSet());
		} else {
			source = convertOnRead(propertyConverter, source);
		}
		return source;
	}

	private Object convertOnRead(ElasticsearchPersistentPropertyConverter propertyConverter, Object source) {
		if (String.class.isAssignableFrom(source.getClass())) {
			source = propertyConverter.read((String) source);
		}
		return source;
	}

	/**
	 * Reads the given {@link Collection} into a collection of the given {@link TypeInformation}.
	 *
	 * @param targetType must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 * @return the converted {@link Collection} or array, will never be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private Object readCollectionOrArray(TypeInformation<?> targetType, Collection<?> source) {

		Assert.notNull(targetType, "Target type must not be null!");

		Class<?> collectionType = targetType.isSubTypeOf(Collection.class) //
				? targetType.getType() //
				: List.class;

		TypeInformation<?> componentType = targetType.getComponentType() != null //
				? targetType.getComponentType() //
				: ClassTypeInformation.OBJECT;
		Class<?> rawComponentType = componentType.getType();

		Collection<Object> items = targetType.getType().isArray() //
				? new ArrayList<>(source.size()) //
				: CollectionFactory.createCollection(collectionType, rawComponentType, source.size());

		if (source.isEmpty()) {
			return getPotentiallyConvertedSimpleRead(items, targetType);
		}

		for (Object element : source) {

			if (element instanceof Map) {
				items.add(read(componentType, (Map<String, Object>) element));
			} else {

				if (!Object.class.equals(rawComponentType) && element instanceof Collection) {
					if (!rawComponentType.isArray() && !ClassUtils.isAssignable(Iterable.class, rawComponentType)) {
						throw new MappingException(
								String.format(INCOMPATIBLE_TYPES, element, element.getClass(), rawComponentType));
					}
				}
				if (element instanceof List) {
					items.add(readCollectionOrArray(componentType, (Collection<Object>) element));
				} else {
					items.add(getPotentiallyConvertedSimpleRead(element, rawComponentType));
				}
			}
		}

		return getPotentiallyConvertedSimpleRead(items, targetType.getType());
	}

	@SuppressWarnings("unchecked")
	private <R> R readMap(TypeInformation<?> type, Map<String, Object> source) {

		Assert.notNull(source, "Document must not be null!");

		Class<?> mapType = typeMapper.readType(source, type).getType();

		TypeInformation<?> keyType = type.getComponentType();
		TypeInformation<?> valueType = type.getMapValueType();

		Class<?> rawKeyType = keyType != null ? keyType.getType() : null;
		Class<?> rawValueType = valueType != null ? valueType.getType() : null;

		Map<Object, Object> map = CollectionFactory.createMap(mapType, rawKeyType, source.keySet().size());

		for (Entry<String, Object> entry : source.entrySet()) {

			if (typeMapper.isTypeKey(entry.getKey())) {
				continue;
			}

			Object key = entry.getKey();

			if (rawKeyType != null && !rawKeyType.isAssignableFrom(key.getClass())) {
				key = conversionService.convert(key, rawKeyType);
			}

			Object value = entry.getValue();
			TypeInformation<?> defaultedValueType = valueType != null ? valueType : ClassTypeInformation.OBJECT;

			if (value instanceof Map) {
				map.put(key, read(defaultedValueType, (Map<String, Object>) value));
			} else if (value instanceof List) {
				map.put(key,
						readCollectionOrArray(valueType != null ? valueType : ClassTypeInformation.LIST, (List<Object>) value));
			} else {
				map.put(key, getPotentiallyConvertedSimpleRead(value, rawValueType));
			}
		}

		return (R) map;
	}

	@Nullable
	private Object getPotentiallyConvertedSimpleRead(@Nullable Object value, TypeInformation<?> targetType) {
		return getPotentiallyConvertedSimpleRead(value, targetType.getType());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Nullable
	private Object getPotentiallyConvertedSimpleRead(@Nullable Object value, @Nullable Class<?> target) {

		if (target == null || value == null || ClassUtils.isAssignableValue(target, value)) {
			return value;
		}

		if (getConversions().hasCustomReadTarget(value.getClass(), target)) {
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
							throw new MappingException("failed to set scripted field: " + name + " with value: " + value, e);
						} catch (IllegalAccessException e) {
							throw new MappingException("failed to access scripted field: " + name, e);
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
		TypeInformation<? extends Object> type = ClassTypeInformation.from(entityType);

		if (requiresTypeHint(entityType)) {
			typeMapper.writeType(type, sink);
		}

		writeInternal(source, sink, type);
	}

	/**
	 * Internal write conversion method which should be used for nested invocations.
	 *
	 * @param source
	 * @param sink
	 * @param typeHint
	 */
	@SuppressWarnings("unchecked")
	protected void writeInternal(@Nullable Object source, Map<String, Object> sink,
			@Nullable TypeInformation<?> typeHint) {

		if (null == source) {
			return;
		}

		Class<?> entityType = source.getClass();
		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(entityType, Map.class);

		if (customTarget.isPresent()) {
			Map<String, Object> result = conversionService.convert(source, Map.class);
			sink.putAll(result);
			return;
		}

		if (Map.class.isAssignableFrom(entityType)) {
			writeMapInternal((Map<Object, Object>) source, sink, ClassTypeInformation.MAP);
			return;
		}

		if (Collection.class.isAssignableFrom(entityType)) {
			writeCollectionInternal((Collection<?>) source, ClassTypeInformation.LIST, (Collection<?>) sink);
			return;
		}

		ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(entityType);
		addCustomTypeKeyIfNecessary(typeHint, source, sink);
		writeInternal(source, sink, entity);
	}

	/**
	 * Internal write conversion method which should be used for nested invocations.
	 *
	 * @param source
	 * @param sink
	 * @param typeHint
	 */
	protected void writeInternal(@Nullable Object source, Map<String, Object> sink,
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		if (source == null) {
			return;
		}

		if (null == entity) {
			throw new MappingException("No mapping metadata found for entity of type " + source.getClass().getName());
		}

		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);
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

				if (property.storeNullValue()) {
					sink.set(property, null);
				}

				continue;
			}

			if (property.hasPropertyConverter()) {
				value = propertyConverterWrite(property, value);
				sink.set(property, value);
			} else if (TemporalAccessor.class.isAssignableFrom(property.getActualType())
					&& !getConversions().hasCustomWriteTarget(value.getClass())) {

				// log at most 5 times
				String propertyName = entity.getType().getSimpleName() + '.' + property.getName();
				String key = propertyName + "-write";
				int count = propertyWarnings.computeIfAbsent(key, k -> 0);
				if (count < 5) {
					LOGGER.warn(
							"Type {} of property {} is a TemporalAccessor class but has neither a @Field annotation defining the date type nor a registered converter for writing!"
									+ " It will be mapped to a complex object in Elasticsearch!",
							property.getType().getSimpleName(), propertyName);
					propertyWarnings.put(key, count + 1);
				}
			} else if (!isSimpleType(value)) {
				writeProperty(property, value, sink);
			} else {
				Object writeSimpleValue = getPotentiallyConvertedSimpleWrite(value, Object.class);
				if (writeSimpleValue != null) {
					sink.set(property, writeSimpleValue);
				}
			}
		}
	}

	private Object propertyConverterWrite(ElasticsearchPersistentProperty property, Object value) {
		ElasticsearchPersistentPropertyConverter propertyConverter = Objects
				.requireNonNull(property.getPropertyConverter());

		if (value instanceof List) {
			value = ((List<?>) value).stream().map(propertyConverter::write).collect(Collectors.toList());
		} else if (value instanceof Set) {
			value = ((Set<?>) value).stream().map(propertyConverter::write).collect(Collectors.toSet());
		} else {
			value = propertyConverter.write(value);
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	protected void writeProperty(ElasticsearchPersistentProperty property, Object value, MapValueAccessor sink) {

		Optional<Class<?>> customWriteTarget = getConversions().getCustomWriteTarget(value.getClass());

		if (customWriteTarget.isPresent()) {
			Class<?> writeTarget = customWriteTarget.get();
			sink.set(property, conversionService.convert(value, writeTarget));
			return;
		}

		TypeInformation<?> valueType = ClassTypeInformation.from(value.getClass());
		TypeInformation<?> type = property.getTypeInformation();

		if (valueType.isCollectionLike()) {
			List<Object> collectionInternal = createCollection(asCollection(value), property);
			sink.set(property, collectionInternal);
			return;
		}

		if (valueType.isMap()) {
			Map<String, Object> mapDbObj = createMap((Map<?, ?>) value, property);
			sink.set(property, mapDbObj);
			return;
		}

		// Lookup potential custom target type
		Optional<Class<?>> basicTargetType = conversions.getCustomWriteTarget(value.getClass());

		if (basicTargetType.isPresent()) {

			sink.set(property, conversionService.convert(value, basicTargetType.get()));
			return;
		}

		ElasticsearchPersistentEntity<?> entity = valueType.isSubTypeOf(property.getType())
				? mappingContext.getRequiredPersistentEntity(value.getClass())
				: mappingContext.getRequiredPersistentEntity(type);

		Object existingValue = sink.get(property);
		Map<String, Object> document = existingValue instanceof Map ? (Map<String, Object>) existingValue
				: Document.create();

		addCustomTypeKeyIfNecessary(ClassTypeInformation.from(property.getRawType()), value, document);
		writeInternal(value, document, entity);
		sink.set(property, document);
	}

	/**
	 * Writes the given {@link Collection} using the given {@link ElasticsearchPersistentProperty} information.
	 *
	 * @param collection must not be {@literal null}.
	 * @param property must not be {@literal null}.
	 * @return
	 */
	protected List<Object> createCollection(Collection<?> collection, ElasticsearchPersistentProperty property) {
		return writeCollectionInternal(collection, property.getTypeInformation(), new ArrayList<>(collection.size()));
	}

	/**
	 * Writes the given {@link Map} using the given {@link ElasticsearchPersistentProperty} information.
	 *
	 * @param map must not {@literal null}.
	 * @param property must not be {@literal null}.
	 * @return
	 */
	protected Map<String, Object> createMap(Map<?, ?> map, ElasticsearchPersistentProperty property) {

		Assert.notNull(map, "Given map must not be null!");
		Assert.notNull(property, "PersistentProperty must not be null!");

		return writeMapInternal(map, new LinkedHashMap<>(map.size()), property.getTypeInformation());
	}

	/**
	 * Writes the given {@link Map} to the given {@link Document} considering the given {@link TypeInformation}.
	 *
	 * @param source must not be {@literal null}.
	 * @param sink must not be {@literal null}.
	 * @param propertyType must not be {@literal null}.
	 * @return
	 */
	protected Map<String, Object> writeMapInternal(Map<?, ?> source, Map<String, Object> sink,
			TypeInformation<?> propertyType) {

		for (Map.Entry<?, ?> entry : source.entrySet()) {

			Object key = entry.getKey();
			Object value = entry.getValue();

			if (isSimpleType(key.getClass())) {

				String simpleKey = potentiallyConvertMapKey(key);
				if (value == null || isSimpleType(value)) {
					sink.put(simpleKey, getPotentiallyConvertedSimpleWrite(value, Object.class));
				} else if (value instanceof Collection || value.getClass().isArray()) {
					sink.put(simpleKey,
							writeCollectionInternal(asCollection(value), propertyType.getMapValueType(), new ArrayList<>()));
				} else {
					Map<String, Object> document = Document.create();
					TypeInformation<?> valueTypeInfo = propertyType.isMap() ? propertyType.getMapValueType()
							: ClassTypeInformation.OBJECT;
					writeInternal(value, document, valueTypeInfo);

					sink.put(simpleKey, document);
				}
			} else {
				throw new MappingException("Cannot use a complex object as a key value.");
			}
		}

		return sink;
	}

	/**
	 * Populates the given {@link Collection sink} with converted values from the given {@link Collection source}.
	 *
	 * @param source the collection to create a {@link Collection} for, must not be {@literal null}.
	 * @param type the {@link TypeInformation} to consider or {@literal null} if unknown.
	 * @param sink the {@link Collection} to write to.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Object> writeCollectionInternal(Collection<?> source, @Nullable TypeInformation<?> type,
			Collection<?> sink) {

		TypeInformation<?> componentType = null;

		List<Object> collection = sink instanceof List ? (List<Object>) sink : new ArrayList<>(sink);

		if (type != null) {
			componentType = type.getComponentType();
		}

		for (Object element : source) {

			Class<?> elementType = element == null ? null : element.getClass();

			if (elementType == null || conversions.isSimpleType(elementType)) {
				collection.add(getPotentiallyConvertedSimpleWrite(element,
						componentType != null ? componentType.getType() : Object.class));
			} else if (element instanceof Collection || elementType.isArray()) {
				collection.add(writeCollectionInternal(asCollection(element), componentType, new ArrayList<>()));
			} else {
				Map<String, Object> document = Document.create();
				writeInternal(element, document, componentType);
				collection.add(document);
			}
		}

		return collection;
	}

	/**
	 * Returns a {@link String} representation of the given {@link Map} key
	 *
	 * @param key
	 * @return
	 */
	private String potentiallyConvertMapKey(Object key) {

		if (key instanceof String) {
			return (String) key;
		}

		return conversions.hasCustomWriteTarget(key.getClass(), String.class)
				? (String) getPotentiallyConvertedSimpleWrite(key, Object.class)
				: key.toString();
	}

	/**
	 * Checks whether we have a custom conversion registered for the given value into an arbitrary simple Elasticsearch
	 * type. Returns the converted value if so. If not, we perform special enum handling or simply return the value as is.
	 *
	 * @param value
	 * @return
	 */
	@Nullable
	private Object getPotentiallyConvertedSimpleWrite(@Nullable Object value, @Nullable Class<?> typeHint) {

		if (value == null) {
			return null;
		}

		if (typeHint != null && Object.class != typeHint) {

			if (conversionService.canConvert(value.getClass(), typeHint)) {
				value = conversionService.convert(value, typeHint);
			}
		}

		Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(value.getClass());

		if (customTarget.isPresent()) {
			return conversionService.convert(value, customTarget.get());
		}

		if (ObjectUtils.isArray(value)) {

			if (value instanceof byte[]) {
				return value;
			}
			return asCollection(value);
		}

		return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
	}

	/**
	 * @deprecated since 4.2, use {@link #getPotentiallyConvertedSimpleWrite(Object, Class)} instead.
	 */
	@Nullable
	@Deprecated
	protected Object getWriteSimpleValue(Object value) {
		Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(value.getClass());

		if (customTarget.isPresent()) {
			return conversionService.convert(value, customTarget.get());
		}

		return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
	}

	/**
	 * @deprecated since 4.2, use {@link #writeInternal(Object, Map, TypeInformation)} instead.
	 */
	@Deprecated
	protected Object getWriteComplexValue(ElasticsearchPersistentProperty property, TypeInformation<?> typeHint,
			Object value) {

		Document document = Document.create();
		writeInternal(value, document, property.getTypeInformation());

		return document;
	}

	// endregion

	// region helper methods

	/**
	 * Adds custom type information to the given {@link Map} if necessary. That is if the value is not the same as the one
	 * given. This is usually the case if you store a subtype of the actual declared type of the property.
	 *
	 * @param type
	 * @param value must not be {@literal null}.
	 * @param sink must not be {@literal null}.
	 */
	protected void addCustomTypeKeyIfNecessary(@Nullable TypeInformation<?> type, Object value,
			Map<String, Object> sink) {

		Class<?> reference = type != null ? type.getActualType().getType() : Object.class;
		Class<?> valueType = ClassUtils.getUserClass(value.getClass());

		boolean notTheSameClass = !valueType.equals(reference);
		if (notTheSameClass) {
			typeMapper.writeType(valueType, sink);
		}
	}

	/**
	 * Check if a given type requires a type hint (aka {@literal _class} attribute) when writing to the document.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if not a simple type, {@link Collection} or type with custom write target.
	 */
	private boolean requiresTypeHint(Class<?> type) {

		return !isSimpleType(type) && !ClassUtils.isAssignable(Collection.class, type)
				&& !conversions.hasCustomWriteTarget(type, Document.class);
	}

	/**
	 * Compute the type to use by checking the given entity against the store type;
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
		return !Map.class.isAssignableFrom(type) && getConversions().isSimpleType(type);
	}

	/**
	 * Returns given object as {@link Collection}. Will return the {@link Collection} as is if the source is a
	 * {@link Collection} already, will convert an array into a {@link Collection} or simply create a single element
	 * collection for everything else.
	 *
	 * @param source
	 * @return
	 */
	private static Collection<?> asCollection(Object source) {

		if (source instanceof Collection) {
			return (Collection<?>) source;
		}

		return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
	}
	// endregion

	// region queries
	@Override
	public void updateCriteriaQuery(CriteriaQuery criteriaQuery, Class<?> domainClass) {

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainClass);

		if (persistentEntity != null) {
			for (Criteria chainedCriteria : criteriaQuery.getCriteria().getCriteriaChain()) {
				updateCriteria(chainedCriteria, persistentEntity);
			}
			for (Criteria subCriteria : criteriaQuery.getCriteria().getSubCriteria()) {
				for (Criteria chainedCriteria : subCriteria.getCriteriaChain()) {
					updateCriteria(chainedCriteria, persistentEntity);
				}
			}
		}
	}

	private void updateCriteria(Criteria criteria, ElasticsearchPersistentEntity<?> persistentEntity) {
		Field field = criteria.getField();

		if (field == null) {
			return;
		}

		String name = field.getName();
		ElasticsearchPersistentProperty property = persistentEntity.getPersistentProperty(name);

		if (property != null && property.getName().equals(name)) {
			field.setName(property.getFieldName());

			if (property.hasPropertyConverter()) {
				ElasticsearchPersistentPropertyConverter propertyConverter = Objects
						.requireNonNull(property.getPropertyConverter());
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

			org.springframework.data.elasticsearch.annotations.Field fieldAnnotation = property
					.findAnnotation(org.springframework.data.elasticsearch.annotations.Field.class);

			if (fieldAnnotation != null) {
				field.setFieldType(fieldAnnotation.type());
			}
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

			String fieldName = property.getFieldName();

			if (target instanceof Document) {
				// nested objects may have properties like 'id' which are recognized as isIdProperty() but they are not
				// Documents
				Document document = (Document) target;

				if (property.isIdProperty() && document.hasId()) {
					Object id = null;

					// take the id property from the document source if available
					if (!fieldName.contains(".")) {
						id = target.get(fieldName);
					}
					return id != null ? id : document.getId();
				}

				if (property.isVersionProperty() && document.hasVersion()) {
					return document.getVersion();
				}

			}

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

		public void set(ElasticsearchPersistentProperty property, @Nullable Object value) {

			if (value != null) {

				if (property.isIdProperty()) {
					((Document) target).setId(value.toString());
				}

				if (property.isVersionProperty()) {
					((Document) target).setVersion((Long) value);
				}
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

		final MapValueAccessor accessor;
		final SpELExpressionEvaluator evaluator;

		ElasticsearchPropertyValueProvider(MapValueAccessor accessor, SpELExpressionEvaluator evaluator) {
			this.accessor = accessor;
			this.evaluator = evaluator;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyValue(ElasticsearchPersistentProperty property) {

			String expression = property.getSpelExpression();
			Object value = expression != null ? evaluator.evaluate(expression) : accessor.get(property);

			if (value == null) {
				return null;
			}

			return readValue(value, property, property.getTypeInformation());
		}
	}

	/**
	 * Extension of {@link SpELExpressionParameterValueProvider} to recursively trigger value conversion on the raw
	 * resolved SpEL value.
	 *
	 * @author Mark Paluch
	 */
	private class ConverterAwareSpELExpressionParameterValueProvider
			extends SpELExpressionParameterValueProvider<ElasticsearchPersistentProperty> {

		/**
		 * Creates a new {@link ConverterAwareSpELExpressionParameterValueProvider}.
		 *
		 * @param evaluator must not be {@literal null}.
		 * @param conversionService must not be {@literal null}.
		 * @param delegate must not be {@literal null}.
		 */
		public ConverterAwareSpELExpressionParameterValueProvider(SpELExpressionEvaluator evaluator,
				ConversionService conversionService, ParameterValueProvider<ElasticsearchPersistentProperty> delegate) {

			super(evaluator, conversionService, delegate);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.SpELExpressionParameterValueProvider#potentiallyConvertSpelValue(java.lang.Object, org.springframework.data.mapping.PreferredConstructor.Parameter)
		 */
		@Override
		protected <T> T potentiallyConvertSpelValue(Object object,
				PreferredConstructor.Parameter<T, ElasticsearchPersistentProperty> parameter) {
			return readValue(object, parameter.getType());
		}
	}

	enum NoOpParameterValueProvider implements ParameterValueProvider<ElasticsearchPersistentProperty> {

		INSTANCE;

		@Override
		public <T> T getParameterValue(PreferredConstructor.Parameter<T, ElasticsearchPersistentProperty> parameter) {
			return null;
		}
	}

}
