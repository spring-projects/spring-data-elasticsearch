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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequest;
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
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.client.support.AliasData;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.support.SearchHitsUtil;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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
 * @author Massimiliano Poggi
 */
public class ElasticsearchRestTemplate extends AbstractElasticsearchTemplate {

	private RestHighLevelClient client;

	public ElasticsearchRestTemplate(RestHighLevelClient client) {
		initialize(client, createElasticsearchConverter());
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {
		initialize(client, elasticsearchConverter);
	}

	private void initialize(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {
		Assert.notNull(client, "Client must not be null!");
		this.client = client;
		initialize(elasticsearchConverter);
	}

	@Override
	public boolean addAlias(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest request = requestFactory.indicesAddAliasesRequest(query, index);
		try {
			return client.indices().updateAliases(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to update aliases with request: " + request, e);
		}
	}

	@Override
	public boolean removeAlias(AliasQuery query, IndexCoordinates index) {
		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		IndicesAliasesRequest indicesAliasesRequest = requestFactory.indicesRemoveAliasesRequest(query, index);
		try {
			return client.indices().updateAliases(indicesAliasesRequest, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to update aliases with indicesRemoveAliasesRequest: " + indicesAliasesRequest, e);
		}
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {
		CreateIndexRequest request = requestFactory.createIndexRequest(indexName, settings);
		try {
			return client.indices().create(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Error for creating index: " + request.toString(), e);
		}
	}

	@Override
	public boolean putMapping(IndexCoordinates index, Object mapping) {
		Assert.notNull(index, "No index defined for putMapping()");
		PutMappingRequest request = requestFactory.putMappingRequest(index, mapping);
		try {
			return client.indices().putMapping(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Failed to put mapping for " + index.getIndexName(), e);
		}
	}

	@Override
	public Map<String, Object> getMapping(IndexCoordinates index) {
		Assert.notNull(index, "No index defined for getMapping()");
		RestClient restClient = client.getLowLevelClient();
		try {
			Request request = new Request("GET",
					'/' + index.getIndexName() + "/_mapping/" + index.getTypeName() + "?include_type_name=true");
			Response response = restClient.performRequest(request);
			return convertMappingResponse(EntityUtils.toString(response.getEntity()), index.getTypeName());
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + index.getIndexName()
					+ " type : " + index.getTypeName() + ' ', e);
		}
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
	public <T> T get(GetQuery query, Class<T> clazz, IndexCoordinates index) {
		GetRequest request = requestFactory.getRequest(query, index);
		try {
			GetResponse response = client.get(request, RequestOptions.DEFAULT);
			return elasticsearchConverter.mapDocument(DocumentAdapters.from(response), clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while getting for request: " + request.toString(), e);
		}
	}

	@Nullable
	private <T> T getObjectFromPage(Page<T> page) {
		int contentSize = page.getContent().size();
		Assert.isTrue(contentSize < 2, "Expected 1 but found " + contentSize + " results");
		return contentSize > 0 ? page.getContent().get(0) : null;
	}

	@Override
	protected MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request) {
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
	public <T> AggregatedPage<T> queryForPage(Query query, Class<T> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		SearchResponse response;
		try {
			response = client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + searchRequest.toString(), e);
		}
		return elasticsearchConverter.mapResults(SearchDocumentResponse.from(response), clazz, query.getPageable());
	}

	@Override
	public <T> T query(Query query, ResultsExtractor<T> resultsExtractor, @Nullable Class<T> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		try {
			SearchResponse result = client.search(searchRequest, RequestOptions.DEFAULT);
			return resultsExtractor.extract(result);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + searchRequest.toString(), e);
		}
	}

	@Override
	public <T> List<T> queryForList(Query query, Class<T> clazz, IndexCoordinates index) {
		return queryForPage(query, clazz, index).getContent();
	}

	@Override
	public List<String> queryForIds(Query query, Class<?> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		try {
			SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
			return extractIds(response);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + searchRequest.toString(), e);
		}
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
	public long count(Query query, Class<?> clazz, IndexCoordinates index) {
		Assert.notNull(index, "index must not be null");
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		searchRequest.source().size(0);

		try {
			return SearchHitsUtil.getTotalCount(client.search(searchRequest, RequestOptions.DEFAULT).getHits());
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + searchRequest.toString(), e);
		}
	}

	@Override
	public <T> List<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {
		Assert.notNull(index, "index must not be null");
		Assert.notEmpty(query.getIds(), "No Id define for Query");
		MultiGetRequest request = requestFactory.multiGetRequest(query, index);
		try {
			MultiGetResponse result = client.mget(request, RequestOptions.DEFAULT);
			return elasticsearchConverter.mapDocuments(DocumentAdapters.from(result), clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while multiget for request: " + request.toString(), e);
		}
	}

	@Override
	public String index(IndexQuery query, IndexCoordinates index) {
		IndexRequest request = requestFactory.indexRequest(query, index);
		try {
			String documentId = client.index(request, RequestOptions.DEFAULT).getId();

			// We should call this because we are not going through a mapper.
			if (query.getObject() != null) {
				setPersistentEntityId(query.getObject(), documentId);
			}
			return documentId;
		} catch (IOException e) {
			throw new ElasticsearchException("Error while index for request: " + request.toString(), e);
		}
	}

	@Override
	public UpdateResponse update(UpdateQuery query, IndexCoordinates index) {
		UpdateRequest request = requestFactory.updateRequest(query, index);
		try {
			return client.update(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while update for request: " + request.toString(), e);
		}
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
		BulkRequest bulkRequest = requestFactory.bulkRequest(queries, bulkOptions, index);
		try {
			checkForBulkOperationFailure(client.bulk(bulkRequest, RequestOptions.DEFAULT));
		} catch (IOException e) {
			throw new ElasticsearchException("Error while bulk for request: " + bulkRequest.toString(), e);
		}
	}

	@Override
	public boolean indexExists(String indexName) {
		GetIndexRequest request = new GetIndexRequest(indexName);
		try {
			return client.indices().exists(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while for indexExists request: " + request.toString(), e);
		}
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
	public String delete(String id, IndexCoordinates index) {
		DeleteRequest request = new DeleteRequest(index.getIndexName(), index.getTypeName(), id);
		try {
			return client.delete(request, RequestOptions.DEFAULT).getId();
		} catch (IOException e) {
			throw new ElasticsearchException("Error while deleting item request: " + request.toString(), e);
		}
	}

	@Override
	public void delete(DeleteQuery deleteQuery, IndexCoordinates index) {
		DeleteByQueryRequest deleteByQueryRequest = requestFactory.deleteByQueryRequest(deleteQuery, index);
		try {
			client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for delete request: " + deleteByQueryRequest.toString(), e);
		}
	}

	@Override
	public <T> ScrolledPage<T> startScroll(long scrollTimeInMillis, Query query, Class<T> clazz, IndexCoordinates index) {
		Assert.notNull(query.getPageable(), "Query.pageable is required for scan & scroll");
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		searchRequest.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		try {
			SearchResponse result = client.search(searchRequest, RequestOptions.DEFAULT);
			return elasticsearchConverter.mapResults(SearchDocumentResponse.from(result), clazz, null);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + searchRequest.toString(), e);
		}
	}

	@Override
	public <T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz) {
		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));
		SearchResponse response;
		try {
			response = client.searchScroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
		return elasticsearchConverter.mapResults(SearchDocumentResponse.from(response), clazz, Pageable.unpaged());
	}

	@Override
	public void clearScroll(String scrollId) {
		ClearScrollRequest request = new ClearScrollRequest();
		request.addScrollId(scrollId);
		try {
			client.clearScroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
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
			Response response = restClient.performRequest(new Request("GET", "/" + indexName + "/_settings"));
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

	@Override
	public void refresh(IndexCoordinates index) {
		Assert.notNull(index, "No index defined for refresh()");
		try {
			client.indices().refresh(refreshRequest(index.getIndexNames()), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to refresh index: " + index, e);
		}
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		List<AliasMetaData> aliases = null;
		RestClient restClient = client.getLowLevelClient();
		Response response;
		String aliasResponse;

		try {
			response = restClient.performRequest(new Request("GET", '/' + indexName + "/_alias/*"));
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
			node = node.findValue("aliases");

			if (node == null) {
				return Collections.emptyList();
			}

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
		SearchRequest searchRequest = new SearchRequest(index.getIndexNames());
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.suggest(suggestion);
		searchRequest.source(sourceBuilder);

		try {
			return client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Could not execute search request : " + searchRequest.toString(), e);
		}
	}
}
