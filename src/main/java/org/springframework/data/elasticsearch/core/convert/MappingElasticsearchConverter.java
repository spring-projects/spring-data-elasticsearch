/*
 * Copyright 2013-2019 the original author or authors.
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

import lombok.RequiredArgsConstructor;

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
import java.util.stream.Collectors;

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
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

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <R> R read(Class<R> type, Document source) {
		return doRead(source, ClassTypeInformation.from((Class<R>) ClassUtils.getUserClass(type)));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected <R> R doRead(Document source, TypeInformation<R> typeHint) {
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

	@SuppressWarnings("unchecked")
	@Override
	public void write(@Nullable Object source, Document sink) {

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

	protected void doWrite(@Nullable Object source, Document sink, @Nullable TypeInformation<? extends Object> typeHint) {

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

	protected void writeEntity(ElasticsearchPersistentEntity<?> entity, Object source, Document sink,
			@Nullable TypeInformation containingStructure) {

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

			if (!isSimpleType(value)) {
				writeProperty(property, value, sink);
			} else {
				sink.set(property, getWriteSimpleValue(value));
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

	@Override
	@Nullable
	public <T> T mapDocument(@Nullable Document document, Class<T> type) {

		if (document == null) {
			return null;
		}

		Object mappedResult = read(type, document);

		if (mappedResult == null) {
			return (T) null;
		}

		return type.isInterface() || !ClassUtils.isAssignableValue(type, mappedResult)
				? getProjectionFactory().createProjection(type, mappedResult)
				: type.cast(mappedResult);
	}

	@Override
	public <T> SearchHits<T> read(Class<T> type, SearchDocumentResponse searchDocumentResponse) {

		Assert.notNull(type, "type must not be null");
		Assert.notNull(searchDocumentResponse, "searchDocumentResponse must not be null");

		long totalHits = searchDocumentResponse.getTotalHits();
		float maxScore = searchDocumentResponse.getMaxScore();
		String scrollId = searchDocumentResponse.getScrollId();
		List<SearchHit<T>> searchHits = searchDocumentResponse.getSearchDocuments().stream() //
				.map(searchDocument -> read(type, searchDocument)) //
				.collect(Collectors.toList());
		Aggregations aggregations = searchDocumentResponse.getAggregations();
		return new SearchHits<T>(totalHits, maxScore, scrollId, searchHits, aggregations);
	}

	@Override
	public <T> SearchHit<T> read(Class<T> type, SearchDocument searchDocument) {

		Assert.notNull(type, "type must not be null");
		Assert.notNull(searchDocument, "searchDocument must not be null");

		String id = searchDocument.hasId() ? searchDocument.getId() : null;
		float score = searchDocument.getScore();
		Object[] sortValues = searchDocument.getSortValues();
		T content = mapDocument(searchDocument, type);

		return new SearchHit<T>(id, score, sortValues, content);
	}

	@Override
	public <T> List<T> mapDocuments(List<Document> documents, Class<T> type) {
		return documents.stream().map(it -> mapDocument(it, type)).collect(Collectors.toList());
	}

	@Override
	public Document mapObject(Object source) {

		Document target = Document.create();
		write(source, target);
		return target;
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

	@Override
	public <T> AggregatedPage<SearchHit<T>> mapResults(SearchDocumentResponse response, Class<T> type,
			Pageable pageable) {

		List<SearchHit<T>> results = response.getSearchDocuments().stream() //
				.map(searchDocument -> read(type, searchDocument)) //
				.collect(Collectors.toList());

		return new AggregatedPageImpl<>(results, pageable, response);
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

	static class MapValueAccessor {

		final Map<String, Object> target;

		MapValueAccessor(Map<String, Object> target) {
			this.target = target;
		}

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

			while (source != null && parts.hasNext()) {

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

		@SuppressWarnings("unchecked")
		private Map<String, Object> getAsMap(Object result) {

			if (result instanceof Map) {
				return (Map) result;
			}

			throw new IllegalArgumentException(String.format("%s is not a Map.", result));
		}
	}

	@RequiredArgsConstructor
	class ElasticsearchPropertyValueProvider implements PropertyValueProvider<ElasticsearchPersistentProperty> {

		final MapValueAccessor mapValueAccessor;

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyValue(ElasticsearchPersistentProperty property) {
			return (T) readValue(mapValueAccessor.get(property), property, property.getTypeInformation());
		}

	}

}
