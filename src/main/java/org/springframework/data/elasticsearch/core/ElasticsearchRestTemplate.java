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
import java.util.Set;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
import org.springframework.data.elasticsearch.core.client.support.AliasData;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * @author Zetang Zeng
 * @author Peter Nowak
 * @author Ivan Greene
 * @author Christoph Strobl
 * @author Lorenzo Spinelli
 * @author Dmitriy Yakovlev
 * @author Roman Puchkovskiy
 * @author Martin Choraine
 * @author Farid Azaza
 * @author Peter-Josef Meisch
 * @author Mathias Teier
 * @author Gyula Attila Csorogi
 */
public class ElasticsearchRestTemplate extends AbstractElasticsearchTemplate implements EsClient<RestHighLevelClient> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchRestTemplate.class);
	private static final String FIELD_SCORE = "_score";

	private final RestHighLevelClient client;
	private final ResultsMapper resultsMapper;
	private final ObjectMapper objectMapper;

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

		super(elasticsearchConverter);

		Assert.notNull(client, "Client must not be null!");
		Assert.notNull(resultsMapper, "ResultsMapper must not be null!");

		this.client = client;
		this.resultsMapper = resultsMapper;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public RestHighLevelClient getClient() {
		return client;
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
		} else if (StringUtils.hasText(query.getRouting())) {
			aliasAction.routing(query.getRouting());
		} else if (StringUtils.hasText(query.getSearchRouting())) {
			aliasAction.searchRouting(query.getSearchRouting());
		} else if (StringUtils.hasText(query.getIndexRouting())) {
			aliasAction.indexRouting(query.getIndexRouting());
		}

		IndicesAliasesRequest request = Requests.indexAliasesRequest().addAliasAction(aliasAction);
		return updateAliases(request).isAcknowledged();
	}

	@Override
	public boolean removeAlias(AliasQuery query) {

		Assert.notNull(query.getIndexName(), "No index defined to remove alias");
		Assert.notNull(query.getAliasName(), "No alias defined to remove alias");

		AliasActions aliasAction = IndicesAliasesRequest.AliasActions.remove() //
			.index(query.getIndexName()) //
			.alias(query.getAliasName());

		IndicesAliasesRequest request = Requests.indexAliasesRequest() //
				.addAliasAction(aliasAction);

		return updateAliases(request).isAcknowledged();
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz) {
		return createIndexIfNotCreated(clazz);
	}

	@Override
	public boolean createIndex(String indexName) {

		Assert.notNull(indexName, "No index defined for query");
		try {
			return client.indices().create(new CreateIndexRequest(indexName), RequestOptions.DEFAULT).isAcknowledged();
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to create index " + indexName, e);
		}
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

		PutMappingRequest request = Requests.putMappingRequest(indexName).type(type);
		if (mapping instanceof String) {
			request.source(String.valueOf(mapping), XContentType.JSON);
		} else if (mapping instanceof Map) {
			request.source((Map) mapping);
		} else if (mapping instanceof XContentBuilder) {
			request.source((XContentBuilder) mapping);
		}
		try {
			return client.indices().putMapping(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Failed to put mapping for " + indexName, e);
		}
	}

	@Override
	public Map<String, Object> getMapping(String indexName, String type) {

		Assert.notNull(indexName, "No index defined to get mapping");
		Assert.notNull(type, "No type defined to get mapping");

		Request request = new Request("GET", String.format("/%s/_mapping/%s", indexName, type));
		Response response = lowLevelRequest(request);

		Map<String, Object> mappings;
		try {
			mappings = convertMappingResponse(EntityUtils.toString(response.getEntity()), type);
		} catch (Exception e) {
			throw new ElasticsearchException("Error while convert to mapping for index: " + indexName + " type: " + type, e);
		}

		return mappings;
	}

	@Override
	public <T> Map<String, Object> getMapping(Class<T> clazz) {
		IndexCoordinates indexCoordinates = operations.determineIndex(clazz, null, null);
		return getMapping(indexCoordinates.getIndexName(), indexCoordinates.getTypeName());
	}

	private Map<String, Object> convertMappingResponse(String mappingResponse, String type) {
		try {
			JsonNode node = objectMapper.readTree(mappingResponse) //
					.findValue("mappings") //
					.findValue(type);

			String mappings = objectMapper.writeValueAsString(node);
			return objectMapper.readValue(mappings, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + mappingResponse, e);
		}
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

		GetRequest request = Requests.getRequest(indexCoordinates.getIndexName()) //
				.type(indexCoordinates.getTypeName()) //
				.id(query.getId());

		try {
			GetResponse response = client.get(request, RequestOptions.DEFAULT);
			return mapper.mapResult(response, clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("Error on get request: " + request.toString(), e);
		}
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

	private <T> List<Page<T>> doMultiSearch(List<SearchQuery> queries, Class<T> clazz, MultiSearchRequest request,
			SearchResultMapper resultsMapper) {

		MultiSearchResponse.Item[] items = multiSearchItems(request);
		List<Page<T>> res = new ArrayList<>(queries.size());
		int c = 0;
		for (SearchQuery query : queries) {
			res.add(resultsMapper.mapResults(items[c++].getResponse(), clazz, query.getPageable()));
		}
		return res;
	}

	private List<Page<?>> doMultiSearch(List<SearchQuery> queries, List<Class<?>> classes, MultiSearchRequest request,
			SearchResultMapper resultsMapper) {

		MultiSearchResponse.Item[] items = multiSearchItems(request);
		List<Page<?>> res = new ArrayList<>(queries.size());
		int c = 0;
		Iterator<Class<?>> it = classes.iterator();
		for (SearchQuery query : queries) {
			res.add(resultsMapper.mapResults(items[c++].getResponse(), it.next(), query.getPageable()));
		}
		return res;
	}

	@Override
	public <T> List<Page<T>> queryForPage(List<SearchQuery> queries, Class<T> clazz, SearchResultMapper mapper) {

		MultiSearchRequest request = new MultiSearchRequest();
		for (SearchQuery query : queries) {
			request.add(prepareSearch(prepareSearch(query, clazz), query));
		}
		return doMultiSearch(queries, clazz, request, mapper);
	}

	@Override
	public List<Page<?>> queryForPage(List<SearchQuery> queries, List<Class<?>> classes) {
		return queryForPage(queries, classes, resultsMapper);
	}

	@Override
	public List<Page<?>> queryForPage(List<SearchQuery> queries, List<Class<?>> classes, SearchResultMapper mapper) {

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
		SearchRequest request = prepareSearch(query, (ElasticsearchPersistentEntity<?>) null);
		return extractIds(doSearch(request, query));
	}

	@Override
	public <T> Page<T> queryForPage(CriteriaQuery criteriaQuery, Class<T> clazz) {

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());
		SearchRequest request = prepareSearch(criteriaQuery, clazz);

		if (elasticsearchQuery != null) {
			request.source().query(elasticsearchQuery);
		} else {
			request.source().query(QueryBuilders.matchAllQuery());
		}

		if (criteriaQuery.getMinScore() > 0) {
			request.source().minScore(criteriaQuery.getMinScore());
		}

		if (elasticsearchFilter != null) {
			request.source().postFilter(elasticsearchFilter);
		}

		SearchResponse response = search(request);
		return resultsMapper.mapResults(response, clazz, criteriaQuery.getPageable());
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {

		SearchRequest request = prepareSearch(query, clazz);
		request.source().query((wrapperQuery(query.getSource())));

		SearchResponse response = search(request);
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

	private long doCount(SearchRequest searchRequest, QueryBuilder elasticsearchQuery, QueryBuilder elasticsearchFilter) {

		if (elasticsearchQuery != null) {
			searchRequest.source().query(elasticsearchQuery);
		} else {
			searchRequest.source().query(QueryBuilders.matchAllQuery());
		}
		if (elasticsearchFilter != null) {
			searchRequest.source().postFilter(elasticsearchFilter);
		}

		SearchResponse response = search(searchRequest);
		return response.getHits().getTotalHits();
	}

	private <T> SearchRequest prepareCount(Query query, Class<T> clazz) {

		MultiIndexCoordinates indexCoordinates;
		if (clazz == null) {
			// There is no entity type specified and the index types are optional for
			// count query. In this case create the MultiIndexCoordinates manually.
			indexCoordinates = new MultiIndexCoordinates(query.getIndices(), query.getTypes());
		} else {
			indexCoordinates = operations.determineIndexes(clazz, query.getIndices(), query.getTypes());
		}

		SearchRequest countRequestBuilder = new SearchRequest(indexCoordinates.getIndexNames());

		if (indexCoordinates.getTypeNames() != null) {
			countRequestBuilder.types(indexCoordinates.getTypeNames());
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

		MultiGetRequest request = new MultiGetRequest();

		if (searchQuery.getFields() != null && !searchQuery.getFields().isEmpty()) {
			searchQuery.addSourceFilter(new FetchSourceFilter(ElasticsearchUtils.toArray(searchQuery.getFields()), null));
		}

		for (String id : searchQuery.getIds()) {

			MultiGetRequest.Item item = new MultiGetRequest.Item(indexCoordinates.getIndexName(),
					indexCoordinates.getTypeName(), id);

			if (searchQuery.getRoute() != null) {
				item = item.routing(searchQuery.getRoute());
			}

			request.add(item);
		}

		try {
			return client.mget(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error on multi get request: " + request.toString(), e);
		}
	}

	@Override
	public <T> List<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper multiGetResultMapper) {
		return multiGetResultMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	@Override
	public String index(IndexQuery query) {

		String documentId;
		IndexRequest request = prepareIndex(query);
		try {
			documentId = client.index(request, RequestOptions.DEFAULT).getId();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while index for request: " + request.toString(), e);
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

		UpdateRequest request = prepareUpdate(query);
		try {
			return client.update(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while update for request: " + request.toString(), e);
		}
	}

	private UpdateRequest prepareUpdate(UpdateQuery query) {

		IndexCoordinates indexCoordinates = operations.determineIndex(query.getClazz(), query.getIndexName(),
				query.getType());
		Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
		Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");
		Assert.notNull(query.getId(), "No id defined for query");
		Assert.notNull(query.getUpdateRequest(), "No update request defined for query");

		UpdateRequest updateRequest = new UpdateRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(),
				query.getId());
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
	public void bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions) {

		Assert.notNull(queries, "List of IndexQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		BulkRequest bulkRequest = new BulkRequest();
		setBulkOptions(bulkRequest, bulkOptions);
		for (IndexQuery query : queries) {
			bulkRequest.add(prepareIndex(query));
		}

		bulk(bulkRequest);
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions) {

		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		BulkRequest bulkRequest = new BulkRequest();
		setBulkOptions(bulkRequest, bulkOptions);
		for (UpdateQuery query : queries) {
			bulkRequest.add(prepareUpdate(query));
		}

		bulk(bulkRequest);
	}

	private static void setBulkOptions(BulkRequest bulkRequest, BulkOptions bulkOptions) {

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
	}

	@Override
	public <T> boolean indexExists(Class<T> clazz) {
		return indexExists(operations.determineIndexName(clazz));
	}

	@Override
	public boolean indexExists(String indexName) {

		Assert.notNull(indexName, "No index defined for operation");
		try {
			GetIndexRequest request = new GetIndexRequest(indexName);
			return client.indices().exists(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error on indices exists request for index " + indexName, e);
		}
	}

	@Override
	public boolean typeExists(String index, String type) {

		Request request = new Request("HEAD", String.format("%s/_mapping/%s", index, type));
		Response response = lowLevelRequest(request);

		return response.getStatusLine().getStatusCode() == 200;
	}

	@Override
	public <T> boolean deleteIndex(Class<T> clazz) {
		return deleteIndex(operations.determineIndexName(clazz));
	}

	@Override
	public boolean deleteIndex(String indexName) {

		Assert.notNull(indexName, "No index defined for delete");
		if (indexExists(indexName)) {
			DeleteIndexRequest request = new DeleteIndexRequest(indexName);
			try {
				return client.indices().delete(request, RequestOptions.DEFAULT).isAcknowledged();
			} catch (IOException e) {
				throw new ElasticsearchException("Error on indices delete request: " + request.toString(), e);
			}
		}
		return false;
	}

	@Override
	public String delete(String indexName, String type, String id) {

		DeleteRequest request = Requests.deleteRequest(indexName) //
				.type(type) //
				.id(id);

		try {
			return client.delete(request, RequestOptions.DEFAULT).getId();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while deleting item request: " + request.toString(), e);
		}
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

		DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(indexCoordinates.getIndexName()) //
				.setDocTypes(indexCoordinates.getTypeName()) //
				.setQuery(query.getQuery()) //
				.setAbortOnVersionConflict(false) //
				.setRefresh(true);

		if (query.getPageSize() != null)
			deleteByQueryRequest.setBatchSize(query.getPageSize());

		if (query.getScrollTimeInMillis() != null)
			deleteByQueryRequest.setScroll(TimeValue.timeValueMillis(query.getScrollTimeInMillis()));

		try {
			client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for delete request: " + deleteByQueryRequest.toString(), e);
		}
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
		Assert.notNull(elasticsearchQuery, "Query can not be null");
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(elasticsearchQuery);
		delete(deleteQuery, clazz);
	}

	private <T> SearchRequest prepareScroll(Query query, long scrollTimeInMillis, Class<T> clazz) {
		return prepareScroll(query, scrollTimeInMillis, operations.getPersistentEntity(clazz));
	}

	private SearchRequest prepareScroll(Query query, long scrollTimeInMillis,
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		MultiIndexCoordinates indexCoordinates = operations.determineIndexes(entity, query.getIndices(), query.getTypes());
		Assert.notNull(indexCoordinates.getIndexNames(), "No indicies defined for query");
		Assert.notNull(indexCoordinates.getTypeNames(), "No types defined for query");

		SearchRequest request = new SearchRequest(indexCoordinates.getIndexNames()) //
				.types(indexCoordinates.getTypeNames()) //
				.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		SearchSourceBuilder sourceBuilder = request.source() //
				.from(0) //
				.version(true);

		if (query.getPageable().isPaged()) {
			sourceBuilder.size(query.getPageable().getPageSize());
		}

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			sourceBuilder.fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (!CollectionUtils.isEmpty(query.getFields())) {
			sourceBuilder.fetchSource(ElasticsearchUtils.toArray(query.getFields()), null);
		}

		if (query.getSort() != null) {
			prepareSort(query, sourceBuilder, entity);
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
				sourceBuilder.highlighter(highlightBuilder);
			}
		}

		return request;
	}

	private SearchResponse doScroll(SearchRequest request, CriteriaQuery criteriaQuery) {

		Assert.notNull(criteriaQuery.getIndices(), "No index defined for query");
		Assert.notNull(criteriaQuery.getTypes(), "No type defined for query");
		Assert.notNull(criteriaQuery.getPageable(), "Query.pageable required for scroll");

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());

		if (elasticsearchQuery != null) {
			request.source().query(elasticsearchQuery);
		} else {
			request.source().query(QueryBuilders.matchAllQuery());
		}

		if (elasticsearchFilter != null) {
			request.source().postFilter(elasticsearchFilter);
		}
		request.source().version(true);

		return search(request);
	}

	private SearchResponse doScroll(SearchRequest request, SearchQuery searchQuery) {

		Assert.notNull(searchQuery.getIndices(), "No index defined for query");
		Assert.notNull(searchQuery.getTypes(), "No type defined for query");
		Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scroll");

		if (searchQuery.getQuery() != null) {
			request.source().query(searchQuery.getQuery());
		} else {
			request.source().query(QueryBuilders.matchAllQuery());
		}

		if (searchQuery.getFilter() != null) {
			request.source().postFilter(searchQuery.getFilter());
		}
		request.source().version(true);

		if (!CollectionUtils.isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder<?> sort : searchQuery.getElasticsearchSorts()) {
				request.source().sort(sort);
			}
		}

		return search(request);
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

		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));
		SearchResponse response = scroll(request);
		return resultsMapper.mapResults(response, clazz, Pageable.unpaged());
	}

	@Override
	public <T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz,
			SearchResultMapper mapper) {

		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));
		SearchResponse response = scroll(request);
		return mapper.mapResults(response, clazz, Pageable.unpaged());
	}

	@Override
	public void clearScroll(String scrollId) {

		ClearScrollRequest request = new ClearScrollRequest();
		request.addScrollId(scrollId);
		try {
			// TODO Something useful with the response.
			client.clearScroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
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

	private SearchResponse doSearch(SearchRequest searchRequest, SearchQuery searchQuery) {
		return search(prepareSearch(searchRequest, searchQuery));
	}

	private SearchRequest prepareSearch(SearchRequest searchRequest, SearchQuery searchQuery) {

		if (searchQuery.getFilter() != null) {
			searchRequest.source().postFilter(searchQuery.getFilter());
		}

		if (!CollectionUtils.isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder<?> sort : searchQuery.getElasticsearchSorts()) {
				searchRequest.source().sort(sort);
			}
		}

		if (!searchQuery.getScriptFields().isEmpty()) {
			// _source should be return all the time
			// searchRequest.addStoredField("_source");
			for (ScriptField scriptedField : searchQuery.getScriptFields()) {
				searchRequest.source().scriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		if (searchQuery.getCollapseBuilder() != null) {
			searchRequest.source().collapse(searchQuery.getCollapseBuilder());
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
			searchRequest.source().highlighter(highlightBuilder);
		}

		if (!CollectionUtils.isEmpty(searchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : searchQuery.getIndicesBoost()) {
				searchRequest.source().indexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!CollectionUtils.isEmpty(searchQuery.getAggregations())) {
			for (AbstractAggregationBuilder<?> aggregationBuilder : searchQuery.getAggregations()) {
				searchRequest.source().aggregation(aggregationBuilder);
			}
		}

		if (!CollectionUtils.isEmpty(searchQuery.getFacets())) {
			for (FacetRequest aggregatedFacet : searchQuery.getFacets()) {
				searchRequest.source().aggregation(aggregatedFacet.getFacet());
			}
		}

		if (searchQuery.getQuery() != null) {
			searchRequest.source().query(searchQuery.getQuery());
		}

		return searchRequest;
	}

	private <T> boolean createIndexIfNotCreated(Class<T> clazz) {
		return indexExists(operations.determineIndexName(clazz)) || createIndexWithSettings(clazz);
	}

	private <T> boolean createIndexWithSettings(Class<T> clazz) {

		String indexName = operations.determineIndexName(clazz);

		if (clazz.isAnnotationPresent(Setting.class)) {
			String settingPath = clazz.getAnnotation(Setting.class).settingPath();
			Assert.hasText(settingPath, "settingPath of Setting annotation must not be null");
			String settings = ResourceUtil.readFileFromClasspath(settingPath);
			Assert.hasText(settings, "settings from path " + settingPath + " must not be empty");
			if (!StringUtils.isEmpty(settings)) {
				return createIndex(indexName, settings);
			}
		}

		return createIndex(indexName, operations.getIndexSettings(clazz));
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {

		Assert.notNull(indexName, "No index defined for operation");

		CreateIndexRequest request = new CreateIndexRequest(indexName);
		if (settings instanceof String) {
			request.settings(String.valueOf(settings), Requests.INDEX_CONTENT_TYPE);
		} else if (settings instanceof Map) {
			request.settings((Map) settings);
		} else if (settings instanceof XContentBuilder) {
			request.settings((XContentBuilder) settings);
		}
		try {
			return client.indices().create(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Error on indices create request: " + request.toString(), e);
		}
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz, Object settings) {
		return createIndex(operations.determineIndexName(clazz), settings);
	}

	@Override
	public <T> Map<String, Object> getSetting(Class<T> clazz) {
		return getSetting(operations.determineIndexName(clazz));
	}

	@Override // TODO change interface to return Settings.
	public Map<String, Object> getSetting(String indexName) {

		Assert.notNull(indexName, "No index defined to get settings");

		Request request = new Request("GET", String.format("/%s/_settings", indexName));
		Response response = lowLevelRequest(request);

		try {
			return convertSettingResponse(EntityUtils.toString(response.getEntity()), indexName);

		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting settings for indexName : " + indexName, e);
		}
	}

	private Map<String, Object> convertSettingResponse(String settingResponse, String indexName) {
		try {
			Settings settings = Settings.fromXContent(XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY,
					DeprecationHandler.THROW_UNSUPPORTED_OPERATION, settingResponse));
			String prefix = indexName + ".settings.";
			// Backwards compatibility. TODO Change to return Settings object.
			Map<String, Object> result = new HashMap<>();
			Set<String> keySet = settings.keySet();
			for (String key : keySet) {
				result.put(key.substring(prefix.length()), settings.get(key));
			}
			return result;
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + settingResponse, e);
		}

	}

	private <T> SearchRequest prepareSearch(Query query, Class<T> clazz) {
		return prepareSearch(query, operations.getPersistentEntity(clazz));
	}

	private SearchRequest prepareSearch(Query query, @Nullable ElasticsearchPersistentEntity<?> entity) {

		MultiIndexCoordinates indexCoordinates = operations.determineIndexes(entity, query.getIndices(), query.getTypes());
		Assert.notNull(indexCoordinates.getIndexNames(), "No indicies defined for query");
		Assert.notNull(indexCoordinates.getTypeNames(), "No types defined for query");

		int startRecord = 0;
		SearchRequest request = new SearchRequest(indexCoordinates.getIndexNames()) //
				.types(indexCoordinates.getTypeNames());

		SearchSourceBuilder sourceBuilder = request.source() //
				.version(true) //
				.trackScores(query.getTrackScores());

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			sourceBuilder.fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable().isPaged()) {
			startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
			sourceBuilder.size(query.getPageable().getPageSize());
		}
		sourceBuilder.from(startRecord);

		if (!query.getFields().isEmpty()) {
			sourceBuilder.fetchSource(ElasticsearchUtils.toArray(query.getFields()), null);
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(query.getIndicesOptions());
		}

		if (query.getSort() != null) {
			prepareSort(query, sourceBuilder, entity);
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

		return request;
	}

	private void prepareSort(Query query, SearchSourceBuilder sourceBuilder,
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		for (Sort.Order order : query.getSort()) {
			SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC;

			if (FIELD_SCORE.equals(order.getProperty())) {
				ScoreSortBuilder sort = SortBuilders //
						.scoreSort() //
						.order(sortOrder);

				sourceBuilder.sort(sort);
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

				sourceBuilder.sort(sort);
			}
		}
	}

	private IndexRequest prepareIndex(IndexQuery query) {
		try {
			IndexRequest indexRequest = null;
			Entity<?> entity = query.getObject() != null ? operations.forEntity(query.getObject()) : null;

			if (entity != null) {
				IndexCoordinates indexCoordinates = operations.determineIndex(entity, query.getIndexName(), query.getType());
				Assert.notNull(indexCoordinates.getIndexName(), "No index defined for query");
				Assert.notNull(indexCoordinates.getTypeName(), "No type defined for query");

				String id = StringUtils.isEmpty(query.getId()) ? Objects.toString(entity.getId(), null) : query.getId();
				// If we have a query id and a document id, do not ask ES to generate one.
				if (id != null) {
					indexRequest = new IndexRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName(), id);
				} else {
					indexRequest = new IndexRequest(indexCoordinates.getIndexName(), indexCoordinates.getTypeName());
				}
				indexRequest.source(resultsMapper.getEntityMapper().mapToString(query.getObject()),
						Requests.INDEX_CONTENT_TYPE);

			} else if (query.getSource() != null) {
				Assert.notNull(query.getIndexName(), "No index defined for query");
				Assert.notNull(query.getType(), "No type defined for query");

				indexRequest = new IndexRequest(query.getIndexName(), query.getType(), query.getId()).source(query.getSource(),
						Requests.INDEX_CONTENT_TYPE);

			} else {
				throw new ElasticsearchException("Object or source is null, failed to index document");
			}

			if (query.getVersion() != null) {
				indexRequest.version(query.getVersion());

				if (entity != null) {
					indexRequest.versionType(entity.getPersistentEntity().getVersionType());
				}
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
	public void refresh(String indexName) {

		Assert.notNull(indexName, "No index defined to refresh");
		try {
			// TODO Do something with the response.
			client.indices().refresh(Requests.refreshRequest(indexName), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to refresh index: " + indexName, e);
		}
	}

	@Override
	public <T> void refresh(Class<T> clazz) {
		refresh(operations.determineIndexName(clazz));
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {

		Request request = new Request("GET", String.format("/%s/_alias/*", indexName));
		Response response = lowLevelRequest(request);

		try {
			return convertAliasResponse(EntityUtils.toString(response.getEntity()));
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
	List<AliasMetaData> convertAliasResponse(String aliasResponse) {
		try {
			JsonNode node = objectMapper.readTree(aliasResponse);
			node = node.findValue("aliases");

			Map<String, AliasData> aliasData = objectMapper.readValue(objectMapper.writeValueAsString(node),
					new TypeReference<Map<String, AliasData>>() {
					});

			Iterable<Map.Entry<String, AliasData>> aliasIter = aliasData.entrySet();
			List<AliasMetaData> aliasMetaDataList = new ArrayList<AliasMetaData>();

			for (Map.Entry<String, AliasData> aliasentry : aliasIter) {
				AliasData data = aliasentry.getValue();
				aliasMetaDataList.add(AliasMetaData.newAliasMetaDataBuilder(aliasentry.getKey()).filter(data.getFilter())
						.routing(data.getRouting()).searchRouting(data.getSearch_routing()).indexRouting(data.getIndex_routing())
						.build());
			}
			return aliasMetaDataList;
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + aliasResponse, e);
		}
	}

	protected ResultsMapper getResultsMapper() {
		return resultsMapper;
	}

	@Deprecated
	public static String readFileFromClasspath(String url) {
		return ResourceUtil.readFileFromClasspath(url);
	}

	public SearchResponse suggest(SuggestBuilder suggestion, String... indices) {

		Assert.notNull(indices, "No index defined for query");
		SearchRequest searchRequest = new SearchRequest(indices);
		searchRequest.source().suggest(suggestion);

		return search(searchRequest);
	}

	public SearchResponse suggest(SuggestBuilder suggestion, Class<?> clazz) {
		return suggest(suggestion, operations.determineIndexName(clazz));
	}

	private SearchResponse search(SearchRequest request) {
		try {
			return client.search(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error on search request: " + request.toString(), e);
		}
	}

	private SearchResponse scroll(SearchScrollRequest request) {
		try {
			return client.scroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error on scroll request: " + request.toString(), e);
		}
	}

	private MultiSearchResponse.Item[] multiSearchItems(MultiSearchRequest request) {
		try {
			MultiSearchResponse response = client.msearch(request, RequestOptions.DEFAULT);
			MultiSearchResponse.Item[] items = response.getResponses();
			Assert.isTrue(items.length == request.requests().size(), "Response should has same length with queries");
			return items;
		} catch (IOException e) {
			throw new ElasticsearchException("Error on multi search request: " + request.toString(), e);
		}
	}

	private void bulk(BulkRequest request) {
		try {
			BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);

			if (response.hasFailures()) {
				Map<String, String> failedDocuments = new HashMap<>();
				for (BulkItemResponse item : response.getItems()) {
					if (item.isFailed())
						failedDocuments.put(item.getId(), item.getFailureMessage());
				}
				throw new ElasticsearchException(
						"Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
								+ failedDocuments + "]",
						failedDocuments);
			}
		} catch (IOException e) {
			throw new ElasticsearchException("Error on bulk request: " + request.toString(), e);
		}
	}

	private AcknowledgedResponse updateAliases(IndicesAliasesRequest request) {
		try {
			return client.indices().updateAliases(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("error on update aliases request: " + request, e);
		}
	}

	private Response lowLevelRequest(Request request) {
		RestClient restClient = client.getLowLevelClient();
		try {
			return restClient.performRequest(request);
		} catch (Exception e) {
			throw new ElasticsearchException("Error on low level request: " + request.toString(), e);
		}
	}

}
