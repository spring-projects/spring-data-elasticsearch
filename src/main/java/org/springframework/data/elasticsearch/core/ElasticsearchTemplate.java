/*
 * Copyright 2013-2018 the original author or authors.
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

import org.elasticsearch.action.*;
import org.elasticsearch.action.admin.indices.alias.*;
import org.elasticsearch.action.admin.indices.alias.get.*;
import org.elasticsearch.action.admin.indices.create.*;
import org.elasticsearch.action.admin.indices.delete.*;
import org.elasticsearch.action.admin.indices.mapping.get.*;
import org.elasticsearch.action.admin.indices.mapping.put.*;
import org.elasticsearch.action.admin.indices.refresh.*;
import org.elasticsearch.action.admin.indices.settings.get.*;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.*;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.metadata.*;
import org.elasticsearch.common.*;
import org.elasticsearch.common.settings.*;
import org.elasticsearch.common.unit.*;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.suggest.*;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.*;
import org.springframework.data.elasticsearch.core.aggregation.*;
import org.springframework.data.elasticsearch.core.convert.*;
import org.springframework.data.elasticsearch.core.facet.*;
import org.springframework.data.elasticsearch.core.mapping.*;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.util.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static java.util.function.Function.*;
import static org.elasticsearch.client.Requests.*;
import static org.elasticsearch.index.VersionType.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.util.CollectionUtils.*;

/**
 * ElasticsearchTemplate
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Kevin Leturc
 * @author Mason Chan
 * @author Young Gu
 * @author Oliver Gierke
 * @author Mark Janssen
 * @author Chris White
 * @author Mark Paluch
 * @author Ilkang Na
 * @author Alen Turkovic
 * @author Sascha Woo
 * @author Ted Liang
 * @author Jean-Baptiste Nizet
 * @author Nikita Guchakov
 */
public class ElasticsearchTemplate extends AbstractElasticTemplate
		implements ElasticsearchOperations, EsClient<Client> {

	private static final Logger QUERY_LOGGER = LoggerFactory
			.getLogger("org.springframework.data.elasticsearch.core.QUERY");

	private String searchTimeout;
	private Client client;

	public ElasticsearchTemplate(Client client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ElasticsearchTemplate(Client client, EntityMapper entityMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), entityMapper);
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter,
			EntityMapper entityMapper) {
		this(client, elasticsearchConverter,
				new DefaultResultMapper(elasticsearchConverter.getMappingContext(), entityMapper));
	}

	public ElasticsearchTemplate(Client client, ResultsMapper resultsMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), resultsMapper);
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter) {
		this(client, elasticsearchConverter, new DefaultResultMapper(elasticsearchConverter.getMappingContext()));
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter,
			ResultsMapper resultsMapper) {
		super(elasticsearchConverter, resultsMapper);
		Assert.notNull(client, "Client must not be null!");
		this.client = client;
	}

	@Override
	public Client getClient() {
		return client;
	}

	public void setSearchTimeout(String searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	@Override
	protected boolean createIndex(CreateIndexRequest indexRequest) {
		return client.admin().indices().create(indexRequest).actionGet().isAcknowledged();
	}

	@Override
	public boolean putMapping(String indexName, String type, Object mapping) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");
		PutMappingRequestBuilder requestBuilder = client.admin().indices().preparePutMapping(indexName).setType(type);
		if (mapping instanceof String) {
			requestBuilder.setSource(String.valueOf(mapping), XContentType.JSON);
		} else if (mapping instanceof Map) {
			requestBuilder.setSource((Map) mapping);
		} else if (mapping instanceof XContentBuilder) {
			requestBuilder.setSource((XContentBuilder) mapping);
		}
		return requestBuilder.execute().actionGet().isAcknowledged();
	}

	// impl
	@Override
	public Map getMapping(String indexName, String type) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");
		try {
			return client.admin().indices().getMappings(new GetMappingsRequest().indices(indexName).types(type)).actionGet()
					.getMappings().get(indexName).get(type).getSourceAsMap();
		} catch (Exception e) {
			throw new ElasticsearchException(
					"Error while getting mapping for indexName : " + indexName + " type : " + type + " " + e.getMessage());
		}
	}

	@Override
	protected GetResponse queryForObject(GetQuery query, String indexName, String indexType) {
		return client.prepareGet(indexName, indexType, query.getId()).execute().actionGet();
	}

	@Override
	public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
		SearchResponse response = doSearch(prepareSearch(query), query);
		return resultsExtractor.extract(response);
	}

	@Override
	SearchResponse getIdsResponse(SearchQuery query) {
		SearchRequestBuilder request = prepareSearch(query).setQuery(query.getQuery());
		if (query.getFilter() != null) {
			request.setPostFilter(query.getFilter());
		}
		return getSearchResponse(request);
	}

	@Override
	public <T> Page<T> queryForPage(CriteriaQuery criteriaQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());
		SearchRequestBuilder searchRequestBuilder = prepareSearch(criteriaQuery, clazz);

		if (elasticsearchQuery != null) {
			searchRequestBuilder.setQuery(elasticsearchQuery);
		} else {
			searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
		}

		if (criteriaQuery.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(criteriaQuery.getMinScore());
		}

		if (elasticsearchFilter != null)
			searchRequestBuilder.setPostFilter(elasticsearchFilter);

		SearchResponse response = getSearchResponse(searchRequestBuilder);
		return getResultsMapper().mapResults(response, clazz, criteriaQuery.getPageable());
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {
		WrapperQueryBuilder queryBuilder = wrapperQuery(query.getSource());
		SearchRequestBuilder requestBuilder = prepareSearch(query, clazz).setQuery(queryBuilder);
		SearchResponse response = getSearchResponse(requestBuilder);
		return mapper.mapResults(response, clazz, query.getPageable());
	}

	@Override
	public <T> long count(CriteriaQuery criteriaQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());

		if (elasticsearchFilter == null) {
			return doCount(prepareCount(criteriaQuery, clazz), elasticsearchQuery);
		} else {
			// filter could not be set into CountRequestBuilder, convert request into search request
			return doCount(prepareSearch(criteriaQuery, clazz), elasticsearchQuery, elasticsearchFilter);
		}
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
		SearchResponse response = doSearch(prepareSearch(query, clazz), query);
		return mapper.mapResults(response, clazz, query.getPageable());
	}

	@Override
	public <T> long count(SearchQuery searchQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = searchQuery.getQuery();
		QueryBuilder elasticsearchFilter = searchQuery.getFilter();

		if (elasticsearchFilter == null) {
			return doCount(prepareCount(searchQuery, clazz), elasticsearchQuery);
		} else {
			// filter could not be set into CountRequestBuilder, convert request into search request
			return doCount(prepareSearch(searchQuery, clazz), elasticsearchQuery, elasticsearchFilter);
		}
	}

	private long doCount(SearchRequestBuilder countRequestBuilder, QueryBuilder elasticsearchQuery) {

		if (elasticsearchQuery != null) {
			countRequestBuilder.setQuery(elasticsearchQuery);
		}
		return countRequestBuilder.execute().actionGet().getHits().getTotalHits();
	}

	private long doCount(SearchRequestBuilder searchRequestBuilder, QueryBuilder elasticsearchQuery,
			QueryBuilder elasticsearchFilter) {
		if (elasticsearchQuery != null) {
			searchRequestBuilder.setQuery(elasticsearchQuery);
		} else {
			searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
		}
		if (elasticsearchFilter != null) {
			searchRequestBuilder.setPostFilter(elasticsearchFilter);
		}
		return searchRequestBuilder.execute().actionGet().getHits().getTotalHits();
	}

	private <T> SearchRequestBuilder prepareCount(Query query, Class<T> clazz) {
		String[] indexName = isEmpty(query.getIndices()) ? retrieveIndexNameFromPersistentEntity(clazz)
				: query.getIndices().toArray(new String[0]);
		Assert.notNull(indexName, "No index defined for Query");
		String[] types = isEmpty(query.getTypes()) ? retrieveTypeFromPersistentEntity(clazz)
				: query.getTypes().toArray(new String[0]);

		SearchRequestBuilder countRequestBuilder = client.prepareSearch(indexName);

		if (types != null) {
			countRequestBuilder.setTypes(types);
		}
		countRequestBuilder.setSize(0);
		return countRequestBuilder;
	}

	@Override
	MultiGetResponse executeMultiRequest(Stream<MultiGetRequest.Item> itemStream) {
		MultiGetRequestBuilder builder = client.prepareMultiGet();
		itemStream.forEachOrdered(builder::add);
		return builder.execute().actionGet();
	}

	@Override
	public String index(IndexQuery query) {
		String documentId = prepareIndex(query).execute().actionGet().getId();
		// We should call this because we are not going through a mapper.
		if (query.getObject() != null) {
			setPersistentEntityId(query.getObject(), documentId);
		}
		return documentId;
	}

	@Override
	public UpdateResponse update(UpdateQuery query) {
		return this.prepareUpdate(query).execute().actionGet();
	}

	private UpdateRequestBuilder prepareUpdate(UpdateQuery query) {
		String indexName = !StringUtils.isEmpty(query.getIndexName()) ? query.getIndexName()
				: getPersistentEntityFor(query.getClazz()).getIndexName();
		String type = !StringUtils.isEmpty(query.getType()) ? query.getType()
				: getPersistentEntityFor(query.getClazz()).getIndexType();
		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notNull(query.getId(), "No Id define for Query");
		Assert.notNull(query.getUpdateRequest(), "No IndexRequest define for Query");
		UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(indexName, type, query.getId());
		updateRequestBuilder.setRouting(query.getUpdateRequest().routing());

		if (query.getUpdateRequest().script() == null) {
			// doc
			if (query.DoUpsert()) {
				updateRequestBuilder.setDocAsUpsert(true).setDoc(query.getUpdateRequest().doc());
			} else {
				updateRequestBuilder.setDoc(query.getUpdateRequest().doc());
			}
		} else {
			// or script
			updateRequestBuilder.setScript(query.getUpdateRequest().script());
		}

		return updateRequestBuilder;
	}

	@Override
	public void bulkIndex(List<IndexQuery> queries) {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (IndexQuery query : queries) {
			bulkRequest.add(prepareIndex(query));
		}
		BulkFailsHandler.checkForBulkUpdateFailure(bulkRequest.execute().actionGet());
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries) {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (UpdateQuery query : queries) {
			bulkRequest.add(prepareUpdate(query));
		}
		BulkFailsHandler.checkForBulkUpdateFailure(bulkRequest.execute().actionGet());
	}

	@Override
	public boolean indexExists(String indexName) {
		return client.admin().indices().exists(indicesExistsRequest(indexName)).actionGet().isExists();
	}

	@Override
	public boolean typeExists(String index, String type) {
		return client.admin().cluster().prepareState().execute().actionGet().getState().metaData().index(index)
				.getMappings().containsKey(type);
	}

	@Override
	public boolean deleteIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for delete operation");
		if (!indexExists(indexName)) {
			return false;
		}
		return client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet().isAcknowledged();
	}

	@Override
	public String delete(String indexName, String type, String id) {
		return client.prepareDelete(indexName, type, id).execute().actionGet().getId();
	}

	@Override
	protected void bulkDelete(String indexName, String typeName, List<String> ids) {
		BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
		for (String id : ids) {
			bulkRequestBuilder.add(client.prepareDelete(indexName, typeName, id));
		}

		if (bulkRequestBuilder.numberOfActions() > 0) {
			bulkRequestBuilder.execute().actionGet();
		}
	}

	private <T> SearchRequestBuilder prepareScroll(Query query, long scrollTimeInMillis, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareScroll(query, scrollTimeInMillis);
	}

	private SearchRequestBuilder prepareScroll(Query query, long scrollTimeInMillis) {
		SearchRequestBuilder requestBuilder = client.prepareSearch(toArray(query.getIndices()))
				.setTypes(toArray(query.getTypes())).setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).setFrom(0)
				.setVersion(true);

		if (query.getPageable().isPaged()) {
			requestBuilder.setSize(query.getPageable().getPageSize());
		}

		if (!isEmpty(query.getFields())) {
			requestBuilder.setFetchSource(toArray(query.getFields()), null);
		}
		return requestBuilder;
	}

	@Override
	protected SearchResponse scroll(Class<?> clazz, long scrollTimeInMillis, SearchQuery searchQuery) {
		SearchRequestBuilder requestBuilder = prepareScroll(searchQuery, scrollTimeInMillis, clazz);
		if (searchQuery.getFilter() != null) {
			requestBuilder.setPostFilter(searchQuery.getFilter());
		}

		return getSearchResponse(requestBuilder.setQuery(searchQuery.getQuery()));
	}

	@Override
	protected SearchResponse scroll(Class<?> clazz, long scrollTimeInMillis, CriteriaQuery criteriaQuery,
			QueryBuilder elasticsearchQuery, QueryBuilder elasticsearchFilter) {
		SearchRequestBuilder requestBuilder = prepareScroll(criteriaQuery, scrollTimeInMillis, clazz);
		requestBuilder.setQuery(elasticsearchQuery != null ? elasticsearchQuery : QueryBuilders.matchAllQuery());

		if (elasticsearchFilter != null) {
			requestBuilder.setPostFilter(elasticsearchFilter);
		}

		return getSearchResponse(requestBuilder);
	}

	@Override
	public <T> Page<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchResponse response = getSearchResponse(
				client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute());
		return mapper.mapResults(response, clazz, Pageable.unpaged());
	}

	@Override
	public void clearScroll(String scrollId) {
		client.prepareClearScroll().addScrollId(scrollId).execute().actionGet();
	}

	private SearchResponse doSearch(SearchRequestBuilder searchRequest, SearchQuery searchQuery) {
		if (searchQuery.getFilter() != null) {
			searchRequest.setPostFilter(searchQuery.getFilter());
		}

		if (!isEmpty(searchQuery.getElasticsearchSorts())) {
			searchQuery.getElasticsearchSorts().forEach(searchRequest::addSort);
		}

		if (!searchQuery.getScriptFields().isEmpty()) {
			// _source should be return all the time
			// searchRequest.addStoredField("_source");
			searchQuery.getScriptFields()
					.forEach(scriptedField -> searchRequest.addScriptField(scriptedField.fieldName(), scriptedField.script()));
		}

		if (searchQuery.getHighlightFields() != null || searchQuery.getHighlightBuilder() != null) {
			searchRequest.highlighter(highlightFromQuery(searchQuery));
		}

		if (!isEmpty(searchQuery.getIndicesBoost())) {
			searchQuery.getIndicesBoost()
					.forEach(indexBoost -> searchRequest.addIndexBoost(indexBoost.getIndexName(), indexBoost.getBoost()));
		}

		if (!isEmpty(searchQuery.getAggregations())) {
			searchQuery.getAggregations().forEach(searchRequest::addAggregation);
		}

		if (!isEmpty(searchQuery.getFacets())) {
			searchQuery.getFacets().stream().map(FacetRequest::getFacet).forEach(searchRequest::addAggregation);
		}
		return getSearchResponse(searchRequest.setQuery(searchQuery.getQuery()));
	}

	private SearchResponse getSearchResponse(SearchRequestBuilder requestBuilder) {
		QUERY_LOGGER.debug("{}", requestBuilder);
		return getSearchResponse(requestBuilder.execute());
	}

	private SearchResponse getSearchResponse(ActionFuture<SearchResponse> response) {
		return searchTimeout == null ? response.actionGet() : response.actionGet(searchTimeout);
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {
		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);
		if (settings instanceof String) {
			createIndexRequestBuilder.setSettings(String.valueOf(settings), Requests.INDEX_CONTENT_TYPE);
		} else if (settings instanceof Map) {
			createIndexRequestBuilder.setSettings((Map) settings);
		} else if (settings instanceof XContentBuilder) {
			createIndexRequestBuilder.setSettings((XContentBuilder) settings);
		}
		return createIndexRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	public Map getSetting(String indexName) {
		Assert.notNull(indexName, "No index defined for getSettings");
		Settings settings = client.admin().indices().getSettings(new GetSettingsRequest()).actionGet().getIndexToSettings()
				.get(indexName);
		return settings.keySet().stream().collect(Collectors.toMap(identity(), settings::get));
	}

	private <T> SearchRequestBuilder prepareSearch(Query query, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareSearch(query);
	}

	private SearchRequestBuilder prepareSearch(Query query) {
		Assert.notNull(query.getIndices(), "No index defined for Query");
		Assert.notNull(query.getTypes(), "No type defined for Query");

		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(toArray(query.getIndices()))
				.setSearchType(query.getSearchType()).setTypes(toArray(query.getTypes())).setVersion(true)
				.setRouting(query.getRoute()).setTrackScores(query.getTrackScores());

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			searchRequestBuilder.setFetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		int startRecord = 0;
		if (query.getPageable().isPaged()) {
			startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
			searchRequestBuilder.setSize(query.getPageable().getPageSize());
		}
		searchRequestBuilder.setFrom(startRecord);

		if (!query.getFields().isEmpty()) {
			searchRequestBuilder.setFetchSource(toArray(query.getFields()), null);
		}

		if (query.getIndicesOptions() != null) {
			searchRequestBuilder.setIndicesOptions(query.getIndicesOptions());
		}

		if (query.getSort() != null) {
			query.getSort().forEach(order -> searchRequestBuilder.addSort(toSortBuilder(order)));
		}

		if (query.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(query.getMinScore());
		}
		return searchRequestBuilder;
	}

	private IndexRequestBuilder prepareIndex(IndexQuery query) {
		try {
			String indexName = StringUtils.isEmpty(query.getIndexName())
					? retrieveIndexNameFromPersistentEntity(query.getObject().getClass())[0]
					: query.getIndexName();
			String type = StringUtils.isEmpty(query.getType())
					? retrieveTypeFromPersistentEntity(query.getObject().getClass())[0]
					: query.getType();

			IndexRequestBuilder indexRequestBuilder;
			if (query.getObject() != null) {
				String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
				indexRequestBuilder = client.prepareIndex(indexName, type, id);
				indexRequestBuilder.setSource(getResultsMapper().getEntityMapper().mapToString(query.getObject()),
						Requests.INDEX_CONTENT_TYPE);
			} else if (query.getSource() != null) {
				indexRequestBuilder = client.prepareIndex(indexName, type, query.getId()).setSource(query.getSource(),
						Requests.INDEX_CONTENT_TYPE);
			} else {
				throw new ElasticsearchException(
						"object or source is null, failed to index the document [id: " + query.getId() + "]");
			}
			if (query.getVersion() != null) {
				indexRequestBuilder.setVersion(query.getVersion());
				indexRequestBuilder.setVersionType(EXTERNAL);
			}

			if (query.getParentId() != null) {
				indexRequestBuilder.setParent(query.getParentId());
			}

			return indexRequestBuilder;
		} catch (IOException e) {
			throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
		}
	}

	@Override
	void refresh(RefreshRequest request) {
		client.admin().indices().refresh(request).actionGet();
	}

	@Override
	public Boolean addAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		final IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.add()
				.alias(query.getAliasName()).index(query.getIndexName());

		if (query.getFilterBuilder() != null) {
			aliasAction.filter(query.getFilterBuilder());
		} else if (query.getFilter() != null) {
			aliasAction.filter(query.getFilter());
		} else if (!StringUtils.isEmpty(query.getRouting())) {
			aliasAction.routing(query.getRouting());
		} else if (!StringUtils.isEmpty(query.getSearchRouting())) {
			aliasAction.searchRouting(query.getSearchRouting());
		} else if (!StringUtils.isEmpty(query.getIndexRouting())) {
			aliasAction.indexRouting(query.getIndexRouting());
		}
		return client.admin().indices().prepareAliases().addAliasAction(aliasAction).execute().actionGet().isAcknowledged();
	}

	@Override
	public Boolean removeAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		return client.admin().indices().prepareAliases().removeAlias(query.getIndexName(), query.getAliasName()).execute()
				.actionGet().isAcknowledged();
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		return client.admin().indices().getAliases(new GetAliasesRequest().indices(indexName)).actionGet().getAliases()
				.get(indexName);
	}

	@Override
	public SearchResponse suggest(SuggestBuilder suggestion, String... indices) {
		return client.prepareSearch(indices).suggest(suggestion).get();
	}

}
