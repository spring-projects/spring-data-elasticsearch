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

import co.elastic.clients.ApiClient;
import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.Transport;

import java.io.IOException;

import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;

/**
 * base class for a template that uses one of the {@link co.elastic.clients.elasticsearch.ElasticsearchClient}'s child
 * clients like {@link ElasticsearchClusterClient} or
 * {@link co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public abstract class ChildTemplate<T extends Transport, CLIENT extends ApiClient<T, CLIENT>> {

	protected final CLIENT client;
	protected final RequestConverter requestConverter;
	protected final ResponseConverter responseConverter;
	protected final ElasticsearchExceptionTranslator exceptionTranslator;

	public ChildTemplate(CLIENT client, ElasticsearchConverter elasticsearchConverter) {
		this.client = client;
		JsonpMapper jsonpMapper = client._transport().jsonpMapper();
		requestConverter = new RequestConverter(elasticsearchConverter, jsonpMapper);
		responseConverter = new ResponseConverter(jsonpMapper);
		exceptionTranslator = new ElasticsearchExceptionTranslator(jsonpMapper);
	}

	/**
	 * Callback interface to be used with {@link #execute(ClientCallback)} for operating directly on the client.
	 */
	@FunctionalInterface
	public interface ClientCallback<CLIENT, RESULT> {
		RESULT doWithClient(CLIENT client) throws IOException;
	}

	/**
	 * Execute a callback with the client and provide exception translation.
	 *
	 * @param callback the callback to execute, must not be {@literal null}
	 * @param <RESULT> the type returned from the callback
	 * @return the callback result
	 */
	public <RESULT> RESULT execute(ClientCallback<CLIENT, RESULT> callback) {

		Assert.notNull(callback, "callback must not be null");

		try {
			return callback.doWithClient(client);
		} catch (IOException | RuntimeException e) {
			throw exceptionTranslator.translateException(e);
		}
	}
}
