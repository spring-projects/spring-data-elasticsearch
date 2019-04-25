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
package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity;
import org.springframework.data.elasticsearch.core.EntityOperations.Entity;
import org.springframework.data.elasticsearch.core.EntityOperations.IndexCoordinates;
import org.springframework.data.elasticsearch.core.EntityOperations.MultiIndexCoordinates;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
 * @author Zetang Zeng
 * @author Ivan Greene
 * @author Christoph Strobl
 * @author Dmitriy Yakovlev
 * @author Peter-Josef Meisch
 * @author Martin Choraine
 * @author Farid Azaza
 * @author Gyula Attila Csorogi
 */
public class ElasticsearchTemplate extends AbstractElasticsearchTemplate implements EsClient<Client> {

	private static final Logger QUERY_LOGGER = LoggerFactory
			.getLogger("org.springframework.data.elasticsearch.core.QUERY");
	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchTemplate.class);
	private static final String FIELD_SCORE = "_score";

	private Client client;
	private ResultsMapper resultsMapper;
	private String searchTimeout;

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

		super(elasticsearchConverter);

		Assert.notNull(client, "Client must not be null!");
		Assert.notNull(resultsMapper, "ResultsMapper must not be null!");

		this.client = client;
		this.resultsMapper = resultsMapper;
	}

	@Override
	public Client getClient() {
		return client;
	}

	public void setSearchTimeout(String searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	@Override
	public boolean addAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined to add alias");
		Assert.notNull(query.getAliasName(), "No alias defined to add alias");
		IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.add()
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
	public boolean removeAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined to remove alias");
		Assert.notNull(query.getAliasName(), "No alias defined to remove alias");
		return client.admin().indices().prepareAliases().removeAlias(query.getIndexName(), query.getAliasName()).execute()
				.actionGet().isAcknowledged();
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz) {
		return createIndexIfNotCreated(clazz);
	}

	@Override
	public boolean createIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for query");
		return client.admin().indices().create(Requests.createIndexRequest(indexName)).actionGet().isAcknowledged();
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz) {
		return putMapping(clazz, buildMapping(clazz));
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz, Object mapping) {
		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, null, null);
		return putMapping(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), mapping);
	}

	@Override
	public <T> boolean putMapping(String indexName, String type, Class<T> clazz) {
		return putMapping(indexName, type, buildMapping(clazz));
	}

	@Override
	public boolean putMapping(String indexName, String type, Object mapping) {

		Assert.notNull(indexName, "No index defined to put mapping");
		Assert.notNull(type, "No type defined to put mapping");

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

	@Override
	public Map<String, Object> getMapping(String indexName, String type) {

		Assert.notNull(indexName, "No index defined to put mapping");
		Assert.notNull(type, "No type defined to put mapping");

		Map<String, Object> mappings = null;
		try {
			mappings = client.admin().indices().getMappings(new GetMappingsRequest().indices(indexName).types(type))
					.actionGet().getMappings().get(indexName).get(type).getSourceAsMap();
		} catch (Exception e) {
			throw new ElasticsearchException(
					"Error while getting mapping for indexName : " + indexName + " type : " + type + " " + e.getMessage());
		}
		return mappings;
	}

	@Override
	public <T> Map<String, Object> getMapping(Class<T> clazz) {
		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, null, null);
		return getMapping(indexCoordinates.getIndexName(), indexCoordinates.getTypeName());
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz) {
		return queryForObject(query, clazz, resultsMapper);
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {

		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, null, null);
		Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
		Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");

		GetResponse response = client
				.prepareGet(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), query.getId()).execute()
				.actionGet();

		return mapper.mapResult(response, clazz);
	}

	@Override
	public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {

		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public <T> T queryForObject(StringQuery query, Class<T> clazz) {

		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {

		SearchResponse response = doSearch(prepareSearch(query, clazz), query);
		return mapper.mapResults(response, clazz, query.getPageable());
	}

	@Override
	public <T> List<Page<T>> queryForPage(List<SearchQuery> queries, Class<T> clazz) {
		return queryForPage(queries, clazz, resultsMapper);
	}

	@Override
	public <T> List<Page<T>> queryForPage(List<SearchQuery> queries, Class<T> clazz, SearchResultMapper mapper) {

		MultiSearchRequest request = new MultiSearchRequest();
		for (SearchQuery query : queries) {
			request.add(prepareSearch(prepareSearch(query, clazz), query));
		}
		return doMultiSearch(queries, clazz, request, mapper);
	}

	private <T> List<Page<T>> doMultiSearch(List<SearchQuery> queries, Class<T> clazz, MultiSearchRequest request,
			SearchResultMapper resultsMapper) {

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);
		List<Page<T>> res = new ArrayList<>(queries.size());
		int c = 0;
		for (SearchQuery query : queries) {
			res.add(resultsMapper.mapResults(items[c++].getResponse(), clazz, query.getPageable()));
		}
		return res;
	}

	private List<Page<?>> doMultiSearch(List<SearchQuery> queries, List<Class<?>> classes, MultiSearchRequest request,
			SearchResultMapper resultsMapper) {

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);
		List<Page<?>> res = new ArrayList<>(queries.size());
		int c = 0;
		Iterator<Class<?>> it = classes.iterator();
		for (SearchQuery query : queries) {
			res.add(resultsMapper.mapResults(items[c++].getResponse(), it.next(), query.getPageable()));
		}
		return res;
	}

	private MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request) {

		ActionFuture<MultiSearchResponse> future = client.multiSearch(request);
		MultiSearchResponse response = future.actionGet();
		MultiSearchResponse.Item[] items = response.getResponses();
		Assert.isTrue(items.length == request.requests().size(), "Response should have same length with queries");
		return items;
	}

	@Override
	public List<Page<?>> queryForPage(List<SearchQuery> queries, List<Class<?>> classes) {
		return queryForPage(queries, classes, resultsMapper);
	}

	@Override
	public List<Page<?>> queryForPage(List<SearchQuery> queries, List<Class<?>> classes, SearchResultMapper mapper) {

		Assert.isTrue(queries.size() == classes.size(), "Queries should have same length with classes");
		MultiSearchRequest request = new MultiSearchRequest();
		Iterator<Class<?>> it = classes.iterator();
		for (SearchQuery query : queries) {
			request.add(prepareSearch(prepareSearch(query, it.next()), query));
		}
		return doMultiSearch(queries, classes, request, mapper);
	}

	@Override
	public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
		SearchResponse response = doSearch(prepareSearch(query, (ElasticsearchPersistentEntity<?>) null), query);
		return resultsExtractor.extract(response);
	}

	@Override
	public <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<T> queryForList(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<String> queryForIds(SearchQuery query) {
		SearchRequestBuilder request = prepareSearch(query, (ElasticsearchPersistentEntity<?>) null);
		return extractIds(doSearch(request, query));
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
		return resultsMapper.mapResults(response, clazz, criteriaQuery.getPageable());
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {
		SearchResponse response = getSearchResponse(prepareSearch(query, clazz).setQuery(wrapperQuery(query.getSource())));
		return mapper.mapResults(response, clazz, query.getPageable());
	}

	@Override
	public <T> CloseableIterator<T> stream(CriteriaQuery query, Class<T> clazz) {
		long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		return doStream(scrollTimeInMillis, startScroll(scrollTimeInMillis, query, clazz), clazz, resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz) {
		return stream(query, clazz, resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
		long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		return doStream(scrollTimeInMillis, startScroll(scrollTimeInMillis, query, clazz, mapper), clazz, mapper);
	}

	private <T> CloseableIterator<T> doStream(long scrollTimeInMillis, ScrolledPage<T> page, Class<T> clazz,
			SearchResultMapper mapper) {
		return StreamQueries.streamResults(page, scrollId -> continueScroll(scrollId, scrollTimeInMillis, clazz, mapper),
				this::clearScroll);
	}

	@Override
	public <T> long count(CriteriaQuery criteriaQuery, Class<T> clazz) {

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());

		return doCount(prepareCount(criteriaQuery, clazz), elasticsearchQuery, elasticsearchFilter);
	}

	@Override
	public <T> long count(SearchQuery searchQuery, Class<T> clazz) {

		QueryBuilder elasticsearchQuery = searchQuery.getQuery();
		QueryBuilder elasticsearchFilter = searchQuery.getFilter();

		return doCount(prepareCount(searchQuery, clazz), elasticsearchQuery, elasticsearchFilter);
	}

	@Override
	public <T> long count(CriteriaQuery query) {
		Assert.notEmpty(query.getIndices(), "No indices defined for query.");
		return count(query, null);
	}

	@Override
	public <T> long count(SearchQuery query) {
		Assert.notEmpty(query.getIndices(), "No indices defined for query.");
		return count(query, null);
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

		MultiIndexCoordinates indexCoordinates;
		if (clazz == null) {
			// There is no entity type specified and the index types are optional for
			// count query. In this case create the MultiIndexCoordinates manually.
			indexCoordinates = new MultiIndexCoordinates(query.getIndices(), query.getTypes());
		} else {
			indexCoordinates = operations.determineIndexes(clazz, query.getIndices(), query.getTypes());
		}

		Assert.notNull(indexCoordinates.getIndexNames(), "No indicies defined for query");

		SearchRequestBuilder countRequestBuilder = client.prepareSearch(indexCoordinates.getIndexNames()) //
				.setSize(0);

		if (indexCoordinates.getTypeNames() != null) {
			countRequestBuilder.setTypes(indexCoordinates.getTypeNames());
		}

		return countRequestBuilder;
	}

	@Override
	public <T> List<T> multiGet(SearchQuery searchQuery, Class<T> clazz) {
		return resultsMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	private <T> MultiGetResponse getMultiResponse(Query searchQuery, Class<T> clazz) {

		String queryIndex = CollectionUtils.isEmpty(searchQuery.getIndices()) ? null : searchQuery.getIndices().get(0);
		String queryType = CollectionUtils.isEmpty(searchQuery.getTypes()) ? null : searchQuery.getTypes().get(0);
		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, queryIndex, queryType);
		Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
		Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");
		Assert.notEmpty(searchQuery.getIds(), "No ids defined for query");

		MultiGetRequestBuilder builder = client.prepareMultiGet();

		if (searchQuery.getFields() != null && !searchQuery.getFields().isEmpty()) {
			searchQuery.addSourceFilter(new FetchSourceFilter(ElasticsearchUtils.toArray(searchQuery.getFields()), null));
		}

		for (String id : searchQuery.getIds()) {

			MultiGetRequest.Item item = new MultiGetRequest.Item(indexCoordinates.getIndexName(),
					indexCoordinates.getTypeName(), id);

			if (searchQuery.getRoute() != null) {
				item = item.routing(searchQuery.getRoute());
			}

			builder.add(item);
		}
		return builder.execute().actionGet();
	}

	@Override
	public <T> List<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper getResultMapper) {
		return getResultMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	@Override
	public String index(IndexQuery query) {

		String documentId;
		IndexRequestBuilder request = prepareIndex(query);
		try {
			documentId = request.execute().actionGet().getId();
		} catch (IllegalStateException e) {
			throw new ElasticsearchException("Error while index " + query, e);
		}
		// We should call this because we are not going through a mapper.
		if (query.getObject() != null) {
			AdaptibleEntity<?> entity = operations.forEntity(query.getObject(), elasticsearchConverter.getConversionService());
			entity.populateIdIfNecessary(documentId);
		}
		return documentId;
	}

	@Override
	public UpdateResponse update(UpdateQuery query) {
		return this.prepareUpdate(query).execute().actionGet();
	}

	private UpdateRequestBuilder prepareUpdate(UpdateQuery query) {

		IndexCoordinates indexCoordinates = operations.determineIndex(query.getClazz(), query.getIndexName(),
				query.getType());
		Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
		Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");
		Assert.notNull(query.getId(), "No id defined for query");
		Assert.notNull(query.getUpdateRequest(), "No update request defined for query");

		UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(indexCoordinates.getIndexName(),
				indexCoordinates.getTypeName(), query.getId());
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
	public void bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions) {

		Assert.notNull(queries, "List of IndexQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		BulkRequestBuilder bulkRequest = client.prepareBulk();
		setBulkOptions(bulkRequest, bulkOptions);
		for (IndexQuery query : queries) {
			bulkRequest.add(prepareIndex(query));
		}
		checkForBulkUpdateFailure(bulkRequest.execute().actionGet());
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions) {

		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		BulkRequestBuilder bulkRequest = client.prepareBulk();
		setBulkOptions(bulkRequest, bulkOptions);
		for (UpdateQuery query : queries) {
			bulkRequest.add(prepareUpdate(query));
		}
		checkForBulkUpdateFailure(bulkRequest.execute().actionGet());
	}

	private static void setBulkOptions(BulkRequestBuilder bulkRequest, BulkOptions bulkOptions) {

		if (bulkOptions.getTimeout() != null) {
			bulkRequest.setTimeout(bulkOptions.getTimeout());
		}

		if (bulkOptions.getRefreshPolicy() != null) {
			bulkRequest.setRefreshPolicy(bulkOptions.getRefreshPolicy());
		}

		if (bulkOptions.getWaitForActiveShards() != null) {
			bulkRequest.setWaitForActiveShards(bulkOptions.getWaitForActiveShards());
		}

		if (bulkOptions.getPipeline() != null) {
			bulkRequest.pipeline(bulkOptions.getPipeline());
		}

		if (bulkOptions.getRoutingId() != null) {
			bulkRequest.routing(bulkOptions.getRoutingId());
		}
	}

	private void checkForBulkUpdateFailure(BulkResponse bulkResponse) {

		if (bulkResponse.hasFailures()) {
			Map<String, String> failedDocuments = new HashMap<>();
			for (BulkItemResponse item : bulkResponse.getItems()) {
				if (item.isFailed())
					failedDocuments.put(item.getId(), item.getFailureMessage());
			}
			throw new ElasticsearchException(
					"Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + "]",
					failedDocuments);
		}
	}

	@Override
	public <T> boolean indexExists(Class<T> clazz) {
		return indexExists(operations.determineIndexName(clazz));
	}

	@Override
	public boolean indexExists(String indexName) {

		Assert.notNull(indexName, "No index defined for operation");
		return client.admin().indices().exists(Requests.indicesExistsRequest(indexName)).actionGet().isExists();
	}

	@Override
	public boolean typeExists(String index, String type) {
		return client.admin().cluster().prepareState().execute().actionGet().getState().metaData().index(index)
				.getMappings().containsKey(type);
	}

	@Override
	public <T> boolean deleteIndex(Class<T> clazz) {

		return deleteIndex(operations.determineIndexName(clazz));
	}

	@Override
	public boolean deleteIndex(String indexName) {

		Assert.notNull(indexName, "No index defined for delete operation");
		if (indexExists(indexName)) {
			return client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet().isAcknowledged();
		}
		return false;
	}

	@Override
	public String delete(String indexName, String type, String id) {
		return client.prepareDelete(indexName, type, id).execute().actionGet().getId();
	}

	@Override
	public <T> String delete(Class<T> clazz, String id) {

		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, null, null);
		return delete(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), id);
	}

	@Override
	public <T> void delete(DeleteQuery query, Class<T> clazz) {

		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, query.getIndex(), query.getType());
		Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
		Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");

		DeleteByQueryRequestBuilder requestBuilder = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE) //
				.source(indexCoordinates.getIndexName()) //
				.filter(query.getQuery()) //
				.abortOnVersionConflict(false) //
				.refresh(true);

		SearchRequestBuilder source = requestBuilder.source() //
				.setTypes(indexCoordinates.getTypeName());

		if (query.getScrollTimeInMillis() != null)
			source.setScroll(TimeValue.timeValueMillis(query.getScrollTimeInMillis()));

		requestBuilder.get();
	}

	@Override
	public void delete(DeleteQuery deleteQuery) {
		Assert.notNull(deleteQuery.getIndex(), "No index defined for query");
		Assert.notNull(deleteQuery.getType(), "No type defined for query");
		delete(deleteQuery, null);
	}

	@Override
	public <T> void delete(CriteriaQuery criteriaQuery, Class<T> clazz) {

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		Assert.notNull(elasticsearchQuery, "Query can not be null.");
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(elasticsearchQuery);
		delete(deleteQuery, clazz);
	}

	private <T> SearchRequestBuilder prepareScroll(Query query, long scrollTimeInMillis, Class<T> clazz) {
		return prepareScroll(query, scrollTimeInMillis, operations.getPersistentEntity(clazz));
	}

	private SearchRequestBuilder prepareScroll(Query query, long scrollTimeInMillis,
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		MultiIndexCoordinates indexCoordinates = operations.determineIndexes(entity, query.getIndices(), query.getTypes());
		Assert.notNull(indexCoordinates.getIndexNames(), "No indicies defined for query");
		Assert.notNull(indexCoordinates.getTypeNames(), "No types defined for query");

		SearchRequestBuilder requestBuilder = client.prepareSearch(indexCoordinates.getIndexNames()) //
				.setTypes(indexCoordinates.getTypeNames()) //
				.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)) //
				.setFrom(0) //
				.setVersion(true);

		if (query.getPageable().isPaged()) {
			requestBuilder.setSize(query.getPageable().getPageSize());
		}

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			requestBuilder.setFetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (!CollectionUtils.isEmpty(query.getFields())) {
			requestBuilder.setFetchSource(ElasticsearchUtils.toArray(query.getFields()), null);
		}

		if (query.getSort() != null) {
			prepareSort(query, requestBuilder, entity);
		}

		if (query instanceof SearchQuery) {
			SearchQuery searchQuery = (SearchQuery) query;

			if (searchQuery.getHighlightFields() != null || searchQuery.getHighlightBuilder() != null) {
				HighlightBuilder highlightBuilder = searchQuery.getHighlightBuilder();
				if (highlightBuilder == null) {
					highlightBuilder = new HighlightBuilder();
				}
				if (searchQuery.getHighlightFields() != null) {
					for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
						highlightBuilder.field(highlightField);
					}
				}
				requestBuilder.highlighter(highlightBuilder);
			}
		}

		return requestBuilder;
	}

	private SearchResponse doScroll(SearchRequestBuilder requestBuilder, CriteriaQuery criteriaQuery) {

		Assert.notNull(criteriaQuery.getIndices(), "No index defined for query");
		Assert.notNull(criteriaQuery.getTypes(), "No type defined for query");
		Assert.notNull(criteriaQuery.getPageable(), "Query.pageable is required for scroll");

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());

		if (elasticsearchQuery != null) {
			requestBuilder.setQuery(elasticsearchQuery);
		} else {
			requestBuilder.setQuery(QueryBuilders.matchAllQuery());
		}

		if (elasticsearchFilter != null) {
			requestBuilder.setPostFilter(elasticsearchFilter);
		}

		return getSearchResponse(requestBuilder);
	}

	private SearchResponse doScroll(SearchRequestBuilder requestBuilder, SearchQuery searchQuery) {

		Assert.notNull(searchQuery.getIndices(), "No index defined for query");
		Assert.notNull(searchQuery.getTypes(), "No type defined for query");
		Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scroll");

		if (searchQuery.getFilter() != null) {
			requestBuilder.setPostFilter(searchQuery.getFilter());
		}

		if (!CollectionUtils.isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder<?> sort : searchQuery.getElasticsearchSorts()) {
				requestBuilder.addSort(sort);
			}
		}

		return getSearchResponse(requestBuilder.setQuery(searchQuery.getQuery()));
	}

	@Override
	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz) {

		SearchResponse response = doScroll(prepareScroll(searchQuery, scrollTimeInMillis, clazz), searchQuery);
		return resultsMapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz) {

		SearchResponse response = doScroll(prepareScroll(criteriaQuery, scrollTimeInMillis, clazz), criteriaQuery);
		return resultsMapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz,
			SearchResultMapper mapper) {

		SearchResponse response = doScroll(prepareScroll(searchQuery, scrollTimeInMillis, clazz), searchQuery);
		return mapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz,
			SearchResultMapper mapper) {

		SearchResponse response = doScroll(prepareScroll(criteriaQuery, scrollTimeInMillis, clazz), criteriaQuery);
		return mapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz) {
		SearchResponse response = getSearchResponse(
				client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute());
		return resultsMapper.mapResults(response, clazz, Pageable.unpaged());
	}

	@Override
	public <T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz,
			SearchResultMapper mapper) {

		SearchResponse response = getSearchResponse(
				client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute());
		return mapper.mapResults(response, clazz, Pageable.unpaged());
	}

	@Override
	public void clearScroll(String scrollId) {
		client.prepareClearScroll().addScrollId(scrollId).execute().actionGet();
	}

	@Override
	public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {

		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, query.getIndexName(), query.getType());
		Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
		Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");
		Assert.notNull(query.getId(), "No document id defined for query");

		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = moreLikeThisQuery(
				ElasticsearchUtils.toArray(new MoreLikeThisQueryBuilder.Item(indexCoordinates.getIndexName(),
						indexCoordinates.getTypeName(), query.getId())));

		if (query.getMinTermFreq() != null) {
			moreLikeThisQueryBuilder.minTermFreq(query.getMinTermFreq());
		}
		if (query.getMaxQueryTerms() != null) {
			moreLikeThisQueryBuilder.maxQueryTerms(query.getMaxQueryTerms());
		}
		if (!CollectionUtils.isEmpty(query.getStopWords())) {
			moreLikeThisQueryBuilder.stopWords(ElasticsearchUtils.toArray(query.getStopWords()));
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

		return queryForPage(new NativeSearchQueryBuilder().withQuery(moreLikeThisQueryBuilder).build(), clazz);
	}

	private SearchResponse doSearch(SearchRequestBuilder searchRequest, SearchQuery searchQuery) {

		SearchRequestBuilder requestBuilder = prepareSearch(searchRequest, searchQuery);
		return getSearchResponse(requestBuilder);
	}

	private SearchRequestBuilder prepareSearch(SearchRequestBuilder searchRequest, SearchQuery searchQuery) {

		if (searchQuery.getFilter() != null) {
			searchRequest.setPostFilter(searchQuery.getFilter());
		}

		if (!CollectionUtils.isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder<?> sort : searchQuery.getElasticsearchSorts()) {
				searchRequest.addSort(sort);
			}
		}

		if (!searchQuery.getScriptFields().isEmpty()) {
			// _source should be return all the time
			// searchRequest.addStoredField("_source");
			for (ScriptField scriptedField : searchQuery.getScriptFields()) {
				searchRequest.addScriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		if (searchQuery.getCollapseBuilder() != null) {
			searchRequest.setCollapse(searchQuery.getCollapseBuilder());
		}

		if (searchQuery.getHighlightFields() != null || searchQuery.getHighlightBuilder() != null) {
			HighlightBuilder highlightBuilder = searchQuery.getHighlightBuilder();
			if (highlightBuilder == null) {
				highlightBuilder = new HighlightBuilder();
			}
			if (searchQuery.getHighlightFields() != null) {
				for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
					highlightBuilder.field(highlightField);
				}
			}
			searchRequest.highlighter(highlightBuilder);
		}

		if (!CollectionUtils.isEmpty(searchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : searchQuery.getIndicesBoost()) {
				searchRequest.addIndexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!CollectionUtils.isEmpty(searchQuery.getAggregations())) {
			for (AbstractAggregationBuilder<?> aggregationBuilder : searchQuery.getAggregations()) {
				searchRequest.addAggregation(aggregationBuilder);
			}
		}

		if (!CollectionUtils.isEmpty(searchQuery.getFacets())) {
			for (FacetRequest aggregatedFacet : searchQuery.getFacets()) {
				searchRequest.addAggregation(aggregatedFacet.getFacet());
			}
		}
		return searchRequest.setQuery(searchQuery.getQuery());
	}

	private SearchResponse getSearchResponse(SearchRequestBuilder requestBuilder) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(requestBuilder.toString());
		}

		return getSearchResponse(requestBuilder.execute());
	}

	private SearchResponse getSearchResponse(ActionFuture<SearchResponse> response) {
		return searchTimeout == null ? response.actionGet() : response.actionGet(searchTimeout);
	}

	private <T> boolean createIndexIfNotCreated(Class<T> clazz) {
		return indexExists(operations.determineIndexName(clazz)) || createIndexWithSettings(clazz);
	}

	private <T> boolean createIndexWithSettings(Class<T> clazz) {

		String indexName = operations.determineIndexName(clazz);

		if (clazz.isAnnotationPresent(Setting.class)) {
			String settingPath = clazz.getAnnotation(Setting.class).settingPath();
			Assert.hasText(settingPath, "settingPath of Setting annotation must not be empty");
			String settings = ResourceUtil.readFileFromClasspath(settingPath);
			Assert.hasText(settings, "settings from path " + settingPath + " must not be empty");
			return createIndex(indexName, settings);
		}

		return createIndex(indexName, operations.getIndexSettings(clazz));
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
	public <T> boolean createIndex(Class<T> clazz, Object settings) {

		String indexName = operations.determineIndexName(clazz);
		Assert.notNull(indexName, "No index defined for query");
		return createIndex(indexName, settings);
	}

	@Override
	public <T> Map<String, Object> getSetting(Class<T> clazz) {
		return getSetting(operations.determineIndexName(clazz));
	}

	@Override
	public Map<String, Object> getSetting(String indexName) {

		Assert.notNull(indexName, "No index defined to get settings");
		Settings settings = client.admin() //
				.indices() //
				.getSettings(new GetSettingsRequest()) //
				.actionGet() //
				.getIndexToSettings() //
				.get(indexName);

		return settings.keySet().stream().collect(Collectors.toMap((key) -> key, (key) -> settings.get(key)));
	}

	private <T> SearchRequestBuilder prepareSearch(Query query, Class<T> clazz) {
		return prepareSearch(query, operations.getPersistentEntity(clazz));
	}

	private SearchRequestBuilder prepareSearch(Query query, @Nullable ElasticsearchPersistentEntity<?> entity) {

		MultiIndexCoordinates indexCoordinates = operations.determineIndexes(entity, query.getIndices(), query.getTypes());
		Assert.notNull(indexCoordinates.getIndexNames(), "No indicies defined for query");
		Assert.notNull(indexCoordinates.getTypeNames(), "No types defined for query");

		int startRecord = 0;
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexCoordinates.getIndexNames()) //
				.setSearchType(query.getSearchType()) //
				.setTypes(indexCoordinates.getTypeNames()) //
				.setVersion(true) //
				.setTrackScores(query.getTrackScores());

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			searchRequestBuilder.setFetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable().isPaged()) {
			startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
			searchRequestBuilder.setSize(query.getPageable().getPageSize());
		}
		searchRequestBuilder.setFrom(startRecord);

		if (!query.getFields().isEmpty()) {
			searchRequestBuilder.setFetchSource(ElasticsearchUtils.toArray(query.getFields()), null);
		}

		if (query.getIndicesOptions() != null) {
			searchRequestBuilder.setIndicesOptions(query.getIndicesOptions());
		}

		if (query.getSort() != null) {
			prepareSort(query, searchRequestBuilder, entity);
		}

		if (query.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(query.getMinScore());
		}

		if (query.getPreference() != null) {
			searchRequestBuilder.setPreference(query.getPreference());
		}

		return searchRequestBuilder;
	}

	private void prepareSort(Query query, SearchRequestBuilder searchRequestBuilder,
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		for (Sort.Order order : query.getSort()) {
			SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC;

			if (FIELD_SCORE.equals(order.getProperty())) {
				ScoreSortBuilder sort = SortBuilders //
						.scoreSort() //
						.order(sortOrder);

				searchRequestBuilder.addSort(sort);
			} else {
				ElasticsearchPersistentProperty property = entity != null //
						? entity.getPersistentProperty(order.getProperty()) //
						: null;
				String fieldName = property != null ? property.getFieldName() : order.getProperty();
				FieldSortBuilder sort = SortBuilders //
						.fieldSort(fieldName) //
						.order(sortOrder);

				if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
					sort.missing("_first");
				} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
					sort.missing("_last");
				}

				searchRequestBuilder.addSort(sort);
			}
		}
	}

	private IndexRequestBuilder prepareIndex(IndexQuery query) {
		try {
			IndexRequestBuilder indexRequestBuilder = null;
			Entity<?> entity = query.getObject() != null ? operations.forEntity(query.getObject()) : null;

			if (entity != null) {
				IndexCoordinates indexCoordinates = operations.determineIndex(entity, query.getIndexName(), query.getType());
				Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
				Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");

				String id = StringUtils.isEmpty(query.getId()) ? Objects.toString(entity.getId(), null) : query.getId();
				// If we have a query id and a document id, do not ask ES to generate one.
				if (id != null) {
					indexRequestBuilder = client.prepareIndex(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(),
							id);
				} else {
					indexRequestBuilder = client.prepareIndex(indexCoordinates.getIndexName(), indexCoordinates.getTypeName());
				}
				indexRequestBuilder.setSource(resultsMapper.getEntityMapper().mapToString(query.getObject()),
						Requests.INDEX_CONTENT_TYPE);

			} else if (query.getSource() != null) {
				Assert.notNull(query.getIndexName(), "No index defined for query");
				Assert.notNull(query.getType(), "No type defined for query");

				indexRequestBuilder = client.prepareIndex(query.getIndexName(), query.getType(), query.getId())
						.setSource(query.getSource(), Requests.INDEX_CONTENT_TYPE);
			} else {
				throw new ElasticsearchException("Object or source is null, failed to index document");
			}
			if (query.getVersion() != null) {
				indexRequestBuilder.setVersion(query.getVersion());

				if (entity != null) {
					indexRequestBuilder.setVersionType(entity.getPersistentEntity().getVersionType());
				}
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
	public void refresh(String indexName) {

		Assert.notNull(indexName, "No index defined to refresh");
		client.admin().indices().refresh(Requests.refreshRequest(indexName)).actionGet();
	}

	@Override
	public <T> void refresh(Class<T> clazz) {
		refresh(operations.determineIndexName(clazz));
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		return client.admin() //
				.indices() //
				.getAliases(new GetAliasesRequest().indices(indexName)).actionGet() //
				.getAliases() //
				.get(indexName);
	}

	protected ResultsMapper getResultsMapper() {
		return resultsMapper;
	}

	@Deprecated
	public static String readFileFromClasspath(String url) {
		return ResourceUtil.readFileFromClasspath(url);
	}

	public SearchResponse suggest(SuggestBuilder suggestion, String... indices) {
		return client.prepareSearch(indices).suggest(suggestion).get();
	}

	public SearchResponse suggest(SuggestBuilder suggestion, Class<?> clazz) {

		String indexName = operations.determineIndexName(clazz);
		Assert.notNull(indexName, "No index defined for query");
		return suggest(suggestion, indexName);
	}

}
