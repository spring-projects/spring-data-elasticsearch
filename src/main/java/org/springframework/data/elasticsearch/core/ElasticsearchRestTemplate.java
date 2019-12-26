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

import java.io.IOException;
import java.util.List;

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
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.support.SearchHitsUtil;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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

	// region Initialization
	public ElasticsearchRestTemplate(RestHighLevelClient client) {
		initialize(client, createElasticsearchConverter());
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {
		initialize(client, elasticsearchConverter);
	}

	private void initialize(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {
		Assert.notNull(client, "Client must not be null!");
		this.client = client;
		initialize(elasticsearchConverter, new DefaultIndexOperations(client, elasticsearchConverter));
	}
	// endregion

	// region DocumentOperations
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
	public <T> T get(GetQuery query, Class<T> clazz, IndexCoordinates index) {
		GetRequest request = requestFactory.getRequest(query, index);
		try {
			GetResponse response = client.get(request, RequestOptions.DEFAULT);
			return elasticsearchConverter.mapDocument(DocumentAdapters.from(response), clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while getting for request: " + request.toString(), e);
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

	@Override
	public String delete(String id, IndexCoordinates index) {
		DeleteRequest request = new DeleteRequest(index.getIndexName(), id);
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
	public UpdateResponse update(UpdateQuery query, IndexCoordinates index) {
		UpdateRequest request = requestFactory.updateRequest(query, index);
		try {
			return client.update(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while update for request: " + request.toString(), e);
		}
	}

	private void doBulkOperation(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		BulkRequest bulkRequest = requestFactory.bulkRequest(queries, bulkOptions, index);
		try {
			checkForBulkOperationFailure(client.bulk(bulkRequest, RequestOptions.DEFAULT));
		} catch (IOException e) {
			throw new ElasticsearchException("Error while bulk for request: " + bulkRequest.toString(), e);
		}
	}
	// endregion

	// region SearchOperations
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
	public <T> SearchHits<T> search(Query query, Class<T> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		SearchResponse response;
		try {
			response = client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request: " + searchRequest.toString(), e);
		}
		return elasticsearchConverter.read(clazz, SearchDocumentResponse.from(response));
	}

	@Override
	public <T> ScrolledPage<SearchHit<T>> searchScrollStart(long scrollTimeInMillis, Query query, Class<T> clazz,
			IndexCoordinates index) {

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
	public <T> ScrolledPage<SearchHit<T>> searchScrollContinue(@Nullable String scrollId, long scrollTimeInMillis,
			Class<T> clazz) {
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
	public void searchScrollClear(String scrollId) {
		ClearScrollRequest request = new ClearScrollRequest();
		request.addScrollId(scrollId);
		try {
			client.clearScroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error for search request with scroll: " + request.toString(), e);
		}
	}

	@Override
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
	// endregion
}
