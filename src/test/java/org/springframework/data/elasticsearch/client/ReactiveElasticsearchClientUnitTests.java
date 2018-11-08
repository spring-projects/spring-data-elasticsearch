/*
 * Copyright 2018. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.elasticsearch.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import reactor.test.StepVerifier;

import java.util.Map;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.client.ReactiveMockClientTestsUtils.MockDelegatingElasticsearchClientProvider;
import org.springframework.data.elasticsearch.client.ReactiveMockClientTestsUtils.WebClientProvider.Receive;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * @author Christoph Strobl
 * @currentRead Golden Fool - Robin Hobb
 */
public class ReactiveElasticsearchClientUnitTests {

	static final String HOST = ":9200";

	MockDelegatingElasticsearchClientProvider<ClientProvider> hostProvider;
	ReactiveElasticsearchClient client;

	@Before
	public void setUp() {

		hostProvider = ReactiveMockClientTestsUtils.provider(HOST);
		client = new ReactiveElasticsearchClient(hostProvider);
	}

	// --> PING

	@Test // DATAES-488
	public void pingShouldReturnTrueOnHttp200() {

		hostProvider.when(HOST).receive(Receive::ok);

		client.ping().as(StepVerifier::create).expectNext(true).verifyComplete();

	}

	// --> GET

	@Test // DATAES-488
	public void getShouldHitGetEndpoint() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("get-by-id-no-hit"));

		client.get(new GetRequest("twitter").id("1")) //
				.then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(hostProvider.client(HOST)).method(HttpMethod.GET);
		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {
			verify(requestBodyUriSpec).uri(eq("/twitter/_all/1"), any(Map.class));
		});
	}

	@Test // DATAES-488
	public void getShouldReturnExistingDocument() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("get-by-id-ok"));

		client.get(new GetRequest("twitter").id("1")) //
				.as(StepVerifier::create) //
				.consumeNextWith(result -> {

					assertThat(result.isExists()).isTrue();
					assertThat(result.getIndex()).isEqualTo("twitter");
					assertThat(result.getId()).isEqualTo("1");
					assertThat(result.getSource()) //
							.containsEntry("user", "kimchy") //
							.containsEntry("message", "Trying out Elasticsearch, so far so good?") //
							.containsKey("post_date");
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void getShouldReturnNonExisting() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("get-by-id-no-hit"));

		client.get(new GetRequest("twitter").id("1")) //
				.as(StepVerifier::create) //
				.consumeNextWith(result -> {
					assertThat(result.isExists()).isFalse();
				}) //
				.verifyComplete();
	}

	// --> SEARCH

	@Test // DATAES-488
	public void searchShouldHitSearchEndpoint() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("search-ok-no-hits"));

		client.search(new SearchRequest("twitter")).as(StepVerifier::create).verifyComplete();

		verify(hostProvider.client(HOST)).method(HttpMethod.POST);
		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {
			verify(requestBodyUriSpec).uri(eq("/twitter/_search"), any(Map.class));
		});
	}

	@Test // DATAES-488
	public void searchShouldReturnSingleResultCorrectly() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("search-ok-single-hit"));

		client.search(new SearchRequest("twitter")) //
				.as(StepVerifier::create) //
				.consumeNextWith(hit -> {

					assertThat(hit.getId()).isEqualTo("2");
					assertThat(hit.getIndex()).isEqualTo("twitter");
					assertThat(hit.getSourceAsMap()) //
							.containsEntry("user", "kimchy") //
							.containsEntry("message", "Another tweet, will it be indexed?") //
							.containsKey("post_date");
				}).verifyComplete();
	}

	@Test // DATAES-488
	public void searchShouldReturnMultipleResultsCorrectly() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("search-ok-multiple-hits"));

		client.search(new SearchRequest("twitter")) //
				.as(StepVerifier::create) //
				.consumeNextWith(hit -> {

					assertThat(hit.getId()).isEqualTo("2");
					assertThat(hit.getIndex()).isEqualTo("twitter");
					assertThat(hit.getSourceAsMap()) //
							.containsEntry("user", "kimchy") //
							.containsEntry("message", "Another tweet, will it be indexed?") //
							.containsKey("post_date");
				}) //
				.consumeNextWith(hit -> {

					assertThat(hit.getId()).isEqualTo("1");
					assertThat(hit.getIndex()).isEqualTo("twitter");
					assertThat(hit.getSourceAsMap()) //
							.containsEntry("user", "kimchy") //
							.containsEntry("message", "Trying out Elasticsearch, so far so good?") //
							.containsKey("post_date");
				}).verifyComplete();
	}

	@Test // DATAES-488
	public void searchShouldReturnEmptyFluxIfNothingFound() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("search-ok-no-hits"));

		client.search(new SearchRequest("twitter")) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	// --> INDEX

	@Test // DATAES-488
	public void indexNewShouldHitCreateEndpoint() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("index-ok-created"));

		client.index(new IndexRequest("twitter").id("10").create(true).source(" { foo : \"bar\" }", XContentType.JSON))
				.then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(hostProvider.client(HOST)).method(HttpMethod.PUT);
		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {

			verify(requestBodyUriSpec).uri(eq("/twitter/10/_create"), any(Map.class));
			verify(requestBodyUriSpec).contentType(MediaType.APPLICATION_JSON);
		});
	}

	@Test // DATAES-488
	public void indexExistingShouldHitUpdateEndpoint() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("index-ok-updated"));

		client.index(new IndexRequest("twitter").id("10").source(" { foo : \"bar\" }", XContentType.JSON)).then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(hostProvider.client(HOST)).method(HttpMethod.PUT);
		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {

			verify(requestBodyUriSpec).uri(eq("/twitter/10"), any(Map.class));
			verify(requestBodyUriSpec).contentType(MediaType.APPLICATION_JSON);
		});
	}

	@Test // DATAES-488
	public void indexShouldReturnCreatedWhenNewDocumentIndexed() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("index-ok-created"));

		client.index(new IndexRequest("twitter").id("10").create(true).source(" { foo : \"bar\" }", XContentType.JSON))
				.as(StepVerifier::create) //
				.consumeNextWith(response -> {

					assertThat(response.getId()).isEqualTo("10");
					assertThat(response.getIndex()).isEqualTo("twitter");
					assertThat(response.getResult()).isEqualTo(Result.CREATED);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void indexShouldReturnUpdatedWhenExistingDocumentIndexed() {

		hostProvider.when(HOST) //
				.receive(Receive::json) //
				.body(fromPath("index-ok-updated"));

		client.index(new IndexRequest("twitter").id("1").source(" { foo : \"bar\" }", XContentType.JSON))
				.as(StepVerifier::create) //
				.consumeNextWith(response -> {

					assertThat(response.getId()).isEqualTo("1");
					assertThat(response.getIndex()).isEqualTo("twitter");
					assertThat(response.getResult()).isEqualTo(Result.UPDATED);
				}) //
				.verifyComplete();
	}

	static Resource fromPath(String filename) {
		return new ClassPathResource("/org/springframework/data/elasticsearch/client/" + filename + ".json");
	}
}
