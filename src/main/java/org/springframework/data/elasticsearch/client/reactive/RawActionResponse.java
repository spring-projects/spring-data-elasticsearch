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

import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

import org.elasticsearch.action.ActionResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Extension to {@link ActionResponse} that also delegates to {@link ClientResponse}.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 * @since 3.2
 */
class RawActionResponse extends ActionResponse {

	private final ClientResponse delegate;

	private RawActionResponse(ClientResponse delegate) {
		this.delegate = delegate;
	}

	static RawActionResponse create(ClientResponse response) {
		return new RawActionResponse(response);
	}

	public HttpStatus statusCode() {
		return delegate.statusCode();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#headers()
	 */
	public ClientResponse.Headers headers() {
		return delegate.headers();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#body(org.springframework.web.reactive.function.BodyExtractor)
	 */
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		return delegate.body(extractor);
	}

	/*
	 * (non-Javadoc)
	 * until Elasticsearch 7.4 this empty implementation was available in the abstract base class
	 */
	@Override
	public void writeTo(StreamOutput out) throws IOException {
	}
}
