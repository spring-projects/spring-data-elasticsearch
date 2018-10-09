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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.elasticsearch.client.ClientProvider.HostState;
import org.springframework.data.elasticsearch.client.ClientProvider.HostState.State;
import org.springframework.data.elasticsearch.client.ClientProvider.VerificationMode;
import org.springframework.data.elasticsearch.client.ReactiveMockClientTestsUtils.MockDelegatingElasticsearchClientProvider;
import org.springframework.data.elasticsearch.client.ReactiveMockClientTestsUtils.WebClientProvider.Receive;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * @author Christoph Strobl
 * @currentRead Golden Fool - Robin Hobb
 */
public class MultiClientProviderUnitTests {

	static final String HOST_1 = ":9200";
	static final String HOST_2 = ":9201";
	static final String HOST_3 = ":9202";

	MockDelegatingElasticsearchClientProvider<MultiNodeClientProvider> mock;
	MultiNodeClientProvider provider;

	@Before
	public void setUp() {

		mock = ReactiveMockClientTestsUtils.multi(HOST_1, HOST_2, HOST_3);
		provider = mock.getDelegate();
	}

	@Test // DATAES-488
	public void refreshHostStateShouldUpdateNodeStateCorrectly() {

		mock.when(HOST_1).receive(Receive::error);
		mock.when(HOST_2).receive(Receive::ok);
		mock.when(HOST_3).receive(Receive::ok);

		provider.refresh().as(StepVerifier::create).verifyComplete();

		assertThat(provider.status()).extracting(HostState::getState).containsExactly(State.OFFLINE, State.ONLINE,
				State.ONLINE);
	}

	@Test // DATAES-488
	public void getActiveReturnsFirstActiveHost() {

		mock.when(HOST_1).receive(Receive::error);
		mock.when(HOST_2).receive(Receive::ok);
		mock.when(HOST_3).receive(Receive::error);

		provider.getActive().as(StepVerifier::create).expectNext(mock.client(HOST_2)).verifyComplete();
	}

	@Test // DATAES-488
	public void getActiveErrorsWhenNoActiveHostFound() {

		mock.when(HOST_1).receive(Receive::error);
		mock.when(HOST_2).receive(Receive::error);
		mock.when(HOST_3).receive(Receive::error);

		provider.getActive().as(StepVerifier::create).expectError(IllegalStateException.class);
	}

	@Test // DATAES-488
	public void lazyModeDoesNotResolveHostsTwice() {

		mock.when(HOST_1).receive(Receive::error);
		mock.when(HOST_2).receive(Receive::ok);
		mock.when(HOST_3).receive(Receive::error);

		provider.refresh().as(StepVerifier::create).verifyComplete();

		provider.getActive(VerificationMode.LAZY).as(StepVerifier::create).expectNext(mock.client(HOST_2)).verifyComplete();

		verify(mock.client(":9201")).head();
	}

	@Test // DATAES-488
	public void alwaysModeDoesNotResolveHostsTwice() {

		mock.when(HOST_1).receive(Receive::error);
		mock.when(HOST_2).receive(Receive::ok);
		mock.when(HOST_3).receive(Receive::error);

		provider.refresh().as(StepVerifier::create).verifyComplete();

		provider.getActive(VerificationMode.ALWAYS).as(StepVerifier::create).expectNext(mock.client(HOST_2))
				.verifyComplete();

		verify(mock.client(HOST_2), times(2)).head();
	}

	@Test // DATAES-488
	public void triesDeadHostsIfNoActiveFound() {

		mock.when(HOST_1).receive(Receive::error);
		mock.when(HOST_2).get(requestHeadersUriSpec -> {

			ClientResponse response1 = mock(ClientResponse.class);
			Receive.error(response1);

			ClientResponse response2 = mock(ClientResponse.class);
			Receive.ok(response2);

			when(requestHeadersUriSpec.exchange()).thenReturn(Mono.just(response1), Mono.just(response2));
		});

		mock.when(HOST_3).receive(Receive::error);

		provider.refresh().as(StepVerifier::create).verifyComplete();
		assertThat(provider.status()).extracting(HostState::getState).containsExactly(State.OFFLINE, State.OFFLINE,
				State.OFFLINE);

		provider.getActive().as(StepVerifier::create).expectNext(mock.client(HOST_2)).verifyComplete();

		verify(mock.client(HOST_2), times(2)).head();
	}
}
