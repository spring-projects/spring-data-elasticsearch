/*
 * Copyright 2020 the original author or authors.
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
import static org.elasticsearch.search.internal.SearchContext.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

/**
 * @author Peter-Josef Meisch
 */
@ExtendWith(MockitoExtension.class)
class DefaultReactiveElasticsearchClientTest {

	@Mock private HostProvider hostProvider;

	@Mock private Function<SearchRequest, Request> searchRequestConverter;

	private DefaultReactiveElasticsearchClient client;

	@BeforeEach
	void setUp() {
		client = new DefaultReactiveElasticsearchClient(hostProvider, new RequestCreator() {
			@Override
			public Function<SearchRequest, Request> search() {
				return searchRequestConverter;
			}
		}) {
			@Override
			public Mono<ResponseSpec> execute(ReactiveElasticsearchClientCallback callback) {
				return Mono.empty();
			}
		};
	}

	@Test
	void shouldSetAppropriateRequestParametersOnCount() {

		SearchRequest searchRequest = new SearchRequest("someindex") //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.count(searchRequest).as(StepVerifier::create).verifyComplete();

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(searchRequestConverter).apply(captor.capture());
		SearchSourceBuilder source = captor.getValue().source();
		assertThat(source.size()).isEqualTo(0);
		assertThat(source.trackTotalHitsUpTo()).isEqualTo(TRACK_TOTAL_HITS_ACCURATE);
		assertThat(source.fetchSource()).isEqualTo(FetchSourceContext.DO_NOT_FETCH_SOURCE);
	}
}
