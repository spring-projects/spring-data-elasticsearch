/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.data.elasticsearch.client.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.ElasticsearchHost.State;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * @author Christoph Strobl
 * @since 4.0
 */
class MultiNodeHostProvider implements HostProvider {

	private final HttpHeaders headers;
	private final Consumer<Throwable> errorListener;
	private final Map<String, ElasticsearchHost> hosts;

	MultiNodeHostProvider(HttpHeaders headers, Consumer<Throwable> errorListener, String... hosts) {

		this.headers = headers;
		this.errorListener = errorListener;
		this.hosts = new ConcurrentHashMap<>();
		for (String host : hosts) {
			this.hosts.put(host, new ElasticsearchHost(host, State.UNKNOWN));
		}
	}

	public Mono<ClusterInformation> clusterInfo() {
		return nodes(null).map(this::updateNodeState).buffer(hosts.size())
				.then(Mono.just(new ClusterInformation(new LinkedHashSet<>(this.hosts.values()))));
	}

	Collection<ElasticsearchHost> getCachedHostState() {
		return hosts.values();
	}

	@Override
	public HttpHeaders getDefaultHeaders() {
		return this.headers;
	}

	@Override
	public Mono<String> lookupActiveHost(VerificationMode verificationMode) {

		if (VerificationMode.LAZY.equals(verificationMode)) {
			for (ElasticsearchHost entry : hosts()) {
				if (entry.isOnline()) {
					return Mono.just(entry.getHost());
				}
			}
		}

		return findActiveHostInKnownActives() //
				.switchIfEmpty(findActiveHostInUnresolved()) //
				.switchIfEmpty(findActiveHostInDead()) //
				.switchIfEmpty(Mono.error(() -> new NoReachableHostException(new LinkedHashSet<>(getCachedHostState()))));
	}

	@Override
	public HostProvider withDefaultHeaders(HttpHeaders headers) {
		return new MultiNodeHostProvider(headers, errorListener, hosts.keySet().toArray(new String[0]));
	}

	@Override
	public HostProvider withErrorListener(Consumer<Throwable> errorListener) {
		return new MultiNodeHostProvider(headers, errorListener, hosts.keySet().toArray(new String[0]));
	}

	private Mono<String> findActiveHostInKnownActives() {
		return findActiveForSate(State.ONLINE);
	}

	private Mono<String> findActiveHostInUnresolved() {
		return findActiveForSate(State.UNKNOWN);
	}

	private Mono<String> findActiveHostInDead() {
		return findActiveForSate(State.OFFLINE);
	}

	private Mono<String> findActiveForSate(State state) {
		return nodes(state).map(this::updateNodeState).filter(ElasticsearchHost::isOnline).map(it -> it.getHost()).next();
	}

	private ElasticsearchHost updateNodeState(Tuple2<String, ClientResponse> tuple2) {

		State state = tuple2.getT2().statusCode().isError() ? State.OFFLINE : State.ONLINE;
		ElasticsearchHost elasticsearchHost = new ElasticsearchHost(tuple2.getT1(), state);
		hosts.put(tuple2.getT1(), elasticsearchHost);
		return elasticsearchHost;
	}

	private Flux<Tuple2<String, ClientResponse>> nodes(@Nullable State state) {

		return Flux.fromIterable(hosts()) //
				.filter(entry -> state != null ? entry.getState().equals(state) : true) //
				.map(ElasticsearchHost::getHost) //
				.flatMap(host -> {

					Mono<ClientResponse> exchange = createWebClient(host, headers) //
							.head().uri("/").exchange().doOnError(throwable -> {

								hosts.put(host, new ElasticsearchHost(host, State.OFFLINE));
								errorListener.accept(throwable);
							});

					return Mono.just(host).zipWith(exchange);
				}) //
				.onErrorContinue((throwable, o) -> {
					errorListener.accept(throwable);
				});
	}

	private List<ElasticsearchHost> hosts() {

		List<ElasticsearchHost> hosts = new ArrayList<>(this.hosts.values());
		Collections.shuffle(hosts);

		return hosts;
	}
}
