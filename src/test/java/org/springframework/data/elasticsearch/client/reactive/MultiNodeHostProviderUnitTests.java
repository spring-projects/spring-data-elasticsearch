/*
 * Copyright 2018-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.invocation.InvocationOnMock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.ElasticsearchHost.State;
import org.springframework.data.elasticsearch.client.reactive.HostProvider.Verification;
import org.springframework.data.elasticsearch.client.reactive.ReactiveMockClientTestsUtils.MockDelegatingElasticsearchHostProvider;
import org.springframework.data.elasticsearch.client.reactive.ReactiveMockClientTestsUtils.MockWebClientProvider.Receive;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.function.Function;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
public class MultiNodeHostProviderUnitTests {

	static final String HOST_1 = ":9200";
	static final String HOST_2 = ":9201";
	static final String HOST_3 = ":9202";

	MockDelegatingElasticsearchHostProvider<MultiNodeHostProvider> multiNodeDelegatingHostProvider;
	MultiNodeHostProvider delegateHostProvider;

	@BeforeEach
	public void setUp() {

		multiNodeDelegatingHostProvider = ReactiveMockClientTestsUtils.multi(HOST_1, HOST_2, HOST_3);
		delegateHostProvider = multiNodeDelegatingHostProvider.getDelegate();
	}

	@Test // DATAES-488
	public void refreshHostStateShouldUpdateNodeStateCorrectly() {

		multiNodeDelegatingHostProvider.when(HOST_1).receive(Receive::error);
		multiNodeDelegatingHostProvider.when(HOST_2).receive(Receive::ok);
		multiNodeDelegatingHostProvider.when(HOST_3).receive(Receive::ok);

		delegateHostProvider.clusterInfo().as(StepVerifier::create).expectNextCount(1).verifyComplete();

		assertThat(delegateHostProvider.getCachedHostState()).extracting(ElasticsearchHost::getState)
				.containsExactly(State.OFFLINE, State.ONLINE, State.ONLINE);
	}

	@Test // DATAES-488
	public void getActiveReturnsFirstActiveHost() {

		multiNodeDelegatingHostProvider.when(HOST_1).receive(Receive::error);
		multiNodeDelegatingHostProvider.when(HOST_2).receive(Receive::ok);
		multiNodeDelegatingHostProvider.when(HOST_3).receive(Receive::error);

		delegateHostProvider.getActive().as(StepVerifier::create).expectNext(multiNodeDelegatingHostProvider.client(HOST_2))
				.verifyComplete();
	}

	@Test // DATAES-488
	public void getActiveErrorsWhenNoActiveHostFound() {

		multiNodeDelegatingHostProvider.when(HOST_1).receive(Receive::error);
		multiNodeDelegatingHostProvider.when(HOST_2).receive(Receive::error);
		multiNodeDelegatingHostProvider.when(HOST_3).receive(Receive::error);

		delegateHostProvider.getActive().as(StepVerifier::create).expectError(IllegalStateException.class);
	}

	@Test // DATAES-488
	public void lazyModeDoesNotResolveHostsTwice() {

		multiNodeDelegatingHostProvider.when(HOST_1).receive(Receive::error);
		multiNodeDelegatingHostProvider.when(HOST_2).receive(Receive::ok);
		multiNodeDelegatingHostProvider.when(HOST_3).receive(Receive::error);

		delegateHostProvider.clusterInfo().as(StepVerifier::create).expectNextCount(1).verifyComplete();

		delegateHostProvider.getActive(Verification.LAZY).as(StepVerifier::create)
				.expectNext(multiNodeDelegatingHostProvider.client(HOST_2)).verifyComplete();

		verify(multiNodeDelegatingHostProvider.client(":9201")).head();
	}

	@Test // DATAES-488
	public void alwaysModeDoesNotResolveHostsTwice() {

		multiNodeDelegatingHostProvider.when(HOST_1).receive(Receive::error);
		multiNodeDelegatingHostProvider.when(HOST_2).receive(Receive::ok);
		multiNodeDelegatingHostProvider.when(HOST_3).receive(Receive::error);

		delegateHostProvider.clusterInfo().as(StepVerifier::create).expectNextCount(1).verifyComplete();

		delegateHostProvider.getActive(Verification.ACTIVE).as(StepVerifier::create)
				.expectNext(multiNodeDelegatingHostProvider.client(HOST_2)).verifyComplete();

		verify(multiNodeDelegatingHostProvider.client(HOST_2), times(2)).head();
	}

	@Test // DATAES-488
	public void triesDeadHostsIfNoActiveFound() {

		multiNodeDelegatingHostProvider.when(HOST_1).receive(Receive::error);
		multiNodeDelegatingHostProvider.when(HOST_2).get(requestHeadersUriSpec -> {

			ClientResponse response1 = mock(ClientResponse.class);
			when(response1.releaseBody()).thenReturn(Mono.empty());
			Receive.error(response1);

			ClientResponse response2 = mock(ClientResponse.class);
			when(response2.releaseBody()).thenReturn(Mono.empty());
			Receive.ok(response2);

			when(requestHeadersUriSpec.exchangeToMono(any()))//
					.thenAnswer(invocation -> getAnswer(invocation, response1)) //
					.thenAnswer(invocation -> getAnswer(invocation, response2));
		});

		multiNodeDelegatingHostProvider.when(HOST_3).receive(Receive::error);

		delegateHostProvider.clusterInfo().as(StepVerifier::create).expectNextCount(1).verifyComplete();
		assertThat(delegateHostProvider.getCachedHostState()).extracting(ElasticsearchHost::getState)
				.containsExactly(State.OFFLINE, State.OFFLINE, State.OFFLINE);

		delegateHostProvider.getActive().as(StepVerifier::create).expectNext(multiNodeDelegatingHostProvider.client(HOST_2))
				.verifyComplete();

		verify(multiNodeDelegatingHostProvider.client(HOST_2), times(2)).head();
	}

	private Mono<?> getAnswer(InvocationOnMock invocation, ClientResponse response) {
		final Function<ClientResponse, ? extends Mono<?>> responseHandler = invocation.getArgument(0);

		if (responseHandler != null) {
			return responseHandler.apply(response);
		}
		return Mono.empty();
	}
}
