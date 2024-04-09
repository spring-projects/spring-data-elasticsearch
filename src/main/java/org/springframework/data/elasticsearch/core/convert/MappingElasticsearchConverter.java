/*
 * Copyright 2013-2024 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.Field;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.*;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.standard.SpelExpressionParser;
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
 * @author Anton Naydenov
 * @author vdisk
 * @author Junghoon Ban
 * @since 3.2
 */
public class MappingElasticsearchConverter
		implements ElasticsearchConverter, ApplicationContextAware, InitializingBean, EnvironmentCapable {

	private static final String INCOMPATIBLE_TYPES = "Cannot convert %1$s of type %2$s into an instance of %3$s! Implement a custom Converter<%2$s, %3$s> and register it with the CustomConversions.";
	private static final String INVALID_TYPE_TO_READ = "Expected to read Document %s into type %s but didn't find a PersistentEntity for the latter!";

	private static final Log LOGGER = LogFactory.getLog(MappingElasticsearchConverter.class);

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;
	private CustomConversions conversions = new ElasticsearchCustomConversions(Collections.emptyList());

	protected @Nullable Environment environment;

	private final SpELContext spELContext = new SpELContext(new MapAccessor());
	private final SpelExpressionParser expressionParser = new SpelExpressionParser();
	private final CachingValueExpressionEvaluatorFactory expressionEvaluatorFactory = new CachingValueExpressionEvaluatorFactory(
			expressionParser, this, spELContext);

	private final EntityInstantiators instantiators = new EntityInstantiators();
	private final ElasticsearchTypeMapper typeMapper;

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

		if (mappingContext instanceof ApplicationContextAware contextAware) {
			contextAware.setApplicationContext(applicationContext);
		}
	}

	@Override
	public Environment getEnvironment() {
		if (environment == null) {
			environment = new StandardEnvironment();
		}
		return environment;
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

	@Override
	public void afterPropertiesSet() {
		DateFormatterRegistrar.addDateConverters(conversionService);
		conversions.registerConvertersIn(conversionService);
	}

	public ElasticsearchTypeMapper getTypeMapper() {
		return typeMapper;
	}

	// region read/write

	@Override
	public <R> R read(Class<R> type, Document source) {

		Reader reader = new Reader(mappingContext, conversionService, conversions, typeMapper, expressionEvaluatorFactory,
				instantiators);
		return reader.read(type, source);
	}

	@Override
	public void write(Object source, Document sink) {

		Assert.notNull(source, "source to map must not be null");

		Writer writer = new Writer(mappingContext, conversionService, conversions, typeMapper);
		writer.write(source, sink);
	}

	/**
	 * base class for {@link Reader} and {@link Writer} keeping the common properties
	 */
	private static class Base {

		protected final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
		protected final ElasticsearchTypeMapper typeMapper;
		protected final GenericConversionService conversionService;
		protected final CustomConversions conversions;
		protected final ConcurrentHashMap<String, Integer> propertyWarnings = new ConcurrentHashMap<>();

		private Base(
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
				GenericConversionService conversionService, CustomConversions conversions, ElasticsearchTypeMapper typeMapper) {
			this.mappingContext = mappingContext;
			this.conversionService = conversionService;
			this.conversions = conversions;
			this.typeMapper = typeMapper;
		}
	}

	/**
	 * Class to do the actual reading. The methods originally were in the MappingElasticsearchConverter class, but are
	 * refactored to allow for keeping state during the conversion of an object.
	 */
	private static class Reader extends Base {

		private final EntityInstantiators instantiators;
		private final CachingValueExpressionEvaluatorFactory expressionEvaluatorFactory;

		public Reader(
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
				GenericConversionService conversionService, CustomConversions conversions, ElasticsearchTypeMapper typeMapper,
				CachingValueExpressionEvaluatorFactory expressionEvaluatorFactory, EntityInstantiators instantiators) {

			super(mappingContext, conversionService, conversions, typeMapper);
			this.expressionEvaluatorFactory = expressionEvaluatorFactory;
			this.instantiators = instantiators;
		}

		/**
		 * Reads the given source into the given type.
		 *
		 * @param type the type to convert the given source to.
		 * @param source the source to create an object of the given type from.
		 * @return the object that was read
		 */
		<R> R read(Class<R> type, Document source) {

			// noinspection unchecked
			TypeInformation<R> typeInformation = TypeInformation.of((Class<R>) ClassUtils.getUserClass(type));
			R r = read(typeInformation, source);

			if (r == null) {
				throw new ConversionException("could not convert into object of class " + type);
			}

			return r;
		}

		@Nullable
		@SuppressWarnings("unchecked")
		private <R> R read(TypeInformation<R> typeInformation, Map<String, Object> source) {

			Assert.notNull(source, "Source must not be null!");

			TypeInformation<? extends R> typeToUse = typeMapper.readType(source, typeInformation);
			Class<? extends R> rawType = typeToUse.getType();

			if (conversions.hasCustomReadTarget(source.getClass(), rawType)) {
				return conversionService.convert(source, rawType);
			}

			if (Document.class.isAssignableFrom(rawType)) {
				return (R) source;
			}

			if (typeToUse.isMap()) {
				return readMap(typeToUse, source);
			}

			if (typeToUse.equals(TypeInformation.OBJECT)) {
				return (R) source;
			}

			// Retrieve persistent entity info
			ElasticsearchPersistentEntity<?> entity = mappingContext.getPersistentEntity(typeToUse);

			if (entity == null) {
				throw new MappingException(String.format(INVALID_TYPE_TO_READ, source, typeToUse.getType()));
			}

			return readEntity(entity, source);
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
				TypeInformation<?> defaultedValueType = valueType != null ? valueType : TypeInformation.OBJECT;

				if (value instanceof Map) {
					map.put(key, read(defaultedValueType, (Map<String, Object>) value));
				} else if (value instanceof List) {
					map.put(key,
							readCollectionOrArray(valueType != null ? valueType : TypeInformation.LIST, (List<Object>) value));
				} else {
					map.put(key, getPotentiallyConvertedSimpleRead(value, rawValueType));
				}
			}

			return (R) map;
		}

		private <R> R readEntity(ElasticsearchPersistentEntity<?> entity, Map<String, Object> source) {

			ElasticsearchPersistentEntity<?> targetEntity = computeClosestEntity(entity, source);
			ValueExpressionEvaluator evaluator = expressionEvaluatorFactory.create(source);
			MapValueAccessor accessor = new MapValueAccessor(source);

			InstanceCreatorMetadata<?> creatorMetadata = entity.getInstanceCreatorMetadata();

			ParameterValueProvider<ElasticsearchPersistentProperty> propertyValueProvider = creatorMetadata != null
					&& creatorMetadata.hasParameters() ? getParameterProvider(entity, accessor, evaluator)
							: NoOpParameterValueProvider.INSTANCE;

			EntityInstantiator instantiator = instantiators.getInstantiatorFor(targetEntity);

			@SuppressWarnings({ "unchecked" })
			R instance = (R) instantiator.createInstance(targetEntity, propertyValueProvider);

			if (!targetEntity.requiresPropertyPopulation()) {
				return instance;
			}

			Document document = (source instanceof Document) ? (Document) source : null;

			ElasticsearchPropertyValueProvider valueProvider = new ElasticsearchPropertyValueProvider(accessor, evaluator);
			try {
				R result = readProperties(targetEntity, instance, valueProvider);

				if (document != null) {
					if (document.hasId()) {
						ElasticsearchPersistentProperty idProperty = targetEntity.getIdProperty();
						PersistentPropertyAccessor<R> propertyAccessor = new ConvertingPropertyAccessor<>(
								targetEntity.getPropertyAccessor(result), conversionService);
						// Only deal with String because ES generated Ids are strings !
						if (idProperty != null && idProperty.isReadable() && idProperty.getType().isAssignableFrom(String.class)) {
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

				if (source instanceof SearchDocument searchDocument) {
					populateScriptFields(targetEntity, result, searchDocument);
				}
				return result;
			} catch (ConversionException e) {
				String documentId = (document != null && document.hasId()) ? document.getId() : null;
				throw new MappingConversionException(documentId, e);
			}
		}

		private ParameterValueProvider<ElasticsearchPersistentProperty> getParameterProvider(
				ElasticsearchPersistentEntity<?> entity, MapValueAccessor source, ValueExpressionEvaluator evaluator) {

			ElasticsearchPropertyValueProvider provider = new ElasticsearchPropertyValueProvider(source, evaluator);

			// TODO: Support for non-static inner classes via ObjectPath
			// noinspection ConstantConditions
			PersistentEntityParameterValueProvider<ElasticsearchPersistentProperty> parameterProvider = new PersistentEntityParameterValueProvider<>(
					entity, provider, null);

			return new ConverterAwareValueExpressionParameterValueProvider(evaluator, conversionService, parameterProvider);
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

			for (ElasticsearchPersistentProperty property : entity) {

				if (entity.isCreatorArgument(property) || !property.isReadable() || property.isSeqNoPrimaryTermProperty()
						|| property.isIndexedIndexNameProperty()) {
					continue;
				}

				Object value = valueProvider.getPropertyValue(property);
				if (value != null) {
					accessor.setProperty(property, value);
				}
			}

			return accessor.getBean();
		}

		@Nullable
		protected <R> R readValue(@Nullable Object value, ElasticsearchPersistentProperty property,
				TypeInformation<?> type) {

			if (value == null) {
				return null;
			}

			Class<?> rawType = type.getType();

			if (property.hasPropertyValueConverter()) {
				// noinspection unchecked
				return (R) propertyConverterRead(property, value);
			} else if (TemporalAccessor.class.isAssignableFrom(property.getType())
					&& !conversions.hasCustomReadTarget(value.getClass(), rawType)) {

				// log at most 5 times
				String propertyName = property.getOwner().getType().getSimpleName() + '.' + property.getName();
				String key = propertyName + "-read";
				int count = propertyWarnings.computeIfAbsent(key, k -> 0);
				if (count < 5) {
					LOGGER.warn(String.format(
							"Type %s of property %s is a TemporalAccessor class but has neither a @Field annotation defining the date type nor a registered converter for reading!"
									+ " It cannot be mapped from a complex object in Elasticsearch!",
							property.getType().getSimpleName(), propertyName));
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

				TypeInformation<?> collectionComponentType = getCollectionComponentType(type);
				if (collectionComponentType != null) {
					Object o = read(collectionComponentType, (Map<String, Object>) value);
					return (o != null) ? getCollectionWithSingleElement(type, collectionComponentType, o) : null;
				}
				return (T) read(type, (Map<String, Object>) value);
			} else {

				TypeInformation<?> collectionComponentType = getCollectionComponentType(type);
				if (collectionComponentType != null
						&& collectionComponentType.isAssignableFrom(TypeInformation.of(value.getClass()))) {
					Object o = getPotentiallyConvertedSimpleRead(value, collectionComponentType);
					return (o != null) ? getCollectionWithSingleElement(type, collectionComponentType, o) : null;
				}

				return (T) getPotentiallyConvertedSimpleRead(value, rawType);
			}
		}

		@SuppressWarnings("unchecked")
		private static <T> T getCollectionWithSingleElement(TypeInformation<?> collectionType,
				TypeInformation<?> componentType, Object element) {
			Collection<Object> collection = CollectionFactory.createCollection(collectionType.getType(),
					componentType.getType(), 1);
			collection.add(element);
			return (T) collection;
		}

		/**
		 * @param type the type to check
		 * @return the collection type if type is a collection, null otherwise,
		 */
		@Nullable
		TypeInformation<?> getCollectionComponentType(TypeInformation<?> type) {
			return type.isCollectionLike() ? type.getComponentType() : null;
		}

		private Object propertyConverterRead(ElasticsearchPersistentProperty property, Object source) {
			PropertyValueConverter propertyValueConverter = Objects.requireNonNull(property.getPropertyValueConverter());

			if (source instanceof String[] strings) {
				// convert to a List
				source = Arrays.asList(strings);
			}

			if (source instanceof List<?> list) {
				source = list.stream().map(it -> convertOnRead(propertyValueConverter, it)).collect(Collectors.toList());
			} else if (source instanceof Set<?> set) {
				source = set.stream().map(it -> convertOnRead(propertyValueConverter, it)).collect(Collectors.toSet());
			} else {
				source = convertOnRead(propertyValueConverter, source);
			}
			return source;
		}

		private Object convertOnRead(PropertyValueConverter propertyValueConverter, Object source) {
			return propertyValueConverter.read(source);
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
					: TypeInformation.OBJECT;
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

			if (conversions.hasCustomReadTarget(value.getClass(), target)) {
				return conversionService.convert(value, target);
			}

			if (Enum.class.isAssignableFrom(target)) {
				return Enum.valueOf((Class<Enum>) target, value.toString());
			}

			try {
				return conversionService.convert(value, target);
			} catch (ConverterNotFoundException e) {
				return convertFromCollectionToObject(value, target);
			}
		}

		/**
		 * we need the conversion from a collection to the first element for example in the case when reading the
		 * constructor parameter of an entity from a scripted return. Originally this was handle in the conversionService,
		 * but will be removed from spring-data-commons, so we do it here
		 */
		@Nullable
		private Object convertFromCollectionToObject(Object value, Class<?> target) {

			if (value.getClass().isArray()) {
				// noinspection ArraysAsListWithZeroOrOneArgument
				value = Arrays.asList(value);
			}

			if (value instanceof Collection<?> collection && !collection.isEmpty()) {
				value = collection.iterator().next();
			}

			return conversionService.convert(value, target);
		}

		private <T> void populateScriptFields(ElasticsearchPersistentEntity<?> entity, T result,
				SearchDocument searchDocument) {
			Map<String, List<Object>> fields = searchDocument.getFields();
			entity.doWithProperties((SimplePropertyHandler) property -> {
				if (property.isAnnotationPresent(ScriptedField.class) && fields.containsKey(property.getName())) {
					ScriptedField scriptedField = property.findAnnotation(ScriptedField.class);
					// noinspection ConstantConditions
					String name = scriptedField.name().isEmpty() ? property.getName() : scriptedField.name();
					Object value = searchDocument.getFieldValue(name);

					entity.getPropertyAccessor(result).setProperty(property, value);
				}
			});
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

		class ElasticsearchPropertyValueProvider implements PropertyValueProvider<ElasticsearchPersistentProperty> {

			final MapValueAccessor accessor;
			final ValueExpressionEvaluator evaluator;

			ElasticsearchPropertyValueProvider(MapValueAccessor accessor, ValueExpressionEvaluator evaluator) {
				this.accessor = accessor;
				this.evaluator = evaluator;
			}

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
		 * Extension of {@link ValueExpressionParameterValueProvider} to recursively trigger value conversion on the raw
		 * resolved SpEL value.
		 *
		 * @author Mark Paluch
		 */
		private class ConverterAwareValueExpressionParameterValueProvider
				extends ValueExpressionParameterValueProvider<ElasticsearchPersistentProperty> {

			/**
			 * Creates a new {@link ConverterAwareValueExpressionParameterValueProvider}.
			 *
			 * @param evaluator must not be {@literal null}.
			 * @param conversionService must not be {@literal null}.
			 * @param delegate must not be {@literal null}.
			 */
			public ConverterAwareValueExpressionParameterValueProvider(ValueExpressionEvaluator evaluator,
					ConversionService conversionService, ParameterValueProvider<ElasticsearchPersistentProperty> delegate) {

				super(evaluator, conversionService, delegate);
			}

			@Override
			protected <T> T potentiallyConvertExpressionValue(Object object,
					Parameter<T, ElasticsearchPersistentProperty> parameter) {
				return readValue(object, parameter.getType());
			}
		}

		enum NoOpParameterValueProvider implements ParameterValueProvider<ElasticsearchPersistentProperty> {

			INSTANCE;

			@Override
			public <T> T getParameterValue(Parameter<T, ElasticsearchPersistentProperty> parameter) {
				return null;
			}
		}
	}

	/**
	 * Class to do the actual writing. The methods originally were in the MappingElasticsearchConverter class, but are
	 * refactored to allow for keeping state during the conversion of an object.
	 */
	static private class Writer extends Base {

		private boolean writeTypeHints = true;

		public Writer(
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
				GenericConversionService conversionService, CustomConversions conversions, ElasticsearchTypeMapper typeMapper) {
			super(mappingContext, conversionService, conversions, typeMapper);
		}

		void write(Object source, Document sink) {

			if (source instanceof Map) {
				// noinspection unchecked
				sink.putAll((Map<String, Object>) source);
				return;
			}

			Class<?> entityType = ClassUtils.getUserClass(source.getClass());
			ElasticsearchPersistentEntity<?> entity = mappingContext.getPersistentEntity(entityType);

			if (entity != null) {
				writeTypeHints = entity.writeTypeHints();
			}

			TypeInformation<?> typeInformation = TypeInformation.of(entityType);

			if (writeTypeHints && requiresTypeHint(entityType)) {
				typeMapper.writeType(typeInformation, sink);
			}

			writeInternal(source, sink, typeInformation);
		}

		/**
		 * Internal write conversion method which should be used for nested invocations.
		 *
		 * @param source the object to write
		 * @param sink the destination
		 * @param typeInformation type information for the source
		 */
		@SuppressWarnings("unchecked")
		private void writeInternal(@Nullable Object source, Map<String, Object> sink,
				@Nullable TypeInformation<?> typeInformation) {

			if (null == source) {
				return;
			}

			Class<?> entityType = source.getClass();
			Optional<Class<?>> customTarget = conversions.getCustomWriteTarget(entityType, Map.class);

			if (customTarget.isPresent()) {
				Map<String, Object> result = conversionService.convert(source, Map.class);

				if (result != null) {
					sink.putAll(result);
				}
				return;
			}

			if (Map.class.isAssignableFrom(entityType)) {
				writeMapInternal((Map<Object, Object>) source, sink, TypeInformation.MAP);
				return;
			}

			if (Collection.class.isAssignableFrom(entityType)) {
				writeCollectionInternal((Collection<?>) source, TypeInformation.LIST, (Collection<?>) sink);
				return;
			}

			ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(entityType);
			addCustomTypeKeyIfNecessary(source, sink, typeInformation);
			writeInternal(source, sink, entity);
		}

		/**
		 * Internal write conversion method which should be used for nested invocations.
		 *
		 * @param source the object to write
		 * @param sink the destination
		 * @param entity entity for the source
		 */
		private void writeInternal(@Nullable Object source, Map<String, Object> sink,
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

		private boolean isSimpleType(Object value) {
			return isSimpleType(value.getClass());
		}

		private boolean isSimpleType(Class<?> type) {
			return !Map.class.isAssignableFrom(type) && conversions.isSimpleType(type);
		}

		/**
		 * Writes the given {@link Map} to the given {@link Document} considering the given {@link TypeInformation}.
		 *
		 * @param source must not be {@literal null}.
		 * @param sink must not be {@literal null}.
		 * @param propertyType must not be {@literal null}.
		 */
		private Map<String, Object> writeMapInternal(Map<?, ?> source, Map<String, Object> sink,
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
								: TypeInformation.OBJECT;
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

				if (elementType == null || isSimpleType(elementType)) {
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

		private void writeProperties(ElasticsearchPersistentEntity<?> entity, PersistentPropertyAccessor<?> accessor,
				MapValueAccessor sink) {

			for (ElasticsearchPersistentProperty property : entity) {

				if (!property.isWritable() //
						|| property.isIndexedIndexNameProperty() //
						|| (property.isIdProperty() && !entity.storeIdInSource()) //
						|| (property.isVersionProperty() && !entity.storeVersionInSource())) {
					continue;
				}

				if (property.isIdProperty() && !entity.storeIdInSource()) {
					continue;
				}

				Object value = accessor.getProperty(property);

				if (value == null) {

					if (property.storeNullValue()) {
						sink.set(property, null);
					}

					continue;
				}

				if (!property.storeEmptyValue() && hasEmptyValue(value)) {
					continue;
				}

				if (property.hasPropertyValueConverter()) {
					value = propertyConverterWrite(property, value);
					sink.set(property, value);
				} else if (TemporalAccessor.class.isAssignableFrom(property.getActualType())
						&& !conversions.hasCustomWriteTarget(value.getClass())) {

					// log at most 5 times
					String propertyName = entity.getType().getSimpleName() + '.' + property.getName();
					String key = propertyName + "-write";
					int count = propertyWarnings.computeIfAbsent(key, k -> 0);
					if (count < 5) {
						LOGGER.warn(String.format(
								"Type %s of property %s is a TemporalAccessor class but has neither a @Field annotation defining the date type nor a registered converter for writing!"
										+ " It will be mapped to a complex object in Elasticsearch!",
								property.getType().getSimpleName(), propertyName));
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

		private static boolean hasEmptyValue(Object value) {

			return value instanceof String s && s.isEmpty() || value instanceof Collection<?> c && c.isEmpty()
					|| value instanceof Map<?, ?> m && m.isEmpty();
		}

		@SuppressWarnings("unchecked")
		protected void writeProperty(ElasticsearchPersistentProperty property, Object value, MapValueAccessor sink) {

			Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(value.getClass());

			if (customWriteTarget.isPresent()) {
				Class<?> writeTarget = customWriteTarget.get();
				sink.set(property, conversionService.convert(value, writeTarget));
				return;
			}

			TypeInformation<?> valueType = TypeInformation.of(value.getClass());
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

			addCustomTypeKeyIfNecessary(value, document, TypeInformation.of(property.getRawType()));
			writeInternal(value, document, entity);
			sink.set(property, document);
		}

		/**
		 * Adds custom typeInformation information to the given {@link Map} if necessary. That is if the value is not the
		 * same as the one given. This is usually the case if you store a subtype of the actual declared typeInformation of
		 * the property.
		 *
		 * @param source must not be {@literal null}.
		 * @param sink must not be {@literal null}.
		 * @param type type to compare to
		 */
		private void addCustomTypeKeyIfNecessary(Object source, Map<String, Object> sink,
				@Nullable TypeInformation<?> type) {

			if (!writeTypeHints) {
				return;
			}

			Class<?> reference;

			if (type == null) {
				reference = Object.class;
			} else {
				TypeInformation<?> actualType = type.getActualType();
				reference = actualType == null ? Object.class : actualType.getType();
			}
			Class<?> valueType = ClassUtils.getUserClass(source.getClass());

			boolean notTheSameClass = !valueType.equals(reference);
			if (notTheSameClass) {
				typeMapper.writeType(valueType, sink);
			}
		}

		/**
		 * Returns a {@link String} representation of the given {@link Map} key
		 *
		 * @param key the key to convert
		 */
		private String potentiallyConvertMapKey(Object key) {

			if (key instanceof String) {
				return (String) key;
			}

			if (conversions.hasCustomWriteTarget(key.getClass(), String.class)) {
				Object potentiallyConvertedSimpleWrite = getPotentiallyConvertedSimpleWrite(key, Object.class);

				if (potentiallyConvertedSimpleWrite == null) {
					return key.toString();
				}
				return (String) potentiallyConvertedSimpleWrite;
			}
			return key.toString();
		}

		/**
		 * Checks whether we have a custom conversion registered for the given value into an arbitrary simple Elasticsearch
		 * type. Returns the converted value if so. If not, we perform special enum handling or simply return the value as
		 * is.
		 *
		 * @param value value to convert
		 */
		@Nullable
		private Object getPotentiallyConvertedSimpleWrite(@Nullable Object value, @Nullable Class<?> typeHint) {

			if (value == null) {
				return null;
			}

			if (typeHint != null && Object.class != typeHint) {

				if (conversionService.canConvert(value.getClass(), typeHint)) {
					value = conversionService.convert(value, typeHint);

					if (value == null) {
						return null;
					}
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

		private Object propertyConverterWrite(ElasticsearchPersistentProperty property, Object value) {
			PropertyValueConverter propertyValueConverter = Objects.requireNonNull(property.getPropertyValueConverter());

			if (value instanceof List) {
				value = ((List<?>) value).stream().map(propertyValueConverter::write).collect(Collectors.toList());
			} else if (value instanceof Set) {
				value = ((Set<?>) value).stream().map(propertyValueConverter::write).collect(Collectors.toSet());
			} else {
				value = propertyValueConverter.write(value);
			}
			return value;
		}

		/**
		 * Writes the given {@link Collection} using the given {@link ElasticsearchPersistentProperty} information.
		 *
		 * @param collection must not be {@literal null}.
		 * @param property must not be {@literal null}.
		 */
		protected List<Object> createCollection(Collection<?> collection, ElasticsearchPersistentProperty property) {
			return writeCollectionInternal(collection, property.getTypeInformation(), new ArrayList<>(collection.size()));
		}

		/**
		 * Writes the given {@link Map} using the given {@link ElasticsearchPersistentProperty} information.
		 *
		 * @param map must not {@literal null}.
		 * @param property must not be {@literal null}.
		 */
		protected Map<String, Object> createMap(Map<?, ?> map, ElasticsearchPersistentProperty property) {

			Assert.notNull(map, "Given map must not be null!");
			Assert.notNull(property, "PersistentProperty must not be null!");

			return writeMapInternal(map, new LinkedHashMap<>(map.size()), property.getTypeInformation());
		}

		/**
		 * Returns given object as {@link Collection}. Will return the {@link Collection} as is if the source is a
		 * {@link Collection} already, will convert an array into a {@link Collection} or simply create a single element
		 * collection for everything else.
		 *
		 * @param source object to convert
		 */
		private static Collection<?> asCollection(Object source) {

			if (source instanceof Collection<?> collection) {
				return collection;
			}

			return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
		}
	}
	// endregion

	// region queries
	@Override
	public void updateQuery(Query query, @Nullable Class<?> domainClass) {

		Assert.notNull(query, "query must not be null");

		if (query instanceof BaseQuery baseQuery) {

			if (baseQuery.queryIsUpdatedByConverter()) {
				return;
			}
		}

		if (domainClass == null) {
			return;
		}

		updatePropertiesInFieldsAndSourceFilter(query, domainClass);

		if (query instanceof CriteriaQuery criteriaQuery) {
			updatePropertiesInCriteriaQuery(criteriaQuery, domainClass);
		}

		if (query instanceof BaseQuery baseQuery) {
			baseQuery.setQueryIsUpdatedByConverter(true);
		}
	}

	private void updatePropertiesInFieldsAndSourceFilter(Query query, Class<?> domainClass) {

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainClass);

		if (persistentEntity != null) {
			List<String> fields = query.getFields();

			if (!fields.isEmpty()) {
				query.setFields(updateFieldNames(fields, persistentEntity));
			}

			List<String> storedFields = query.getStoredFields();
			if (!CollectionUtils.isEmpty(storedFields)) {
				query.setStoredFields(updateFieldNames(storedFields, persistentEntity));
			}

			SourceFilter sourceFilter = query.getSourceFilter();

			if (sourceFilter != null) {

				String[] includes = null;
				String[] excludes = null;

				if (sourceFilter.getIncludes() != null) {
					includes = updateFieldNames(Arrays.asList(sourceFilter.getIncludes()), persistentEntity)
							.toArray(new String[] {});
				}

				if (sourceFilter.getExcludes() != null) {
					excludes = updateFieldNames(Arrays.asList(sourceFilter.getExcludes()), persistentEntity)
							.toArray(new String[] {});
				}

				query.addSourceFilter(new FetchSourceFilter(includes, excludes));
			}
		}
	}

	/**
	 * relaces the fieldName with the property name of a property of the persistentEntity with the corresponding
	 * fieldname. If no such property exists, the original fieldName is kept.
	 *
	 * @param fieldNames list of fieldnames
	 * @param persistentEntity the persistent entity to check
	 * @return an updated list of field names
	 */
	private List<String> updateFieldNames(List<String> fieldNames, ElasticsearchPersistentEntity<?> persistentEntity) {
		return fieldNames.stream().map(fieldName -> updateFieldName(persistentEntity, fieldName))
				.collect(Collectors.toList());
	}

	@NotNull
	private String updateFieldName(ElasticsearchPersistentEntity<?> persistentEntity, String fieldName) {
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(fieldName);
		return persistentProperty != null ? persistentProperty.getFieldName() : fieldName;
	}

	private void updatePropertiesInCriteriaQuery(CriteriaQuery criteriaQuery, Class<?> domainClass) {

		Assert.notNull(criteriaQuery, "criteriaQuery must not be null");
		Assert.notNull(domainClass, "domainClass must not be null");

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainClass);

		if (persistentEntity != null) {
			for (Criteria chainedCriteria : criteriaQuery.getCriteria().getCriteriaChain()) {
				updatePropertiesInCriteria(chainedCriteria, persistentEntity);
			}
			for (Criteria subCriteria : criteriaQuery.getCriteria().getSubCriteria()) {
				for (Criteria chainedCriteria : subCriteria.getCriteriaChain()) {
					updatePropertiesInCriteria(chainedCriteria, persistentEntity);
				}
			}
		}
	}

	private void updatePropertiesInCriteria(Criteria criteria, ElasticsearchPersistentEntity<?> persistentEntity) {

		Field field = criteria.getField();

		if (field == null) {
			return;
		}

		String[] fieldNames = field.getName().split("\\.");

		ElasticsearchPersistentEntity<?> currentEntity = persistentEntity;
		ElasticsearchPersistentProperty persistentProperty = null;
		int propertyCount = 0;
		boolean isNested = false;

		for (int i = 0; i < fieldNames.length; i++) {
			persistentProperty = currentEntity.getPersistentProperty(fieldNames[i]);

			if (persistentProperty != null) {
				propertyCount++;
				fieldNames[i] = persistentProperty.getFieldName();

				org.springframework.data.elasticsearch.annotations.Field fieldAnnotation = persistentProperty
						.findAnnotation(org.springframework.data.elasticsearch.annotations.Field.class);

				if (fieldAnnotation != null && fieldAnnotation.type() == FieldType.Nested) {
					isNested = true;
				}

				try {
					currentEntity = mappingContext.getPersistentEntity(persistentProperty.getActualType());
				} catch (Exception e) {
					// using system types like UUIDs will lead to java.lang.reflect.InaccessibleObjectException in JDK 16
					// so if we cannot get an entity here, bail out.
					currentEntity = null;
				}
			}

			if (currentEntity == null) {
				break;
			}
		}

		field.setName(String.join(".", fieldNames));

		if (propertyCount > 1 && isNested) {
			List<String> propertyNames = Arrays.asList(fieldNames);
			field.setPath(String.join(".", propertyNames.subList(0, propertyCount - 1)));
		}

		if (persistentProperty != null) {

			if (persistentProperty.hasPropertyValueConverter()) {
				PropertyValueConverter propertyValueConverter = Objects
						.requireNonNull(persistentProperty.getPropertyValueConverter());
				criteria.getQueryCriteriaEntries().forEach(criteriaEntry -> {

					if (criteriaEntry.getKey().hasValue()) {
						Object value = criteriaEntry.getValue();

						if (value.getClass().isArray()) {
							Object[] objects = (Object[]) value;

							for (int i = 0; i < objects.length; i++) {
								objects[i] = propertyValueConverter.write(objects[i]);
							}
						} else {
							criteriaEntry.setValue(propertyValueConverter.write(value));
						}
					}
				});
			}

			org.springframework.data.elasticsearch.annotations.Field fieldAnnotation = persistentProperty
					.findAnnotation(org.springframework.data.elasticsearch.annotations.Field.class);

			if (fieldAnnotation != null) {
				field.setFieldType(fieldAnnotation.type());
			}
		}
	}

	@Override
	public String updateFieldNames(String propertyPath, ElasticsearchPersistentEntity<?> persistentEntity) {

		Assert.notNull(propertyPath, "propertyPath must not be null");
		Assert.notNull(persistentEntity, "persistentEntity must not be null");

		var properties = propertyPath.split("\\.", 2);

		if (properties.length > 0) {
			var propertyName = properties[0];
			var fieldName = updateFieldName(persistentEntity, propertyName);

			if (properties.length > 1) {
				var persistentProperty = persistentEntity.getPersistentProperty(propertyName);

				if (persistentProperty != null) {
					ElasticsearchPersistentEntity<?> nestedPersistentEntity = mappingContext
							.getPersistentEntity(persistentProperty);
					if (nestedPersistentEntity != null) {
						return fieldName + '.' + updateFieldNames(properties[1], nestedPersistentEntity);
					} else {
						return fieldName;
					}
				}
			}
			return fieldName;
		} else {
			return propertyPath;
		}

	}

	// endregion

	@SuppressWarnings("ClassCanBeRecord")
	static class MapValueAccessor {

		final Map<String, Object> target;

		MapValueAccessor(Map<String, Object> target) {
			this.target = target;
		}

		@Nullable
		public Object get(ElasticsearchPersistentProperty property) {

			String fieldName = property.getFieldName();

			if (target instanceof Document document) {
				// nested objects may have properties like 'id' which are recognized as isIdProperty() but they are not
				// Documents

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

			if (property.hasExplicitFieldName() || !fieldName.contains(".")) {
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

}
