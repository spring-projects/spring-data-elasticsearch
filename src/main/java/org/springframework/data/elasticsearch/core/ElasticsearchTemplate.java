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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.support.SearchHitsUtil;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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
 * @deprecated as of 4.0
 */
@Deprecated
public class ElasticsearchTemplate extends AbstractElasticsearchTemplate {

	private static final Logger QUERY_LOGGER = LoggerFactory
			.getLogger("org.springframework.data.elasticsearch.core.QUERY");

	private Client client;
	private String searchTimeout;

	public ElasticsearchTemplate(Client client) {
		initialize(client, createElasticsearchConverter());
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter) {
		initialize(client, elasticsearchConverter);
	}

	private void initialize(Client client, ElasticsearchConverter elasticsearchConverter) {
		Assert.notNull(client, "Client must not be null!");
		this.client = client;
		initialize(elasticsearchConverter);
	}

	public void setSearchTimeout(String searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	@Override
	public boolean addAlias(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest.AliasActions aliasAction = requestFactory.aliasAction(query, index);
		return client.admin().indices().prepareAliases().addAliasAction(aliasAction).execute().actionGet().isAcknowledged();
	}

	@Override
	public boolean removeAlias(AliasQuery query, IndexCoordinates index) {
		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		return client.admin().indices().prepareAliases().removeAlias(index.getIndexName(), query.getAliasName()).execute()
				.actionGet().isAcknowledged();
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {
		CreateIndexRequestBuilder createIndexRequestBuilder = requestFactory.createIndexRequestBuilder(client, indexName,
				settings);
		return createIndexRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	public boolean putMapping(IndexCoordinates index, Object mapping) {
		Assert.notNull(index, "No index defined for putMapping()");
		PutMappingRequestBuilder requestBuilder = requestFactory.putMappingRequestBuilder(client, index, mapping);
		return requestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	public Map<String, Object> getMapping(IndexCoordinates index) {
		Assert.notNull(index, "No index defined for putMapping()");
		try {
			return client.admin().indices()
					.getMappings(new GetMappingsRequest().indices(index.getIndexNames()).types(index.getTypeNames())).actionGet()
					.getMappings().get(index.getIndexName()).get(index.getTypeName()).getSourceAsMap();
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + index.getIndexName()
					+ " type : " + index.getTypeName() + ' ' + e.getMessage());
		}
	}

	@Override
	public <T> T get(GetQuery query, Class<T> clazz, IndexCoordinates index) {
		GetRequestBuilder getRequestBuilder = requestFactory.getRequestBuilder(client, query, index);
		GetResponse response = getRequestBuilder.execute().actionGet();
		T entity = elasticsearchConverter.mapDocument(DocumentAdapters.from(response), clazz);
		return entity;
	}

	@Nullable
	private <T> T getObjectFromPage(Page<T> page) {
		int contentSize = page.getContent().size();
		Assert.isTrue(contentSize < 2, "Expected 1 but found " + contentSize + " results");
		return contentSize > 0 ? page.getContent().get(0) : null;
	}

	@Override
	protected MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request) {
		ActionFuture<MultiSearchResponse> future = client.multiSearch(request);
		MultiSearchResponse response = future.actionGet();
		MultiSearchResponse.Item[] items = response.getResponses();
		Assert.isTrue(items.length == request.requests().size(), "Response should have same length with queries");
		return items;
	}

	@Override
	public <T> T query(Query query, ResultsExtractor<T> resultsExtractor, Class<T> clazz, IndexCoordinates index) {
		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, clazz, index);
		SearchResponse response = getSearchResponse(searchRequestBuilder);
		return resultsExtractor.extract(response);
	}

	@Override
	public <T> List<T> queryForList(Query query, Class<T> clazz, IndexCoordinates index) {
		return queryForPage(query, clazz, index).getContent();
	}

	@Override
	public List<String> queryForIds(Query query, Class<?> clazz, IndexCoordinates index) {
		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, clazz, index);
		SearchResponse response = getSearchResponse(searchRequestBuilder);
		return extractIds(response);
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(Query query, Class<T> clazz, IndexCoordinates index) {
		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, clazz, index);
		SearchResponse response = getSearchResponse(searchRequestBuilder);
		return elasticsearchConverter.mapResults(SearchDocumentResponse.from(response), clazz, query.getPageable());
	}

	@Override
	public <T> CloseableIterator<T> stream(Query query, Class<T> clazz, IndexCoordinates index) {
		long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		return doStream(scrollTimeInMillis, startScroll(scrollTimeInMillis, query, clazz, index), clazz);
	}

	private <T> CloseableIterator<T> doStream(long scrollTimeInMillis, ScrolledPage<T> page, Class<T> clazz) {
		return StreamQueries.streamResults(page, scrollId -> continueScroll(scrollId, scrollTimeInMillis, clazz),
				this::clearScroll);
	}

	@Override
	public long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {
		Assert.notNull(index, "index must not be null");
		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, clazz, index);
		searchRequestBuilder.setSize(0);

		return SearchHitsUtil.getTotalCount(getSearchResponse(searchRequestBuilder).getHits());
	}

	@Override
	public <T> List<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {
		Assert.notNull(index, "index must not be null");
		Assert.notEmpty(query.getIds(), "No Id define for Query");
		MultiGetRequestBuilder builder = requestFactory.multiGetRequestBuilder(client, query, index);
		return elasticsearchConverter.mapDocuments(DocumentAdapters.from(builder.execute().actionGet()), clazz);
	}

	@Override
	public String index(IndexQuery query, IndexCoordinates index) {
		IndexRequestBuilder indexRequestBuilder = requestFactory.indexRequestBuilder(client, query, index);
		String documentId = indexRequestBuilder.execute().actionGet().getId();
		// We should call this because we are not going through a mapper.
		if (query.getObject() != null) {
			setPersistentEntityId(query.getObject(), documentId);
		}
		return documentId;
	}

	@Override
	public UpdateResponse update(UpdateQuery query, IndexCoordinates index) {
		UpdateRequestBuilder updateRequestBuilder = requestFactory.updateRequestBuilderFor(client, query, index);
		return updateRequestBuilder.execute().actionGet();
	}

	@Override
	public void bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		Assert.notNull(queries, "List of IndexQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		doBulkOperation(queries, bulkOptions, index);
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		doBulkOperation(queries, bulkOptions, index);
	}

	private void doBulkOperation(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		BulkRequestBuilder bulkRequest = requestFactory.bulkRequestBuilder(client, queries, bulkOptions, index);
		checkForBulkOperationFailure(bulkRequest.execute().actionGet());
	}

	@Override
	public boolean indexExists(String indexName) {
		return client.admin().indices().exists(indicesExistsRequest(indexName)).actionGet().isExists();
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
	public String delete(String id, IndexCoordinates index) {
		return client.prepareDelete(index.getIndexName(), index.getTypeName(), id).execute().actionGet().getId();
	}

	@Override
	public void delete(DeleteQuery deleteQuery, IndexCoordinates index) {
		requestFactory.deleteByQueryRequestBuilder(client, deleteQuery, index).get();
	}

	@Override
	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, Query query, Class<T> clazz, IndexCoordinates index) {
		Assert.notNull(query.getPageable(), "Query.pageable is required for scan & scroll");
		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, clazz, index);
		searchRequestBuilder.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis));
		SearchResponse response = getSearchResponse(searchRequestBuilder);
		return elasticsearchConverter.mapResults(SearchDocumentResponse.from(response), clazz, null);
	}

	@Override
	public <T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz) {
		SearchResponse response = getSearchResponseWithTimeout(
				client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute());
		return elasticsearchConverter.mapResults(SearchDocumentResponse.from(response), clazz, Pageable.unpaged());
	}

	@Override
	public void clearScroll(String scrollId) {
		client.prepareClearScroll().addScrollId(scrollId).execute().actionGet();
	}

	private SearchResponse getSearchResponse(SearchRequestBuilder requestBuilder) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(requestBuilder.toString());
		}
		return getSearchResponseWithTimeout(requestBuilder.execute());
	}

	private SearchResponse getSearchResponseWithTimeout(ActionFuture<SearchResponse> response) {
		return searchTimeout == null ? response.actionGet() : response.actionGet(searchTimeout);
	}

	@Override
	public <T> Map<String, Object> getSetting(Class<T> clazz) {
		return getSetting(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public Map<String, Object> getSetting(String indexName) {
		Assert.notNull(indexName, "No index defined for getSettings");
		Settings settings = client.admin().indices().getSettings(new GetSettingsRequest()).actionGet().getIndexToSettings()
				.get(indexName);
		return settings.keySet().stream().collect(Collectors.toMap((key) -> key, (key) -> settings.get(key)));
	}

	@Override
	public void refresh(IndexCoordinates index) {
		Assert.notNull(index, "No index defined for refresh()");
		client.admin().indices().refresh(refreshRequest(index.getIndexNames())).actionGet();
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		return client.admin().indices().getAliases(new GetAliasesRequest().indices(indexName)).actionGet().getAliases()
				.get(indexName);
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

	public SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index) {
		return client.prepareSearch(index.getIndexNames()).suggest(suggestion).get();
	}

}
