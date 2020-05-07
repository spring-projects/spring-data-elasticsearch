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

import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoAction;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequestBuilder;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
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
 * @author Roman Puchkovskiy
 * @deprecated as of 4.0
 */
@Deprecated
public class ElasticsearchTemplate extends AbstractElasticsearchTemplate {
	private static final Logger QUERY_LOGGER = LoggerFactory
			.getLogger("org.springframework.data.elasticsearch.core.QUERY");

	private Client client;
	@Nullable private String searchTimeout;

	private final ElasticsearchExceptionTranslator exceptionTranslator = new ElasticsearchExceptionTranslator();

	// region Initialization
	public ElasticsearchTemplate(Client client) {
		this.client = client;
		initialize(client, createElasticsearchConverter());
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter) {
		this.client = client;
		initialize(client, elasticsearchConverter);
	}

	private void initialize(Client client, ElasticsearchConverter elasticsearchConverter) {

		Assert.notNull(client, "Client must not be null!");

		this.client = client;
		initialize(elasticsearchConverter);
	}
	// endregion

	// region IndexOperations
	@Override
	public IndexOperations indexOps(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		return new DefaultTransportIndexOperations(client, elasticsearchConverter, clazz);
	}

	@Override
	public IndexOperations indexOps(IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");

		return new DefaultTransportIndexOperations(client, elasticsearchConverter, index);
	}
	// endregion

	// region getter/setter
	@Nullable
	public String getSearchTimeout() {
		return searchTimeout;
	}

	public void setSearchTimeout(String searchTimeout) {
		this.searchTimeout = searchTimeout;
	}
	// endregion

	// region DocumentOperations
	@Override
	public String index(IndexQuery query, IndexCoordinates index) {

		maybeCallbackBeforeConvertWithQuery(query, index);

		IndexRequestBuilder indexRequestBuilder = requestFactory.indexRequestBuilder(client, query, index);
		ActionFuture<IndexResponse> future = indexRequestBuilder.execute();
		IndexResponse response;
		try {
			response = future.actionGet();
		} catch (RuntimeException e) {
			throw translateException(e);
		}
		String documentId = response.getId();

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
		GetRequestBuilder getRequestBuilder = requestFactory.getRequestBuilder(client, id, index);
		GetResponse response = getRequestBuilder.execute().actionGet();

		DocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		return callback.doWith(DocumentAdapters.from(response));
	}

	@Override
	public <T> List<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");
		Assert.notEmpty(query.getIds(), "No Ids defined for Query");

		MultiGetRequestBuilder builder = requestFactory.multiGetRequestBuilder(client, query, index);

		DocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		List<Document> documents = DocumentAdapters.from(builder.execute().actionGet());
		return documents.stream().map(callback::doWith).collect(Collectors.toList());
	}

	@Override
	protected boolean doExists(String id, IndexCoordinates index) {
		GetRequestBuilder getRequestBuilder = requestFactory.getRequestBuilder(client, id, index);
		getRequestBuilder.setFetchSource(false);
		return getRequestBuilder.execute().actionGet().isExists();
	}

	@Override
	public List<String> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "List of IndexQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		List<String> ids = doBulkOperation(queries, bulkOptions, index);

		maybeCallbackAfterSaveWithQueries(queries, index);

		return ids;
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

		return client.prepareDelete(index.getIndexName(), IndexCoordinates.TYPE, elasticsearchConverter.convertId(id))
				.execute().actionGet().getId();
	}

	@Override
	@Deprecated
	public void delete(DeleteQuery deleteQuery, IndexCoordinates index) {
		requestFactory.deleteByQueryRequestBuilder(client, deleteQuery, index).get();
	}

	@Override
	public void delete(Query query, Class<?> clazz, IndexCoordinates index) {
		requestFactory.deleteByQueryRequestBuilder(client, query, clazz, index).get();
	}

	@Override
	public String delete(Object entity, IndexCoordinates index) {
		return super.delete(entity, index);
	}

	@Override
	public UpdateResponse update(UpdateQuery query, IndexCoordinates index) {
		UpdateRequestBuilder updateRequestBuilder = requestFactory.updateRequestBuilderFor(client, query, index);
		org.elasticsearch.action.update.UpdateResponse updateResponse = updateRequestBuilder.execute().actionGet();
		UpdateResponse.Result result = UpdateResponse.Result.valueOf(updateResponse.getResult().name());
		return new UpdateResponse(result);
	}

	private List<String> doBulkOperation(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		maybeCallbackBeforeConvertWithQueries(queries, index);
		BulkRequestBuilder bulkRequest = requestFactory.bulkRequestBuilder(client, queries, bulkOptions, index);
		return checkForBulkOperationFailure(bulkRequest.execute().actionGet());
	}
	// endregion

	// region SearchOperations
	@Override
	public long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		final boolean trackTotalHits = query.getTrackTotalHits();
		query.setTrackTotalHits(true);
		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, clazz, index);
		query.setTrackTotalHits(trackTotalHits);
		searchRequestBuilder.setSize(0);

		return SearchHitsUtil.getTotalCount(getSearchResponse(searchRequestBuilder).getHits());
	}

	@Override
	public <T> SearchHits<T> search(Query query, Class<T> clazz, IndexCoordinates index) {
		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, clazz, index);
		SearchResponse response = getSearchResponse(searchRequestBuilder);

		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);
		return callback.doWith(SearchDocumentResponse.from(response));
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollStart(long scrollTimeInMillis, Query query, Class<T> clazz,
			IndexCoordinates index) {

		Assert.notNull(query.getPageable(), "pageable of query must not be null.");

		ActionFuture<SearchResponse> action = requestFactory //
				.searchRequestBuilder(client, query, clazz, index) //
				.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)) //
				.execute();

		SearchResponse response = getSearchResponseWithTimeout(action);

		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);
		return callback.doWith(SearchDocumentResponse.from(response));
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollContinue(@Nullable String scrollId, long scrollTimeInMillis,
			Class<T> clazz, IndexCoordinates index) {

		ActionFuture<SearchResponse> action = client //
				.prepareSearchScroll(scrollId) //
				.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)) //
				.execute();

		SearchResponse response = getSearchResponseWithTimeout(action);

		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);
		return callback.doWith(SearchDocumentResponse.from(response));
	}

	@Override
	public void searchScrollClear(List<String> scrollIds) {
		client.prepareClearScroll().setScrollIds(scrollIds).execute().actionGet();
	}

	@Override
	public SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index) {
		return client.prepareSearch(index.getIndexNames()).suggest(suggestion).get();
	}

	@Override
	protected MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request) {
		ActionFuture<MultiSearchResponse> future = client.multiSearch(request);
		MultiSearchResponse response = future.actionGet();
		MultiSearchResponse.Item[] items = response.getResponses();
		Assert.isTrue(items.length == request.requests().size(), "Response should have same length with queries");
		return items;
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
	// endregion

	// region helper methods
	@Override
	protected String getClusterVersion() {

		try {
			NodesInfoResponse nodesInfoResponse = client.admin().cluster()
					.nodesInfo(new NodesInfoRequestBuilder(client, NodesInfoAction.INSTANCE).request()).actionGet();
			if (!nodesInfoResponse.getNodes().isEmpty()) {
				return nodesInfoResponse.getNodes().get(0).getVersion().toString();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public Client getClient() {
		return client;
	}
	// endregion

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
}
