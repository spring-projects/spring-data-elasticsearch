/*
 * Copyright 2018-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
public class DefaultWebClientProviderUnitTests {

	@Test // DATAES-488
	public void shouldCacheClients() {

		DefaultWebClientProvider provider = new DefaultWebClientProvider("http", null);

		WebClient client1 = provider.get(InetSocketAddress.createUnresolved("localhost", 9200));
		WebClient shouldBeCachedInstanceOfClient1 = provider.get(InetSocketAddress.createUnresolved("localhost", 9200));

		WebClient notClient1ButAnotherInstance = provider.get(InetSocketAddress.createUnresolved("127.0.0.1", 9200));

		assertThat(shouldBeCachedInstanceOfClient1).isSameAs(client1);
		assertThat(notClient1ButAnotherInstance).isNotSameAs(client1);
	}

	@Test // DATAES-719
	void shouldCallWebClientConfigurer() {
		AtomicReference<Boolean> configurerCalled = new AtomicReference<>(false);
		Function<WebClient, WebClient> configurer = webClient -> {
			configurerCalled.set(true);
			return webClient;
		};
		WebClientProvider provider = new DefaultWebClientProvider("http", null).withWebClientConfigurer(configurer);

		provider.get(InetSocketAddress.createUnresolved("localhost", 9200));

		assertThat(configurerCalled).hasValue(true);
	}
}
