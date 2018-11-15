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

import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.springframework.data.elasticsearch.client.ClientProvider.HostState.State;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Christoph Strobl
 * @since 4.0
 */
class SingleNodeClientProvider implements ClientProvider {

	private final HttpHeaders headers;
	private final Consumer<Throwable> errorListener;
	private final String hostname;
	private volatile HostState state;

	SingleNodeClientProvider(HttpHeaders headers, Consumer<Throwable> errorListener, String host) {

		this.headers = headers;
		this.errorListener = errorListener;
		this.hostname = host;
		this.state = new HostState(hostname, State.UNKNOWN);
	}

	public Mono<Void> refresh() {

		return createWebClient(hostname, headers) //
				.head().uri("/").exchange() //
				.flatMap(it -> {
					state = HostState.online(hostname);
					return Mono.<Void> empty();
				}).onErrorResume(throwable -> {

					state = HostState.offline(hostname);
					errorListener.accept(throwable);
					return Mono.empty();
				});
	}

	@Override
	public Mono<String> lookupActiveHost(VerificationMode verificationMode) {
		return Mono.just(hostname);
	}

	@Override
	public Mono<WebClient> getActive(VerificationMode verificationMode) {
		return getActive(verificationMode, headers);
	}

	@Override
	public Collection<HostState> status() {
		return Collections.singleton(state);
	}

	@Override
	public ClientProvider withDefaultHeaders(HttpHeaders headers) {
		return new SingleNodeClientProvider(headers, errorListener, hostname);
	}

	@Override
	public ClientProvider withErrorListener(Consumer<Throwable> errorListener) {
		return new SingleNodeClientProvider(headers, errorListener, hostname);
	}
}
