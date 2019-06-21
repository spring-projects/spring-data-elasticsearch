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

import static org.elasticsearch.client.Requests.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
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
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
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
import org.springframework.util.Assert;
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
 */
public class ElasticsearchRestTemplate
		implements ElasticsearchOperations, EsClient<RestHighLevelClient>, ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchRestTemplate.class);
	private RestHighLevelClient client;
	private ElasticsearchConverter elasticsearchConverter;
	private ResultsMapper resultsMapper;
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

		Assert.notNull(client, "Client must not be null!");
		Assert.notNull(elasticsearchConverter, "ElasticsearchConverter must not be null!");
		Assert.notNull(resultsMapper, "ResultsMapper must not be null!");

		this.client = client;
		this.elasticsearchConverter = elasticsearchConverter;
		this.resultsMapper = resultsMapper;
	}

	@Override
	public RestHighLevelClient getClient() {
		return client;
	}

	public void setSearchTimeout(String searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	@Override
	public boolean addAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.add()
				.alias(query.getAliasName()).index(query.getIndexName());

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
			return client.indices().updateAliases(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to update aliases with request: " + request, e);
		}
	}

	@Override
	public boolean removeAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		AliasActions aliasAction = new AliasActions(AliasActions.Type.REMOVE);
		request.addAliasAction(aliasAction);
		try {
			return client.indices().updateAliases(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to update aliases with request: " + request, e);
		}
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz) {
		return createIndexIfNotCreated(clazz);
	}

	@Override
	public boolean createIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for Query");
		try {
			return client.indices().create(createIndexRequest(indexName), RequestOptions.DEFAULT).isAcknowledged();
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to create index " + indexName, e);
		}
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz) {
		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();
			if (hasText(mappingPath)) {
				String mappings = ResourceUtil.readFileFromClasspath(mappingPath);
				if (hasText(mappings)) {
					return putMapping(clazz, mappings);
				}
			} else {
				logger.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
			}
		}
		try {
			MappingBuilder mappingBuilder = new MappingBuilder(elasticsearchConverter);
			return putMapping(clazz, mappingBuilder.buildPropertyMapping(clazz));
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz, Object mapping) {
		return putMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType(),
				mapping);
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
			return client.indices().putMapping(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Failed to put mapping for " + indexName, e);
		}
	}

	@Override
	public Map<String, Object> getMapping(String indexName, String type) {
		Assert.notNull(indexName, "No index defined for getMapping()");
		Assert.notNull(type, "No type defined for getMapping()");
		Map<String, Object> mappings = null;
		RestClient restClient = client.getLowLevelClient();
		try {
			Response response = restClient.performRequest("GET", "/" + indexName + "/_mapping/" + type);
			mappings = convertMappingResponse(EntityUtils.toString(response.getEntity()), type);
		} catch (Exception e) {
			throw new ElasticsearchException(
					"Error while getting mapping for indexName : " + indexName + " type : " + type + " ", e);
		}
		return mappings;
	}

	@Override
	public <T> Map<String, Object> getMapping(Class<T> clazz) {
		return getMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType());
	}

	private Map<String, Object> convertMappingResponse(String mappingResponse, String type) {
		ObjectMapper mapper = new ObjectMapper();

		try {
			Map<String, Object> result = null;
			JsonNode node = mapper.readTree(mappingResponse);

			node = node.findValue("mappings").findValue(type);
			result = mapper.readValue(mapper.writeValueAsString(node), HashMap.class);

			return result;
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + mappingResponse, e);
		}

	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return elasticsearchConverter;
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz) {
		return queryForObject(query, clazz, resultsMapper);
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {
		ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
		GetRequest request = new GetRequest(persistentEntity.getIndexName(), persistentEntity.getIndexType(),
				query.getId());
		GetResponse response;
		try {
			response = client.get(request, RequestOptions.DEFAULT);
			return mapper.mapResult(response, clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while getting for request: " + request.toString(), e);
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
		MultiSearchResponse response;
		try {
			response = client.multiSearch(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + request.toString(), e);
		}
		MultiSearchResponse.Item[] items = response.getResponses();
		Assert.isTrue(items.length == request.requests().size(), "Response should has same length with queries");
		return items;
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
		SearchResponse response = doSearch(prepareSearch(query, Optional.ofNullable(query.getQuery())), query);
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
		SearchRequest request = prepareSearch(query, Optional.ofNullable(query.getQuery()));
		request.source().query(query.getQuery());
		if (query.getFilter() != null) {
			request.source().postFilter(query.getFilter());
		}
		SearchResponse response;
		try {
			response = client.search(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + request.toString(), e);
		}
		return extractIds(response);
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

		if (elasticsearchFilter != null)
			request.source().postFilter(elasticsearchFilter);
		if (logger.isDebugEnabled()) {
			logger.debug("doSearch query:\n" + request.toString());
		}

		SearchResponse response;
		try {
			response = client.search(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + request.toString(), e);
		}
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
		SearchResponse response;
		try {
			response = client.search(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + request.toString(), e);
		}
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

	@Override
	public <T> long count(CriteriaQuery query) {
		return count(query, null);
	}

	@Override
	public <T> long count(SearchQuery query) {
		return count(query, null);
	}

	private long doCount(SearchRequest countRequest, QueryBuilder elasticsearchQuery) {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		if (elasticsearchQuery != null) {
			sourceBuilder.query(elasticsearchQuery);
		}
		countRequest.source(sourceBuilder);

		try {
			return client.search(countRequest, RequestOptions.DEFAULT).getHits().getTotalHits();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while searching for request: " + countRequest.toString(), e);
		}
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
		SearchResponse response;
		try {
			response = client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + searchRequest.toString(), e);
		}
		return response.getHits().getTotalHits();
	}

	private <T> SearchRequest prepareCount(Query query, Class<T> clazz) {
		String indexName[] = !isEmpty(query.getIndices())
				? query.getIndices().toArray(new String[query.getIndices().size()])
				: retrieveIndexNameFromPersistentEntity(clazz);
		String types[] = !isEmpty(query.getTypes()) ? query.getTypes().toArray(new String[query.getTypes().size()])
				: retrieveTypeFromPersistentEntity(clazz);

		Assert.notNull(indexName, "No index defined for Query");

		SearchRequest countRequestBuilder = new SearchRequest(indexName);

		if (types != null) {
			countRequestBuilder.types(types);
		}
		return countRequestBuilder;
	}

	@Override
	public <T> List<T> multiGet(SearchQuery searchQuery, Class<T> clazz) {
		return resultsMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	private <T> MultiGetResponse getMultiResponse(Query searchQuery, Class<T> clazz) {

		String indexName = !isEmpty(searchQuery.getIndices()) ? searchQuery.getIndices().get(0)
				: getPersistentEntityFor(clazz).getIndexName();
		String type = !isEmpty(searchQuery.getTypes()) ? searchQuery.getTypes().get(0)
				: getPersistentEntityFor(clazz).getIndexType();

		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notEmpty(searchQuery.getIds(), "No Id define for Query");

		MultiGetRequest request = new MultiGetRequest();

		if (searchQuery.getFields() != null && !searchQuery.getFields().isEmpty()) {
			searchQuery.addSourceFilter(new FetchSourceFilter(toArray(searchQuery.getFields()), null));
		}

		for (String id : searchQuery.getIds()) {

			MultiGetRequest.Item item = new MultiGetRequest.Item(indexName, type, id);

			if (searchQuery.getRoute() != null) {
				item = item.routing(searchQuery.getRoute());
			}

			request.add(item);
		}
		try {
			return client.multiGet(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while multiget for request: " + request.toString(), e);
		}
	}

	@Override
	public <T> List<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper getResultMapper) {
		return getResultMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
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
			setPersistentEntityId(query.getObject(), documentId);
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
		String indexName = hasText(query.getIndexName()) ? query.getIndexName()
				: getPersistentEntityFor(query.getClazz()).getIndexName();
		String type = hasText(query.getType()) ? query.getType() : getPersistentEntityFor(query.getClazz()).getIndexType();
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
			checkForBulkUpdateFailure(client.bulk(bulkRequest, RequestOptions.DEFAULT));
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
			checkForBulkUpdateFailure(client.bulk(bulkRequest, RequestOptions.DEFAULT));
		} catch (IOException e) {
			throw new ElasticsearchException("Error while bulk for request: " + bulkRequest.toString(), e);
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
		return indexExists(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public boolean indexExists(String indexName) {
		GetIndexRequest request = new GetIndexRequest();
		request.indices(indexName);
		try {
			return client.indices().exists(request, RequestOptions.DEFAULT);
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
	public <T> boolean deleteIndex(Class<T> clazz) {
		return deleteIndex(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public boolean deleteIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for delete operation");
		if (indexExists(indexName)) {
			DeleteIndexRequest request = new DeleteIndexRequest(indexName);
			try {
				return client.indices().delete(request, RequestOptions.DEFAULT).isAcknowledged();
			} catch (IOException e) {
				throw new ElasticsearchException("Error while deleting index request: " + request.toString(), e);
			}
		}
		return false;
	}

	@Override
	public String delete(String indexName, String type, String id) {
		DeleteRequest request = new DeleteRequest(indexName, type, id);
		try {
			return client.delete(request, RequestOptions.DEFAULT).getId();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while deleting item request: " + request.toString(), e);
		}
	}

	@Override
	public <T> String delete(Class<T> clazz, String id) {
		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
		return delete(persistentEntity.getIndexName(), persistentEntity.getIndexType(), id);
	}

	@Override
	public <T> void delete(DeleteQuery deleteQuery, Class<T> clazz) {

		String indexName = hasText(deleteQuery.getIndex()) ? deleteQuery.getIndex()
				: getPersistentEntityFor(clazz).getIndexName();
		String typeName = hasText(deleteQuery.getType()) ? deleteQuery.getType()
				: getPersistentEntityFor(clazz).getIndexType();

		DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(indexName) //
				.setDocTypes(typeName) //
				.setQuery(deleteQuery.getQuery()) //
				.setAbortOnVersionConflict(false) //
				.setRefresh(true);

		if (deleteQuery.getPageSize() != null)
			deleteByQueryRequest.setBatchSize(deleteQuery.getPageSize());

		if (deleteQuery.getScrollTimeInMillis() != null)
			deleteByQueryRequest.setScroll(TimeValue.timeValueMillis(deleteQuery.getScrollTimeInMillis()));

		try {
			client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for delete request: " + deleteByQueryRequest.toString(), e);
		}
	}

	@Override
	public void delete(DeleteQuery deleteQuery) {
		Assert.notNull(deleteQuery.getIndex(), "No index defined for Query");
		Assert.notNull(deleteQuery.getType(), "No type define for Query");
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

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			searchSourceBuilder.fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (!isEmpty(query.getFields())) {
			searchSourceBuilder.fetchSource(toArray(query.getFields()), null);
		}

		if (query.getSort() != null) {
			prepareSort(query, searchSourceBuilder);
		}

		request.source(searchSourceBuilder);
		return request;
	}

	private SearchResponse doScroll(SearchRequest request, CriteriaQuery criteriaQuery) {
		Assert.notNull(criteriaQuery.getIndices(), "No index defined for Query");
		Assert.notNull(criteriaQuery.getTypes(), "No type define for Query");
		Assert.notNull(criteriaQuery.getPageable(), "Query.pageable is required for scan & scroll");

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

		try {
			return client.search(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
	}

	private SearchResponse doScroll(SearchRequest request, SearchQuery searchQuery) {
		Assert.notNull(searchQuery.getIndices(), "No index defined for Query");
		Assert.notNull(searchQuery.getTypes(), "No type define for Query");
		Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scan & scroll");

		if (searchQuery.getQuery() != null) {
			request.source().query(searchQuery.getQuery());
		} else {
			request.source().query(QueryBuilders.matchAllQuery());
		}

		if (searchQuery.getFilter() != null) {
			request.source().postFilter(searchQuery.getFilter());
		}
		request.source().version(true);

		if (!isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder sort : searchQuery.getElasticsearchSorts()) {
				request.source().sort(sort);
			}
		}

		try {
			return client.search(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
	}

	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz) {
		SearchResponse response = doScroll(prepareScroll(searchQuery, scrollTimeInMillis, clazz), searchQuery);
		return resultsMapper.mapResults(response, clazz, null);
	}

	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz) {
		SearchResponse response = doScroll(prepareScroll(criteriaQuery, scrollTimeInMillis, clazz), criteriaQuery);
		return resultsMapper.mapResults(response, clazz, null);
	}

	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchResponse response = doScroll(prepareScroll(searchQuery, scrollTimeInMillis, clazz), searchQuery);
		return mapper.mapResults(response, clazz, null);
	}

	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchResponse response = doScroll(prepareScroll(criteriaQuery, scrollTimeInMillis, clazz), criteriaQuery);
		return mapper.mapResults(response, clazz, null);
	}

	public <T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz) {
		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));
		SearchResponse response;
		try {
			response = client.searchScroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
		return resultsMapper.mapResults(response, clazz, Pageable.unpaged());
	}

	public <T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));
		SearchResponse response;
		try {
			response = client.searchScroll(request, RequestOptions.DEFAULT);
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
			ClearScrollResponse response = client.clearScroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
	}

	@Override
	public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {

		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
		String indexName = hasText(query.getIndexName()) ? query.getIndexName() : persistentEntity.getIndexName();
		String type = hasText(query.getType()) ? query.getType() : persistentEntity.getIndexType();

		Assert.notNull(indexName, "No 'indexName' defined for MoreLikeThisQuery");
		Assert.notNull(type, "No 'type' defined for MoreLikeThisQuery");
		Assert.notNull(query.getId(), "No document id defined for MoreLikeThisQuery");

		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = moreLikeThisQuery(
				toArray(new MoreLikeThisQueryBuilder.Item(indexName, type, query.getId())));

		if (query.getMinTermFreq() != null) {
			moreLikeThisQueryBuilder.minTermFreq(query.getMinTermFreq());
		}
		if (query.getMaxQueryTerms() != null) {
			moreLikeThisQueryBuilder.maxQueryTerms(query.getMaxQueryTerms());
		}
		if (!isEmpty(query.getStopWords())) {
			moreLikeThisQueryBuilder.stopWords(toArray(query.getStopWords()));
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
		prepareSearch(searchRequest, searchQuery);

		try {
			return client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + searchRequest.toString(), e);
		}
	}

	private SearchRequest prepareSearch(SearchRequest searchRequest, SearchQuery searchQuery) {
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

		if (!isEmpty(searchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : searchQuery.getIndicesBoost()) {
				searchRequest.source().indexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(searchQuery.getAggregations())) {
			for (AbstractAggregationBuilder aggregationBuilder : searchQuery.getAggregations()) {
				searchRequest.source().aggregation(aggregationBuilder);
			}
		}

		if (!isEmpty(searchQuery.getFacets())) {
			for (FacetRequest aggregatedFacet : searchQuery.getFacets()) {
				searchRequest.source().aggregation(aggregatedFacet.getFacet());
			}
		}
		return searchRequest;
	}

	private SearchResponse getSearchResponse(ActionFuture<SearchResponse> response) {
		return searchTimeout == null ? response.actionGet() : response.actionGet(searchTimeout);
	}

	private <T> boolean createIndexIfNotCreated(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName()) || createIndexWithSettings(clazz);
	}

	private <T> boolean createIndexWithSettings(Class<T> clazz) {
		if (clazz.isAnnotationPresent(Setting.class)) {
			String settingPath = clazz.getAnnotation(Setting.class).settingPath();
			if (hasText(settingPath)) {
				String settings = ResourceUtil.readFileFromClasspath(settingPath);
				if (hasText(settings)) {
					return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
				}
			} else {
				logger.info("settingPath in @Setting has to be defined. Using default instead.");
			}
		}
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), getDefaultSettings(getPersistentEntityFor(clazz)));
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
			return client.indices().create(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Error for creating index: " + request.toString(), e);
		}
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz, Object settings) {
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
	}

	private <T> Map getDefaultSettings(ElasticsearchPersistentEntity<T> persistentEntity) {

		if (persistentEntity.isUseServerConfiguration())
			return new HashMap();

		return new MapBuilder<String, String>().put("index.number_of_shards", String.valueOf(persistentEntity.getShards()))
				.put("index.number_of_replicas", String.valueOf(persistentEntity.getReplicas()))
				.put("index.refresh_interval", persistentEntity.getRefreshInterval())
				.put("index.store.type", persistentEntity.getIndexStoreType()).map();
	}

	@Override
	public <T> Map getSetting(Class<T> clazz) {
		return getSetting(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override // TODO change interface to return Settings.
	public Map getSetting(String indexName) {
		Assert.notNull(indexName, "No index defined for getSettings");
		ObjectMapper objMapper = new ObjectMapper();
		Map settings = null;
		RestClient restClient = client.getLowLevelClient();
		try {
			Response response = restClient.performRequest("GET", "/" + indexName + "/_settings");
			settings = convertSettingResponse(EntityUtils.toString(response.getEntity()), indexName);

		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting settings for indexName : " + indexName, e);
		}
		return settings;
	}

	private Map<String, String> convertSettingResponse(String settingResponse, String indexName) {
		ObjectMapper mapper = new ObjectMapper();

		try {
			Settings settings = Settings.fromXContent(XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY,
					DeprecationHandler.THROW_UNSUPPORTED_OPERATION, settingResponse));
			String prefix = indexName + ".settings.";
			// Backwards compatibility. TODO Change to return Settings object.
			Map<String, String> result = new HashMap<String, String>();
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

		int startRecord = 0;
		SearchRequest request = new SearchRequest(toArray(query.getIndices()));
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		request.types(toArray(query.getTypes()));
		sourceBuilder.version(true);
		sourceBuilder.trackScores(query.getTrackScores());

		if (builder.isPresent()) {
			sourceBuilder.query(builder.get());
		}

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
			sourceBuilder.fetchSource(toArray(query.getFields()), null);
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(query.getIndicesOptions());
		}

		if (query.getSort() != null) {
			prepareSort(query, sourceBuilder);
		}

		if (query.getMinScore() > 0) {
			sourceBuilder.minScore(query.getMinScore());
		}

		if (query.getPreference() != null) {
			request.preference(query.getPreference());
		}

		request.source(sourceBuilder);
		return request;
	}

	private void prepareSort(Query query, SearchSourceBuilder sourceBuilder) {
		for (Sort.Order order : query.getSort()) {
			FieldSortBuilder sort = SortBuilders.fieldSort(order.getProperty())
					.order(order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC);
			if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
				sort.missing("_first");
			} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
				sort.missing("_last");
			}
			sourceBuilder.sort(sort);
		}
	}

	private IndexRequest prepareIndex(IndexQuery query) {
		try {
			String indexName = StringUtils.isEmpty(query.getIndexName())
					? retrieveIndexNameFromPersistentEntity(query.getObject().getClass())[0]
					: query.getIndexName();
			String type = StringUtils.isEmpty(query.getType())
					? retrieveTypeFromPersistentEntity(query.getObject().getClass())[0]
					: query.getType();

			IndexRequest indexRequest = null;

			if (query.getObject() != null) {
				String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
				// If we have a query id and a document id, do not ask ES to generate one.
				if (id != null) {
					indexRequest = new IndexRequest(indexName, type, id);
				} else {
					indexRequest = new IndexRequest(indexName, type);
				}
				indexRequest.source(resultsMapper.getEntityMapper().mapToString(query.getObject()),
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
				VersionType versionType = retrieveVersionTypeFromPersistentEntity(query.getObject().getClass());
				indexRequest.versionType(versionType);
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
		Assert.notNull(indexName, "No index defined for refresh()");
		try {
			// TODO: Do something with the response.
			client.indices().refresh(refreshRequest(indexName), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to refresh index: " + indexName, e);
		}
	}

	@Override
	public <T> void refresh(Class<T> clazz) {
		refresh(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		List<AliasMetaData> aliases = null;
		RestClient restClient = client.getLowLevelClient();
		Response response;
		String aliasResponse;

		try {
			response = restClient.performRequest("GET", "/" + indexName + "/_alias/*");
			aliasResponse = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + indexName, e);
		}

		return convertAliasResponse(aliasResponse);
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
			String name = names.next();
			node = node.findValue("aliases");

			Map<String, AliasData> aliasData = mapper.readValue(mapper.writeValueAsString(node),
					new TypeReference<Map<String, AliasData>>() {});

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

	@Override
	public ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz) {
		Assert.isTrue(clazz.isAnnotationPresent(Document.class), "Unable to identify index name. " + clazz.getSimpleName()
				+ " is not a Document. Make sure the document class is annotated with @Document(indexName=\"foo\")");
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	private String getPersistentEntityId(Object entity) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(entity.getClass());
		Object identifier = persistentEntity.getIdentifierAccessor(entity).getIdentifier();

		if (identifier != null) {
			return identifier.toString();
		}

		return null;
	}

	private void setPersistentEntityId(Object entity, String id) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(entity.getClass());
		ElasticsearchPersistentProperty idProperty = persistentEntity.getIdProperty();

		// Only deal with text because ES generated Ids are strings !

		if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
			persistentEntity.getPropertyAccessor(entity).setProperty(idProperty, id);
		}
	}

	private void setPersistentEntityIndexAndType(Query query, Class clazz) {
		if (query.getIndices().isEmpty()) {
			query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
		}
		if (query.getTypes().isEmpty()) {
			query.addTypes(retrieveTypeFromPersistentEntity(clazz));
		}
	}

	private String[] retrieveIndexNameFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[] { getPersistentEntityFor(clazz).getIndexName() };
		}
		return null;
	}

	private String[] retrieveTypeFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[] { getPersistentEntityFor(clazz).getIndexType() };
		}
		return null;
	}

	private VersionType retrieveVersionTypeFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return getPersistentEntityFor(clazz).getVersionType();
		}
		return VersionType.EXTERNAL;
	}

	private List<String> extractIds(SearchResponse response) {
		List<String> ids = new ArrayList<>();
		for (SearchHit hit : response.getHits()) {
			if (hit != null) {
				ids.add(hit.getId());
			}
		}
		return ids;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (elasticsearchConverter instanceof ApplicationContextAware) {
			((ApplicationContextAware) elasticsearchConverter).setApplicationContext(context);
		}
	}

	private static String[] toArray(List<String> values) {
		String[] valuesAsArray = new String[values.size()];
		return values.toArray(valuesAsArray);
	}

	private static MoreLikeThisQueryBuilder.Item[] toArray(MoreLikeThisQueryBuilder.Item... values) {
		return values;
	}

	protected ResultsMapper getResultsMapper() {
		return resultsMapper;
	}

	@Deprecated
	public static String readFileFromClasspath(String url) {
		return ResourceUtil.readFileFromClasspath(url);
	}

	public SearchResponse suggest(SuggestBuilder suggestion, String... indices) {
		SearchRequest searchRequest = new SearchRequest(indices);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.suggest(suggestion);
		searchRequest.source(sourceBuilder);

		try {
			return client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Could not execute search request : " + searchRequest.toString(), e);
		}
	}

	public SearchResponse suggest(SuggestBuilder suggestion, Class clazz) {
		return suggest(suggestion, retrieveIndexNameFromPersistentEntity(clazz));
	}
}
