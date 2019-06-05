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

import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.util.Assert;

/**
 * Utility class for common access to reactive Elasticsearch clients. {@link ReactiveRestClients} consolidates set up
 * routines for the various drivers into a single place.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
public final class ReactiveRestClients {

	private ReactiveRestClients() {}

	/**
	 * Start here to create a new client tailored to your needs.
	 *
	 * @return new instance of {@link ReactiveElasticsearchClient}.
	 */
	public static ReactiveElasticsearchClient create(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

		return DefaultReactiveElasticsearchClient.create(clientConfiguration);
	}
}
