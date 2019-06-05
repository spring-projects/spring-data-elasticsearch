/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.client.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.ElasticsearchHost.State;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link HostProvider} for a cluster of nodes.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
class MultiNodeHostProvider implements HostProvider {

	private final WebClientProvider clientProvider;
	private final Map<InetSocketAddress, ElasticsearchHost> hosts;

	MultiNodeHostProvider(WebClientProvider clientProvider, InetSocketAddress... endpoints) {

		this.clientProvider = clientProvider;
		this.hosts = new ConcurrentHashMap<>();
		for (InetSocketAddress endpoint : endpoints) {
			this.hosts.put(endpoint, new ElasticsearchHost(endpoint, State.UNKNOWN));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.HostProvider#clusterInfo()
	 */
	public Mono<ClusterInformation> clusterInfo() {
		return nodes(null).map(this::updateNodeState).buffer(hosts.size())
				.then(Mono.just(new ClusterInformation(new LinkedHashSet<>(this.hosts.values()))));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.HostProvider#createWebClient(java.net.InetSocketAddress)
	 */
	@Override
	public WebClient createWebClient(InetSocketAddress endpoint) {
		return this.clientProvider.get(endpoint);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.HostProvider#lookupActiveHost(org.springframework.data.elasticsearch.client.reactive.HostProvider.Verification)
	 */
	@Override
	public Mono<InetSocketAddress> lookupActiveHost(Verification verification) {

		if (Verification.LAZY.equals(verification)) {
			for (ElasticsearchHost entry : hosts()) {
				if (entry.isOnline()) {
					return Mono.just(entry.getEndpoint());
				}
			}
		}

		return findActiveHostInKnownActives() //
				.switchIfEmpty(findActiveHostInUnresolved()) //
				.switchIfEmpty(findActiveHostInDead()) //
				.switchIfEmpty(Mono.error(() -> new NoReachableHostException(new LinkedHashSet<>(getCachedHostState()))));
	}

	Collection<ElasticsearchHost> getCachedHostState() {
		return hosts.values();
	}

	private Mono<InetSocketAddress> findActiveHostInKnownActives() {
		return findActiveForSate(State.ONLINE);
	}

	private Mono<InetSocketAddress> findActiveHostInUnresolved() {
		return findActiveForSate(State.UNKNOWN);
	}

	private Mono<InetSocketAddress> findActiveHostInDead() {
		return findActiveForSate(State.OFFLINE);
	}

	private Mono<InetSocketAddress> findActiveForSate(State state) {
		return nodes(state).map(this::updateNodeState).filter(ElasticsearchHost::isOnline)
				.map(ElasticsearchHost::getEndpoint).next();
	}

	private ElasticsearchHost updateNodeState(Tuple2<InetSocketAddress, ClientResponse> tuple2) {

		State state = tuple2.getT2().statusCode().isError() ? State.OFFLINE : State.ONLINE;
		ElasticsearchHost elasticsearchHost = new ElasticsearchHost(tuple2.getT1(), state);
		hosts.put(tuple2.getT1(), elasticsearchHost);
		return elasticsearchHost;
	}

	private Flux<Tuple2<InetSocketAddress, ClientResponse>> nodes(@Nullable State state) {

		return Flux.fromIterable(hosts()) //
				.filter(entry -> state == null || entry.getState().equals(state)) //
				.map(ElasticsearchHost::getEndpoint) //
				.flatMap(host -> {

					Mono<ClientResponse> exchange = createWebClient(host) //
							.head().uri("/").exchange().doOnError(throwable -> {

								hosts.put(host, new ElasticsearchHost(host, State.OFFLINE));
								clientProvider.getErrorListener().accept(throwable);
							});

					return Mono.just(host).zipWith(exchange);
				}) //
				.onErrorContinue((throwable, o) -> clientProvider.getErrorListener().accept(throwable));
	}

	private List<ElasticsearchHost> hosts() {

		List<ElasticsearchHost> hosts = new ArrayList<>(this.hosts.values());
		Collections.shuffle(hosts);

		return hosts;
	}
}
