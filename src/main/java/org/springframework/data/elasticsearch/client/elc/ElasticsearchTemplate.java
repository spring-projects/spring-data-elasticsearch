/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static org.springframework.data.elasticsearch.client.elc.TypeUtils.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.Version;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.client.UnsupportedBackendOperation;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.data.elasticsearch.core.ElasticsearchOperations} using the new
 * Elasticsearch client.
 *
 * @author Peter-Josef Meisch
 * @author Hamid Rahimi
 * @author Illia Ulianov
 * @author Haibo Liu
 * @since 4.4
 */
public class ElasticsearchTemplate extends AbstractElasticsearchTemplate {

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchTemplate.class);

	private final ElasticsearchClient client;
	private final RequestConverter requestConverter;
	private final ResponseConverter responseConverter;
	private final JsonpMapper jsonpMapper;
	private final ElasticsearchExceptionTranslator exceptionTranslator;

	// region _initialization
	public ElasticsearchTemplate(ElasticsearchClient client) {

		Assert.notNull(client, "client must not be null");

		this.client = client;
		this.jsonpMapper = client._transport().jsonpMapper();
		requestConverter = new RequestConverter(elasticsearchConverter, jsonpMapper);
		responseConverter = new ResponseConverter(jsonpMapper);
		exceptionTranslator = new ElasticsearchExceptionTranslator(jsonpMapper);
	}

	public ElasticsearchTemplate(ElasticsearchClient client, ElasticsearchConverter elasticsearchConverter) {
		super(elasticsearchConverter);

		Assert.notNull(client, "client must not be null");

		this.client = client;
		this.jsonpMapper = client._transport().jsonpMapper();
		requestConverter = new RequestConverter(elasticsearchConverter, jsonpMapper);
		responseConverter = new ResponseConverter(jsonpMapper);
		exceptionTranslator = new ElasticsearchExceptionTranslator(jsonpMapper);
	}

	@Override
	protected AbstractElasticsearchTemplate doCopy() {
		return new ElasticsearchTemplate(client, elasticsearchConverter);
	}
	// endregion

	// region child templates
	@Override
	public IndexOperations indexOps(Class<?> clazz) {
		return new IndicesTemplate(client.indices(), getClusterTemplate(), elasticsearchConverter, clazz);
	}

	@Override
	public IndexOperations indexOps(IndexCoordinates index) {
		return new IndicesTemplate(client.indices(), getClusterTemplate(), elasticsearchConverter, index);
	}

	@Override
	public ClusterOperations cluster() {
		return getClusterTemplate();
	}

	private ClusterTemplate getClusterTemplate() {
		return new ClusterTemplate(client.cluster(), elasticsearchConverter);
	}
	// endregion

	// region document operations
	@Override
	@Nullable
	public <T> T get(String id, Class<T> clazz, IndexCoordinates index) {

		GetRequest getRequest = requestConverter.documentGetRequest(elasticsearchConverter.convertId(id),
				routingResolver.getRouting(), index);
		GetResponse<EntityAsMap> getResponse = execute(client -> client.get(getRequest, EntityAsMap.class));

		ReadDocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		return callback.doWith(DocumentAdapters.from(getResponse));
	}

	@Override
	public <T> List<MultiGetItem<T>> multiGet(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(clazz, "clazz must not be null");

		MgetRequest request = requestConverter.documentMgetRequest(query, clazz, index);
		MgetResponse<EntityAsMap> result = execute(client -> client.mget(request, EntityAsMap.class));

		ReadDocumentCallback<T> callback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);

		return DocumentAdapters.from(result).stream() //
				.map(multiGetItem -> MultiGetItem.of( //
						multiGetItem.isFailed() ? null : callback.doWith(multiGetItem.getItem()), multiGetItem.getFailure())) //
				.collect(Collectors.toList());
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(bulkOptions, "bulkOptions must not be null");
		Assert.notNull(index, "index must not be null");

		doBulkOperation(queries, bulkOptions, index);
	}

	@Override
	public ByQueryResponse delete(DeleteQuery query, Class<?> clazz) {
		return delete(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public ByQueryResponse delete(Query query, Class<?> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");

		DeleteByQueryRequest request = requestConverter.documentDeleteByQueryRequest(query, routingResolver.getRouting(),
				clazz, index, getRefreshPolicy());

		DeleteByQueryResponse response = execute(client -> client.deleteByQuery(request));

		return responseConverter.byQueryResponse(response);
	}

	@Override
	public ByQueryResponse delete(DeleteQuery query, Class<?> clazz, IndexCoordinates index) {
		Assert.notNull(query, "query must not be null");

		DeleteByQueryRequest request = requestConverter.documentDeleteByQueryRequest(query, routingResolver.getRouting(),
				clazz, index, getRefreshPolicy());

		DeleteByQueryResponse response = execute(client -> client.deleteByQuery(request));

		return responseConverter.byQueryResponse(response);
	}

	@Override
	public UpdateResponse update(UpdateQuery updateQuery, IndexCoordinates index) {

		UpdateRequest<Document, ?> request = requestConverter.documentUpdateRequest(updateQuery, index, getRefreshPolicy(),
				routingResolver.getRouting());
		co.elastic.clients.elasticsearch.core.UpdateResponse<Document> response = execute(
				client -> client.update(request, Document.class));
		return UpdateResponse.of(result(response.result()));
	}

	@Override
	public ByQueryResponse updateByQuery(UpdateQuery updateQuery, IndexCoordinates index) {

		Assert.notNull(updateQuery, "updateQuery must not be null");
		Assert.notNull(index, "index must not be null");

		UpdateByQueryRequest request = requestConverter.documentUpdateByQueryRequest(updateQuery, index,
				getRefreshPolicy());

		UpdateByQueryResponse byQueryResponse = execute(client -> client.updateByQuery(request));
		return responseConverter.byQueryResponse(byQueryResponse);
	}

	@Override
	public String doIndex(IndexQuery query, IndexCoordinates indexCoordinates) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		IndexRequest<?> indexRequest = requestConverter.documentIndexRequest(query, indexCoordinates, refreshPolicy);

		IndexResponse indexResponse = execute(client -> client.index(indexRequest));

		Object queryObject = query.getObject();

		if (queryObject != null) {
			query.setObject(entityOperations.updateIndexedObject(
					queryObject,
					new IndexedObjectInformation(
							indexResponse.id(),
							indexResponse.index(),
							indexResponse.seqNo(),
							indexResponse.primaryTerm(),
							indexResponse.version()),
					elasticsearchConverter,
					routingResolver));
		}

		return indexResponse.id();
	}

	@Override
	protected boolean doExists(String id, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		ExistsRequest request = requestConverter.documentExistsRequest(id, routingResolver.getRouting(), index);

		return execute(client -> client.exists(request)).value();
	}

	@Override
	protected String doDelete(String id, @Nullable String routing, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		DeleteRequest request = requestConverter.documentDeleteRequest(elasticsearchConverter.convertId(id), routing, index,
				getRefreshPolicy());
		return execute(client -> client.delete(request)).id();
	}

	@Override
	public ReindexResponse reindex(ReindexRequest reindexRequest) {

		Assert.notNull(reindexRequest, "reindexRequest must not be null");

		co.elastic.clients.elasticsearch.core.ReindexRequest reindexRequestES = requestConverter.reindex(reindexRequest,
				true);
		co.elastic.clients.elasticsearch.core.ReindexResponse reindexResponse = execute(
				client -> client.reindex(reindexRequestES));
		return responseConverter.reindexResponse(reindexResponse);
	}

	@Override
	public String submitReindex(ReindexRequest reindexRequest) {

		co.elastic.clients.elasticsearch.core.ReindexRequest reindexRequestES = requestConverter.reindex(reindexRequest,
				false);
		co.elastic.clients.elasticsearch.core.ReindexResponse reindexResponse = execute(
				client -> client.reindex(reindexRequestES));

		if (reindexResponse.task() == null) {
			throw new UnsupportedBackendOperation("ElasticsearchClient did not return a task id on submit request");
		}

		return reindexResponse.task();
	}

	@Override
	public List<IndexedObjectInformation> doBulkOperation(List<?> queries, BulkOptions bulkOptions,
			IndexCoordinates index) {

		BulkRequest bulkRequest = requestConverter.documentBulkRequest(queries, bulkOptions, index, refreshPolicy);
		BulkResponse bulkResponse = execute(client -> client.bulk(bulkRequest));
		List<IndexedObjectInformation> indexedObjectInformationList = checkForBulkOperationFailure(bulkResponse);
		updateIndexedObjectsWithQueries(queries, indexedObjectInformationList);
		return indexedObjectInformationList;
	}

	// endregion

	@Override
	public String getClusterVersion() {
		return execute(client -> client.info().version().number());
	}

	@Override
	public String getVendor() {
		return "Elasticsearch";
	}

	@Override
	public String getRuntimeLibraryVersion() {
		return Version.VERSION != null ? Version.VERSION.toString() : "0.0.0.?";
	}

	// region search operations
	@Override
	public long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		SearchRequest searchRequest = requestConverter.searchRequest(query, routingResolver.getRouting(), clazz, index,
				true);

		SearchResponse<EntityAsMap> searchResponse = execute(client -> client.search(searchRequest, EntityAsMap.class));

		return searchResponse.hits().total().value();
	}

	@Override
	public <T> SearchHits<T> search(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(clazz, "clazz must not be null");
		Assert.notNull(index, "index must not be null");

		if (query instanceof SearchTemplateQuery searchTemplateQuery) {
			return doSearch(searchTemplateQuery, clazz, index);
		} else {
			return doSearch(query, clazz, index);
		}
	}

	protected <T> SearchHits<T> doSearch(Query query, Class<T> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = requestConverter.searchRequest(query, routingResolver.getRouting(), clazz, index,
				false);
		SearchResponse<EntityAsMap> searchResponse = execute(client -> client.search(searchRequest, EntityAsMap.class));

		// noinspection DuplicatedCode
		ReadDocumentCallback<T> readDocumentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		SearchDocumentResponse.EntityCreator<T> entityCreator = getEntityCreator(readDocumentCallback);
		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);

		return callback.doWith(SearchDocumentResponseBuilder.from(searchResponse, entityCreator, jsonpMapper));
	}

	protected <T> SearchHits<T> doSearch(SearchTemplateQuery query, Class<T> clazz, IndexCoordinates index) {
		var searchTemplateRequest = requestConverter.searchTemplate(query, routingResolver.getRouting(), index);
		var searchTemplateResponse = execute(client -> client.searchTemplate(searchTemplateRequest, EntityAsMap.class));

		// noinspection DuplicatedCode
		ReadDocumentCallback<T> readDocumentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		SearchDocumentResponse.EntityCreator<T> entityCreator = getEntityCreator(readDocumentCallback);
		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);

		return callback.doWith(SearchDocumentResponseBuilder.from(searchTemplateResponse, entityCreator, jsonpMapper));
	}

	@Override
	protected <T> SearchHits<T> doSearch(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(clazz, "clazz must not be null");
		Assert.notNull(index, "index must not be null");

		return search(NativeQuery.builder() //
				.withQuery(q -> q.moreLikeThis(requestConverter.moreLikeThisQuery(query, index)))//
				.withPageable(query.getPageable()) //
				.build(), clazz, index);
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollStart(long scrollTimeInMillis, Query query, Class<T> clazz,
			IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(query.getPageable(), "pageable of query must not be null.");

		SearchRequest request = requestConverter.searchRequest(query, routingResolver.getRouting(), clazz, index, false,
				scrollTimeInMillis);
		SearchResponse<EntityAsMap> response = execute(client -> client.search(request, EntityAsMap.class));

		return getSearchScrollHits(clazz, index, response);
	}

	@Override
	public <T> SearchScrollHits<T> searchScrollContinue(String scrollId, long scrollTimeInMillis, Class<T> clazz,
			IndexCoordinates index) {

		Assert.notNull(scrollId, "scrollId must not be null");

		ScrollRequest request = ScrollRequest
				.of(sr -> sr.scrollId(scrollId).scroll(Time.of(t -> t.time(scrollTimeInMillis + "ms"))));
		ScrollResponse<EntityAsMap> response = execute(client -> client.scroll(request, EntityAsMap.class));

		return getSearchScrollHits(clazz, index, response);
	}

	private <T> SearchScrollHits<T> getSearchScrollHits(Class<T> clazz, IndexCoordinates index,
			ResponseBody<EntityAsMap> response) {
		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchScrollHits<T>> callback = new ReadSearchScrollDocumentResponseCallback<>(clazz,
				index);

		return callback
				.doWith(SearchDocumentResponseBuilder.from(response, getEntityCreator(documentCallback), jsonpMapper));
	}

	@Override
	public void searchScrollClear(List<String> scrollIds) {

		Assert.notNull(scrollIds, "scrollIds must not be null");

		if (!scrollIds.isEmpty()) {
			ClearScrollRequest request = ClearScrollRequest.of(csr -> csr.scrollId(scrollIds));
			execute(client -> client.clearScroll(request));
		}
	}

	@Override
	public <T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(clazz, "clazz must not be null");

		int size = queries.size();
		// noinspection unchecked
		return multiSearch(queries, Collections.nCopies(size, clazz), Collections.nCopies(size, index))
				.stream().map(searchHits -> (SearchHits<T>) searchHits)
				.collect(Collectors.toList());
	}

	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(classes, "classes must not be null");
		Assert.isTrue(queries.size() == classes.size(), "queries and classes must have the same size");

		return multiSearch(queries, classes, classes.stream().map(this::getIndexCoordinatesFor).toList());
	}

	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes,
			IndexCoordinates index) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(classes, "classes must not be null");
		Assert.notNull(index, "index must not be null");
		Assert.isTrue(queries.size() == classes.size(), "queries and classes must have the same size");

		return multiSearch(queries, classes, Collections.nCopies(queries.size(), index));
	}

	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes,
			List<IndexCoordinates> indexes) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(classes, "classes must not be null");
		Assert.notNull(indexes, "indexes must not be null");
		Assert.isTrue(queries.size() == classes.size() && queries.size() == indexes.size(),
				"queries, classes and indexes must have the same size");

		List<MultiSearchQueryParameter> multiSearchQueryParameters = new ArrayList<>(queries.size());
		Iterator<Class<?>> it = classes.iterator();
		Iterator<IndexCoordinates> indexesIt = indexes.iterator();

		Assert.isTrue(!queries.isEmpty(), "queries should have at least 1 query");
		boolean isSearchTemplateQuery = queries.get(0) instanceof SearchTemplateQuery;

		for (Query query : queries) {
			Assert.isTrue((query instanceof SearchTemplateQuery) == isSearchTemplateQuery,
					"SearchTemplateQuery can't be mixed with other types of query in multiple search");

			Class<?> clazz = it.next();
			IndexCoordinates index = indexesIt.next();
			multiSearchQueryParameters.add(new MultiSearchQueryParameter(query, clazz, index));
		}

		return multiSearch(multiSearchQueryParameters, isSearchTemplateQuery);
	}

	private List<SearchHits<?>> multiSearch(List<MultiSearchQueryParameter> multiSearchQueryParameters,
			boolean isSearchTemplateQuery) {
		return isSearchTemplateQuery ? doMultiTemplateSearch(multiSearchQueryParameters.stream()
				.map(p -> new MultiSearchTemplateQueryParameter((SearchTemplateQuery) p.query, p.clazz, p.index))
				.toList())
				: doMultiSearch(multiSearchQueryParameters);
	}

	private List<SearchHits<?>> doMultiTemplateSearch(
			List<MultiSearchTemplateQueryParameter> mSearchTemplateQueryParameters) {
		MsearchTemplateRequest request = requestConverter.searchMsearchTemplateRequest(mSearchTemplateQueryParameters,
				routingResolver.getRouting());

		MsearchTemplateResponse<EntityAsMap> response = execute(
				client -> client.msearchTemplate(request, EntityAsMap.class));
		List<MultiSearchResponseItem<EntityAsMap>> responseItems = response.responses();

		Assert.isTrue(mSearchTemplateQueryParameters.size() == responseItems.size(),
				"number of response items does not match number of requests");

		int size = mSearchTemplateQueryParameters.size();
		List<Class<?>> classes = mSearchTemplateQueryParameters
				.stream().map(MultiSearchTemplateQueryParameter::clazz).collect(Collectors.toList());
		List<IndexCoordinates> indices = mSearchTemplateQueryParameters
				.stream().map(MultiSearchTemplateQueryParameter::index).collect(Collectors.toList());

		return getSearchHitsFromMsearchResponse(size, classes, indices, responseItems);
	}

	private List<SearchHits<?>> doMultiSearch(List<MultiSearchQueryParameter> multiSearchQueryParameters) {

		MsearchRequest request = requestConverter.searchMsearchRequest(multiSearchQueryParameters,
				routingResolver.getRouting());

		MsearchResponse<EntityAsMap> msearchResponse = execute(client -> client.msearch(request, EntityAsMap.class));
		List<MultiSearchResponseItem<EntityAsMap>> responseItems = msearchResponse.responses();

		Assert.isTrue(multiSearchQueryParameters.size() == responseItems.size(),
				"number of response items does not match number of requests");

		int size = multiSearchQueryParameters.size();
		List<Class<?>> classes = multiSearchQueryParameters
				.stream().map(MultiSearchQueryParameter::clazz).collect(Collectors.toList());
		List<IndexCoordinates> indices = multiSearchQueryParameters
				.stream().map(MultiSearchQueryParameter::index).collect(Collectors.toList());

		return getSearchHitsFromMsearchResponse(size, classes, indices, responseItems);
	}

	/**
	 * {@link MsearchResponse} and {@link MsearchTemplateResponse} share the same {@link MultiSearchResponseItem}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<SearchHits<?>> getSearchHitsFromMsearchResponse(int size, List<Class<?>> classes,
			List<IndexCoordinates> indices, List<MultiSearchResponseItem<EntityAsMap>> responseItems) {
		List<SearchHits<?>> searchHitsList = new ArrayList<>(size);
		Iterator<Class<?>> clazzIter = classes.iterator();
		Iterator<IndexCoordinates> indexIter = indices.iterator();
		Iterator<MultiSearchResponseItem<EntityAsMap>> responseIterator = responseItems.iterator();

		while (clazzIter.hasNext() && indexIter.hasNext()) {
			MultiSearchResponseItem<EntityAsMap> responseItem = responseIterator.next();

			if (responseItem.isResult()) {

				Class clazz = clazzIter.next();
				IndexCoordinates index = indexIter.next();
				ReadDocumentCallback<?> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, clazz,
						index);
				SearchDocumentResponseCallback<SearchHits<?>> callback = new ReadSearchDocumentResponseCallback<>(clazz,
						index);

				SearchHits<?> searchHits = callback.doWith(
						SearchDocumentResponseBuilder.from(responseItem.result(), getEntityCreator(documentCallback), jsonpMapper));

				searchHitsList.add(searchHits);
			} else {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn(String.format("multisearch response contains failure: %s",
							responseItem.failure().error().reason()));
				}
			}
		}

		return searchHitsList;
	}

	/**
	 * value class combining the information needed for a single query in a multisearch request.
	 */
	record MultiSearchQueryParameter(Query query, Class<?> clazz, IndexCoordinates index) {
	}

	/**
	 * value class combining the information needed for a single query in a template multisearch request.
	 */
	record MultiSearchTemplateQueryParameter(SearchTemplateQuery query, Class<?> clazz, IndexCoordinates index) {
	}

	@Override
	public String openPointInTime(IndexCoordinates index, Duration keepAlive, Boolean ignoreUnavailable) {

		Assert.notNull(index, "index must not be null");
		Assert.notNull(keepAlive, "keepAlive must not be null");
		Assert.notNull(ignoreUnavailable, "ignoreUnavailable must not be null");

		var request = requestConverter.searchOpenPointInTimeRequest(index, keepAlive, ignoreUnavailable);
		return execute(client -> client.openPointInTime(request)).id();
	}

	@Override
	public Boolean closePointInTime(String pit) {

		Assert.notNull(pit, "pit must not be null");

		ClosePointInTimeRequest request = requestConverter.searchClosePointInTime(pit);
		var response = execute(client -> client.closePointInTime(request));
		return response.succeeded();
	}

	// endregion

	// region script methods
	@Override
	public boolean putScript(Script script) {

		Assert.notNull(script, "script must not be null");

		var request = requestConverter.scriptPut(script);
		return execute(client -> client.putScript(request)).acknowledged();
	}

	@Nullable
	@Override
	public Script getScript(String name) {

		Assert.notNull(name, "name must not be null");

		var request = requestConverter.scriptGet(name);
		return responseConverter.scriptResponse(execute(client -> client.getScript(request)));
	}

	public boolean deleteScript(String name) {

		Assert.notNull(name, "name must not be null");

		DeleteScriptRequest request = requestConverter.scriptDelete(name);
		return execute(client -> client.deleteScript(request)).acknowledged();
	}
	// endregion

	// region client callback
	/**
	 * Callback interface to be used with {@link #execute(ElasticsearchTemplate.ClientCallback)} for operating directly on
	 * the {@link ElasticsearchClient}.
	 */
	@FunctionalInterface
	public interface ClientCallback<T> {
		T doWithClient(ElasticsearchClient client) throws IOException;
	}

	/**
	 * Execute a callback with the {@link ElasticsearchClient} and provide exception translation.
	 *
	 * @param callback the callback to execute, must not be {@literal null}
	 * @param <T> the type returned from the callback
	 * @return the callback result
	 */
	public <T> T execute(ElasticsearchTemplate.ClientCallback<T> callback) {

		Assert.notNull(callback, "callback must not be null");

		try {
			return callback.doWithClient(client);
		} catch (IOException | RuntimeException e) {
			throw exceptionTranslator.translateException(e);
		}
	}
	// endregion

	// region helper methods
	@Override
	public Query matchAllQuery() {
		return NativeQuery.builder().withQuery(qb -> qb.matchAll(mab -> mab)).build();
	}

	@Override
	public Query idsQuery(List<String> ids) {
		return NativeQuery.builder().withQuery(qb -> qb.ids(iq -> iq.values(ids))).build();
	}

	@Override
	public BaseQueryBuilder queryBuilderWithIds(List<String> ids) {
		return NativeQuery.builder().withIds(ids);
	}

	/**
	 * extract the list of {@link IndexedObjectInformation} from a {@link BulkResponse}.
	 *
	 * @param bulkResponse the response to evaluate
	 * @return the list of the {@link IndexedObjectInformation}s
	 */
	protected List<IndexedObjectInformation> checkForBulkOperationFailure(BulkResponse bulkResponse) {

		if (bulkResponse.errors()) {
			Map<String, BulkFailureException.FailureDetails> failedDocuments = new HashMap<>();
			for (BulkResponseItem item : bulkResponse.items()) {

				if (item.error() != null) {
					failedDocuments.put(item.id(), new BulkFailureException.FailureDetails(item.status(), item.error().reason()));
				}
			}
			throw new BulkFailureException(
					"Bulk operation has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + ']',
					failedDocuments);
		}

		return bulkResponse.items().stream().map(
				item -> new IndexedObjectInformation(item.id(), item.index(), item.seqNo(), item.primaryTerm(), item.version()))
				.collect(Collectors.toList());

	}
	// endregion

}
