/*
 * Copyright 2018-2023 the original author or authors.
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
import reactor.util.function.Tuple2;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * @author Peter-Josef Meisch
 * @since 3.2
 * @deprecated since 5.0
 */
@Deprecated
class MultiNodeHostProvider implements HostProvider<MultiNodeHostProvider> {

	private final static Log LOGGER = LogFactory.getLog(MultiNodeHostProvider.class);

	private final WebClientProvider clientProvider;
	private final Map<InetSocketAddress, ElasticsearchHost> hosts;

	MultiNodeHostProvider(WebClientProvider clientProvider, InetSocketAddress... endpoints) {

		this.clientProvider = clientProvider;
		this.hosts = new ConcurrentHashMap<>();
		for (InetSocketAddress endpoint : endpoints) {
			this.hosts.put(endpoint, new ElasticsearchHost(endpoint, State.UNKNOWN));
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("initialized with " + hosts);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.erhlc.HostProvider#clusterInfo()
	 */
	@Override
	public Mono<ClusterInformation> clusterInfo() {
		return checkNodes(null).map(this::updateNodeState).buffer(hosts.size())
				.then(Mono.just(new ClusterInformation(new LinkedHashSet<>(this.hosts.values()))));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.erhlc.HostProvider#createWebClient(java.net.InetSocketAddress)
	 */
	@Override
	public WebClient createWebClient(InetSocketAddress endpoint) {
		return this.clientProvider.get(endpoint);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.erhlc.HostProvider#lookupActiveHost(org.springframework.data.elasticsearch.client.erhlc.HostProvider.Verification)
	 */
	@Override
	public Mono<InetSocketAddress> lookupActiveHost(Verification verification) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("lookupActiveHost " + verification + " from " + hosts());
		}

		if (Verification.LAZY.equals(verification)) {
			for (ElasticsearchHost entry : hosts()) {
				if (entry.isOnline()) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("lookupActiveHost returning " + entry);
					}
					return Mono.just(entry.getEndpoint());
				}
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("no online host found with LAZY");
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("searching for active host");
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
		return findActiveForState(State.ONLINE);
	}

	private Mono<InetSocketAddress> findActiveHostInUnresolved() {
		return findActiveForState(State.UNKNOWN);
	}

	private Mono<InetSocketAddress> findActiveHostInDead() {
		return findActiveForState(State.OFFLINE);
	}

	private Mono<InetSocketAddress> findActiveForState(State state) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("findActiveForState state " + state + ", current hosts: " + hosts);
		}

		return checkNodes(state) //
				.map(this::updateNodeState) //
				.filter(ElasticsearchHost::isOnline) //
				.map(elasticsearchHost -> {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("findActiveForState returning host " + elasticsearchHost);
					}
					return elasticsearchHost;
				}).map(ElasticsearchHost::getEndpoint) //
				.takeLast(1) //
				.next();
	}

	private ElasticsearchHost updateNodeState(Tuple2<InetSocketAddress, State> tuple2) {

		State state = tuple2.getT2();
		ElasticsearchHost elasticsearchHost = new ElasticsearchHost(tuple2.getT1(), state);
		hosts.put(tuple2.getT1(), elasticsearchHost);
		return elasticsearchHost;
	}

	private Flux<Tuple2<InetSocketAddress, State>> checkNodes(@Nullable State state) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("checkNodes() with state " + state);
		}

		return Flux.fromIterable(hosts()) //
				.filter(entry -> state == null || entry.getState().equals(state)) //
				.map(ElasticsearchHost::getEndpoint) //
				.concatMap(host -> {

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("checking host " + host);
					}

					Mono<ClientResponse> clientResponseMono = createWebClient(host) //
							.head().uri("/") //
							.exchangeToMono(Mono::just) //
							.timeout(Duration.ofSeconds(1)) //
							.doOnError(throwable -> {
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace("error checking host " + host + ", " + throwable.getMessage());
								}
								hosts.put(host, new ElasticsearchHost(host, State.OFFLINE));
								clientProvider.getErrorListener().accept(throwable);
							});

					return Mono.just(host) //
							.zipWith(clientResponseMono.flatMap(it -> it.releaseBody() //
									.thenReturn(it.statusCode().isError() ? State.OFFLINE : State.ONLINE)));
				}) //
				.map(tuple -> {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("check result " + tuple);
					}
					return tuple;
				}).onErrorContinue((throwable, o) -> clientProvider.getErrorListener().accept(throwable));
	}

	private List<ElasticsearchHost> hosts() {

		List<ElasticsearchHost> hosts = new ArrayList<>(this.hosts.values());
		Collections.shuffle(hosts);

		return hosts;
	}
}
