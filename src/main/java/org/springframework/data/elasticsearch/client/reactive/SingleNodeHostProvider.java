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

import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Collections;

import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.ElasticsearchHost.State;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link HostProvider} for a single host.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
class SingleNodeHostProvider implements HostProvider {

	private final WebClientProvider clientProvider;
	private final InetSocketAddress endpoint;
	private volatile ElasticsearchHost state;

	SingleNodeHostProvider(WebClientProvider clientProvider, InetSocketAddress endpoint) {

		this.clientProvider = clientProvider;
		this.endpoint = endpoint;
		this.state = new ElasticsearchHost(this.endpoint, State.UNKNOWN);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.HostProvider#clusterInfo()
	 */
	@Override
	public Mono<ClusterInformation> clusterInfo() {

		return createWebClient(endpoint) //
				.head().uri("/").exchange() //
				.flatMap(it -> {

					if (it.statusCode().isError()) {
						state = ElasticsearchHost.offline(endpoint);
					} else {
						state = ElasticsearchHost.online(endpoint);
					}
					return Mono.just(state);
				}).onErrorResume(throwable -> {

					state = ElasticsearchHost.offline(endpoint);
					clientProvider.getErrorListener().accept(throwable);
					return Mono.just(state);
				}) //
				.flatMap(it -> Mono.just(new ClusterInformation(Collections.singleton(it))));
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

		if (Verification.LAZY.equals(verification) && state.isOnline()) {
			return Mono.just(endpoint);
		}

		return clusterInfo().flatMap(it -> {

			ElasticsearchHost host = it.getNodes().iterator().next();
			if (host.isOnline()) {
				return Mono.just(host.getEndpoint());
			}

			return Mono.error(() -> new NoReachableHostException(Collections.singleton(host)));
		});
	}

	ElasticsearchHost getCachedHostState() {
		return state;
	}
}
