/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.elasticsearch.backend.elasticsearch7;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.backend.elasticsearch7.cluster.ElasticsearchClusterOperations;
import org.springframework.data.elasticsearch.backend.elasticsearch7.document.DocumentAdapters;
import org.springframework.data.elasticsearch.backend.elasticsearch7.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.clients.elasticsearch7.RequestFactory;
import org.springframework.data.elasticsearch.clients.elasticsearch7.ResponseConverter;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
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
 * @author Farid Faoudi
 */
public class ElasticsearchRestTemplate extends AbstractElasticsearchRestTransportTemplate {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchRestTemplate.class);

	private final RestHighLevelClient client;
	private final ElasticsearchExceptionTranslator exceptionTranslator = new ElasticsearchExceptionTranslator();

	// region Initialization
	public ElasticsearchRestTemplate(RestHighLevelClient client) {

		Assert.notNull(client, "Client must not be null!");

		this.client = client;

		initialize(createElasticsearchConverter());
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {

		Assert.notNull(client, "Client must not be null!");

		this.client = client;

		initialize(elasticsearchConverter);
	}

	@Override
	protected AbstractElasticsearchTemplate doCopy() {
		return new ElasticsearchRestTemplate(client, elasticsearchConverter);
	}

	// endregion

	// region IndexOperations
	@Override
	public IndexOperations indexOps(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		return new RestIndexTemplate(this, clazz);
	}

	@Override
	public IndexOperations indexOps(IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");

		return new RestIndexTemplate(this, index);
	}
	// endregion

	// region ClusterOperations
	@Override
	public ClusterOperations cluster() {
		return ElasticsearchClusterOperations.forTemplate(this);
	}
	// endregion

	// region DocumentOperations
	public String doIndex(IndexQuery query, IndexCoordinates index) {

		IndexRequest request = prepareWriteRequest(requestFactory.indexRequest(query, index));
		IndexResponse indexResponse = execute(client -> client.index(request, RequestOptions.DEFAULT));

		Object queryObject = query.getObject();

		if (queryObject != null) {
			query.setObject(updateIndexedObject(queryObject, IndexedObjectInformation.of(indexResponse.getId(),
					indexResponse.getSeqNo(), indexResponse.getPrimaryTerm(), indexResponse.getVersion())));
		}

		return indexResponse.getId();
	}

	@Override
	@Nullable
	public <T> T get(String id, Class<T> clazz, IndexCoordinates index) {

		GetRequest request = requestFactory.getRequest(id, routingResolver.getRouting(), index);
		GetResponse response = execute(client -> client.get(request, RequestOptions.DEFAULT));

		DocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		return callback.doWith(DocumentAdapters.from(response));
	}

	@Override
	public <T> List<MultiGetItem<T>> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");

		MultiGetRequest request = requestFactory.multiGetRequest(query, clazz, index);
		MultiGetResponse result = execute(client -> client.mget(request, RequestOptions.DEFAULT));

		DocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		return DocumentAdapters.from(result).stream() //
				.map(multiGetItem -> MultiGetItem.of( //
						multiGetItem.isFailed() ? null : callback.doWith(multiGetItem.getItem()), multiGetItem.getFailure())) //
				.collect(Collectors.toList());
	}

	@Override
	protected boolean doExists(String id, IndexCoordinates index) {
		GetRequest request = requestFactory.getRequest(id, routingResolver.getRouting(), index);
		request.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE);
		return execute(client -> client.get(request, RequestOptions.DEFAULT).isExists());
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		doBulkOperation(queries, bulkOptions, index);
	}

	@Override
	protected String doDelete(String id, @Nullable String routing, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		DeleteRequest request = prepareWriteRequest(
				requestFactory.deleteRequest(elasticsearchConverter.convertId(id), routing, index));
		return execute(client -> client.delete(request, RequestOptions.DEFAULT).getId());
	}

	@Override
	public ByQueryResponse delete(Query query, Class<?> clazz, IndexCoordinates index) {
		DeleteByQueryRequest deleteByQueryRequest = requestFactory.deleteByQueryRequest(query, clazz, index);
		return ResponseConverter
				.byQueryResponseOf(execute(client -> client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT)));
	}

	@Override
	public UpdateResponse update(UpdateQuery query, IndexCoordinates index) {
		UpdateRequest request = requestFactory.updateRequest(query, index);

		if (query.getRefreshPolicy() == null && getRefreshPolicy() != null) {
			request.setRefreshPolicy(RequestFactory.toElasticsearchRefreshPolicy(getRefreshPolicy()));
		}

		if (query.getRouting() == null && routingResolver.getRouting() != null) {
			request.routing(routingResolver.getRouting());
		}

		UpdateResponse.Result result = UpdateResponse.Result
				.valueOf(execute(client -> client.update(request, RequestOptions.DEFAULT)).getResult().name());
		return new UpdateResponse(result);
	}

	@Override
	public ByQueryResponse updateByQuery(UpdateQuery query, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		final UpdateByQueryRequest updateByQueryRequest = requestFactory.updateByQueryRequest(query, index);

		if (query.getRefreshPolicy() == null && getRefreshPolicy() != null) {
			updateByQueryRequest.setRefresh(getRefreshPolicy() == RefreshPolicy.IMMEDIATE);
		}

		if (query.getRouting() == null && routingResolver.getRouting() != null) {
			updateByQueryRequest.setRouting(routingResolver.getRouting());
		}

		final BulkByScrollResponse bulkByScrollResponse = execute(
				client -> client.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT));
		return ResponseConverter.byQueryResponseOf(bulkByScrollResponse);
	}

	public List<IndexedObjectInformation> doBulkOperation(List<?> queries, BulkOptions bulkOptions,
			IndexCoordinates index) {
		BulkRequest bulkRequest = prepareWriteRequest(requestFactory.bulkRequest(queries, bulkOptions, index));
		List<IndexedObjectInformation> indexedObjectInformationList = checkForBulkOperationFailure(
				execute(client -> client.bulk(bulkRequest, RequestOptions.DEFAULT)));
		updateIndexedObjectsWithQueries(queries, indexedObjectInformationList);
		return indexedObjectInformationList;
	}

	/**
	 * Pre process the write request before it is sent to the server, eg. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param request must not be {@literal null}.
	 * @param <R>
	 * @return the processed {@link WriteRequest}.
	 */
	protected <R extends WriteRequest<R>> R prepareWriteRequest(R request) {

		if (refreshPolicy == null) {
			return request;
		}

		return request.setRefreshPolicy(RequestFactory.toElasticsearchRefreshPolicy(refreshPolicy));
	}

	// endregion

	// region SearchOperations
	@Override
	public long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		final Boolean trackTotalHits = query.getTrackTotalHits();
		query.setTrackTotalHits(true);
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		query.setTrackTotalHits(trackTotalHits);

		searchRequest.source().size(0);

		return SearchHitsUtil
				.getTotalCount(execute(client -> client.search(searchRequest, RequestOptions.DEFAULT).getHits()));
	}

	@Override
	public <T> SearchHits<T> search(Query query, Class<T> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		SearchResponse response = execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<T>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);

		return callback.doWith(SearchDocumentResponse.from(response, documentCallback::doWith));
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollStart(long scrollTimeInMillis, Query query, Class<T> clazz,
			IndexCoordinates index) {

		Assert.notNull(query.getPageable(), "pageable of query must not be null.");

		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		searchRequest.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		SearchResponse response = execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<T>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);
		return callback.doWith(SearchDocumentResponse.from(response, documentCallback::doWith));
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollContinue(@Nullable String scrollId, long scrollTimeInMillis,
			Class<T> clazz, IndexCoordinates index) {

		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		SearchResponse response = execute(client -> client.scroll(request, RequestOptions.DEFAULT));

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<T>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);
		return callback.doWith(SearchDocumentResponse.from(response, documentCallback::doWith));
	}

	@Override
	public void searchScrollClear(List<String> scrollIds) {
		try {
			ClearScrollRequest request = new ClearScrollRequest();
			request.scrollIds(scrollIds);
			execute(client -> client.clearScroll(request, RequestOptions.DEFAULT));
		} catch (Exception e) {
			LOGGER.warn("Could not clear scroll: {}", e.getMessage());
		}
	}

	@Override
	public SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index) {
		SearchRequest searchRequest = requestFactory.searchRequest(suggestion, index);
		return execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));
	}

	@Override
	protected MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request) {
		MultiSearchResponse response = execute(client -> client.multiSearch(request, RequestOptions.DEFAULT));
		MultiSearchResponse.Item[] items = response.getResponses();
		Assert.isTrue(items.length == request.requests().size(), "Response should has same length with queries");
		return items;
	}
	// endregion

	// region ClientCallback
	/**
	 * Callback interface to be used with {@link #execute(ClientCallback)} for operating directly on
	 * {@link RestHighLevelClient}.
	 *
	 * @since 4.0
	 */
	@FunctionalInterface
	public interface ClientCallback<T> {
		T doWithClient(RestHighLevelClient client) throws IOException;
	}

	/**
	 * Execute a callback with the {@link RestHighLevelClient}
	 *
	 * @param callback the callback to execute, must not be {@literal null}
	 * @param <T> the type returned from the callback
	 * @return the callback result
	 * @since 4.0
	 */
	public <T> T execute(ClientCallback<T> callback) {

		Assert.notNull(callback, "callback must not be null");

		try {
			return callback.doWithClient(client);
		} catch (IOException | RuntimeException e) {
			throw translateException(e);
		}
	}

	/**
	 * translates an Exception if possible. Exceptions that are no {@link RuntimeException}s are wrapped in a
	 * RuntimeException
	 *
	 * @param exception the Exception to map
	 * @return the potentially translated RuntimeException.
	 * @since 4.0
	 */
	private RuntimeException translateException(Exception exception) {

		RuntimeException runtimeException = exception instanceof RuntimeException ? (RuntimeException) exception
				: new RuntimeException(exception.getMessage(), exception);
		RuntimeException potentiallyTranslatedException = exceptionTranslator
				.translateExceptionIfPossible(runtimeException);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
	}

	// endregion

	// region helper methods
	@Override
	protected String getClusterVersion() {
		try {
			return execute(client -> client.info(RequestOptions.DEFAULT)).getVersion().getNumber();
		} catch (Exception ignored) {}
		return null;
	}
	// endregion
}
