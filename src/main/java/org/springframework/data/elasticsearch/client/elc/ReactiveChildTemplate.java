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
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.Transport;
import reactor.core.publisher.Flux;

import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;

/**
 * base class for a reactive template that uses on of the {@link ReactiveElasticsearchClient}'s child clients.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ReactiveChildTemplate<T extends Transport, CLIENT extends ApiClient<T, CLIENT>> {
	protected final CLIENT client;
	protected final ElasticsearchConverter elasticsearchConverter;
	protected final RequestConverter requestConverter;
	protected final ResponseConverter responseConverter;
	protected final ElasticsearchExceptionTranslator exceptionTranslator;

	public ReactiveChildTemplate(CLIENT client, ElasticsearchConverter elasticsearchConverter) {
		this.client = client;
		this.elasticsearchConverter = elasticsearchConverter;
		JsonpMapper jsonpMapper = client._transport().jsonpMapper();
		requestConverter = new RequestConverter(elasticsearchConverter, jsonpMapper);
		responseConverter = new ResponseConverter(jsonpMapper);
		exceptionTranslator = new ElasticsearchExceptionTranslator(jsonpMapper);
	}

	/**
	 * Callback interface to be used with {@link #execute(ClientCallback)} for operating directly on the client.
	 */
	@FunctionalInterface
	public interface ClientCallback<CLIENT, RESULT extends Publisher<?>> {
		RESULT doWithClient(CLIENT client);
	}

	/**
	 * Execute a callback with the client and provide exception translation.
	 *
	 * @param callback the callback to execute, must not be {@literal null}
	 * @param <RESULT> the type returned from the callback
	 * @return the callback result
	 */
	public <RESULT> Publisher<RESULT> execute(ClientCallback<CLIENT, Publisher<RESULT>> callback) {

		Assert.notNull(callback, "callback must not be null");

		return Flux.defer(() -> callback.doWithClient(client)).onErrorMap(exceptionTranslator::translateException);
	}

}
