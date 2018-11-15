/*
 * Copyright 2018. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.elasticsearch.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Collection;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.elasticsearch.client.ClientProvider.HostState;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A reactive client to connect to Elasticsearch. <br />
 * 
 * @author Christoph Strobl
 * @since 4.0
 */
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
	 * Execute the given {@link MultiGetRequest} against the {@literal multi-get} API to retrieve multiple documents by
	 * id.
	 *
	 * @param multiGetRequest must not be {@literal null}.
	 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html">Multi Get API on
	 *      elastic.co</a>
	 * @return the {@link Flux} emitting the {@link GetResult result}.
	 */
	default Flux<GetResult> multiGet(MultiGetRequest multiGetRequest) {
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
	 * @return the {@link Flux} emitting the {@link GetResult result}.
	 */
	Flux<GetResult> multiGet(HttpHeaders headers, MultiGetRequest multiGetRequest);

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
	 * Compose the actual command/s to run against Elasticsearch using the underlying {@link WebClient connection}.
	 * {@link #execute(ReactiveElasticsearchClientCallback) Execute} selects an active server from the available ones and
	 * retries operations that fail with a {@link ConnectException} on another node if the previously selected one becomes
	 * unavailable.
	 *
	 * @param callback the {@link ReactiveElasticsearchClientCallback callback} wielding the actual command to run.
	 * @return the {@link Mono} emitting the {@link ClientResponse} once subscribed.
	 */
	Mono<ClientResponse> execute(ReactiveElasticsearchClientCallback callback);

	/**
	 * Get the current client {@link Status}. <br />
	 * <strong>NOTE</strong> the actual implementation might choose to actively check the current cluster state by pinging
	 * known nodes.
	 *
	 * @return
	 */
	Mono<Status> status();

	/**
	 * Low level callback interface operating upon {@link WebClient} to send commands towards elasticsearch.
	 *
	 * @author Christoph Strobl
	 * @since 4.0
	 */
	interface ReactiveElasticsearchClientCallback {
		Mono<ClientResponse> doWithClient(WebClient client);
	}

	/**
	 * Client status information like known hosts.
	 *
	 * @author Christoph Strobl
	 * @since 4.0
	 */
	interface Status {

		/**
		 * Get the list of known hosts and their status.
		 * 
		 * @return never {@literal null}.
		 */
		Collection<HostState> hosts();

		/**
		 * @return {@literal true} if at least one host is available.
		 */
		default boolean isOk() {

			Collection<HostState> hosts = hosts();

			if (CollectionUtils.isEmpty(hosts)) {
				return false;
			}

			return hosts().stream().filter(it -> !it.isOnline()).findFirst().isPresent();
		}
	}
}
