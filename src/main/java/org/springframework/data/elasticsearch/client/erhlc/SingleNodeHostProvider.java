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
 * @author Peter-Josef Meisch
 * @since 3.2
 * @deprecated since 5.0
 */
@Deprecated
class SingleNodeHostProvider implements HostProvider<SingleNodeHostProvider> {

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
	 * @see org.springframework.data.elasticsearch.client.erhlc.HostProvider#clusterInfo()
	 */
	@Override
	public Mono<ClusterInformation> clusterInfo() {

		return createWebClient(endpoint) //
				.head().uri("/") //
				.exchangeToMono(it -> {
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
				}).map(elasticsearchHost -> new ClusterInformation(Collections.singleton(elasticsearchHost)));
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

		if (Verification.LAZY.equals(verification) && state.isOnline()) {
			return Mono.just(endpoint);
		}

		return clusterInfo().handle((information, sink) -> {

			ElasticsearchHost host = information.getNodes().iterator().next();
			if (host.isOnline()) {

				sink.next(host.getEndpoint());
				return;
			}

			sink.error(new NoReachableHostException(Collections.singleton(host)));
		});
	}

	ElasticsearchHost getCachedHostState() {
		return state;
	}
}
