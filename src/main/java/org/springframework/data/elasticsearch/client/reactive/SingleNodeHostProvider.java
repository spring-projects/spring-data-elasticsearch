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
import java.util.function.Consumer;

import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.ElasticsearchHost.State;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.http.HttpHeaders;

/**
 * @author Christoph Strobl
 * @since 4.0
 */
class SingleNodeHostProvider implements HostProvider {

	private final HttpHeaders headers;
	private final Consumer<Throwable> errorListener;
	private final String hostname;
	private volatile ElasticsearchHost state;

	SingleNodeHostProvider(HttpHeaders headers, Consumer<Throwable> errorListener, String host) {

		this.headers = headers;
		this.errorListener = errorListener;
		this.hostname = host;
		this.state = new ElasticsearchHost(hostname, State.UNKNOWN);
	}

	@Override
	public Mono<ClusterInformation> clusterInfo() {

		return createWebClient(hostname, headers) //
				.head().uri("/").exchange() //
				.flatMap(it -> {

					if(it.statusCode().isError()) {
						state = ElasticsearchHost.offline(hostname);
					} else {
						state = ElasticsearchHost.online(hostname);
					}
					return Mono.just(state);
				}).onErrorResume(throwable -> {

					state = ElasticsearchHost.offline(hostname);
					errorListener.accept(throwable);
					return Mono.just(state);
				}) //
				.flatMap(it -> Mono.just(new ClusterInformation(Collections.singleton(it))));
	}

	@Override
	public Mono<String> lookupActiveHost(VerificationMode verificationMode) {

		if (VerificationMode.LAZY.equals(verificationMode) && state.isOnline()) {
			return Mono.just(hostname);
		}

		return clusterInfo().flatMap(it -> {

			ElasticsearchHost host = it.getNodes().iterator().next();
			if (host.isOnline()) {
				return Mono.just(host.getHost());
			}

			return Mono.error(() -> new NoReachableHostException(Collections.singleton(host)));
		});
	}

	@Override
	public HttpHeaders getDefaultHeaders() {
		return this.headers;
	}

	@Override
	public HostProvider withDefaultHeaders(HttpHeaders headers) {
		return new SingleNodeHostProvider(headers, errorListener, hostname);
	}

	@Override
	public HostProvider withErrorListener(Consumer<Throwable> errorListener) {
		return new SingleNodeHostProvider(headers, errorListener, hostname);
	}

	ElasticsearchHost getCachedHostState() {
		return state;
	}

}
