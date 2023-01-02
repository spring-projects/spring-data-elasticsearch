/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.Version;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
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
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.elasticsearch.BulkFailureException;
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
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
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
 * @author Sijia Liu
 * @since 4.4
 * @deprecated since 5.0
 */
@Deprecated
public class ElasticsearchRestTemplate extends AbstractElasticsearchTemplate {

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchRestTemplate.class);

	private final RestHighLevelClient client;
	private final ElasticsearchExceptionTranslator exceptionTranslator = new ElasticsearchExceptionTranslator();
	protected RequestFactory requestFactory;

	// region _initialization
	public ElasticsearchRestTemplate(RestHighLevelClient client) {

		Assert.notNull(client, "Client must not be null!");

		this.client = client;
		requestFactory = new RequestFactory(this.elasticsearchConverter);

	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {

		super(elasticsearchConverter);

		Assert.notNull(client, "Client must not be null!");

		this.client = client;
		requestFactory = new RequestFactory(this.elasticsearchConverter);
	}

	@Override
	protected AbstractElasticsearchTemplate doCopy() {
		ElasticsearchRestTemplate copy = new ElasticsearchRestTemplate(client, elasticsearchConverter);
		copy.requestFactory = this.requestFactory;
		return copy;
	}

	/**
	 * @since 4.0
	 */
	public RequestFactory getRequestFactory() {
		return requestFactory;
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

		UpdateByQueryRequest updateByQueryRequest = requestFactory.updateByQueryRequest(query, index);

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

	@Override
	public ReindexResponse reindex(ReindexRequest reindexRequest) {

		Assert.notNull(reindexRequest, "reindexRequest must not be null");

		org.elasticsearch.index.reindex.ReindexRequest reindexRequestES = requestFactory.reindexRequest(reindexRequest);
		BulkByScrollResponse bulkByScrollResponse = execute(
				client -> client.reindex(reindexRequestES, RequestOptions.DEFAULT));
		return ResponseConverter.reindexResponseOf(bulkByScrollResponse);
	}

	@Override
	public String submitReindex(ReindexRequest reindexRequest) {

		Assert.notNull(reindexRequest, "reindexRequest must not be null");

		org.elasticsearch.index.reindex.ReindexRequest reindexRequestES = requestFactory.reindexRequest(reindexRequest);
		return execute(client -> client.submitReindexTask(reindexRequestES, RequestOptions.DEFAULT).getTask());
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
	 * Preprocess the write request before it is sent to the server, e.g. by setting the
	 * {@link WriteRequest#setRefreshPolicy(String) refresh policy} if applicable.
	 *
	 * @param request must not be {@literal null}.
	 * @param <R> the request type
	 * @return the processed {@link WriteRequest}.
	 */
	protected <R extends WriteRequest<R>> R prepareWriteRequest(R request) {

		if (refreshPolicy == null) {
			return request;
		}

		return request.setRefreshPolicy(RequestFactory.toElasticsearchRefreshPolicy(refreshPolicy));
	}

	/**
	 * extract the list of {@link IndexedObjectInformation} from a {@link BulkResponse}.
	 *
	 * @param bulkResponse the response to evaluate
	 * @return the list of the {@link IndexedObjectInformation}s
	 */
	protected List<IndexedObjectInformation> checkForBulkOperationFailure(BulkResponse bulkResponse) {

		if (bulkResponse.hasFailures()) {
			Map<String, String> failedDocuments = new HashMap<>();
			for (BulkItemResponse item : bulkResponse.getItems()) {

				if (item.isFailed())
					failedDocuments.put(item.getId(), item.getFailureMessage());
			}
			throw new BulkFailureException(
					"Bulk operation has failures. Use BulkFailureException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + ']',
					failedDocuments);
		}

		return Stream.of(bulkResponse.getItems()).map(bulkItemResponse -> {
			DocWriteResponse response = bulkItemResponse.getResponse();
			if (response != null) {
				return IndexedObjectInformation.of(response.getId(), response.getSeqNo(), response.getPrimaryTerm(),
						response.getVersion());
			} else {
				return IndexedObjectInformation.of(bulkItemResponse.getId(), null, null, null);
			}

		}).collect(Collectors.toList());
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

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);

		return callback.doWith(SearchDocumentResponseBuilder.from(response, getEntityCreator(documentCallback)));
	}

	protected <T> SearchHits<T> doSearch(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index) {
		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = requestFactory.moreLikeThisQueryBuilder(query, index);
		return search(
				new NativeSearchQueryBuilder().withQuery(moreLikeThisQueryBuilder).withPageable(query.getPageable()).build(),
				clazz, index);
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollStart(long scrollTimeInMillis, Query query, Class<T> clazz,
			IndexCoordinates index) {

		Assert.notNull(query.getPageable(), "pageable of query must not be null.");

		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		searchRequest.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		SearchResponse response = execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);
		return callback.doWith(SearchDocumentResponseBuilder.from(response, getEntityCreator(documentCallback)));
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollContinue(String scrollId, long scrollTimeInMillis, Class<T> clazz,
			IndexCoordinates index) {

		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		SearchResponse response = execute(client -> client.scroll(request, RequestOptions.DEFAULT));

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);
		return callback.doWith(SearchDocumentResponseBuilder.from(response, getEntityCreator(documentCallback)));
	}

	@Override
	public void searchScrollClear(List<String> scrollIds) {
		try {
			ClearScrollRequest request = new ClearScrollRequest();
			request.scrollIds(scrollIds);
			execute(client -> client.clearScroll(request, RequestOptions.DEFAULT));
		} catch (Exception e) {
			LOGGER.warn(String.format("Could not clear scroll: %s", e.getMessage()));
		}
	}

	public SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index) {
		SearchRequest searchRequest = requestFactory.searchRequest(suggestion, index);
		return execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));
	}

	@Override
	public <T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index) {
		MultiSearchRequest request = new MultiSearchRequest();
		for (Query query : queries) {
			request.add(requestFactory.searchRequest(query, clazz, index));
		}

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);
		List<SearchHits<T>> res = new ArrayList<>(queries.size());
		for (int i = 0; i < queries.size(); i++) {
			res.add(callback
					.doWith(SearchDocumentResponseBuilder.from(items[i].getResponse(), getEntityCreator(documentCallback))));
		}
		return res;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(classes, "classes must not be null");
		Assert.isTrue(queries.size() == classes.size(), "queries and classes must have the same size");

		MultiSearchRequest request = new MultiSearchRequest();
		Iterator<Class<?>> it = classes.iterator();
		for (Query query : queries) {
			Class<?> clazz = it.next();
			request.add(requestFactory.searchRequest(query, clazz, getIndexCoordinatesFor(clazz)));
		}

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);

		List<SearchHits<?>> res = new ArrayList<>(queries.size());
		Iterator<Class<?>> it1 = classes.iterator();
		for (int i = 0; i < queries.size(); i++) {
			Class entityClass = it1.next();

			IndexCoordinates index = getIndexCoordinatesFor(entityClass);
			ReadDocumentCallback<?> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, entityClass, index);
			SearchDocumentResponseCallback<SearchHits<?>> callback = new ReadSearchDocumentResponseCallback<>(entityClass,
					index);

			SearchResponse response = items[i].getResponse();
			res.add(callback.doWith(SearchDocumentResponseBuilder.from(response, getEntityCreator(documentCallback))));
		}
		return res;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes,
			IndexCoordinates index) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(classes, "classes must not be null");
		Assert.notNull(index, "index must not be null");
		Assert.isTrue(queries.size() == classes.size(), "queries and classes must have the same size");

		MultiSearchRequest request = new MultiSearchRequest();
		Iterator<Class<?>> it = classes.iterator();
		for (Query query : queries) {
			request.add(requestFactory.searchRequest(query, it.next(), index));
		}

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);

		List<SearchHits<?>> res = new ArrayList<>(queries.size());
		Iterator<Class<?>> it1 = classes.iterator();
		for (int i = 0; i < queries.size(); i++) {
			Class entityClass = it1.next();

			ReadDocumentCallback<?> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, entityClass, index);
			SearchDocumentResponseCallback<SearchHits<?>> callback = new ReadSearchDocumentResponseCallback<>(entityClass,
					index);

			SearchResponse response = items[i].getResponse();
			res.add(callback.doWith(SearchDocumentResponseBuilder.from(response, getEntityCreator(documentCallback))));
		}
		return res;
	}

	protected MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request) {
		MultiSearchResponse response = execute(client -> client.msearch(request, RequestOptions.DEFAULT));
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
	public String getClusterVersion() {
		try {
			return execute(client -> client.info(RequestOptions.DEFAULT)).getVersion().getNumber();
		} catch (Exception ignored) {}
		return null;
	}

	@Override
	public Query matchAllQuery() {
		return new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).build();
	}

	@Override
	public Query idsQuery(List<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		return new NativeSearchQueryBuilder().withQuery(QueryBuilders.idsQuery().addIds(ids.toArray(new String[] {})))
				.build();
	}

	@Override
	public String getVendor() {
		return "Elasticsearch";
	}

	@Override
	public String getRuntimeLibraryVersion() {
		return Version.CURRENT.toString();
	}

	@Deprecated
	public SearchResponse suggest(SuggestBuilder suggestion, Class<?> clazz) {
		return suggest(suggestion, getIndexCoordinatesFor(clazz));
	}

	// endregion
}
