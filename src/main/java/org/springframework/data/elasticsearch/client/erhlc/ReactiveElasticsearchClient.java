/*
 * Copyright 2018-2022 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Collection;
import java.util.function.Consumer;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.suggest.Suggest;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A reactive client to connect to Elasticsearch.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Henrique Amaral
 * @author Thomas Geese
 * @author Farid Faoudi
 * @author Sijia Liu
 * @since 3.2
 * @see ClientConfiguration
 * @see ReactiveRestClients
 * @deprecated since 5.0
 */
@Deprecated
public interface ReactiveElasticsearchClient {

	/**
	 * Pings the remote Elasticsearch cluster and emits {@literal true} if the ping succeeded, {@literal false} otherwise.
	 *
	 * @return the {@link Mono} emitting the result of the ping attempt.
	 */
	default Mono<Boolean> ping() {
		return ping(HttpHeaders.EMPTY);
	}

	/**
	 * Pings the remote Elasticsearch cluster and emits {@literal true} if the ping succeeded, {@literal false} otherwise.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @return the {@link Mono} emitting the result of the ping attempt.
	 */
	Mono<Boolean> ping(HttpHeaders headers);

	/**
	 * Get the cluster info otherwise provided when sending an HTTP request to port 9200.
	 *
	 * @return the {@link Mono} emitting the result of the info request.
	 */
	default Mono<MainResponse> info() {
		return info(HttpHeaders.EMPTY);
	}

	/**
	 * Get the cluster info otherwise provided when sending an HTTP request to port 9200.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @return the {@link Mono} emitting the result of the info request.
	 */
	Mono<MainResponse> info(HttpHeaders headers);

	/**
	 * Execute a {@link GetRequest} against the {@literal get} API to retrieve a document by id.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link GetResult result}.
	 */
	default Mono<GetResult> get(Consumer<GetRequest> consumer) {

		GetRequest request = new GetRequest();
		consumer.accept(request);
		return get(request);
	}

	/**
	 * Execute the given {@link GetRequest} against the {@literal get} API to retrieve a document by id.
	 *
	 * @param getRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link GetResult result}.
	 */
	default Mono<GetResult> get(GetRequest getRequest) {
		return get(HttpHeaders.EMPTY, getRequest);
	}

	/**
	 * Execute the given {@link GetRequest} against the {@literal get} API to retrieve a document by id.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param getRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html">Get API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link GetResult result}.
	 */
	Mono<GetResult> get(HttpHeaders headers, GetRequest getRequest);

	/**
	 * Execute a {@link MultiGetRequest} against the {@literal multi-get} API to retrieve multiple documents by id.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting the {@link MultiGetItemResponse result}.
	 */
	default Flux<MultiGetItemResponse> multiGet(Consumer<MultiGetRequest> consumer) {

		MultiGetRequest request = new MultiGetRequest();
		consumer.accept(request);
		return multiGet(request);
	}

	/**
	 * Execute the given {@link MultiGetRequest} against the {@literal multi-get} API to retrieve multiple documents by
	 * id.
	 *
	 * @param multiGetRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting the {@link MultiGetItemResponse result}.
	 */
	default Flux<MultiGetItemResponse> multiGet(MultiGetRequest multiGetRequest) {
		return multiGet(HttpHeaders.EMPTY, multiGetRequest);
	}

	/**
	 * Execute the given {@link MultiGetRequest} against the {@literal multi-get} API to retrieve multiple documents by
	 * id.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param multiGetRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting the {@link MultiGetItemResponse result}.
	 */
	Flux<MultiGetItemResponse> multiGet(HttpHeaders headers, MultiGetRequest multiGetRequest);

	/**
	 * Checks for the existence of a document. Emits {@literal true} if it exists, {@literal false} otherwise.
	 *
	 * @param consumer never {@literal null}.
	 * @return the {@link Mono} emitting {@literal true} if it exists, {@literal false} otherwise.
	 */
	default Mono<Boolean> exists(Consumer<GetRequest> consumer) {

		GetRequest request = new GetRequest();
		consumer.accept(request);
		return exists(request);
	}

	/**
	 * Checks for the existence of a document. Emits {@literal true} if it exists, {@literal false} otherwise.
	 *
	 * @param getRequest must not be {@literal null}.
	 * @return the {@link Mono} emitting {@literal true} if it exists, {@literal false} otherwise.
	 */
	default Mono<Boolean> exists(GetRequest getRequest) {
		return exists(HttpHeaders.EMPTY, getRequest);
	}

	/**
	 * Checks for the existence of a document. Emits {@literal true} if it exists, {@literal false} otherwise.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param getRequest must not be {@literal null}.
	 * @return the {@link Mono} emitting {@literal true} if it exists, {@literal false} otherwise.
	 */
	Mono<Boolean> exists(HttpHeaders headers, GetRequest getRequest);

	/**
	 * Execute an {@link IndexRequest} against the {@literal index} API to index a document.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index.html">Index API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link IndexResponse}.
	 */
	default Mono<IndexResponse> index(Consumer<IndexRequest> consumer) {

		IndexRequest request = new IndexRequest();
		consumer.accept(request);
		return index(request);
	}

	/**
	 * Execute the given {@link IndexRequest} against the {@literal index} API to index a document.
	 *
	 * @param indexRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index.html">Index API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link IndexResponse}.
	 */
	default Mono<IndexResponse> index(IndexRequest indexRequest) {
		return index(HttpHeaders.EMPTY, indexRequest);
	}

	/**
	 * Execute the given {@link IndexRequest} against the {@literal index} API to index a document.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param indexRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index.html">Index API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link IndexResponse}.
	 */
	Mono<IndexResponse> index(HttpHeaders headers, IndexRequest indexRequest);

	/**
	 * Gain access to index related commands.
	 *
	 * @return access to index related commands.
	 */
	Indices indices();

	/**
	 * Gain Access to cluster related commands.
	 *
	 * @return Cluster implementations
	 * @since 4.2
	 */
	Cluster cluster();

	/**
	 * Execute an {@link UpdateRequest} against the {@literal update} API to alter a document.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html">Update API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link UpdateResponse}.
	 */
	default Mono<UpdateResponse> update(Consumer<UpdateRequest> consumer) {

		UpdateRequest request = new UpdateRequest();
		consumer.accept(request);
		return update(request);
	}

	/**
	 * Execute the given {@link UpdateRequest} against the {@literal update} API to alter a document.
	 *
	 * @param updateRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html">Update API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link UpdateResponse}.
	 */
	default Mono<UpdateResponse> update(UpdateRequest updateRequest) {
		return update(HttpHeaders.EMPTY, updateRequest);
	}

	/**
	 * Execute the given {@link UpdateRequest} against the {@literal update} API to alter a document.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param updateRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html">Update API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link UpdateResponse}.
	 */
	Mono<UpdateResponse> update(HttpHeaders headers, UpdateRequest updateRequest);

	/**
	 * Execute a {@link DeleteRequest} against the {@literal delete} API to remove a document.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html">Delete API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link DeleteResponse}.
	 */
	default Mono<DeleteResponse> delete(Consumer<DeleteRequest> consumer) {

		DeleteRequest request = new DeleteRequest();
		consumer.accept(request);
		return delete(request);
	}

	/**
	 * Execute the given {@link DeleteRequest} against the {@literal delete} API to remove a document.
	 *
	 * @param deleteRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html">Delete API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link DeleteResponse}.
	 */
	default Mono<DeleteResponse> delete(DeleteRequest deleteRequest) {
		return delete(HttpHeaders.EMPTY, deleteRequest);
	}

	/**
	 * Execute the given {@link DeleteRequest} against the {@literal delete} API to remove a document.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param deleteRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html">Delete API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the {@link DeleteResponse}.
	 */
	Mono<DeleteResponse> delete(HttpHeaders headers, DeleteRequest deleteRequest);

	/**
	 * Execute a {@link SearchRequest} against the {@literal count} API.
	 *
	 * @param consumer new {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-count.html">Count API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the count result.
	 * @since 4.0
	 */
	default Mono<Long> count(Consumer<SearchRequest> consumer) {

		SearchRequest searchRequest = new SearchRequest();
		consumer.accept(searchRequest);
		return count(searchRequest);
	}

	/**
	 * Execute a {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-count.html">Count API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the count result.
	 * @since 4.0
	 */
	default Mono<Long> count(SearchRequest searchRequest) {
		return count(HttpHeaders.EMPTY, searchRequest);
	}

	/**
	 * Execute a {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-count.html">Count API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the count result.
	 * @since 4.0
	 */
	Mono<Long> count(HttpHeaders headers, SearchRequest searchRequest);

	/**
	 * Executes a {@link SearchTemplateRequest} against the {@literal search template} API.
	 *
	 * @param consumer must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html">Search Template
	 *      API on elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	default Flux<SearchHit> searchTemplate(Consumer<SearchTemplateRequest> consumer) {
		SearchTemplateRequest request = new SearchTemplateRequest();
		consumer.accept(request);
		return searchTemplate(request);
	}

	/**
	 * Executes a {@link SearchTemplateRequest} against the {@literal search template} API.
	 *
	 * @param searchTemplateRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html">Search Template
	 *      API on elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	default Flux<SearchHit> searchTemplate(SearchTemplateRequest searchTemplateRequest) {
		return searchTemplate(HttpHeaders.EMPTY, searchTemplateRequest);
	}

	/**
	 * Executes a {@link SearchTemplateRequest} against the {@literal search template} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchTemplateRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html">Search Template
	 *      API on elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	Flux<SearchHit> searchTemplate(HttpHeaders headers, SearchTemplateRequest searchTemplateRequest);

	/**
	 * Execute a {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	default Flux<SearchHit> search(Consumer<SearchRequest> consumer) {

		SearchRequest request = new SearchRequest();
		consumer.accept(request);
		return search(request);
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	default Flux<SearchHit> search(SearchRequest searchRequest) {
		return search(HttpHeaders.EMPTY, searchRequest);
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	Flux<SearchHit> search(HttpHeaders headers, SearchRequest searchRequest);

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API returning the whole response in one Mono.
	 *
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the whole {@link SearchResponse}.
	 * @since 4.1
	 */
	default Mono<SearchResponse> searchForResponse(SearchRequest searchRequest) {
		return searchForResponse(HttpHeaders.EMPTY, searchRequest);
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API returning the whole response in one Mono.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Mono} emitting the whole {@link SearchResponse}.
	 * @since 4.1
	 */
	Mono<SearchResponse> searchForResponse(HttpHeaders headers, SearchRequest searchRequest);

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param consumer never {@literal null}.
	 * @return the {@link Flux} emitting {@link Suggest suggestions} one by one.
	 * @since 4.1
	 */
	default Flux<Suggest> suggest(Consumer<SearchRequest> consumer) {

		SearchRequest request = new SearchRequest();
		consumer.accept(request);
		return suggest(request);
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param searchRequest must not be {@literal null}.
	 * @return the {@link Flux} emitting {@link Suggest suggestions} one by one.
	 * @since 4.1
	 */
	default Flux<Suggest> suggest(SearchRequest searchRequest) {
		return suggest(HttpHeaders.EMPTY, searchRequest);
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchRequest must not be {@literal null}.
	 * @return the {@link Flux} emitting {@link Suggest suggestions} one by one.
	 * @since 4.1
	 */
	Flux<Suggest> suggest(HttpHeaders headers, SearchRequest searchRequest);

	/**
	 * Execute the given {@link SearchRequest} with aggregations against the {@literal search} API.
	 *
	 * @param consumer never {@literal null}.
	 * @return the {@link Flux} emitting {@link Aggregation} one by one.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @since 4.0
	 */
	default Flux<Aggregation> aggregate(Consumer<SearchRequest> consumer) {

		Assert.notNull(consumer, "consumer must not be null");

		SearchRequest request = new SearchRequest();
		consumer.accept(request);
		return aggregate(request);
	}

	/**
	 * Execute the given {@link SearchRequest} with aggregations against the {@literal search} API.
	 *
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting {@link Aggregation} one by one.
	 * @since 4.0
	 */
	default Flux<Aggregation> aggregate(SearchRequest searchRequest) {
		return aggregate(HttpHeaders.EMPTY, searchRequest);
	}

	/**
	 * Execute the given {@link SearchRequest} with aggregations against the {@literal search} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html">Search API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting {@link Aggregation} one by one.
	 * @since 4.0
	 */
	Flux<Aggregation> aggregate(HttpHeaders headers, SearchRequest searchRequest);

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search scroll} API.
	 *
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html">Search
	 *      Scroll API on elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	default Flux<SearchHit> scroll(SearchRequest searchRequest) {
		return scroll(HttpHeaders.EMPTY, searchRequest);
	}

	/**
	 * Execute the given {@link SearchRequest} against the {@literal search scroll} API. <br />
	 * Scroll keeps track of {@link SearchResponse#getScrollId() scrollIds} returned by the server and provides them when
	 * requesting more results via {@code _search/scroll}. All bound server resources are freed on completion.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param searchRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html">Search
	 *      Scroll API on elastic.co</a>
	 * @return the {@link Flux} emitting {@link SearchHit hits} one by one.
	 */
	Flux<SearchHit> scroll(HttpHeaders headers, SearchRequest searchRequest);

	/**
	 * Execute a {@link DeleteByQueryRequest} against the {@literal delete by query} API.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">Delete By
	 *      Query API on elastic.co</a>
	 * @return a {@link Mono} emitting the emitting operation response.
	 */
	default Mono<BulkByScrollResponse> deleteBy(Consumer<DeleteByQueryRequest> consumer) {

		DeleteByQueryRequest request = new DeleteByQueryRequest();
		consumer.accept(request);
		return deleteBy(request);
	}

	/**
	 * Execute a {@link DeleteByQueryRequest} against the {@literal delete by query} API.
	 *
	 * @param deleteRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">Delete By
	 *      Query API on elastic.co</a>
	 * @return a {@link Mono} emitting the emitting operation response.
	 */
	default Mono<BulkByScrollResponse> deleteBy(DeleteByQueryRequest deleteRequest) {
		return deleteBy(HttpHeaders.EMPTY, deleteRequest);
	}

	/**
	 * Execute a {@link DeleteByQueryRequest} against the {@literal delete by query} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param deleteRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html">Delete By
	 *      Query API on elastic.co</a>
	 * @return a {@link Mono} emitting operation response.
	 */
	Mono<BulkByScrollResponse> deleteBy(HttpHeaders headers, DeleteByQueryRequest deleteRequest);

	/**
	 * Execute a {@link UpdateByQueryRequest} against the {@literal update by query} API.
	 *
	 * @param consumer must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">Update By
	 *      * Query API on elastic.co</a>
	 * @return a {@link Mono} emitting operation response.
	 */
	default Mono<ByQueryResponse> updateBy(Consumer<UpdateByQueryRequest> consumer) {

		final UpdateByQueryRequest request = new UpdateByQueryRequest();
		consumer.accept(request);
		return updateBy(request);
	}

	/**
	 * Execute a {@link UpdateByQueryRequest} against the {@literal update by query} API.
	 *
	 * @param updateRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">Update By
	 *      * Query API on elastic.co</a>
	 * @return a {@link Mono} emitting operation response.
	 */
	default Mono<ByQueryResponse> updateBy(UpdateByQueryRequest updateRequest) {
		return updateBy(HttpHeaders.EMPTY, updateRequest);
	}

	/**
	 * Execute a {@link UpdateByQueryRequest} against the {@literal update by query} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param updateRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html">Update By
	 *      * Query API on elastic.co</a>
	 * @return a {@link Mono} emitting operation response.
	 */
	Mono<ByQueryResponse> updateBy(HttpHeaders headers, UpdateByQueryRequest updateRequest);

	/**
	 * Execute a {@link BulkRequest} against the {@literal bulk} API.
	 *
	 * @param consumer never {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk API on
	 *      elastic.co</a>
	 * @return a {@link Mono} emitting the emitting operation response.
	 */
	default Mono<BulkResponse> bulk(Consumer<BulkRequest> consumer) {

		BulkRequest request = new BulkRequest();
		consumer.accept(request);
		return bulk(request);
	}

	/**
	 * Execute a {@link BulkRequest} against the {@literal bulk} API.
	 *
	 * @param bulkRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk API on
	 *      elastic.co</a>
	 * @return a {@link Mono} emitting the emitting operation response.
	 */
	default Mono<BulkResponse> bulk(BulkRequest bulkRequest) {
		return bulk(HttpHeaders.EMPTY, bulkRequest);
	}

	/**
	 * Execute a {@link BulkRequest} against the {@literal bulk} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param bulkRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk API on
	 *      elastic.co</a>
	 * @return a {@link Mono} emitting operation response.
	 */
	Mono<BulkResponse> bulk(HttpHeaders headers, BulkRequest bulkRequest);

	/**
	 * Execute the given {@link ReindexRequest} against the {@literal reindex} API.
	 *
	 * @param consumer must not be {@literal null}
	 * @return the {@link Mono} emitting the response
	 * @since 4.4
	 */
	default Mono<BulkByScrollResponse> reindex(Consumer<ReindexRequest> consumer) {

		ReindexRequest reindexRequest = new ReindexRequest();
		consumer.accept(reindexRequest);
		return reindex(reindexRequest);
	}

	/**
	 * Execute the given {@link ReindexRequest} against the {@literal reindex} API.
	 *
	 * @param reindexRequest must not be {@literal null}
	 * @return the {@link Mono} emitting the response
	 * @since 4.4
	 */
	default Mono<BulkByScrollResponse> reindex(ReindexRequest reindexRequest) {
		return reindex(HttpHeaders.EMPTY, reindexRequest);
	}

	/**
	 * Execute the given {@link ReindexRequest} against the {@literal reindex} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param reindexRequest must not be {@literal null}
	 * @return the {@link Mono} emitting the response
	 * @since 4.4
	 */
	Mono<BulkByScrollResponse> reindex(HttpHeaders headers, ReindexRequest reindexRequest);

	/**
	 * Execute the given {@link ReindexRequest} against the {@literal reindex} API.
	 *
	 * @param consumer must not be {@literal null}
	 * @return the {@link Mono} emitting the task id
	 * @since 4.4
	 */
	default Mono<String> submitReindex(Consumer<ReindexRequest> consumer) {

		ReindexRequest reindexRequest = new ReindexRequest();
		consumer.accept(reindexRequest);
		return submitReindex(reindexRequest);
	}

	/**
	 * Execute the given {@link ReindexRequest} against the {@literal reindex} API.
	 *
	 * @param reindexRequest must not be {@literal null}
	 * @return the {@link Mono} emitting the task id
	 * @since 4.4
	 */
	default Mono<String> submitReindex(ReindexRequest reindexRequest) {
		return submitReindex(HttpHeaders.EMPTY, reindexRequest);
	}

	/**
	 * Execute the given {@link ReindexRequest} against the {@literal reindex} API.
	 *
	 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
	 * @param reindexRequest must not be {@literal null}
	 * @return the {@link Mono} emitting the task id
	 * @since 4.4
	 */
	Mono<String> submitReindex(HttpHeaders headers, ReindexRequest reindexRequest);

	/**
	 * Compose the actual command/s to run against Elasticsearch using the underlying {@link WebClient connection}.
	 * {@link #execute(ReactiveElasticsearchClientCallback) Execute} selects an active server from the available ones and
	 * retries operations that fail with a {@link ConnectException} on another node if the previously selected one becomes
	 * unavailable.
	 *
	 * @param callback the {@link ReactiveElasticsearchClientCallback callback} wielding the actual command to run.
	 * @param <T> the type emitted by the returned Mono.
	 * @return the {@link Mono} emitting the {@link ClientResponse} once subscribed.
	 */
	@SuppressWarnings("JavaDoc")
	<T> Mono<T> execute(ReactiveElasticsearchClientCallback<T> callback);

	/**
	 * Get the current client {@link Status}. <br />
	 * <strong>NOTE</strong> the actual implementation might choose to actively check the current cluster state by pinging
	 * known nodes.
	 *
	 * @return the actual {@link Status} information.
	 */
	Mono<Status> status();

	/**
	 * Low level callback interface operating upon {@link WebClient} to send commands towards elasticsearch.
	 *
	 * @param <T> the type emitted by the returned Mono.
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	interface ReactiveElasticsearchClientCallback<T> {
		Mono<T> doWithClient(WebClient client);
	}

	/**
	 * Cumulative client {@link ElasticsearchHost} information.
	 *
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	interface Status {

		/**
		 * Get the collection of known hosts.
		 *
		 * @return never {@literal null}.
		 */
		Collection<ElasticsearchHost> hosts();

		/**
		 * @return {@literal true} if at least one host is available.
		 */
		default boolean isOk() {

			Collection<ElasticsearchHost> hosts = hosts();

			if (CollectionUtils.isEmpty(hosts)) {
				return false;
			}

			return hosts().stream().anyMatch(ElasticsearchHost::isOnline);
		}
	}

	/**
	 * Encapsulation of methods for accessing the Indices API.
	 *
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/_indices_apis.html">Indices
	 *      API</a>.
	 * @author Christoph Strobl
	 */
	interface Indices {

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.get.GetIndexRequest} against the
		 * {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return the {@link Mono} emitting {@literal true} if the index exists, {@literal false} otherwise.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-exists.html"> Indices
		 *      Exists API on elastic.co</a>
		 * @deprecated since 4.2
		 */
		@Deprecated
		default Mono<Boolean> existsIndex(Consumer<org.elasticsearch.action.admin.indices.get.GetIndexRequest> consumer) {

			org.elasticsearch.action.admin.indices.get.GetIndexRequest request = new org.elasticsearch.action.admin.indices.get.GetIndexRequest();
			consumer.accept(request);
			return existsIndex(request);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.get.GetIndexRequest} against the
		 * {@literal indices} API.
		 *
		 * @param getIndexRequest must not be {@literal null}.
		 * @return the {@link Mono} emitting {@literal true} if the index exists, {@literal false} otherwise.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-exists.html"> Indices
		 *      Exists API on elastic.co</a>
		 * @deprecated since 4.2, use {@link #existsIndex(GetIndexRequest)}
		 */
		@Deprecated
		default Mono<Boolean> existsIndex(org.elasticsearch.action.admin.indices.get.GetIndexRequest getIndexRequest) {
			return existsIndex(HttpHeaders.EMPTY, getIndexRequest);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.get.GetIndexRequest} against the
		 * {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getIndexRequest must not be {@literal null}.
		 * @return the {@link Mono} emitting {@literal true} if the index exists, {@literal false} otherwise.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-exists.html"> Indices
		 *      Exists API on elastic.co</a>
		 * @deprecated since 4.2, use {@link #existsIndex(HttpHeaders, GetIndexRequest)}
		 */
		@Deprecated
		Mono<Boolean> existsIndex(HttpHeaders headers,
				org.elasticsearch.action.admin.indices.get.GetIndexRequest getIndexRequest);

		/**
		 * Execute the given {@link GetIndexRequest} against the {@literal indices} API.
		 *
		 * @param getIndexRequest must not be {@literal null}.
		 * @return the {@link Mono} emitting {@literal true} if the index exists, {@literal false} otherwise.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-exists.html"> Indices
		 *      Exists API on elastic.co</a>
		 * @since 4.2
		 */
		default Mono<Boolean> existsIndex(GetIndexRequest getIndexRequest) {
			return existsIndex(HttpHeaders.EMPTY, getIndexRequest);
		}

		/**
		 * Execute the given {@link GetIndexRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getIndexRequest must not be {@literal null}.
		 * @return the {@link Mono} emitting {@literal true} if the index exists, {@literal false} otherwise.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-exists.html"> Indices
		 *      Exists API on elastic.co</a>
		 * @since 4.2
		 */
		Mono<Boolean> existsIndex(HttpHeaders headers, GetIndexRequest getIndexRequest);

		/**
		 * Execute the given {@link DeleteIndexRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html"> Indices
		 *      Delete API on elastic.co</a>
		 */
		default Mono<Boolean> deleteIndex(Consumer<DeleteIndexRequest> consumer) {

			DeleteIndexRequest request = new DeleteIndexRequest();
			consumer.accept(request);
			return deleteIndex(request);
		}

		/**
		 * Execute the given {@link DeleteIndexRequest} against the {@literal indices} API.
		 *
		 * @param deleteIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html"> Indices
		 *      Delete API on elastic.co</a>
		 */
		default Mono<Boolean> deleteIndex(DeleteIndexRequest deleteIndexRequest) {
			return deleteIndex(HttpHeaders.EMPTY, deleteIndexRequest);
		}

		/**
		 * Execute the given {@link DeleteIndexRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param deleteIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html"> Indices
		 *      Delete API on elastic.co</a>
		 */
		Mono<Boolean> deleteIndex(HttpHeaders headers, DeleteIndexRequest deleteIndexRequest);

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.create.CreateIndexRequest} against the
		 * {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if
		 *         eg. the index already exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html"> Indices
		 *      Create API on elastic.co</a>
		 * @deprecated since 4.2
		 */
		@Deprecated
		default Mono<Boolean> createIndex(
				Consumer<org.elasticsearch.action.admin.indices.create.CreateIndexRequest> consumer) {

			org.elasticsearch.action.admin.indices.create.CreateIndexRequest request = new org.elasticsearch.action.admin.indices.create.CreateIndexRequest();
			consumer.accept(request);
			return createIndex(request);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.create.CreateIndexRequest} against the
		 * {@literal indices} API.
		 *
		 * @param createIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if
		 *         eg. the index already exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html"> Indices
		 *      Create API on elastic.co</a>
		 * @deprecated since 4.2, use {@link #createIndex(CreateIndexRequest)}
		 */
		@Deprecated
		default Mono<Boolean> createIndex(
				org.elasticsearch.action.admin.indices.create.CreateIndexRequest createIndexRequest) {
			return createIndex(HttpHeaders.EMPTY, createIndexRequest);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.create.CreateIndexRequest} against the
		 * {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param createIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if
		 *         eg. the index already exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html"> Indices
		 *      Create API on elastic.co</a>
		 * @deprecated since 4.2, use {@link #createIndex(HttpHeaders, CreateIndexRequest)}
		 */
		@Deprecated
		Mono<Boolean> createIndex(HttpHeaders headers,
				org.elasticsearch.action.admin.indices.create.CreateIndexRequest createIndexRequest);

		/**
		 * Execute the given {@link CreateIndexRequest} against the {@literal indices} API.
		 *
		 * @param createIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if
		 *         eg. the index already exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html"> Indices
		 *      Create API on elastic.co</a>
		 * @since 4.2
		 */
		default Mono<Boolean> createIndex(CreateIndexRequest createIndexRequest) {
			return createIndex(HttpHeaders.EMPTY, createIndexRequest);
		}

		/**
		 * Execute the given {@link CreateIndexRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param createIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if
		 *         eg. the index already exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html"> Indices
		 *      Create API on elastic.co</a>
		 * @since 4.2
		 */
		Mono<Boolean> createIndex(HttpHeaders headers, CreateIndexRequest createIndexRequest);

		/**
		 * Execute the given {@link OpenIndexRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html"> Indices
		 *      Open API on elastic.co</a>
		 */
		default Mono<Void> openIndex(Consumer<OpenIndexRequest> consumer) {

			OpenIndexRequest request = new OpenIndexRequest();
			consumer.accept(request);
			return openIndex(request);
		}

		/**
		 * Execute the given {@link OpenIndexRequest} against the {@literal indices} API.
		 *
		 * @param openIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html"> Indices
		 *      Open API on elastic.co</a>
		 */
		default Mono<Void> openIndex(OpenIndexRequest openIndexRequest) {
			return openIndex(HttpHeaders.EMPTY, openIndexRequest);
		}

		/**
		 * Execute the given {@link OpenIndexRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param openIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html"> Indices
		 *      Open API on elastic.co</a>
		 */
		Mono<Void> openIndex(HttpHeaders headers, OpenIndexRequest openIndexRequest);

		/**
		 * Execute the given {@link CloseIndexRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html"> Indices
		 *      Close API on elastic.co</a>
		 */
		default Mono<Void> closeIndex(Consumer<CloseIndexRequest> consumer) {

			CloseIndexRequest request = new CloseIndexRequest();
			consumer.accept(request);
			return closeIndex(request);
		}

		/**
		 * Execute the given {@link CloseIndexRequest} against the {@literal indices} API.
		 *
		 * @param closeIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html"> Indices
		 *      Close API on elastic.co</a>
		 */
		default Mono<Void> closeIndex(CloseIndexRequest closeIndexRequest) {
			return closeIndex(HttpHeaders.EMPTY, closeIndexRequest);
		}

		/**
		 * Execute the given {@link CloseIndexRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param closeIndexRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html"> Indices
		 *      CLose API on elastic.co</a>
		 */
		Mono<Void> closeIndex(HttpHeaders headers, CloseIndexRequest closeIndexRequest);

		/**
		 * Execute the given {@link RefreshRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html"> Indices
		 *      Refresh API on elastic.co</a>
		 */
		default Mono<Void> refreshIndex(Consumer<RefreshRequest> consumer) {

			RefreshRequest request = new RefreshRequest();
			consumer.accept(request);
			return refreshIndex(request);
		}

		/**
		 * Execute the given {@link RefreshRequest} against the {@literal indices} API.
		 *
		 * @param refreshRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html"> Indices
		 *      Refresh API on elastic.co</a>
		 */
		default Mono<Void> refreshIndex(RefreshRequest refreshRequest) {
			return refreshIndex(HttpHeaders.EMPTY, refreshRequest);
		}

		/**
		 * Execute the given {@link RefreshRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param refreshRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html"> Indices
		 *      Refresh API on elastic.co</a>
		 */
		Mono<Void> refreshIndex(HttpHeaders headers, RefreshRequest refreshRequest);

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest} against the
		 * {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html"> Indices
		 *      Put Mapping API on elastic.co</a>
		 * @deprecated since 4.2
		 */
		@Deprecated
		default Mono<Boolean> putMapping(
				Consumer<org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest> consumer) {

			org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest request = new org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest();
			consumer.accept(request);
			return putMapping(request);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest} against the
		 * {@literal indices} API.
		 *
		 * @param putMappingRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html"> Indices
		 *      Put Mapping API on elastic.co</a>
		 * @deprecated since 4.2, use {@link #putMapping(PutMappingRequest)}
		 */
		@Deprecated
		default Mono<Boolean> putMapping(
				org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest putMappingRequest) {
			return putMapping(HttpHeaders.EMPTY, putMappingRequest);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest} against the
		 * {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param putMappingRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html"> Indices
		 *      Put Mapping API on elastic.co</a>
		 * @deprecated since 4.2, use {@link #putMapping(HttpHeaders, PutMappingRequest)}
		 */
		@Deprecated
		Mono<Boolean> putMapping(HttpHeaders headers,
				org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest putMappingRequest);

		/**
		 * Execute the given {@link PutMappingRequest} against the {@literal indices} API.
		 *
		 * @param putMappingRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html"> Indices
		 *      Put Mapping API on elastic.co</a>
		 * @since 4.2
		 */
		default Mono<Boolean> putMapping(PutMappingRequest putMappingRequest) {
			return putMapping(HttpHeaders.EMPTY, putMappingRequest);
		}

		/**
		 * Execute the given {@link PutMappingRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param putMappingRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html"> Indices
		 *      Put Mapping API on elastic.co</a>
		 * @since 4.2
		 */
		Mono<Boolean> putMapping(HttpHeaders headers, PutMappingRequest putMappingRequest);

		/**
		 * Execute the given {@link FlushRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-flush.html"> Indices Flush
		 *      API on elastic.co</a>
		 */
		default Mono<Void> flushIndex(Consumer<FlushRequest> consumer) {

			FlushRequest request = new FlushRequest();
			consumer.accept(request);
			return flushIndex(request);
		}

		/**
		 * Execute the given {@link RefreshRequest} against the {@literal indices} API.
		 *
		 * @param flushRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-flush.html"> Indices Flush
		 *      API on elastic.co</a>
		 */
		default Mono<Void> flushIndex(FlushRequest flushRequest) {
			return flushIndex(HttpHeaders.EMPTY, flushRequest);
		}

		/**
		 * Execute the given {@link RefreshRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param flushRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-flush.html"> Indices Flush
		 *      API on elastic.co</a>
		 */
		Mono<Void> flushIndex(HttpHeaders headers, FlushRequest flushRequest);

		/**
		 * Execute the given {@link GetSettingsRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-settings.html"> Indices
		 *      Flush API on elastic.co</a>
		 * @since 4.1
		 */
		default Mono<GetSettingsResponse> getSettings(Consumer<GetSettingsRequest> consumer) {

			GetSettingsRequest request = new GetSettingsRequest();
			consumer.accept(request);
			return getSettings(request);
		}

		/**
		 * Execute the given {@link GetSettingsRequest} against the {@literal indices} API.
		 *
		 * @param getSettingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-settings.html"> Indices
		 *      Flush API on elastic.co</a>
		 * @since 4.1
		 */
		default Mono<GetSettingsResponse> getSettings(GetSettingsRequest getSettingsRequest) {
			return getSettings(HttpHeaders.EMPTY, getSettingsRequest);
		}

		/**
		 * Execute the given {@link GetSettingsRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getSettingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-settings.html"> Indices
		 *      Flush API on elastic.co</a>
		 * @since 4.1
		 */
		Mono<GetSettingsResponse> getSettings(HttpHeaders headers, GetSettingsRequest getSettingsRequest);

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest} against the
		 * {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html"> Indices
		 *      Flush API on elastic.co</a>
		 * @since 4.1
		 * @deprecated since 4.2
		 */
		@Deprecated
		default Mono<org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse> getMapping(
				Consumer<org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest> consumer) {

			org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest request = new org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest();
			consumer.accept(request);
			return getMapping(request);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest} against the
		 * {@literal indices} API.
		 *
		 * @param getMappingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html"> Indices
		 *      Flush API on elastic.co</a>
		 * @since 4.1
		 * @deprecated since 4.2, use {@link #getMapping(GetMappingsRequest)}
		 */
		@Deprecated
		default Mono<org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse> getMapping(
				org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest getMappingsRequest) {
			return getMapping(HttpHeaders.EMPTY, getMappingsRequest);
		}

		/**
		 * Execute the given {@link org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest} against the
		 * {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getMappingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html"> Indices
		 *      Flush API on elastic.co</a>
		 * @since 4.1
		 * @deprecated since 4.2, use {@link #getMapping(HttpHeaders, GetMappingsRequest)}
		 */
		@Deprecated
		Mono<org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse> getMapping(HttpHeaders headers,
				org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest getMappingsRequest);

		/**
		 * Execute the given {@link GetMappingsRequest} against the {@literal indices} API.
		 *
		 * @param getMappingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html"> Indices
		 *      Get mapping API on elastic.co</a>
		 * @since 4.2
		 */
		default Mono<GetMappingsResponse> getMapping(GetMappingsRequest getMappingsRequest) {
			return getMapping(HttpHeaders.EMPTY, getMappingsRequest);
		}

		/**
		 * Execute the given {@link GetMappingsRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getMappingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html"> Indices
		 *      Get mapping API on elastic.co</a>
		 * @since 4.2
		 */
		Mono<GetMappingsResponse> getMapping(HttpHeaders headers, GetMappingsRequest getMappingsRequest);

		/**
		 * Execute the given {@link GetFieldMappingsRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-field-mapping.html">
		 *      Indices Flush API on elastic.co</a>
		 * @since 4.2
		 */
		default Mono<GetFieldMappingsResponse> getFieldMapping(Consumer<GetFieldMappingsRequest> consumer) {

			GetFieldMappingsRequest request = new GetFieldMappingsRequest();
			consumer.accept(request);
			return getFieldMapping(request);
		}

		/**
		 * Execute the given {@link GetFieldMappingsRequest} against the {@literal indices} API.
		 *
		 * @param getFieldMappingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-field-mapping.html">
		 *      Indices Flush API on elastic.co</a>
		 * @since 4.2
		 */
		default Mono<GetFieldMappingsResponse> getFieldMapping(GetFieldMappingsRequest getFieldMappingsRequest) {
			return getFieldMapping(HttpHeaders.EMPTY, getFieldMappingsRequest);
		}

		/**
		 * Execute the given {@link GetFieldMappingsRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getFieldMappingsRequest must not be {@literal null}.
		 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error} if eg. the index
		 *         does not exist.
		 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-field-mapping.html">
		 *      Indices Flush API on elastic.co</a>
		 * @since 4.2
		 */
		Mono<GetFieldMappingsResponse> getFieldMapping(HttpHeaders headers,
				GetFieldMappingsRequest getFieldMappingsRequest);

		/**
		 * Execute the given {@link IndicesAliasesRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		default Mono<Boolean> updateAliases(Consumer<IndicesAliasesRequest> consumer) {
			IndicesAliasesRequest indicesAliasesRequest = new IndicesAliasesRequest();
			consumer.accept(indicesAliasesRequest);
			return updateAliases(indicesAliasesRequest);
		}

		/**
		 * Execute the given {@link IndicesAliasesRequest} against the {@literal indices} API.
		 *
		 * @param indicesAliasesRequest must not be {@literal null}
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		default Mono<Boolean> updateAliases(IndicesAliasesRequest indicesAliasesRequest) {
			return updateAliases(HttpHeaders.EMPTY, indicesAliasesRequest);
		}

		/**
		 * Execute the given {@link IndicesAliasesRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param indicesAliasesRequest must not be {@literal null}
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		Mono<Boolean> updateAliases(HttpHeaders headers, IndicesAliasesRequest indicesAliasesRequest);

		/**
		 * Execute the given {@link GetAliasesRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		default Mono<GetAliasesResponse> getAliases(Consumer<GetAliasesRequest> consumer) {
			GetAliasesRequest getAliasesRequest = new GetAliasesRequest();
			consumer.accept(getAliasesRequest);
			return getAliases(getAliasesRequest);
		}

		/**
		 * Execute the given {@link GetAliasesRequest} against the {@literal indices} API.
		 *
		 * @param getAliasesRequest must not be {@literal null}
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		default Mono<GetAliasesResponse> getAliases(GetAliasesRequest getAliasesRequest) {
			return getAliases(HttpHeaders.EMPTY, getAliasesRequest);
		}

		/**
		 * Execute the given {@link GetAliasesRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getAliasesRequest must not be {@literal null}
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		Mono<GetAliasesResponse> getAliases(HttpHeaders headers, GetAliasesRequest getAliasesRequest);

		/**
		 * Execute the given {@link PutIndexTemplateRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		default Mono<Boolean> putTemplate(Consumer<PutIndexTemplateRequest> consumer, String templateName) {
			PutIndexTemplateRequest putIndexTemplateRequest = new PutIndexTemplateRequest(templateName);
			consumer.accept(putIndexTemplateRequest);
			return putTemplate(putIndexTemplateRequest);
		}

		/**
		 * Execute the given {@link PutIndexTemplateRequest} against the {@literal indices} API.
		 *
		 * @param putIndexTemplateRequest must not be {@literal null}
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		default Mono<Boolean> putTemplate(PutIndexTemplateRequest putIndexTemplateRequest) {
			return putTemplate(HttpHeaders.EMPTY, putIndexTemplateRequest);
		}

		/**
		 * Execute the given {@link PutIndexTemplateRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param putIndexTemplateRequest must not be {@literal null}
		 * @return a {@link Mono} signalling operation completion.
		 * @since 4.1
		 */
		Mono<Boolean> putTemplate(HttpHeaders headers, PutIndexTemplateRequest putIndexTemplateRequest);

		/**
		 * Execute the given {@link GetIndexTemplatesRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return a {@link Mono} with the GetIndexTemplatesResponse.
		 * @since 4.1
		 */
		default Mono<GetIndexTemplatesResponse> getTemplate(Consumer<GetIndexTemplatesRequest> consumer) {

			GetIndexTemplatesRequest getIndexTemplatesRequest = new GetIndexTemplatesRequest();
			consumer.accept(getIndexTemplatesRequest);
			return getTemplate(getIndexTemplatesRequest);
		}

		/**
		 * Execute the given {@link GetIndexTemplatesRequest} against the {@literal indices} API.
		 *
		 * @param getIndexTemplatesRequest must not be {@literal null}
		 * @return a {@link Mono} with the GetIndexTemplatesResponse.
		 * @since 4.1
		 */
		default Mono<GetIndexTemplatesResponse> getTemplate(GetIndexTemplatesRequest getIndexTemplatesRequest) {
			return getTemplate(HttpHeaders.EMPTY, getIndexTemplatesRequest);
		}

		/**
		 * Execute the given {@link GetIndexTemplatesRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getIndexTemplatesRequest must not be {@literal null}
		 * @return a {@link Mono} with the GetIndexTemplatesResponse.
		 * @since 4.1
		 */
		Mono<GetIndexTemplatesResponse> getTemplate(HttpHeaders headers, GetIndexTemplatesRequest getIndexTemplatesRequest);

		/**
		 * Execute the given {@link IndexTemplatesExistRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return the {@link Mono} emitting {@literal true} if the template exists, {@literal false} otherwise.
		 * @since 4.1
		 */
		default Mono<Boolean> existsTemplate(Consumer<IndexTemplatesExistRequest> consumer) {

			IndexTemplatesExistRequest indexTemplatesExistRequest = new IndexTemplatesExistRequest();
			consumer.accept(indexTemplatesExistRequest);
			return existsTemplate(indexTemplatesExistRequest);
		}

		/**
		 * Execute the given {@link IndexTemplatesExistRequest} against the {@literal indices} API.
		 *
		 * @param indexTemplatesExistRequest must not be {@literal null}
		 * @return the {@link Mono} emitting {@literal true} if the template exists, {@literal false} otherwise.
		 * @since 4.1
		 */
		default Mono<Boolean> existsTemplate(IndexTemplatesExistRequest indexTemplatesExistRequest) {
			return existsTemplate(HttpHeaders.EMPTY, indexTemplatesExistRequest);
		}

		/**
		 * Execute the given {@link IndexTemplatesExistRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param indexTemplatesExistRequest must not be {@literal null}
		 * @return the {@link Mono} emitting {@literal true} if the template exists, {@literal false} otherwise.
		 * @since 4.1
		 */
		Mono<Boolean> existsTemplate(HttpHeaders headers, IndexTemplatesExistRequest indexTemplatesExistRequest);

		/**
		 * Execute the given {@link DeleteIndexTemplateRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return the {@link Mono} emitting {@literal true} if the template exists, {@literal false} otherwise.
		 * @since 4.1
		 */
		default Mono<Boolean> deleteTemplate(Consumer<DeleteIndexTemplateRequest> consumer) {

			DeleteIndexTemplateRequest deleteIndexTemplateRequest = new DeleteIndexTemplateRequest();
			consumer.accept(deleteIndexTemplateRequest);
			return deleteTemplate(deleteIndexTemplateRequest);
		}

		/**
		 * Execute the given {@link DeleteIndexTemplateRequest} against the {@literal indices} API.
		 *
		 * @param deleteIndexTemplateRequest must not be {@literal null}
		 * @return the {@link Mono} emitting {@literal true} if the template exists, {@literal false} otherwise.
		 * @since 4.1
		 */
		default Mono<Boolean> deleteTemplate(DeleteIndexTemplateRequest deleteIndexTemplateRequest) {
			return deleteTemplate(HttpHeaders.EMPTY, deleteIndexTemplateRequest);
		}

		/**
		 * Execute the given {@link DeleteIndexTemplateRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param deleteIndexTemplateRequest must not be {@literal null}
		 * @return the {@link Mono} emitting {@literal true} if the template exists, {@literal false} otherwise.
		 * @since 4.1
		 */
		Mono<Boolean> deleteTemplate(HttpHeaders headers, DeleteIndexTemplateRequest deleteIndexTemplateRequest);

		/**
		 * Execute the given {@link GetIndexRequest} against the {@literal indices} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return the {@link Mono} emitting the response
		 * @since 4.2
		 */
		default Mono<GetIndexResponse> getIndex(Consumer<GetIndexRequest> consumer) {
			GetIndexRequest getIndexRequest = new GetIndexRequest();
			consumer.accept(getIndexRequest);
			return getIndex(getIndexRequest);
		}

		/**
		 * Execute the given {@link GetIndexRequest} against the {@literal indices} API.
		 *
		 * @param getIndexRequest must not be {@literal null}
		 * @return the {@link Mono} emitting the response
		 * @since 4.2
		 */
		default Mono<GetIndexResponse> getIndex(GetIndexRequest getIndexRequest) {
			return getIndex(HttpHeaders.EMPTY, getIndexRequest);
		}

		/**
		 * Execute the given {@link GetIndexRequest} against the {@literal indices} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param getIndexRequest must not be {@literal null}
		 * @return the {@link Mono} emitting the response
		 * @since 4.2
		 */
		Mono<GetIndexResponse> getIndex(HttpHeaders headers, GetIndexRequest getIndexRequest);
	}

	/**
	 * Encapsulation of methods for accessing the Cluster API.
	 *
	 * @author Peter-Josef Meisch
	 * @since 4.2
	 */
	interface Cluster {

		/**
		 * Execute the given {{@link ClusterHealthRequest}} against the {@literal cluster} API.
		 *
		 * @param consumer never {@literal null}.
		 * @return Mono emitting the {@link ClusterHealthResponse}.
		 */
		default Mono<ClusterHealthResponse> health(Consumer<ClusterHealthRequest> consumer) {

			ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest();
			consumer.accept(clusterHealthRequest);
			return health(clusterHealthRequest);
		}

		/**
		 * Execute the given {{@link ClusterHealthRequest}} against the {@literal cluster} API.
		 *
		 * @param clusterHealthRequest must not be {@literal null} // * @return Mono emitting the
		 *          {@link ClusterHealthResponse}.
		 */
		default Mono<ClusterHealthResponse> health(ClusterHealthRequest clusterHealthRequest) {
			return health(HttpHeaders.EMPTY, clusterHealthRequest);
		}

		/**
		 * Execute the given {{@link ClusterHealthRequest}} against the {@literal cluster} API.
		 *
		 * @param headers Use {@link HttpHeaders} to provide eg. authentication data. Must not be {@literal null}.
		 * @param clusterHealthRequest must not be {@literal null} // * @return Mono emitting the
		 *          {@link ClusterHealthResponse}.
		 */
		Mono<ClusterHealthResponse> health(HttpHeaders headers, ClusterHealthRequest clusterHealthRequest);
	}
}
