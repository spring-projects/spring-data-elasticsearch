/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.index.VersionType.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author Christoph Strobl
 * @since 4.0
 */
public class ReactiveElasticsearchTemplate implements ReactiveElasticsearchOperations {

	private final ReactiveElasticsearchClient client;
	private final ElasticsearchConverter converter;
	private final ResultsMapper resultMapper;
	private final ElasticsearchExceptionTranslator exceptionTranslator;

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client, ElasticsearchConverter converter) {

		this.client = client;
		this.converter = converter;
		this.resultMapper = new DefaultResultMapper(converter.getMappingContext());
		this.exceptionTranslator = new ElasticsearchExceptionTranslator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#exctute(ClientCallback)
	 */
	@Override
	public <T> Publisher<T> execute(ClientCallback<Publisher<T>> callback) {
		return Flux.defer(() -> callback.doWithClient(this.client)).onErrorMap(this::translateException);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#index(Object, String, String)
	 */
	@Override
	public <T> Mono<T> index(T entity, @Nullable String index, @Nullable String type) {

		Assert.notNull(entity, "Entity must not be null!");

		AdaptableEntity<T> adaptableEntity = ConverterAwareAdaptableEntity.of(entity, converter);

		return doIndex(entity, adaptableEntity, index, type) //
				.map(it -> {
					return adaptableEntity.updateIdIfNecessary(it.getId());
				});
	}

	private Mono<IndexResponse> doIndex(Object value, AdaptableEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		return Mono.defer(() -> {

			Object id = entity.getId();

			String indexToUse = indexName(index, entity);
			String typeToUse = typeName(type, entity);

			IndexRequest request = id != null ? new IndexRequest(indexToUse, typeToUse, id.toString())
					: new IndexRequest(indexToUse, typeToUse);

			try {
				request.source(resultMapper.getEntityMapper().mapToString(value), Requests.INDEX_CONTENT_TYPE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			if (entity.isVersioned()) {

				Object version = entity.getVersion();
				if (version != null) {
					request.version(((Number) version).longValue());
					request.versionType(EXTERNAL);
				}
			}

			if (entity.getPersistentEntity().getParentIdProperty() != null) {

				Object parentId = entity.getPropertyValue(entity.getPersistentEntity().getParentIdProperty());
				if (parentId != null) {
					request.parent(parentId.toString());
				}
			}

			return doIndex(request.setRefreshPolicy(RefreshPolicy.IMMEDIATE));
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#findById(String, Class, String, String)
	 */
	@Override
	public <T> Mono<T> findById(String id, Class<T> resultType, @Nullable String index, @Nullable String type) {

		Assert.notNull(id, "Id must not be null!");
		
		ElasticsearchEntity<?> persistentEntity = BasicElasticsearchEntity.of(resultType, converter);

		return doFindById(id, persistentEntity, index, type)
				.map(it -> resultMapper.mapEntity(it.sourceAsString(), resultType));
	}

	private Mono<GetResult> doFindById(String id, ElasticsearchEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		String indexToUse = indexName(index, entity);
		String typeToUse = typeName(type, entity);

		return doFindById(new GetRequest(indexToUse, typeToUse, id));
	}

	@Override
	public <T> Flux<T> query(CriteriaQuery query, Class<T> resultType) {

		ElasticsearchPersistentEntity<?> entity = lookupPersistentEntity(resultType);

		SearchRequest request = new SearchRequest(indices(query, entity));
		request.types(indexTypes(query, entity));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(mappedQuery(query, entity));
		// TODO: request.source().postFilter(elasticsearchFilter); -- filter query

		searchSourceBuilder.version(entity.hasVersionProperty()); // This has been true by default before
		searchSourceBuilder.trackScores(query.getTrackScores());

		if (query.getSourceFilter() != null) {
			searchSourceBuilder.fetchSource(query.getSourceFilter().getIncludes(), query.getSourceFilter().getExcludes());
		}

		if (query.getPageable().isPaged()) {

			long offset = query.getPageable().getOffset();
			if (offset > Integer.MAX_VALUE) {
				throw new IllegalArgumentException(String.format("Offset must not be more than %s", Integer.MAX_VALUE));
			}

			searchSourceBuilder.from((int) offset);
			searchSourceBuilder.size(query.getPageable().getPageSize());
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(query.getIndicesOptions());
		}

		sort(query, entity).forEach(searchSourceBuilder::sort);

		if (query.getMinScore() > 0) {
			searchSourceBuilder.minScore(query.getMinScore());
		}
		request.source(searchSourceBuilder);

		return Flux.from(execute(
				client -> client.search(request).map(it -> resultMapper.mapEntity(it.getSourceAsString(), resultType))));
	}

	// Customization Hooks

	protected Mono<IndexResponse> doIndex(IndexRequest request) {
		return Mono.from(execute(client -> client.index(request)));
	}

	protected Mono<GetResult> doFindById(GetRequest request) {

		return Mono.from(execute(client -> client.get(request))) //
				.onErrorResume((it) -> {

					if (it instanceof HttpClientErrorException) {
						return ((HttpClientErrorException) it).getRawStatusCode() == 404;
					}
					return false;

				}, (it) -> Mono.empty());
	}

	// private helpers

	private static String indexName(@Nullable String index, ElasticsearchEntity<?> entity) {
		return StringUtils.isEmpty(index) ? entity.getIndexName() : index;
	}

	private static String typeName(@Nullable String type, ElasticsearchEntity<?> entity) {
		return StringUtils.isEmpty(type) ? entity.getTypeName() : type;
	}

	private static String[] indices(CriteriaQuery query, ElasticsearchPersistentEntity<?> entity) {

		if (query.getIndices().isEmpty()) {
			return new String[] { entity.getIndexName() };
		}

		return query.getIndices().toArray(new String[0]);
	}

	private static String[] indexTypes(CriteriaQuery query, ElasticsearchPersistentEntity<?> entity) {

		if (query.getTypes().isEmpty()) {
			return new String[] { entity.getIndexType() };
		}

		return query.getTypes().toArray(new String[0]);
	}

	private List<FieldSortBuilder> sort(Query query, ElasticsearchPersistentEntity<?> entity) {

		if (query.getSort() == null || query.getSort().isUnsorted()) {
			return Collections.emptyList();
		}

		List<FieldSortBuilder> mappedSort = new ArrayList<>();
		for (Sort.Order order : query.getSort()) {

			ElasticsearchPersistentProperty property = entity.getPersistentProperty(order.getProperty());
			String fieldName = property != null ? property.getFieldName() : order.getProperty();

			FieldSortBuilder sort = SortBuilders.fieldSort(fieldName)
					.order(order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC);

			if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
				sort.missing("_first");
			} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
				sort.missing("_last");
			}

			mappedSort.add(sort);
		}

		return mappedSort;
	}

	private QueryBuilder mappedQuery(CriteriaQuery query, ElasticsearchPersistentEntity<?> entity) {

		// TODO: we need to actually map the fields to the according field names!
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(query.getCriteria());
		return elasticsearchQuery != null ? elasticsearchQuery : QueryBuilders.matchAllQuery();
	}

	private QueryBuilder mappedFilterQuery(CriteriaQuery query, ElasticsearchPersistentEntity<?> entity) {

		// TODO: this is actually strange in the RestTemplate:L378 - need to chack
		return null;
	}

	private ElasticsearchPersistentEntity<?> lookupPersistentEntity(Class<?> type) {
		return converter.getMappingContext().getPersistentEntity(type);
	}

	private Throwable translateException(Throwable throwable) {

		RuntimeException exception = throwable instanceof RuntimeException ? (RuntimeException) throwable
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = exceptionTranslator.translateExceptionIfPossible(exception);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : throwable;
	}

	/**
	 * @param <T>
	 * @author Christoph Strobl
	 * @since 4.0
	 */
	protected interface ElasticsearchEntity<T> {

		default boolean isIdentifiable() {
			return getPersistentEntity().hasVersionProperty();
		}

		default boolean isVersioned() {
			return getPersistentEntity().hasVersionProperty();
		}

		default ElasticsearchPersistentProperty getIdProperty() {
			return getPersistentEntity().getIdProperty();
		}

		default String getIndexName() {
			return getPersistentEntity().getIndexName();
		}

		default String getTypeName() {
			return getPersistentEntity().getIndexType();
		}

		ElasticsearchPersistentEntity<?> getPersistentEntity();
	}

	protected interface AdaptableEntity<T> extends ElasticsearchEntity<T> {

		PersistentPropertyAccessor<T> getPropertyAccessor();

		IdentifierAccessor getIdentifierAccessor();

		@Nullable
		default Object getId() {
			return getIdentifierAccessor().getIdentifier();
		}

		default Object getVersion() {
			return getPropertyAccessor().getProperty(getPersistentEntity().getRequiredVersionProperty());
		}

		@Nullable
		default Object getPropertyValue(PersistentProperty<?> property) {
			return getPropertyAccessor().getProperty(property);
		}

		default T getBean() {
			return getPropertyAccessor().getBean();
		}

		default T updateIdIfNecessary(Object id) {

			if (id == null || !getPersistentEntity().hasIdProperty() || getId() != null) {
				return getPropertyAccessor().getBean();
			}

			return updatePropertyValue(getPersistentEntity().getIdProperty(), id);
		}

		default T updatePropertyValue(PersistentProperty<?> property, @Nullable Object value) {

			getPropertyAccessor().setProperty(property, value);
			return getPropertyAccessor().getBean();
		}

	}

	protected static class BasicElasticsearchEntity<T> implements ElasticsearchEntity<T> {

		final ElasticsearchPersistentEntity<?> entity;

		BasicElasticsearchEntity(ElasticsearchPersistentEntity<?> entity) {
			this.entity = entity;
		}

		static <T> BasicElasticsearchEntity<T> of(T bean, ElasticsearchConverter converter) {
			return new BasicElasticsearchEntity<>(converter.getMappingContext().getRequiredPersistentEntity(bean.getClass()));
		}

		static <T> BasicElasticsearchEntity<T> of(Class<T> type, ElasticsearchConverter converter) {
			return new BasicElasticsearchEntity<>(converter.getMappingContext().getRequiredPersistentEntity(type));
		}

		@Override
		public ElasticsearchPersistentEntity<?> getPersistentEntity() {
			return entity;
		}
	}

	protected static class ConverterAwareAdaptableEntity<T> implements AdaptableEntity<T> {

		final ElasticsearchPersistentEntity<?> entity;
		final PersistentPropertyAccessor<T> propertyAccessor;
		final IdentifierAccessor idAccessor;
		final ElasticsearchConverter converter;

		ConverterAwareAdaptableEntity(ElasticsearchPersistentEntity<?> entity, IdentifierAccessor idAccessor,
				PersistentPropertyAccessor<T> propertyAccessor, ElasticsearchConverter converter) {

			this.entity = entity;
			this.propertyAccessor = propertyAccessor;
			this.idAccessor = idAccessor;
			this.converter = converter;
		}

		static <T> ConverterAwareAdaptableEntity<T> of(T bean, ElasticsearchConverter converter) {

			ElasticsearchPersistentEntity<?> entity = converter.getMappingContext()
					.getRequiredPersistentEntity(bean.getClass());
			IdentifierAccessor idAccessor = entity.getIdentifierAccessor(bean);
			PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

			return new ConverterAwareAdaptableEntity<>(entity, idAccessor, propertyAccessor, converter);
		}

		@Override
		public PersistentPropertyAccessor<T> getPropertyAccessor() {
			return propertyAccessor;
		}

		@Override
		public IdentifierAccessor getIdentifierAccessor() {

			if (entity.getTypeInformation().isMap()) {

				return () -> {

					Object id = idAccessor.getIdentifier();
					if (id != null) {
						return id;
					}

					Map<?, ?> source = (Map<?, ?>) propertyAccessor.getBean();
					return source.get("id");
				};
			}

			return idAccessor;
		}

		@Override
		public ElasticsearchPersistentEntity<?> getPersistentEntity() {
			return entity;
		}

	}
}
