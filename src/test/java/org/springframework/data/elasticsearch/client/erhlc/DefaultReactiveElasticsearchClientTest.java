/*
 * Copyright 2020-2023 the original author or authors.
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
import static org.elasticsearch.search.internal.SearchContext.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.RestStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriBuilder;

/**
 * @author Peter-Josef Meisch
 */
@ExtendWith(MockitoExtension.class)
class DefaultReactiveElasticsearchClientTest {

	@Mock private HostProvider hostProvider;

	@Mock private Function<SearchRequest, Request> searchRequestConverter;
	@Spy private RequestCreator requestCreator;

	@Mock private WebClient webClient;

	@Test
	void shouldSetAppropriateRequestParametersOnCount() {

		when(requestCreator.search()).thenReturn(searchRequestConverter);
		SearchRequest searchRequest = new SearchRequest("someindex") //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		ReactiveElasticsearchClient client = new DefaultReactiveElasticsearchClient(hostProvider, requestCreator) {
			@Override
			public Mono<ResponseSpec> execute(ReactiveElasticsearchClientCallback callback) {
				return Mono.empty();
			}
		};

		client.count(searchRequest).as(StepVerifier::create).verifyComplete();

		ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
		verify(searchRequestConverter).apply(captor.capture());
		SearchSourceBuilder source = captor.getValue().source();
		assertThat(source.size()).isEqualTo(0);
		assertThat(source.trackTotalHitsUpTo()).isEqualTo(TRACK_TOTAL_HITS_ACCURATE);
		assertThat(source.fetchSource()).isEqualTo(FetchSourceContext.DO_NOT_FETCH_SOURCE);
	}

	@Test // #1712
	@DisplayName("should throw RestStatusException on server 5xx with empty body")
	void shouldThrowRestStatusExceptionOnServer5xxWithEmptyBody() {

		when(hostProvider.getActive(any())).thenReturn(Mono.just(webClient));
		WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		when(requestBodyUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.attribute(any(), any())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
		when(webClient.method(any())).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.exchangeToMono(any())).thenAnswer(invocationOnMock -> {
			Function<ClientResponse, ? extends Mono<?>> responseHandler = invocationOnMock.getArgument(0);
			ClientResponse clientResponse = mock(ClientResponse.class);
			when(clientResponse.statusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
			ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
			when(headers.contentType()).thenReturn(Optional.empty());
			when(clientResponse.headers()).thenReturn(headers);
			when(clientResponse.body(any())).thenReturn(Mono.empty());
			return responseHandler.apply(clientResponse);
		});

		ReactiveElasticsearchClient client = new DefaultReactiveElasticsearchClient(hostProvider, requestCreator);

		client.get(new GetRequest("42")) //
				.as(StepVerifier::create) //
				.expectError(RestStatusException.class) //
				.verify(); //
	}
}
