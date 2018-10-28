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

import static java.util.stream.Collectors.*;
import static org.elasticsearch.index.VersionType.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.*;

import lombok.extern.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import org.apache.http.util.*;
import org.elasticsearch.action.*;
import org.elasticsearch.action.admin.indices.alias.*;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.*;
import org.elasticsearch.action.admin.indices.create.*;
import org.elasticsearch.action.admin.indices.delete.*;
import org.elasticsearch.action.admin.indices.get.*;
import org.elasticsearch.action.admin.indices.mapping.put.*;
import org.elasticsearch.action.admin.indices.refresh.*;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.delete.*;
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
import org.elasticsearch.search.builder.*;
import org.elasticsearch.search.sort.*;
import org.elasticsearch.search.suggest.*;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.*;
import org.springframework.data.elasticsearch.core.aggregation.*;
import org.springframework.data.elasticsearch.core.client.support.*;
import org.springframework.data.elasticsearch.core.convert.*;
import org.springframework.data.elasticsearch.core.mapping.*;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.util.*;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;

/**
 * ElasticsearchRestTemplate
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
 * @author Don Wellington
 */
@Slf4j
public class ElasticsearchRestTemplate extends AbstractElasticTemplate
		implements ElasticsearchOperations, EsClient<RestHighLevelClient> {

	private RestHighLevelClient client;
	private String searchTimeout;

	public ElasticsearchRestTemplate(RestHighLevelClient client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, EntityMapper entityMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), entityMapper);
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter,
			EntityMapper entityMapper) {
		this(client, elasticsearchConverter,
				new DefaultResultMapper(elasticsearchConverter.getMappingContext(), entityMapper));
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ResultsMapper resultsMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), resultsMapper);
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {
		this(client, elasticsearchConverter, new DefaultResultMapper(elasticsearchConverter.getMappingContext()));
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter,
			ResultsMapper resultsMapper) {
		super(elasticsearchConverter, resultsMapper);
		Assert.notNull(elasticsearchConverter, "ElasticsearchConverter must not be null!");
		this.client = client;
	}

	public void setSearchTimeout(String searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	@Override
	public RestHighLevelClient getClient() {
		return client;
	}

	@Override
	public boolean createIndex(CreateIndexRequest indexRequest) throws IOException {
		return client.indices().create(indexRequest).isAcknowledged();
	}

	@Override
	public boolean putMapping(String indexName, String type, Object mapping) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");
		PutMappingRequest request = new PutMappingRequest(indexName).type(type);
		if (mapping instanceof String) {
			request.source(String.valueOf(mapping), XContentType.JSON);
		} else if (mapping instanceof Map) {
			request.source((Map) mapping);
		} else if (mapping instanceof XContentBuilder) {
			request.source((XContentBuilder) mapping);
		}
		try {
			return client.indices().putMapping(request).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Failed to put mapping for " + indexName, e);
		}
	}

	@Override
	public Map getMapping(String indexName, String type) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");
		RestClient restClient = client.getLowLevelClient();
		try {
			Response response = restClient.performRequest("GET", "/" + indexName + "/_mapping/" + type);
			return convertMappingResponse(EntityUtils.toString(response.getEntity()));
		} catch (Exception e) {
			throw new ElasticsearchException(
					"Error while getting mapping for indexName : " + indexName + " type : " + type + " ", e);
		}
	}

	private Map<String, Object> convertMappingResponse(String mappingResponse) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode node = mapper.readTree(mappingResponse).findValue("settings");
			return mapper.readValue(mapper.writeValueAsString(node), HashMap.class);
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + mappingResponse, e);
		}

	}

	@Override
	protected GetResponse queryForObject(GetQuery query, String indexName, String indexType) {
		GetRequest request = new GetRequest(indexName, indexType, query.getId());
		try {
			return client.get(request);
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting for request: " + request.toString(), e);
		}
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
		SearchResponse response = doSearch(prepareSearch(query, clazz), query);
		return mapper.mapResults(response, clazz, query.getPageable());
	}

	@Override
	public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
		SearchResponse response = doSearch(prepareSearch(query, Optional.ofNullable(query.getQuery())), query);
		return resultsExtractor.extract(response);
	}

	@Override
	SearchResponse getIdsResponse(SearchQuery query) {
		SearchRequest request = prepareSearch(query, Optional.ofNullable(query.getQuery()));
		request.source().query(query.getQuery());
		if (query.getFilter() != null) {
			request.source().postFilter(query.getFilter());
		}
		return search(request);
	}

	@Override
	public <T> Page<T> queryForPage(CriteriaQuery criteriaQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());

		SearchRequest request = prepareSearch(criteriaQuery, clazz).routing(criteriaQuery.getRoute());
		request.source().query(elasticsearchQuery != null ? elasticsearchQuery : QueryBuilders.matchAllQuery());

		if (criteriaQuery.getMinScore() > 0) {
			request.source().minScore(criteriaQuery.getMinScore());
		}

		if (elasticsearchFilter != null) {
			request.source().postFilter(elasticsearchFilter);
		}
		log.debug("doSearch query:\n{}", request);

		SearchResponse response = search(request);
		return getResultsMapper().mapResults(response, clazz, criteriaQuery.getPageable());
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper resultMapper) {
		SearchRequest request = prepareSearch(query, clazz);
		request.source().query(wrapperQuery(query.getSource()));
		SearchResponse response = search(request);
		return resultMapper.mapResults(response, clazz, query.getPageable());
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

	private long doCount(SearchRequest countRequest, QueryBuilder elasticsearchQuery) {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		if (elasticsearchQuery != null) {
			sourceBuilder.query(elasticsearchQuery);
		}
		countRequest.source(sourceBuilder);

		try {
			return client.search(countRequest).getHits().getTotalHits();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while searching for request: " + countRequest.toString(), e);
		}
	}

	private long doCount(SearchRequest searchRequest, QueryBuilder elasticsearchQuery, QueryBuilder elasticsearchFilter) {
		searchRequest.source().query(elasticsearchQuery != null ? elasticsearchQuery : QueryBuilders.matchAllQuery());
		if (elasticsearchFilter != null) {
			searchRequest.source().postFilter(elasticsearchFilter);
		}
		try {
			return client.search(searchRequest).getHits().getTotalHits();
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + searchRequest.toString(), e);
		}
	}

	private <T> SearchRequest prepareCount(Query query, Class<T> clazz) {
		String[] indexName = isEmpty(query.getIndices()) ? retrieveIndexNameFromPersistentEntity(clazz)
				: query.getIndices().toArray(new String[0]);
		Assert.notNull(indexName, "No index defined for Query");

		String[] types = isEmpty(query.getTypes()) ? retrieveTypeFromPersistentEntity(clazz)
				: query.getTypes().toArray(new String[0]);

		SearchRequest countRequestBuilder = new SearchRequest(indexName);
		if (types != null) {
			countRequestBuilder.types(types);
		}
		return countRequestBuilder;
	}

	@Override
	MultiGetResponse executeMultiRequest(Stream<MultiGetRequest.Item> items) {
		MultiGetRequest request = new MultiGetRequest();
		items.forEachOrdered(request::add);
		try {
			return client.multiGet(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while multiget for request: " + request.toString(), e);
		}
	}

	@Override
	public String index(IndexQuery query) {
		String documentId;
		IndexRequest request = prepareIndex(query);
		try {
			documentId = client.index(request).getId();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while index for request: " + request.toString(), e);
		}
		// We should call this because we are not going through a mapper.
		if (query.getObject() != null) {
			setPersistentEntityId(query.getObject(), documentId);
		}
		return documentId;
	}

	@Override
	public UpdateResponse update(UpdateQuery query) {
		UpdateRequest request = prepareUpdate(query);
		try {
			return client.update(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while update for request: " + request.toString(), e);
		}
	}

	private UpdateRequest prepareUpdate(UpdateQuery query) {
		String indexName = !StringUtils.isEmpty(query.getIndexName()) ? query.getIndexName()
				: getPersistentEntityFor(query.getClazz()).getIndexName();
		String type = !StringUtils.isEmpty(query.getType()) ? query.getType()
				: getPersistentEntityFor(query.getClazz()).getIndexType();
		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notNull(query.getId(), "No Id define for Query");
		Assert.notNull(query.getUpdateRequest(), "No IndexRequest define for Query");
		UpdateRequest updateRequest = new UpdateRequest(indexName, type, query.getId());
		updateRequest.routing(query.getUpdateRequest().routing());

		if (query.getUpdateRequest().script() == null) {
			// doc
			if (query.DoUpsert()) {
				updateRequest.docAsUpsert(true).doc(query.getUpdateRequest().doc());
			} else {
				updateRequest.doc(query.getUpdateRequest().doc());
			}
		} else {
			// or script
			updateRequest.script(query.getUpdateRequest().script());
		}

		return updateRequest;
	}

	@Override
	public void bulkIndex(List<IndexQuery> queries) {
		BulkRequest bulkRequest = new BulkRequest();
		for (IndexQuery query : queries) {
			bulkRequest.add(prepareIndex(query));
		}
		try {
			BulkFailsHandler.checkForBulkUpdateFailure(client.bulk(bulkRequest));
		} catch (IOException e) {
			throw new ElasticsearchException("Error while bulk for request: " + bulkRequest.toString(), e);
		}
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries) {
		BulkRequest bulkRequest = new BulkRequest();
		for (UpdateQuery query : queries) {
			bulkRequest.add(prepareUpdate(query));
		}
		try {
			BulkFailsHandler.checkForBulkUpdateFailure(client.bulk(bulkRequest));
		} catch (IOException e) {
			throw new ElasticsearchException("Error while bulk for request: " + bulkRequest.toString(), e);
		}
	}

	@Override
	public <T> boolean indexExists(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public boolean indexExists(String indexName) {
		GetIndexRequest request = new GetIndexRequest();
		request.indices(indexName);
		try {
			return client.indices().exists(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while for indexExists request: " + request.toString(), e);
		}
	}

	@Override
	public boolean typeExists(String index, String type) {
		RestClient restClient = client.getLowLevelClient();
		try {
			Response response = restClient.performRequest("HEAD", index + "/_mapping/" + type);
			return (response.getStatusLine().getStatusCode() == 200);
		} catch (Exception e) {
			throw new ElasticsearchException("Error while checking type exists for index: " + index + " type : " + type + " ",
					e);
		}
	}

	@Override
	public boolean deleteIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for delete operation");
		if (!indexExists(indexName)) {
			return false;
		}
		DeleteIndexRequest request = new DeleteIndexRequest(indexName);
		try {
			return client.indices().delete(request).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while deleting index request: " + request.toString(), e);
		}
	}

	@Override
	public String delete(String indexName, String type, String id) {
		DeleteRequest request = new DeleteRequest(indexName, type, id);
		try {
			return client.delete(request).getId();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while deleting item request: " + request.toString(), e);
		}
	}

	@Override
	protected void bulkDelete(String indexName, String typeName, List<String> ids) {
		BulkRequest request = new BulkRequest();
		ids.stream().map(id -> new DeleteRequest(indexName, typeName, id)).forEach(request::add);
		if (request.numberOfActions() > 0) {
			try {
				BulkResponse response = client.bulk(request);
				BulkFailsHandler.checkForBulkUpdateFailure(response);
			} catch (IOException e) {
				throw new ElasticsearchException("Error while deleting bulk: " + request.toString(), e);
			}
		}
	}

	private <T> SearchRequest prepareScroll(Query query, long scrollTimeInMillis, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareScroll(query, scrollTimeInMillis);
	}

	private SearchRequest prepareScroll(Query query, long scrollTimeInMillis) {
		SearchRequest request = new SearchRequest(toArray(query.getIndices()));
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		request.types(toArray(query.getTypes()));
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		if (query.getPageable().isPaged()) {
			searchSourceBuilder.size(query.getPageable().getPageSize());
		}

		if (!isEmpty(query.getFields())) {
			searchSourceBuilder.fetchSource(toArray(query.getFields()), null);
		}
		request.source(searchSourceBuilder);
		return request;
	}

	@Override
	protected SearchResponse scroll(Class<?> clazz, long scrollTimeInMillis, CriteriaQuery criteriaQuery,
			QueryBuilder elasticsearchQuery, QueryBuilder elasticsearchFilter) {
		SearchRequest request = prepareScroll(criteriaQuery, scrollTimeInMillis, clazz);
		if (elasticsearchQuery != null) {
			request.source().query(elasticsearchQuery);
		} else {
			request.source().query(QueryBuilders.matchAllQuery());
		}

		if (elasticsearchFilter != null) {
			request.source().postFilter(elasticsearchFilter);
		}
		request.source().version(true);

		try {
			return client.search(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
	}

	@Override
	protected SearchResponse scroll(Class<?> clazz, long scrollTimeMillis, SearchQuery searchQuery) {
		SearchRequest request = prepareScroll(searchQuery, scrollTimeMillis, clazz);
		if (searchQuery.getFilter() != null) {
			request.source().postFilter(searchQuery.getFilter());
		}
		request.source().version(true);

		try {
			return client.search(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
	}

	@Override
	public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz) {
		SearchResponse response = doScroll(clazz, scrollTimeInMillis, searchQuery);
		return getResultsMapper().mapResults(response, clazz, Pageable.unpaged());
	}

	@Override
	public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchResponse response = doScroll(clazz, scrollTimeInMillis, searchQuery);
		return mapper.mapResults(response, clazz, Pageable.unpaged());
	}

	public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchResponse response = doScroll(clazz, scrollTimeInMillis, criteriaQuery);
		return mapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> Page<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));
		SearchResponse response;
		try {
			response = client.searchScroll(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
		return mapper.mapResults(response, clazz, Pageable.unpaged());
	}

	@Override
	public void clearScroll(String scrollId) {
		ClearScrollRequest request = new ClearScrollRequest();
		request.addScrollId(scrollId);
		try {
			// TODO: Something useful with the response.
			ClearScrollResponse response = client.clearScroll(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
	}

	private SearchResponse doSearch(SearchRequest searchRequest, SearchQuery searchQuery) {
		if (searchQuery.getFilter() != null) {
			searchRequest.source().postFilter(searchQuery.getFilter());
		}

		if (!isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder sort : searchQuery.getElasticsearchSorts()) {
				searchRequest.source().sort(sort);
			}
		}

		if (!searchQuery.getScriptFields().isEmpty()) {
			// _source should be return all the time
			// searchRequest.addStoredField("_source");
			searchQuery.getScriptFields().forEach(
					scriptedField -> searchRequest.source().scriptField(scriptedField.fieldName(), scriptedField.script()));
		}

		if (searchQuery.getHighlightFields() != null || searchQuery.getHighlightBuilder() != null) {
			searchRequest.source().highlighter(highlightFromQuery(searchQuery));
		}

		if (!isEmpty(searchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : searchQuery.getIndicesBoost()) {
				searchRequest.source().indexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(searchQuery.getAggregations())) {
			searchQuery.getAggregations()
					.forEach(aggregationBuilder -> searchRequest.source().aggregation(aggregationBuilder));
		}

		if (!isEmpty(searchQuery.getFacets())) {
			searchQuery.getFacets()
					.forEach(aggregatedFacet -> searchRequest.source().aggregation(aggregatedFacet.getFacet()));
		}

		try {
			return client.search(searchRequest);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + searchRequest.toString(), e);
		}
	}

	private SearchResponse getSearchResponse(ActionFuture<SearchResponse> response) {
		return searchTimeout == null ? response.actionGet() : response.actionGet(searchTimeout);
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		if (settings instanceof String) {
			request.settings(String.valueOf(settings), Requests.INDEX_CONTENT_TYPE);
		} else if (settings instanceof Map) {
			request.settings((Map) settings);
		} else if (settings instanceof XContentBuilder) {
			request.settings((XContentBuilder) settings);
		}
		try {
			return createIndex(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for creating index: " + request.toString(), e);
		}
	}

	@Override // TODO change interface to return Settings.
	public Map getSetting(String indexName) {
		Assert.notNull(indexName, "No index defined for getSettings");
		RestClient restClient = client.getLowLevelClient();
		try {
			Response response = restClient.performRequest("GET", "/" + indexName + "/_settings");
			return convertSettingResponse(EntityUtils.toString(response.getEntity()), indexName);
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting settings for indexName : " + indexName, e);
		}
	}

	private Map<String, String> convertSettingResponse(String settingResponse, String indexName) {
		try {
			Settings settings = Settings.fromXContent(XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY,
					DeprecationHandler.THROW_UNSUPPORTED_OPERATION, settingResponse));
			int prefixLength = indexName.length() + ".settings.".length();
			// Backwards compatibility. TODO Change to return Settings object.
			return settings.keySet().stream().collect(toMap(key -> key.substring(prefixLength), settings::get, (a, b) -> b));
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + settingResponse, e);
		}

	}

	private <T> SearchRequest prepareSearch(Query query, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareSearch(query, Optional.empty());
	}

	private <T> SearchRequest prepareSearch(SearchQuery query, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareSearch(query, Optional.ofNullable(query.getQuery()));
	}

	private SearchRequest prepareSearch(Query query, Optional<QueryBuilder> builder) {
		Assert.notNull(query.getIndices(), "No index defined for Query");
		Assert.notNull(query.getTypes(), "No type defined for Query");

		SearchRequest request = new SearchRequest(toArray(query.getIndices()));
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		request.types(toArray(query.getTypes()));
		request.routing(query.getRoute());
		sourceBuilder.version(true);
		sourceBuilder.trackScores(query.getTrackScores());

		builder.ifPresent(sourceBuilder::query);
		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			sourceBuilder.fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		int startRecord = 0;
		if (query.getPageable().isPaged()) {
			startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
			sourceBuilder.size(query.getPageable().getPageSize());
		}
		sourceBuilder.from(startRecord);

		if (!query.getFields().isEmpty()) {
			sourceBuilder.fetchSource(toArray(query.getFields()), null);
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(query.getIndicesOptions());
		}

		if (query.getSort() != null) {
			query.getSort().forEach(order -> sourceBuilder.sort(toSortBuilder(order)));
		}

		if (query.getMinScore() > 0) {
			sourceBuilder.minScore(query.getMinScore());
		}
		request.source(sourceBuilder);
		return request;
	}

	private IndexRequest prepareIndex(IndexQuery query) {
		try {
			String indexName = StringUtils.isEmpty(query.getIndexName())
					? retrieveIndexNameFromPersistentEntity(query.getObject().getClass())[0]
					: query.getIndexName();
			String type = StringUtils.isEmpty(query.getType())
					? retrieveTypeFromPersistentEntity(query.getObject().getClass())[0]
					: query.getType();

			IndexRequest indexRequest;
			if (query.getObject() != null) {
				String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
				indexRequest = new IndexRequest(indexName, type, id);
				indexRequest.source(getResultsMapper().getEntityMapper().mapToString(query.getObject()),
						Requests.INDEX_CONTENT_TYPE);
			} else if (query.getSource() != null) {
				indexRequest = new IndexRequest(indexName, type, query.getId()).source(query.getSource(),
						Requests.INDEX_CONTENT_TYPE);
			} else {
				throw new ElasticsearchException(
						"object or source is null, failed to index the document [id: " + query.getId() + "]");
			}
			if (query.getVersion() != null) {
				indexRequest.version(query.getVersion());
				indexRequest.versionType(EXTERNAL);
			}

			if (query.getParentId() != null) {
				indexRequest.parent(query.getParentId());
			}

			return indexRequest;
		} catch (IOException e) {
			throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
		}
	}

	@Override
	void refresh(RefreshRequest refreshRequest) {
		try {
			// TODO: Do something with the response.
			client.indices().refresh(refreshRequest);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to refresh index: " + Arrays.toString(refreshRequest.indices()), e);
		}
	}

	@Override
	public Boolean addAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		final AliasActions aliasAction = AliasActions.add().alias(query.getAliasName()).index(query.getIndexName());

		if (query.getFilterBuilder() != null) {
			aliasAction.filter(query.getFilterBuilder());
		} else if (query.getFilter() != null) {
			aliasAction.filter(query.getFilter());
		} else if (hasText(query.getRouting())) {
			aliasAction.routing(query.getRouting());
		} else if (hasText(query.getSearchRouting())) {
			aliasAction.searchRouting(query.getSearchRouting());
		} else if (hasText(query.getIndexRouting())) {
			aliasAction.indexRouting(query.getIndexRouting());
		}

		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(aliasAction);
		try {
			return client.indices().updateAliases(request).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to update aliases with request: " + request, e);
		}
	}

	@Override
	public Boolean removeAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		AliasActions aliasAction = new AliasActions(AliasActions.Type.REMOVE);
		request.addAliasAction(aliasAction);
		try {
			return client.indices().updateAliases(request).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to update aliases with request: " + request, e);
		}
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		try {
			Response response = client.getLowLevelClient().performRequest("GET", "/" + indexName + "/_alias/*");
			String aliasResponse = EntityUtils.toString(response.getEntity());
			return convertAliasResponse(aliasResponse);
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + indexName, e);
		}
	}

	/**
	 * It takes two steps to create a List<AliasMetadata> from the elasticsearch http response because the aliases field
	 * is actually a Map by alias name, but the alias name is on the AliasMetadata.
	 *
	 * @param aliasResponse
	 * @return
	 */
	private List<AliasMetaData> convertAliasResponse(String aliasResponse) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode node = mapper.readTree(aliasResponse);
			Iterator<String> names = node.fieldNames();
			names.next();
			node = node.findValue("aliases");

			Map<String, AliasData> aliasData = mapper.readValue(mapper.writeValueAsString(node),
					new TypeReference<Map<String, AliasData>>() {});
			return aliasData.entrySet().stream().map(aliasEntry -> {
				AliasData data = aliasEntry.getValue();
				return AliasMetaData.newAliasMetaDataBuilder(aliasEntry.getKey()).filter(data.getFilter())
						.routing(data.getRouting()).searchRouting(data.getSearch_routing()).indexRouting(data.getIndex_routing())
						.build();
			}).collect(toList());
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + aliasResponse, e);
		}
	}

	@Override
	public SearchResponse suggest(SuggestBuilder suggestion, String... indices) {
		SearchRequest searchRequest = new SearchRequest(indices);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.suggest(suggestion);
		searchRequest.source(sourceBuilder);

		try {
			return client.search(searchRequest);
		} catch (IOException e) {
			throw new ElasticsearchException("Could not execute search request : " + searchRequest.toString(), e);
		}
	}

	private SearchResponse search(SearchRequest request) {
		SearchResponse response;
		try {
			response = client.search(request);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + request, e);
		}
		return response;
	}
}
