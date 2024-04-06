/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;

import org.elasticsearch.client.RestClient;
import org.springframework.util.Assert;

/**
 * Extension of the {@link ElasticsearchClient} class that implements {@link AutoCloseable}. As the underlying
 * {@link RestClient} must be closed properly this is handled in the {@link #close()} method.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class AutoCloseableElasticsearchClient extends ElasticsearchClient implements AutoCloseable {

	public AutoCloseableElasticsearchClient(ElasticsearchTransport transport) {
		super(transport);
		Assert.notNull(transport, "transport must not be null");
	}

	@Override
	public void close() throws Exception {
		transport.close();
	}
}
