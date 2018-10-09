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

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.springframework.data.elasticsearch.client.ClientProvider.HostState.State;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
@RequiredArgsConstructor
class SingleNodeClientProvider implements ClientProvider {

	private final HttpHeaders headers;
	private final Consumer<Throwable> errorListener;
	private final String host;

	@Override
	public Mono<String> lookupActiveHost(VerificationMode verificationMode) {
		return Mono.just(host);
	}

	@Override
	public Mono<WebClient> getActive(VerificationMode verificationMode) {
		return getActive(verificationMode, headers);
	}

	@Override
	public Collection<HostState> status() {
		return Collections.singleton(new HostState(host, State.ONLINE));
	}

	@Override
	public ClientProvider withDefaultHeaders(HttpHeaders headers) {
		return new SingleNodeClientProvider(headers, errorListener, host);
	}

	@Override
	public ClientProvider withErrorListener(Consumer<Throwable> errorListener) {
		return new SingleNodeClientProvider(headers, errorListener, host);
	}
}
