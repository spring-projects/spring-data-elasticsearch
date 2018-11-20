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

import reactor.core.publisher.Mono;

import java.util.Collections;

import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.ElasticsearchHost.State;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link HostProvider} for a single host.
 *
 * @author Christoph Strobl
 * @since 4.0
 */
class SingleNodeHostProvider implements HostProvider {

	private final WebClientProvider clientProvider;
	private final String baseUrl;
	private volatile ElasticsearchHost state;

	SingleNodeHostProvider(WebClientProvider clientProvider, String baseUrl) {

		this.clientProvider = clientProvider;
		this.baseUrl = baseUrl;
		this.state = new ElasticsearchHost(this.baseUrl, State.UNKNOWN);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.HostProvider#clusterInfo()
	 */
	@Override
	public Mono<ClusterInformation> clusterInfo() {

		return createWebClient(baseUrl) //
				.head().uri("/").exchange() //
				.flatMap(it -> {

					if (it.statusCode().isError()) {
						state = ElasticsearchHost.offline(baseUrl);
					} else {
						state = ElasticsearchHost.online(baseUrl);
					}
					return Mono.just(state);
				}).onErrorResume(throwable -> {

					state = ElasticsearchHost.offline(baseUrl);
					clientProvider.getErrorListener().accept(throwable);
					return Mono.just(state);
				}) //
				.flatMap(it -> Mono.just(new ClusterInformation(Collections.singleton(it))));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.HostProvider#createWebClient(java.lang.String)
	 */
	@Override
	public WebClient createWebClient(String baseUrl) {
		return this.clientProvider.get(baseUrl);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.client.reactive.HostProvider#lookupActiveHost(org.springframework.data.elasticsearch.client.reactive.HostProvider.VerificationMode)
	 */
	@Override
	public Mono<String> lookupActiveHost(VerificationMode verificationMode) {

		if (VerificationMode.LAZY.equals(verificationMode) && state.isOnline()) {
			return Mono.just(baseUrl);
		}

		return clusterInfo().flatMap(it -> {

			ElasticsearchHost host = it.getNodes().iterator().next();
			if (host.isOnline()) {
				return Mono.just(host.getHost());
			}

			return Mono.error(() -> new NoReachableHostException(Collections.singleton(host)));
		});
	}

	ElasticsearchHost getCachedHostState() {
		return state;
	}

}
