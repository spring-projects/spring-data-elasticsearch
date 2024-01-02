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

import co.elastic.clients.ApiClient;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.JsonEndpoint;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.endpoints.EndpointWithResponseMapperAttr;
import co.elastic.clients.util.ObjectBuilder;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Reactive version of {@link co.elastic.clients.elasticsearch.ElasticsearchClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ReactiveElasticsearchClient extends ApiClient<ElasticsearchTransport, ReactiveElasticsearchClient>
		implements AutoCloseable {

	public ReactiveElasticsearchClient(ElasticsearchTransport transport) {
		super(transport, null);
	}

	public ReactiveElasticsearchClient(ElasticsearchTransport transport, @Nullable TransportOptions transportOptions) {
		super(transport, transportOptions);
	}

	@Override
	public ReactiveElasticsearchClient withTransportOptions(@Nullable TransportOptions transportOptions) {
		return new ReactiveElasticsearchClient(transport, transportOptions);
	}

	@Override
	public void close() throws Exception {
		transport.close();
	}

	// region child clients

	public ReactiveElasticsearchClusterClient cluster() {
		return new ReactiveElasticsearchClusterClient(transport, transportOptions);
	}

	public ReactiveElasticsearchIndicesClient indices() {
		return new ReactiveElasticsearchIndicesClient(transport, transportOptions);
	}

	// endregion
	// region info

	public Mono<InfoResponse> info() {
		return Mono
				.fromFuture(transport.performRequestAsync(InfoRequest._INSTANCE, InfoRequest._ENDPOINT, transportOptions));
	}

	public Mono<BooleanResponse> ping() {
		return Mono
				.fromFuture(transport.performRequestAsync(PingRequest._INSTANCE, PingRequest._ENDPOINT, transportOptions));
	}

	// endregion
	// region document

	public <T> Mono<IndexResponse> index(IndexRequest<T> request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, IndexRequest._ENDPOINT, transportOptions));
	}

	public <T> Mono<IndexResponse> index(Function<IndexRequest.Builder<T>, ObjectBuilder<IndexRequest<T>>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return index(fn.apply(new IndexRequest.Builder<>()).build());
	}

	public Mono<BulkResponse> bulk(BulkRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, BulkRequest._ENDPOINT, transportOptions));
	}

	public Mono<BulkResponse> bulk(Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return bulk(fn.apply(new BulkRequest.Builder()).build());
	}

	public <T> Mono<GetResponse<T>> get(GetRequest request, Class<T> tClass) {

		Assert.notNull(request, "request must not be null");

		// code adapted from
		// co.elastic.clients.elasticsearch.ElasticsearchClient.get(co.elastic.clients.elasticsearch.core.GetRequest,
		// java.lang.Class<TDocument>)
		// noinspection unchecked
		JsonEndpoint<GetRequest, GetResponse<T>, ErrorResponse> endpoint = (JsonEndpoint<GetRequest, GetResponse<T>, ErrorResponse>) GetRequest._ENDPOINT;
		endpoint = new EndpointWithResponseMapperAttr<>(endpoint, "co.elastic.clients:Deserializer:_global.get.TDocument",
				getDeserializer(tClass));

		return Mono.fromFuture(transport.performRequestAsync(request, endpoint, transportOptions));
	}

	public Mono<BooleanResponse> exists(ExistsRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, ExistsRequest._ENDPOINT, transportOptions));
	}

	public <T, P> Mono<UpdateResponse<T>> update(UpdateRequest<T, P> request, Class<T> clazz) {

		Assert.notNull(request, "request must not be null");

		// noinspection unchecked
		JsonEndpoint<UpdateRequest<?, ?>, UpdateResponse<T>, ErrorResponse> endpoint = new EndpointWithResponseMapperAttr(
				UpdateRequest._ENDPOINT, "co.elastic.clients:Deserializer:_global.update.TDocument",
				this.getDeserializer(clazz));
		return Mono.fromFuture(transport.performRequestAsync(request, endpoint, this.transportOptions));
	}

	public <T, P> Mono<UpdateResponse<T>> update(
			Function<UpdateRequest.Builder<T, P>, ObjectBuilder<UpdateRequest<T, P>>> fn, Class<T> clazz) {

		Assert.notNull(fn, "fn must not be null");

		return update(fn.apply(new UpdateRequest.Builder<>()).build(), clazz);
	}

	public <T> Mono<GetResponse<T>> get(Function<GetRequest.Builder, ObjectBuilder<GetRequest>> fn, Class<T> tClass) {
		Assert.notNull(fn, "fn must not be null");

		return get(fn.apply(new GetRequest.Builder()).build(), tClass);
	}

	public <T> Mono<MgetResponse<T>> mget(MgetRequest request, Class<T> clazz) {

		Assert.notNull(request, "request must not be null");
		Assert.notNull(clazz, "clazz must not be null");

		// noinspection unchecked
		JsonEndpoint<MgetRequest, MgetResponse<T>, ErrorResponse> endpoint = (JsonEndpoint<MgetRequest, MgetResponse<T>, ErrorResponse>) MgetRequest._ENDPOINT;
		endpoint = new EndpointWithResponseMapperAttr<>(endpoint, "co.elastic.clients:Deserializer:_global.mget.TDocument",
				this.getDeserializer(clazz));

		return Mono.fromFuture(transport.performRequestAsync(request, endpoint, transportOptions));
	}

	public <T> Mono<MgetResponse<T>> mget(Function<MgetRequest.Builder, ObjectBuilder<MgetRequest>> fn, Class<T> clazz) {

		Assert.notNull(fn, "fn must not be null");

		return mget(fn.apply(new MgetRequest.Builder()).build(), clazz);
	}

	public Mono<ReindexResponse> reindex(ReindexRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, ReindexRequest._ENDPOINT, transportOptions));
	}

	public Mono<ReindexResponse> reindex(Function<ReindexRequest.Builder, ObjectBuilder<ReindexRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return reindex(fn.apply(new ReindexRequest.Builder()).build());
	}

	public Mono<DeleteResponse> delete(DeleteRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, DeleteRequest._ENDPOINT, transportOptions));
	}

	public Mono<DeleteResponse> delete(Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return delete(fn.apply(new DeleteRequest.Builder()).build());
	}

	public Mono<DeleteByQueryResponse> deleteByQuery(DeleteByQueryRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, DeleteByQueryRequest._ENDPOINT, transportOptions));
	}

	public Mono<DeleteByQueryResponse> deleteByQuery(
			Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return deleteByQuery(fn.apply(new DeleteByQueryRequest.Builder()).build());
	}

	// endregion
	// region search

	public <T> Mono<ResponseBody<T>> search(SearchRequest request, Class<T> tDocumentClass) {

		Assert.notNull(request, "request must not be null");
		Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request,
				SearchRequest.createSearchEndpoint(this.getDeserializer(tDocumentClass)), transportOptions));
	}

	public <T> Mono<ResponseBody<T>> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn,
			Class<T> tDocumentClass) {

		Assert.notNull(fn, "fn must not be null");
		Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

		return search(fn.apply(new SearchRequest.Builder()).build(), tDocumentClass);
	}

	/**
	 * @since 5.1
	 */
	public <T> Mono<SearchTemplateResponse<T>> searchTemplate(SearchTemplateRequest request, Class<T> tDocumentClass) {

		Assert.notNull(request, "request must not be null");
		Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request,
				SearchTemplateRequest.createSearchTemplateEndpoint(this.getDeserializer(tDocumentClass)), transportOptions));
	}

	/**
	 * @since 5.1
	 */
	public <T> Mono<SearchTemplateResponse<T>> searchTemplate(
			Function<SearchTemplateRequest.Builder, ObjectBuilder<SearchTemplateRequest>> fn, Class<T> tDocumentClass) {

		Assert.notNull(fn, "fn must not be null");

		return searchTemplate(fn.apply(new SearchTemplateRequest.Builder()).build(), tDocumentClass);
	}

	public <T> Mono<ScrollResponse<T>> scroll(ScrollRequest request, Class<T> tDocumentClass) {

		Assert.notNull(request, "request must not be null");
		Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

		// code adapted from
		// co.elastic.clients.elasticsearch.ElasticsearchClient.scroll(co.elastic.clients.elasticsearch.core.ScrollRequest,
		// java.lang.Class<TDocument>)
		// noinspection unchecked
		JsonEndpoint<ScrollRequest, ScrollResponse<T>, ErrorResponse> endpoint = (JsonEndpoint<ScrollRequest, ScrollResponse<T>, ErrorResponse>) ScrollRequest._ENDPOINT;
		endpoint = new EndpointWithResponseMapperAttr<>(endpoint,
				"co.elastic.clients:Deserializer:_global.scroll.TDocument", getDeserializer(tDocumentClass));

		return Mono.fromFuture(transport.performRequestAsync(request, endpoint, transportOptions));
	}

	public <T> Mono<ScrollResponse<T>> scroll(Function<ScrollRequest.Builder, ObjectBuilder<ScrollRequest>> fn,
			Class<T> tDocumentClass) {

		Assert.notNull(fn, "fn must not be null");
		Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

		return scroll(fn.apply(new ScrollRequest.Builder()).build(), tDocumentClass);
	}

	public Mono<ClearScrollResponse> clearScroll(ClearScrollRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, ClearScrollRequest._ENDPOINT, transportOptions));
	}

	public Mono<ClearScrollResponse> clearScroll(
			Function<ClearScrollRequest.Builder, ObjectBuilder<ClearScrollRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return clearScroll(fn.apply(new ClearScrollRequest.Builder()).build());
	}

	/**
	 * @since 5.0
	 */
	public Mono<OpenPointInTimeResponse> openPointInTime(OpenPointInTimeRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, OpenPointInTimeRequest._ENDPOINT, transportOptions));
	}

	/**
	 * @since 5.0
	 */
	public Mono<OpenPointInTimeResponse> openPointInTime(
			Function<OpenPointInTimeRequest.Builder, ObjectBuilder<OpenPointInTimeRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return openPointInTime(fn.apply(new OpenPointInTimeRequest.Builder()).build());
	}

	/**
	 * @since 5.0
	 */
	public Mono<ClosePointInTimeResponse> closePointInTime(ClosePointInTimeRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, ClosePointInTimeRequest._ENDPOINT, transportOptions));
	}

	/**
	 * @since 5.0
	 */
	public Mono<ClosePointInTimeResponse> closePointInTime(
			Function<ClosePointInTimeRequest.Builder, ObjectBuilder<ClosePointInTimeRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return closePointInTime(fn.apply(new ClosePointInTimeRequest.Builder()).build());
	}
	// endregion

	// region script api
	/**
	 * @since 5.1
	 */
	public Mono<PutScriptResponse> putScript(PutScriptRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, PutScriptRequest._ENDPOINT, transportOptions));
	}

	/**
	 * @since 5.1
	 */
	public Mono<PutScriptResponse> putScript(Function<PutScriptRequest.Builder, ObjectBuilder<PutScriptRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return putScript(fn.apply(new PutScriptRequest.Builder()).build());
	}

	/**
	 * @since 5.1
	 */
	public Mono<GetScriptResponse> getScript(GetScriptRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, GetScriptRequest._ENDPOINT, transportOptions));
	}

	/**
	 * @since 5.1
	 */
	public Mono<GetScriptResponse> getScript(Function<GetScriptRequest.Builder, ObjectBuilder<GetScriptRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return getScript(fn.apply(new GetScriptRequest.Builder()).build());
	}

	/**
	 * @since 5.1
	 */
	public Mono<DeleteScriptResponse> deleteScript(DeleteScriptRequest request) {

		Assert.notNull(request, "request must not be null");

		return Mono.fromFuture(transport.performRequestAsync(request, DeleteScriptRequest._ENDPOINT, transportOptions));
	}

	/**
	 * @since 5.1
	 */
	public Mono<DeleteScriptResponse> deleteScript(
			Function<DeleteScriptRequest.Builder, ObjectBuilder<DeleteScriptRequest>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return deleteScript(fn.apply(new DeleteScriptRequest.Builder()).build());
	}
	// endregion

}
