/*
 * Copyright 2018. the original author or authors.
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

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
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
import org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author Christoph Strobl
 * @since 4.0
 */
public class ReactiveElasticsearchTemplate {

	private final ReactiveElasticsearchClient client;
	private final ElasticsearchConverter converter;
	private final DefaultResultMapper mapper;
	private final ElasticsearchExceptionTranslator exceptionTranslator;

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ReactiveElasticsearchTemplate(ReactiveElasticsearchClient client, ElasticsearchConverter converter) {

		this.client = client;
		this.converter = converter;
		this.mapper = new DefaultResultMapper(converter.getMappingContext());
		this.exceptionTranslator = new ElasticsearchExceptionTranslator();
	}

	public <T> Mono<T> index(T entity) {
		return index(entity, null);
	}

	public <T> Mono<T> index(T entity, String index) {
		return index(entity, index, null);
	}

	/**
	 * Add the given entity to the index.
	 *
	 * @param entity
	 * @param index
	 * @param type
	 * @param <T>
	 * @return
	 */
	public <T> Mono<T> index(T entity, String index, String type) {

		ElasticsearchPersistentEntity<?> persistentEntity = lookupPersistentEntity(entity.getClass());
		return doIndex(entity, persistentEntity, index, type) //
				.map(it -> {

					// TODO: update id if necessary!
					// it.getId()
					// it.getVersion()

					return entity;
				});
	}

	public <T> Mono<T> get(String id, Class<T> resultType) {
		return get(id, resultType, null);
	}

	public <T> Mono<T> get(String id, Class<T> resultType, @Nullable String index) {
		return get(id, resultType, index, null);
	}

	/**
	 * Fetch the entity with given id.
	 *
	 * @param id must not be {@literal null}.
	 * @param resultType must not be {@literal null}.
	 * @param index
	 * @param type
	 * @param <T>
	 * @return the {@link Mono} emitting the entity or signalling completion if none found.
	 */
	public <T> Mono<T> get(String id, Class<T> resultType, @Nullable String index, @Nullable String type) {

		ElasticsearchPersistentEntity<?> persistentEntity = lookupPersistentEntity(resultType);
		GetRequest request = new GetRequest(persistentEntity.getIndexName(), persistentEntity.getIndexType(), id);

		return goGet(id, persistentEntity, index, type).map(it -> mapper.mapEntity(it.sourceAsString(), resultType));
	}

	/**
	 * Search the index for entities matching the given {@link CriteriaQuery query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param resultType must not be {@literal null}.
	 * @param <T>
	 * @return
	 */
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

		return Flux.from(
				execute(client -> client.search(request).map(it -> mapper.mapEntity(it.getSourceAsString(), resultType))));
	}

	/**
	 * Execute within a {@link ClientCallback} managing resources and translating errors.
	 *
	 * @param callback must not be {@literal null}.
	 * @param <T>
	 * @return the {@link Publisher} emitting results.
	 */
	public <T> Publisher<T> execute(ClientCallback<Publisher<T>> callback) {
		return Flux.from(callback.doWithClient(this.client)).onErrorMap(this::translateException);
	}

	// Customization Hooks

	protected Mono<GetResult> goGet(String id, ElasticsearchPersistentEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		String indexToUse = indexName(index, entity);
		String typeToUse = typeName(type, entity);

		return doGet(new GetRequest(indexToUse, typeToUse, id));
	}

	protected Mono<GetResult> doGet(GetRequest request) {

		return Mono.from(execute(client -> client.get(request))) //
				.onErrorResume((it) -> {

					if (it instanceof HttpClientErrorException) {
						return ((HttpClientErrorException) it).getRawStatusCode() == 404;
					}
					return false;

				}, (it) -> Mono.empty());
	}

	protected Mono<IndexResponse> doIndex(Object value, ElasticsearchPersistentEntity<?> entity, @Nullable String index,
			@Nullable String type) {

		PersistentPropertyAccessor propertyAccessor = entity.getPropertyAccessor(value);
		Object id = propertyAccessor.getProperty(entity.getIdProperty());

		String indexToUse = indexName(index, entity);
		String typeToUse = typeName(type, entity);

		IndexRequest request = id != null ? new IndexRequest(indexToUse, typeToUse, id.toString())
				: new IndexRequest(indexToUse, typeToUse);

		try {
			request.source(mapper.getEntityMapper().mapToString(value), Requests.INDEX_CONTENT_TYPE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (entity.hasVersionProperty()) {

			Object version = propertyAccessor.getProperty(entity.getVersionProperty());
			if (version != null) {
				request.version(((Number) version).longValue());
				request.versionType(EXTERNAL);
			}
		}

		if (entity.getParentIdProperty() != null) {

			Object parentId = propertyAccessor.getProperty(entity.getParentIdProperty());
			if (parentId != null) {
				request.parent(parentId.toString());
			}
		}

		return doIndex(request);
	}

	protected Mono<IndexResponse> doIndex(IndexRequest request) {
		return Mono.from(execute(client -> client.index(request)));
	}

	// private helpers

	private static String indexName(@Nullable String index, ElasticsearchPersistentEntity<?> entity) {
		return StringUtils.isEmpty(index) ? entity.getIndexName() : index;
	}

	private static String typeName(@Nullable String type, ElasticsearchPersistentEntity<?> entity) {
		return StringUtils.isEmpty(type) ? entity.getIndexType() : type;
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

			FieldSortBuilder sort = SortBuilders.fieldSort(entity.getPersistentProperty(order.getProperty()).getFieldName())
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

		if (!(throwable instanceof RuntimeException)) {
			return throwable;
		}

		RuntimeException ex = exceptionTranslator.translateExceptionIfPossible((RuntimeException) throwable);
		return ex != null ? ex : throwable;
	}

	// Additional types
	public interface ClientCallback<T extends Publisher> {

		T doWithClient(ReactiveElasticsearchClient client);
	}
}
