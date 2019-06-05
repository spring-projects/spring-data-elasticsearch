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

import static org.assertj.core.api.Assertions.*;

import reactor.test.StepVerifier;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.ElasticsearchHost.State;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.data.elasticsearch.client.reactive.ReactiveMockClientTestsUtils.MockDelegatingElasticsearchHostProvider;
import org.springframework.data.elasticsearch.client.reactive.ReactiveMockClientTestsUtils.MockWebClientProvider.Receive;

/**
 * @author Christoph Strobl
 * @currentRead Golden Fool - Robin Hobb
 */
public class SingleNodeHostProviderUnitTests {

	static final String HOST_1 = ":9200";

	MockDelegatingElasticsearchHostProvider<SingleNodeHostProvider> mock;
	SingleNodeHostProvider provider;

	@Before
	public void setUp() {

		mock = ReactiveMockClientTestsUtils.single(HOST_1);
		provider = mock.getDelegate();
	}

	@Test // DATAES-488
	public void refreshHostStateShouldUpdateNodeStateCorrectly() {

		mock.when(HOST_1).receive(Receive::error);

		provider.clusterInfo().as(StepVerifier::create).expectNextCount(1).verifyComplete();

		assertThat(provider.getCachedHostState()).extracting(ElasticsearchHost::getState).isEqualTo(State.OFFLINE);
	}

	@Test // DATAES-488
	public void getActiveReturnsFirstActiveHost() {

		mock.when(HOST_1).receive(Receive::ok);

		provider.clusterInfo().as(StepVerifier::create).expectNextCount(1).verifyComplete();

		assertThat(provider.getCachedHostState()).extracting(ElasticsearchHost::getState).isEqualTo(State.ONLINE);
	}

	@Test // DATAES-488
	public void getActiveErrorsWhenNoActiveHostFound() {

		mock.when(HOST_1).receive(Receive::error);

		provider.getActive().as(StepVerifier::create).expectError(NoReachableHostException.class);
	}
}
