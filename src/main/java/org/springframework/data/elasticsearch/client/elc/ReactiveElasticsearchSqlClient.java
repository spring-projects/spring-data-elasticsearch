/*
 * Copyright 2024-2025 the original author or authors.
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
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.sql.QueryRequest;
import co.elastic.clients.elasticsearch.sql.QueryResponse;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.util.ObjectBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

/**
 * Reactive version of {@link co.elastic.clients.elasticsearch.sql.ElasticsearchSqlClient}.
 *
 * @author Aouichaoui Youssef
 * @since 5.4
 */
public class ReactiveElasticsearchSqlClient extends ApiClient<ElasticsearchTransport, ReactiveElasticsearchSqlClient> {
	public ReactiveElasticsearchSqlClient(ElasticsearchTransport transport, @Nullable TransportOptions transportOptions) {
		super(transport, transportOptions);
	}

	@Override
	public ReactiveElasticsearchSqlClient withTransportOptions(@Nullable TransportOptions transportOptions) {
		return new ReactiveElasticsearchSqlClient(transport, transportOptions);
	}

	/**
	 * Executes a SQL request
	 *
	 * @param fn a function that initializes a builder to create the {@link QueryRequest}.
	 */
	public final Mono<QueryResponse> query(Function<QueryRequest.Builder, ObjectBuilder<QueryRequest>> fn)
			throws IOException, ElasticsearchException {
		return query(fn.apply(new QueryRequest.Builder()).build());
	}

	/**
	 * Executes a SQL request.
	 */
	public Mono<QueryResponse> query(QueryRequest query) {
		return Mono.fromFuture(transport.performRequestAsync(query, QueryRequest._ENDPOINT, transportOptions));
	}

	/**
	 * Executes a SQL request.
	 */
	public Mono<QueryResponse> query() {
		return Mono.fromFuture(
				transport.performRequestAsync(new QueryRequest.Builder().build(), QueryRequest._ENDPOINT, transportOptions));
	}
}
