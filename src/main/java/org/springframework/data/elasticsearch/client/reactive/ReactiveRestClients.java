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
package org.springframework.data.elasticsearch.client.reactive;

import java.util.function.Function;

import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Utility class for common access to reactive Elasticsearch clients. {@link ReactiveRestClients} consolidates set up
 * routines for the various drivers into a single place.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Roman Puchkovskiy
 * @since 3.2
 */
public final class ReactiveRestClients {

	private ReactiveRestClients() {}

	/**
	 * Start here to create a new client tailored to your needs.
	 *
	 * @param clientConfiguration client configuration to use for building {@link ReactiveElasticsearchClient}; must not
	 *          be {@literal null}.
	 * @return new instance of {@link ReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

		return DefaultReactiveElasticsearchClient.create(clientConfiguration);
	}

	/**
	 * Start here to create a new client tailored to your needs.
	 *
	 * @param clientConfiguration client configuration to use for building {@link ReactiveElasticsearchClient}; must not
	 *          be {@literal null}.
	 * @param requestCreator request creator to use in the client; must not be {@literal null}.
	 * @return new instance of {@link ReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(ClientConfiguration clientConfiguration,
			RequestCreator requestCreator) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");
		Assert.notNull(requestCreator, "RequestCreator must not be null!");

		return DefaultReactiveElasticsearchClient.create(clientConfiguration, requestCreator);
	}

	/**
	 * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
	 * the ReactiveElasticsearchClient with a {@link WebClient}
	 *
	 * @since 4.3
	 */
	public interface WebClientConfigurationCallback extends ClientConfiguration.ClientConfigurationCallback<WebClient> {

		static WebClientConfigurationCallback from(Function<WebClient, WebClient> webClientCallback) {

			Assert.notNull(webClientCallback, "webClientCallback must not be null");

			// noinspection NullableProblems
			return webClientCallback::apply;
		}
	}
}
