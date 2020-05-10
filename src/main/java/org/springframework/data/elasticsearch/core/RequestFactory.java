/*
 * Copyright 2019-2020 the original author or authors.
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

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.util.CollectionUtils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory class to create Elasticsearch request instances from Spring Data Elasticsearch query objects.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author Roman Puchkovskiy
 * @since 4.0
 */
class RequestFactory {

	// the default max result window size of Elasticsearch
	static final Integer INDEX_MAX_RESULT_WINDOW = 10_000;

	private final ElasticsearchConverter elasticsearchConverter;

	public RequestFactory(ElasticsearchConverter elasticsearchConverter) {
		this.elasticsearchConverter = elasticsearchConverter;
	}

	public IndicesAliasesRequest.AliasActions aliasAction(AliasQuery query, IndexCoordinates index) {
		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.add()
				.alias(query.getAliasName()).index(index.getIndexName());

		if (query.getFilterBuilder() != null) {
			aliasAction.filter(query.getFilterBuilder());
		} else if (query.getFilter() != null) {
			aliasAction.filter(query.getFilter());
		}

		if (!StringUtils.isEmpty(query.getRouting())) {
			aliasAction.routing(query.getRouting());
		}

		if (!StringUtils.isEmpty(query.getSearchRouting())) {
			aliasAction.searchRouting(query.getSearchRouting());
		}

		if (!StringUtils.isEmpty(query.getIndexRouting())) {
			aliasAction.indexRouting(query.getIndexRouting());
		}

		return aliasAction;
	}

	public BulkRequest bulkRequest(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		BulkRequest bulkRequest = new BulkRequest();

		if (bulkOptions.getTimeout() != null) {
			bulkRequest.timeout(bulkOptions.getTimeout());
		}

		if (bulkOptions.getRefreshPolicy() != null) {
			bulkRequest.setRefreshPolicy(bulkOptions.getRefreshPolicy());
		}

		if (bulkOptions.getWaitForActiveShards() != null) {
			bulkRequest.waitForActiveShards(bulkOptions.getWaitForActiveShards());
		}

		if (bulkOptions.getPipeline() != null) {
			bulkRequest.pipeline(bulkOptions.getPipeline());
		}

		if (bulkOptions.getRoutingId() != null) {
			bulkRequest.routing(bulkOptions.getRoutingId());
		}

		queries.forEach(query -> {

			if (query instanceof IndexQuery) {
				bulkRequest.add(indexRequest((IndexQuery) query, index));
			} else if (query instanceof UpdateQuery) {
				bulkRequest.add(updateRequest((UpdateQuery) query, index));
			}
		});
		return bulkRequest;
	}

	public BulkRequestBuilder bulkRequestBuilder(Client client, List<?> queries, BulkOptions bulkOptions,
			IndexCoordinates index) {
		BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

		if (bulkOptions.getTimeout() != null) {
			bulkRequestBuilder.setTimeout(bulkOptions.getTimeout());
		}

		if (bulkOptions.getRefreshPolicy() != null) {
			bulkRequestBuilder.setRefreshPolicy(bulkOptions.getRefreshPolicy());
		}

		if (bulkOptions.getWaitForActiveShards() != null) {
			bulkRequestBuilder.setWaitForActiveShards(bulkOptions.getWaitForActiveShards());
		}

		if (bulkOptions.getPipeline() != null) {
			bulkRequestBuilder.pipeline(bulkOptions.getPipeline());
		}

		if (bulkOptions.getRoutingId() != null) {
			bulkRequestBuilder.routing(bulkOptions.getRoutingId());
		}

		queries.forEach(query -> {

			if (query instanceof IndexQuery) {
				bulkRequestBuilder.add(indexRequestBuilder(client, (IndexQuery) query, index));
			} else if (query instanceof UpdateQuery) {
				bulkRequestBuilder.add(updateRequestBuilderFor(client, (UpdateQuery) query, index));
			}
		});

		return bulkRequestBuilder;
	}

	@SuppressWarnings("unchecked")
	public CreateIndexRequest createIndexRequest(String indexName, @Nullable Document settings) {
		CreateIndexRequest request = new CreateIndexRequest(indexName);

		if (settings != null) {
			request.settings(settings);
		}
		return request;
	}

	@SuppressWarnings("unchecked")
	public CreateIndexRequestBuilder createIndexRequestBuilder(Client client, String indexName,
			@Nullable Document settings) {
		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

		if (settings != null) {
			createIndexRequestBuilder.setSettings(settings);
		}
		return createIndexRequestBuilder;
	}

	@Deprecated
	public DeleteByQueryRequest deleteByQueryRequest(DeleteQuery deleteQuery, IndexCoordinates index) {
		DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index.getIndexNames()) //
				.setQuery(deleteQuery.getQuery()) //
				.setAbortOnVersionConflict(false) //
				.setRefresh(true);

		if (deleteQuery.getPageSize() != null)
			deleteByQueryRequest.setBatchSize(deleteQuery.getPageSize());

		if (deleteQuery.getScrollTimeInMillis() != null)
			deleteByQueryRequest.setScroll(TimeValue.timeValueMillis(deleteQuery.getScrollTimeInMillis()));

		return deleteByQueryRequest;
	}

	public DeleteByQueryRequest deleteByQueryRequest(Query query, Class<?> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = searchRequest(query, clazz, index);
		DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index.getIndexNames()) //
				.setQuery(searchRequest.source().query()) //
				.setAbortOnVersionConflict(false) //
				.setRefresh(true);

		if (query.isLimiting()) {
			deleteByQueryRequest.setBatchSize(query.getMaxResults());
		}

		if (query.hasScrollTime()) {
			deleteByQueryRequest.setScroll(TimeValue.timeValueMillis(query.getScrollTime().toMillis()));
		}

		return deleteByQueryRequest;
	}

	@Deprecated
	public DeleteByQueryRequestBuilder deleteByQueryRequestBuilder(Client client, DeleteQuery deleteQuery,
			IndexCoordinates index) {
		DeleteByQueryRequestBuilder requestBuilder = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE) //
				.source(index.getIndexNames()) //
				.filter(deleteQuery.getQuery()) //
				.abortOnVersionConflict(false) //
				.refresh(true);

		SearchRequestBuilder source = requestBuilder.source();

		if (deleteQuery.getScrollTimeInMillis() != null)
			source.setScroll(TimeValue.timeValueMillis(deleteQuery.getScrollTimeInMillis()));

		return requestBuilder;
	}

	public DeleteByQueryRequestBuilder deleteByQueryRequestBuilder(Client client, Query query, Class<?> clazz,
			IndexCoordinates index) {
		SearchRequest searchRequest = searchRequest(query, clazz, index);
		DeleteByQueryRequestBuilder requestBuilder = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE) //
				.source(index.getIndexNames()) //
				.filter(searchRequest.source().query()) //
				.abortOnVersionConflict(false) //
				.refresh(true);

		SearchRequestBuilder source = requestBuilder.source();

		if (query.isLimiting()) {
			source.setSize(query.getMaxResults());
		}

		if (query.hasScrollTime()) {
			source.setScroll(TimeValue.timeValueMillis(query.getScrollTime().toMillis()));
		}

		return requestBuilder;
	}

	public GetRequest getRequest(String id, IndexCoordinates index) {
		return new GetRequest(index.getIndexName(), id);
	}

	public GetRequestBuilder getRequestBuilder(Client client, String id, IndexCoordinates index) {
		return client.prepareGet(index.getIndexName(), null, id);
	}

	@Nullable
	public HighlightBuilder highlightBuilder(Query query) {
		HighlightBuilder highlightBuilder = query.getHighlightQuery().map(HighlightQuery::getHighlightBuilder).orElse(null);

		if (highlightBuilder == null) {

			if (query instanceof NativeSearchQuery) {
				NativeSearchQuery searchQuery = (NativeSearchQuery) query;

				if (searchQuery.getHighlightFields() != null || searchQuery.getHighlightBuilder() != null) {
					highlightBuilder = searchQuery.getHighlightBuilder();

					if (highlightBuilder == null) {
						highlightBuilder = new HighlightBuilder();
					}

					if (searchQuery.getHighlightFields() != null) {
						for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
							highlightBuilder.field(highlightField);
						}
					}
				}
			}
		}
		return highlightBuilder;
	}

	public IndexRequest indexRequest(IndexQuery query, IndexCoordinates index) {
		String indexName = index.getIndexName();

		IndexRequest indexRequest;

		if (query.getObject() != null) {
			String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
			// If we have a query id and a document id, do not ask ES to generate one.
			if (id != null) {
				indexRequest = new IndexRequest(indexName).id(id);
			} else {
				indexRequest = new IndexRequest(indexName);
			}
			indexRequest.source(elasticsearchConverter.mapObject(query.getObject()).toJson(), Requests.INDEX_CONTENT_TYPE);
		} else if (query.getSource() != null) {
			indexRequest = new IndexRequest(indexName).id(query.getId()).source(query.getSource(),
					Requests.INDEX_CONTENT_TYPE);
		} else {
			throw new ElasticsearchException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}
		if (query.getVersion() != null) {
			indexRequest.version(query.getVersion());
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(query.getObject().getClass());
			indexRequest.versionType(versionType);
		}

		if (query.getSeqNo() != null) {
			indexRequest.setIfSeqNo(query.getSeqNo());
		}
		if (query.getPrimaryTerm() != null) {
			indexRequest.setIfPrimaryTerm(query.getPrimaryTerm());
		}

		return indexRequest;
	}

	public IndexRequestBuilder indexRequestBuilder(Client client, IndexQuery query, IndexCoordinates index) {
		String indexName = index.getIndexName();
		String type = IndexCoordinates.TYPE;

		IndexRequestBuilder indexRequestBuilder;

		if (query.getObject() != null) {
			String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
			// If we have a query id and a document id, do not ask ES to generate one.
			if (id != null) {
				indexRequestBuilder = client.prepareIndex(indexName, type, id);
			} else {
				indexRequestBuilder = client.prepareIndex(indexName, type);
			}
			indexRequestBuilder.setSource(elasticsearchConverter.mapObject(query.getObject()).toJson(),
					Requests.INDEX_CONTENT_TYPE);
		} else if (query.getSource() != null) {
			indexRequestBuilder = client.prepareIndex(indexName, type, query.getId()).setSource(query.getSource(),
					Requests.INDEX_CONTENT_TYPE);
		} else {
			throw new ElasticsearchException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}
		if (query.getVersion() != null) {
			indexRequestBuilder.setVersion(query.getVersion());
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(query.getObject().getClass());
			indexRequestBuilder.setVersionType(versionType);
		}

		if (query.getSeqNo() != null) {
			indexRequestBuilder.setIfSeqNo(query.getSeqNo());
		}
		if (query.getPrimaryTerm() != null) {
			indexRequestBuilder.setIfPrimaryTerm(query.getPrimaryTerm());
		}

		return indexRequestBuilder;
	}

	public MoreLikeThisQueryBuilder moreLikeThisQueryBuilder(MoreLikeThisQuery query, IndexCoordinates index) {
		MoreLikeThisQueryBuilder.Item item = new MoreLikeThisQueryBuilder.Item(index.getIndexName(), query.getId());

		String[] fields = null;
		if (query.getFields() != null) {
			fields = query.getFields().toArray(new String[] {});
		}

		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = QueryBuilders.moreLikeThisQuery(fields, null,
				new MoreLikeThisQueryBuilder.Item[] { item });

		if (query.getMinTermFreq() != null) {
			moreLikeThisQueryBuilder.minTermFreq(query.getMinTermFreq());
		}

		if (query.getMaxQueryTerms() != null) {
			moreLikeThisQueryBuilder.maxQueryTerms(query.getMaxQueryTerms());
		}

		if (!isEmpty(query.getStopWords())) {
			moreLikeThisQueryBuilder.stopWords(query.getStopWords());
		}

		if (query.getMinDocFreq() != null) {
			moreLikeThisQueryBuilder.minDocFreq(query.getMinDocFreq());
		}

		if (query.getMaxDocFreq() != null) {
			moreLikeThisQueryBuilder.maxDocFreq(query.getMaxDocFreq());
		}

		if (query.getMinWordLen() != null) {
			moreLikeThisQueryBuilder.minWordLength(query.getMinWordLen());
		}

		if (query.getMaxWordLen() != null) {
			moreLikeThisQueryBuilder.maxWordLength(query.getMaxWordLen());
		}

		if (query.getBoostTerms() != null) {
			moreLikeThisQueryBuilder.boostTerms(query.getBoostTerms());
		}

		return moreLikeThisQueryBuilder;
	}

	public SearchRequest searchRequest(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {

		SearchRequest searchRequest = prepareSearchRequest(query, clazz, index);
		QueryBuilder elasticsearchQuery = getQuery(query);
		QueryBuilder elasticsearchFilter = getFilter(query);

		if (elasticsearchQuery != null) {
			searchRequest.source().query(elasticsearchQuery);
		} else {
			searchRequest.source().query(QueryBuilders.matchAllQuery());
		}

		if (elasticsearchFilter != null) {
			searchRequest.source().postFilter(elasticsearchFilter);
		}

		return searchRequest;

	}

	public SearchRequestBuilder searchRequestBuilder(Client client, Query query, @Nullable Class<?> clazz,
			IndexCoordinates index) {

		SearchRequestBuilder searchRequestBuilder = prepareSearchRequestBuilder(query, client, clazz, index);
		QueryBuilder elasticsearchQuery = getQuery(query);
		QueryBuilder elasticsearchFilter = getFilter(query);

		if (elasticsearchQuery != null) {
			searchRequestBuilder.setQuery(elasticsearchQuery);
		} else {
			searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
		}

		if (elasticsearchFilter != null) {
			searchRequestBuilder.setPostFilter(elasticsearchFilter);
		}

		return searchRequestBuilder;
	}

	public UpdateRequest updateRequest(UpdateQuery query, IndexCoordinates index) {

		UpdateRequest updateRequest = new UpdateRequest(index.getIndexName(), query.getId());

		if (query.getScript() != null) {
			Map<String, Object> params = query.getParams();

			if (params == null) {
				params = new HashMap<>();
			}
			Script script = new Script(ScriptType.INLINE, query.getLang(), query.getScript(), params);
			updateRequest.script(script);
		}

		if (query.getDocument() != null) {
			updateRequest.doc(query.getDocument());
		}

		if (query.getUpsert() != null) {
			updateRequest.upsert(query.getUpsert());
		}

		if (query.getRouting() != null) {
			updateRequest.routing(query.getRouting());
		}

		if (query.getScriptedUpsert() != null) {
			updateRequest.scriptedUpsert(query.getScriptedUpsert());
		}

		if (query.getDocAsUpsert() != null) {
			updateRequest.docAsUpsert(query.getDocAsUpsert());
		}

		if (query.getFetchSource() != null) {
			updateRequest.fetchSource(query.getFetchSource());
		}

		if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
			List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
					: Collections.emptyList();
			List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
					: Collections.emptyList();
			updateRequest.fetchSource(includes.toArray(new String[0]), excludes.toArray(new String[0]));
		}

		if (query.getIfSeqNo() != null) {
			updateRequest.setIfSeqNo(query.getIfSeqNo());
		}

		if (query.getIfPrimaryTerm() != null) {
			updateRequest.setIfPrimaryTerm(query.getIfPrimaryTerm());
		}

		if (query.getRefresh() != null) {
			updateRequest.setRefreshPolicy(query.getRefresh().name().toLowerCase());
		}

		if (query.getRetryOnConflict() != null) {
			updateRequest.retryOnConflict(query.getRetryOnConflict());
		}

		if (query.getTimeout() != null) {
			updateRequest.timeout(query.getTimeout());
		}

		if (query.getWaitForActiveShards() != null) {
			updateRequest.waitForActiveShards(ActiveShardCount.parseString(query.getWaitForActiveShards()));
		}

		return updateRequest;
	}

	public UpdateRequestBuilder updateRequestBuilderFor(Client client, UpdateQuery query, IndexCoordinates index) {

		UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(index.getIndexName(), IndexCoordinates.TYPE,
				query.getId());

		if (query.getScript() != null) {
			Map<String, Object> params = query.getParams();

			if (params == null) {
				params = new HashMap<>();
			}
			Script script = new Script(ScriptType.INLINE, query.getLang(), query.getScript(), params);
			updateRequestBuilder.setScript(script);
		}

		if (query.getDocument() != null) {
			updateRequestBuilder.setDoc(query.getDocument());
		}

		if (query.getUpsert() != null) {
			updateRequestBuilder.setUpsert(query.getUpsert());
		}

		if (query.getRouting() != null) {
			updateRequestBuilder.setRouting(query.getRouting());
		}

		if (query.getScriptedUpsert() != null) {
			updateRequestBuilder.setScriptedUpsert(query.getScriptedUpsert());
		}

		if (query.getDocAsUpsert() != null) {
			updateRequestBuilder.setDocAsUpsert(query.getDocAsUpsert());
		}

		if (query.getFetchSource() != null) {
			updateRequestBuilder.setFetchSource(query.getFetchSource());
		}

		if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
			List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
					: Collections.emptyList();
			List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
					: Collections.emptyList();
			updateRequestBuilder.setFetchSource(includes.toArray(new String[0]), excludes.toArray(new String[0]));
		}

		if (query.getIfSeqNo() != null) {
			updateRequestBuilder.setIfSeqNo(query.getIfSeqNo());
		}

		if (query.getIfPrimaryTerm() != null) {
			updateRequestBuilder.setIfPrimaryTerm(query.getIfPrimaryTerm());
		}

		if (query.getRefresh() != null) {
			updateRequestBuilder.setRefreshPolicy(query.getRefresh().name().toLowerCase());
		}

		if (query.getRetryOnConflict() != null) {
			updateRequestBuilder.setRetryOnConflict(query.getRetryOnConflict());
		}

		if (query.getTimeout() != null) {
			updateRequestBuilder.setTimeout(query.getTimeout());
		}

		if (query.getWaitForActiveShards() != null) {
			updateRequestBuilder.setWaitForActiveShards(ActiveShardCount.parseString(query.getWaitForActiveShards()));
		}

		return updateRequestBuilder;
	}

	private SearchRequest prepareSearchRequest(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {
		Assert.notNull(index.getIndexNames(), "No index defined for Query");
		Assert.notEmpty(index.getIndexNames(), "No index defined for Query");

		SearchRequest request = new SearchRequest(index.getIndexNames());
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.version(true);
		sourceBuilder.trackScores(query.getTrackScores());
		if (hasSeqNoPrimaryTermProperty(clazz)) {
			sourceBuilder.seqNoAndPrimaryTerm(true);
		}

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			sourceBuilder.fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable().isPaged()) {
			sourceBuilder.from((int) query.getPageable().getOffset());
			sourceBuilder.size(query.getPageable().getPageSize());
		} else {
			sourceBuilder.from(0);
			sourceBuilder.size(INDEX_MAX_RESULT_WINDOW);
		}

		if (!query.getFields().isEmpty()) {
			sourceBuilder.fetchSource(query.getFields().toArray(new String[0]), null);
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(query.getIndicesOptions());
		}

		if (query.isLimiting()) {
			sourceBuilder.size(query.getMaxResults());
		}

		if (query.getMinScore() > 0) {
			sourceBuilder.minScore(query.getMinScore());
		}

		if (query.getPreference() != null) {
			request.preference(query.getPreference());
		}

		if (query.getSearchType() != null) {
			request.searchType(query.getSearchType());
		}

		prepareSort(query, sourceBuilder, getPersistentEntity(clazz));

		HighlightBuilder highlightBuilder = highlightBuilder(query);

		if (highlightBuilder != null) {
			sourceBuilder.highlighter(highlightBuilder);
		}

		if (query instanceof NativeSearchQuery) {
			prepareNativeSearch((NativeSearchQuery) query, sourceBuilder);

		}

		if (query.getTrackTotalHits()) {
			sourceBuilder.trackTotalHits(query.getTrackTotalHits());
		}

		if (StringUtils.hasLength(query.getRoute())) {
			request.routing(query.getRoute());
		}

		request.source(sourceBuilder);
		return request;
	}

	private boolean hasSeqNoPrimaryTermProperty(@Nullable Class<?> entityClass) {

		if (entityClass == null) {
			return false;
		}

		if (!elasticsearchConverter.getMappingContext().hasPersistentEntityFor(entityClass)) {
			return false;
		}

		ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.getMappingContext()
				.getRequiredPersistentEntity(entityClass);
		return entity.hasSeqNoPrimaryTermProperty();
	}

	public PutMappingRequest putMappingRequest(IndexCoordinates index, Document mapping) {
		PutMappingRequest request = new PutMappingRequest(index.getIndexName());
		request.source(mapping);
		return request;
	}

	@SuppressWarnings("rawtypes")
	public PutMappingRequestBuilder putMappingRequestBuilder(Client client, IndexCoordinates index, Document mapping) {
		PutMappingRequestBuilder requestBuilder = client.admin().indices().preparePutMapping(index.getIndexName())
				.setType(IndexCoordinates.TYPE);
		requestBuilder.setSource(mapping);
		return requestBuilder;
	}

	public MultiGetRequest multiGetRequest(Query query, IndexCoordinates index) {
		MultiGetRequest multiGetRequest = new MultiGetRequest();
		getMultiRequestItems(query, index).forEach(multiGetRequest::add);
		return multiGetRequest;
	}

	public MultiGetRequestBuilder multiGetRequestBuilder(Client client, Query searchQuery, IndexCoordinates index) {
		MultiGetRequestBuilder multiGetRequestBuilder = client.prepareMultiGet();
		getMultiRequestItems(searchQuery, index).forEach(multiGetRequestBuilder::add);
		return multiGetRequestBuilder;
	}

	private List<MultiGetRequest.Item> getMultiRequestItems(Query searchQuery, IndexCoordinates index) {
		List<MultiGetRequest.Item> items = new ArrayList<>();
		if (!isEmpty(searchQuery.getFields())) {
			searchQuery.addSourceFilter(new FetchSourceFilter(toArray(searchQuery.getFields()), null));
		}

		for (String id : searchQuery.getIds()) {
			MultiGetRequest.Item item = new MultiGetRequest.Item(index.getIndexName(), id);

			if (searchQuery.getRoute() != null) {
				item = item.routing(searchQuery.getRoute());
			}
			items.add(item);
		}
		return items;
	}

	private void prepareNativeSearch(NativeSearchQuery query, SearchSourceBuilder sourceBuilder) {

		if (!query.getScriptFields().isEmpty()) {
			for (ScriptField scriptedField : query.getScriptFields()) {
				sourceBuilder.scriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		if (query.getCollapseBuilder() != null) {
			sourceBuilder.collapse(query.getCollapseBuilder());
		}

		if (!isEmpty(query.getIndicesBoost())) {
			for (IndexBoost indexBoost : query.getIndicesBoost()) {
				sourceBuilder.indexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(query.getAggregations())) {
			for (AbstractAggregationBuilder<?> aggregationBuilder : query.getAggregations()) {
				sourceBuilder.aggregation(aggregationBuilder);
			}
		}

	}

	private void prepareNativeSearch(SearchRequestBuilder searchRequestBuilder, NativeSearchQuery nativeSearchQuery) {
		if (!isEmpty(nativeSearchQuery.getScriptFields())) {
			for (ScriptField scriptedField : nativeSearchQuery.getScriptFields()) {
				searchRequestBuilder.addScriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		if (nativeSearchQuery.getCollapseBuilder() != null) {
			searchRequestBuilder.setCollapse(nativeSearchQuery.getCollapseBuilder());
		}

		if (!isEmpty(nativeSearchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : nativeSearchQuery.getIndicesBoost()) {
				searchRequestBuilder.addIndexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(nativeSearchQuery.getAggregations())) {
			for (AbstractAggregationBuilder<?> aggregationBuilder : nativeSearchQuery.getAggregations()) {
				searchRequestBuilder.addAggregation(aggregationBuilder);
			}
		}
	}

	private SearchRequestBuilder prepareSearchRequestBuilder(Query query, Client client, @Nullable Class<?> clazz,
			IndexCoordinates index) {
		Assert.notNull(index.getIndexNames(), "No index defined for Query");
		Assert.notEmpty(index.getIndexNames(), "No index defined for Query");

		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index.getIndexNames()) //
				.setSearchType(query.getSearchType()) //
				.setVersion(true) //
				.setTrackScores(query.getTrackScores());
		if (hasSeqNoPrimaryTermProperty(clazz)) {
			searchRequestBuilder.seqNoAndPrimaryTerm(true);
		}

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			searchRequestBuilder.setFetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable().isPaged()) {
			searchRequestBuilder.setFrom((int) query.getPageable().getOffset());
			searchRequestBuilder.setSize(query.getPageable().getPageSize());
		} else {
			searchRequestBuilder.setFrom(0);
			searchRequestBuilder.setSize(INDEX_MAX_RESULT_WINDOW);
		}

		if (!query.getFields().isEmpty()) {
			searchRequestBuilder.setFetchSource(query.getFields().toArray(new String[0]), null);
		}

		if (query.getIndicesOptions() != null) {
			searchRequestBuilder.setIndicesOptions(query.getIndicesOptions());
		}

		if (query.isLimiting()) {
			searchRequestBuilder.setSize(query.getMaxResults());
		}

		if (query.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(query.getMinScore());
		}

		if (query.getPreference() != null) {
			searchRequestBuilder.setPreference(query.getPreference());
		}

		prepareSort(query, searchRequestBuilder, getPersistentEntity(clazz));

		HighlightBuilder highlightBuilder = highlightBuilder(query);

		if (highlightBuilder != null) {
			searchRequestBuilder.highlighter(highlightBuilder);
		}

		if (query instanceof NativeSearchQuery) {
			prepareNativeSearch(searchRequestBuilder, (NativeSearchQuery) query);
		}

		if (query.getTrackTotalHits()) {
			searchRequestBuilder.setTrackTotalHits(query.getTrackTotalHits());
		}

		if (StringUtils.hasLength(query.getRoute())) {
			searchRequestBuilder.setRouting(query.getRoute());
		}

		return searchRequestBuilder;
	}

	@SuppressWarnings("rawtypes")
	private void prepareSort(Query query, SearchSourceBuilder sourceBuilder,
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		if (query.getSort() != null) {
			query.getSort().forEach(order -> sourceBuilder.sort(getSortBuilder(order, entity)));
		}

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery nativeSearchQuery = (NativeSearchQuery) query;
			List<SortBuilder> sorts = nativeSearchQuery.getElasticsearchSorts();
			if (sorts != null) {
				sorts.forEach(sourceBuilder::sort);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void prepareSort(Query query, SearchRequestBuilder searchRequestBuilder,
			@Nullable ElasticsearchPersistentEntity<?> entity) {
		if (query.getSort() != null) {
			query.getSort().forEach(order -> searchRequestBuilder.addSort(getSortBuilder(order, entity)));
		}

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery nativeSearchQuery = (NativeSearchQuery) query;
			List<SortBuilder> sorts = nativeSearchQuery.getElasticsearchSorts();
			if (sorts != null) {
				sorts.forEach(searchRequestBuilder::addSort);
			}
		}
	}

	private SortBuilder<?> getSortBuilder(Sort.Order order, @Nullable ElasticsearchPersistentEntity<?> entity) {
		SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC;

		if (ScoreSortBuilder.NAME.equals(order.getProperty())) {
			return SortBuilders //
					.scoreSort() //
					.order(sortOrder);
		} else {
			ElasticsearchPersistentProperty property = (entity != null) //
					? entity.getPersistentProperty(order.getProperty()) //
					: null;
			String fieldName = property != null ? property.getFieldName() : order.getProperty();

			if (order instanceof GeoDistanceOrder) {
				GeoDistanceOrder geoDistanceOrder = (GeoDistanceOrder) order;

				GeoDistanceSortBuilder sort = SortBuilders.geoDistanceSort(fieldName, geoDistanceOrder.getGeoPoint().getLat(),
						geoDistanceOrder.getGeoPoint().getLon());

				sort.geoDistance(GeoDistance.fromString(geoDistanceOrder.getDistanceType().name()));
				sort.ignoreUnmapped(geoDistanceOrder.getIgnoreUnmapped());
				sort.sortMode(SortMode.fromString(geoDistanceOrder.getMode().name()));
				sort.unit(DistanceUnit.fromString(geoDistanceOrder.getUnit()));
				return sort;
			} else {
				FieldSortBuilder sort = SortBuilders //
						.fieldSort(fieldName) //
						.order(sortOrder);

				if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
					sort.missing("_first");
				} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
					sort.missing("_last");
				}
				return sort;
			}
		}
	}

	private QueryBuilder getQuery(Query query) {
		QueryBuilder elasticsearchQuery;

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery searchQuery = (NativeSearchQuery) query;
			elasticsearchQuery = searchQuery.getQuery();
		} else if (query instanceof CriteriaQuery) {
			CriteriaQuery criteriaQuery = (CriteriaQuery) query;
			elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		} else if (query instanceof StringQuery) {
			StringQuery stringQuery = (StringQuery) query;
			elasticsearchQuery = wrapperQuery(stringQuery.getSource());
		} else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return elasticsearchQuery;
	}

	public IndicesAliasesRequest indicesAddAliasesRequest(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest.AliasActions aliasAction = aliasAction(query, index);
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(aliasAction);
		return request;
	}

	public IndicesAliasesRequest indicesRemoveAliasesRequest(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.remove() //
				.index(index.getIndexName()) //
				.alias(query.getAliasName());

		return Requests.indexAliasesRequest() //
				.addAliasAction(aliasAction);
	}

	private QueryBuilder getFilter(Query query) {
		QueryBuilder elasticsearchFilter;

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery searchQuery = (NativeSearchQuery) query;
			elasticsearchFilter = searchQuery.getFilter();
		} else if (query instanceof CriteriaQuery) {
			CriteriaQuery criteriaQuery = (CriteriaQuery) query;
			elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());
		} else if (query instanceof StringQuery) {
			elasticsearchFilter = null;
		} else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return elasticsearchFilter;
	}

	@Nullable
	private ElasticsearchPersistentEntity<?> getPersistentEntity(@Nullable Class<?> clazz) {
		return clazz != null ? elasticsearchConverter.getMappingContext().getPersistentEntity(clazz) : null;
	}

	@Nullable
	private String getPersistentEntityId(Object entity) {

		Object identifier = elasticsearchConverter.getMappingContext() //
				.getRequiredPersistentEntity(entity.getClass()) //
				.getIdentifierAccessor(entity).getIdentifier();

		if (identifier != null) {
			return identifier.toString();
		}

		return null;
	}

	private VersionType retrieveVersionTypeFromPersistentEntity(Class<?> clazz) {

		if (clazz != null) {
			return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz).getVersionType();
		}
		return VersionType.EXTERNAL;
	}

	private String[] toArray(List<String> values) {
		String[] valuesAsArray = new String[values.size()];
		return values.toArray(valuesAsArray);
	}

}
