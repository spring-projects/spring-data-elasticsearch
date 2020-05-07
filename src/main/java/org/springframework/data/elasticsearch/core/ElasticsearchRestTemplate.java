/*
 * Copyright 2013-2020 the original author or authors.
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
import java.util.stream.Collectors;

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
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
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
	private ElasticsearchExceptionTranslator exceptionTranslator;

	// region Initialization
	public ElasticsearchRestTemplate(RestHighLevelClient client) {

		Assert.notNull(client, "Client must not be null!");

		this.client = client;
		this.exceptionTranslator = new ElasticsearchExceptionTranslator();

		initialize(createElasticsearchConverter());
	}

	public ElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {

		Assert.notNull(client, "Client must not be null!");

		this.client = client;
		this.exceptionTranslator = new ElasticsearchExceptionTranslator();

		initialize(elasticsearchConverter);
	}

	// endregion

	// region IndexOperations
	@Override
	public IndexOperations indexOps(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		return new DefaultIndexOperations(this, clazz);
	}

	@Override
	public IndexOperations indexOps(IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");

		return new DefaultIndexOperations(this, index);
	}
	// endregion

	// region DocumentOperations
	@Override
	public String index(IndexQuery query, IndexCoordinates index) {

		maybeCallbackBeforeConvertWithQuery(query, index);

		IndexRequest request = requestFactory.indexRequest(query, index);
		String documentId = execute(client -> client.index(request, RequestOptions.DEFAULT).getId());

		// We should call this because we are not going through a mapper.
		Object queryObject = query.getObject();
		if (queryObject != null) {
			setPersistentEntityId(queryObject, documentId);
		}

		maybeCallbackAfterSaveWithQuery(query, index);

		return documentId;
	}

	@Override
	@Nullable
	public <T> T get(String id, Class<T> clazz, IndexCoordinates index) {
		GetRequest request = requestFactory.getRequest(id, index);
		GetResponse response = execute(client -> client.get(request, RequestOptions.DEFAULT));

		DocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		return callback.doWith(DocumentAdapters.from(response));
	}

	@Override
	public <T> List<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");
		Assert.notEmpty(query.getIds(), "No Id define for Query");

		MultiGetRequest request = requestFactory.multiGetRequest(query, index);
		MultiGetResponse result = execute(client -> client.mget(request, RequestOptions.DEFAULT));

		DocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		return DocumentAdapters.from(result).stream().map(callback::doWith).collect(Collectors.toList());
	}

	@Override
	protected boolean doExists(String id, IndexCoordinates index) {
		GetRequest request = requestFactory.getRequest(id, index);
		request.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE);
		return execute(client -> client.get(request, RequestOptions.DEFAULT).isExists());
	}

	@Override
	public List<String> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "List of IndexQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		return doBulkOperation(queries, bulkOptions, index);
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "List of UpdateQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		doBulkOperation(queries, bulkOptions, index);
	}

	@Override
	public String delete(String id, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		DeleteRequest request = new DeleteRequest(index.getIndexName(), elasticsearchConverter.convertId(id));
		return execute(client -> client.delete(request, RequestOptions.DEFAULT).getId());
	}

	@Override
	public void delete(Query query, Class<?> clazz, IndexCoordinates index) {
		DeleteByQueryRequest deleteByQueryRequest = requestFactory.deleteByQueryRequest(query, clazz, index);
		execute(client -> client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT));
	}

	@Override
	@Deprecated
	public void delete(DeleteQuery deleteQuery, IndexCoordinates index) {
		DeleteByQueryRequest deleteByQueryRequest = requestFactory.deleteByQueryRequest(deleteQuery, index);
		execute(client -> client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT));
	}

	@Override
	public UpdateResponse update(UpdateQuery query, IndexCoordinates index) {
		UpdateRequest request = requestFactory.updateRequest(query, index);
		UpdateResponse.Result result = UpdateResponse.Result
				.valueOf(execute(client -> client.update(request, RequestOptions.DEFAULT)).getResult().name());
		return new UpdateResponse(result);
	}

	private List<String> doBulkOperation(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		maybeCallbackBeforeConvertWithQueries(queries, index);
		BulkRequest bulkRequest = requestFactory.bulkRequest(queries, bulkOptions, index);
		List<String> ids = checkForBulkOperationFailure(
				execute(client -> client.bulk(bulkRequest, RequestOptions.DEFAULT)));
		maybeCallbackAfterSaveWithQueries(queries, index);
		return ids;
	}
	// endregion

	// region SearchOperations
	@Override
	public long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		final boolean trackTotalHits = query.getTrackTotalHits();
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

		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);
		return callback.doWith(SearchDocumentResponse.from(response));
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollStart(long scrollTimeInMillis, Query query, Class<T> clazz,
			IndexCoordinates index) {

		Assert.notNull(query.getPageable(), "pageable of query must not be null.");

		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		searchRequest.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		SearchResponse response = execute(client -> client.search(searchRequest, RequestOptions.DEFAULT));

		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);
		return callback.doWith(SearchDocumentResponse.from(response));
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollContinue(@Nullable String scrollId, long scrollTimeInMillis,
			Class<T> clazz, IndexCoordinates index) {

		SearchScrollRequest request = new SearchScrollRequest(scrollId);
		request.scroll(TimeValue.timeValueMillis(scrollTimeInMillis));

		SearchResponse response = execute(client -> client.scroll(request, RequestOptions.DEFAULT));

		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = //
				new ReadSearchScrollDocumentResponseCallback<>(clazz, index);
		return callback.doWith(SearchDocumentResponse.from(response));
	}

	@Override
	public void searchScrollClear(List<String> scrollIds) {
		ClearScrollRequest request = new ClearScrollRequest();
		request.scrollIds(scrollIds);
		execute(client -> client.clearScroll(request, RequestOptions.DEFAULT));
	}

	@Override
	public SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index) {
		SearchRequest searchRequest = new SearchRequest(index.getIndexNames());
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.suggest(suggestion);
		searchRequest.source(sourceBuilder);
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
