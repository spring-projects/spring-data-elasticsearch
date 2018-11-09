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
import static org.springframework.data.elasticsearch.client.ReactiveMockClientTestsUtils.WebClientProvider.Receive.*;

import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
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

	@Test
	public void pingShouldHitMainEndpoint() {

		hostProvider.when(HOST) //
				.receive(Receive::ok);

		client.ping() //
				.then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {
			verify(requestBodyUriSpec).uri(eq("/"), any(Map.class));
		});
	}

	@Test // DATAES-488
	public void pingShouldReturnTrueOnHttp200() {

		hostProvider.when(HOST) //
				.receive(Receive::ok);

		client.ping() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void pingShouldReturnFalseOnNonHttp200() {

		hostProvider.when(HOST) //
				.receive(Receive::error);

		client.ping() //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	// --> INFO

	@Test
	public void infoShouldHitMainEndpoint() {

		hostProvider.when(HOST) //
				.receiveInfo();

		client.info() //
				.then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {
			verify(requestBodyUriSpec).uri(eq("/"), any(Map.class));
		});
	}

	@Test // DATAES-488
	public void infoShouldReturnResponseCorrectly() {

		hostProvider.when(HOST) //
				.receiveInfo();

		client.info() //
				.as(StepVerifier::create) //
				.consumeNextWith(mainResponse -> {}) //
				.verifyComplete();
	}

	// --> GET

	@Test // DATAES-488
	public void getShouldHitGetEndpoint() {

		hostProvider.when(HOST) //
				.receiveGetByIdNotFound();

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
				.receiveGetById();

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
	public void getShouldReturnEmptyForNonExisting() {

		hostProvider.when(HOST) //
				.receiveGetByIdNotFound();

		client.get(new GetRequest("twitter").id("1")) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	// --> MGET

	@Test // DATAES-488
	public void multiGetShouldHitMGetEndpoint() {

		hostProvider.when(HOST) //
				.receiveJsonFromFile("multi-get-ok-2-hits");

		client.multiGet(new MultiGetRequest().add("twitter", "_doc", "1").add("twitter", "_doc", "2")) //
				.then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(hostProvider.client(HOST)).method(HttpMethod.POST);

		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {

			verify(requestBodyUriSpec).uri(eq("/_mget"), any(Map.class));
			verify(requestBodyUriSpec).body(any(Publisher.class), any(Class.class));
		});
	}

	@Test // DATAES-488
	public void multiGetShouldReturnExistingDocuments() {

		hostProvider.when(HOST) //
				.receiveJsonFromFile("multi-get-ok-2-hits");

		client.multiGet(new MultiGetRequest().add("twitter", "_doc", "1").add("twitter", "_doc", "2")) //
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
				.consumeNextWith(result -> {

					assertThat(result.isExists()).isTrue();
					assertThat(result.getIndex()).isEqualTo("twitter");
					assertThat(result.getId()).isEqualTo("2");
					assertThat(result.getSource()) //
							.containsEntry("user", "kimchy") //
							.containsEntry("message", "Another tweet, will it be indexed?") //
							.containsKey("post_date");
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldWorkForNonExistingDocuments() {

		hostProvider.when(HOST) //
				.receiveJsonFromFile("multi-get-ok-2-hits-1-unavailable");

		client.multiGet(new MultiGetRequest().add("twitter", "_doc", "1").add("twitter", "_doc", "2")) //
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
				.consumeNextWith(result -> {

					assertThat(result.isExists()).isTrue();
					assertThat(result.getIndex()).isEqualTo("twitter");
					assertThat(result.getId()).isEqualTo("3");
					assertThat(result.getSource()) //
							.containsEntry("user", "elastic") //
							.containsEntry("message", "Building the site, should be kewl") //
							.containsKey("post_date");
				}) //
				.verifyComplete();
	}

	// --> EXISTS

	@Test // DATAES-488
	public void existsShouldHitGetEndpoint() {

		hostProvider.when(HOST) //
				.receiveGetById();

		client.exists(new GetRequest("twitter").id("1")) //
				.then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(hostProvider.client(HOST)).method(HttpMethod.HEAD);

		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {
			verify(requestBodyUriSpec).uri(eq("/twitter/_all/1"), any(Map.class));
		});
	}

	@Test // DATAES-488
	public void existsShouldReturnTrueIfExists() {

		hostProvider.when(HOST) //
				.receiveGetById();

		client.exists(new GetRequest("twitter").id("1")) //
				.as(StepVerifier::create) //
				.expectNext(true).verifyComplete();
	}

	@Test // DATAES-488
	public void existsShouldReturnFalseIfNotExists() {

		hostProvider.when(HOST) //
				.receiveGetByIdNotFound();

		client.exists(new GetRequest("twitter").id("1")) //
				.as(StepVerifier::create) //
				.expectNext(false).verifyComplete();
	}

	// --> INDEX

	@Test // DATAES-488
	public void indexNewShouldHitCreateEndpoint() {

		hostProvider.when(HOST) //
				.receiveIndexCreated();

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
	public void indexExistingShouldHitEndpointCorrectly() {

		hostProvider.when(HOST) //
				.receiveIndexUpdated();

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
				.receiveIndexCreated();

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
				.receiveIndexUpdated();

		client.index(new IndexRequest("twitter").id("1").source(" { foo : \"bar\" }", XContentType.JSON))
				.as(StepVerifier::create) //
				.consumeNextWith(response -> {

					assertThat(response.getId()).isEqualTo("1");
					assertThat(response.getIndex()).isEqualTo("twitter");
					assertThat(response.getResult()).isEqualTo(Result.UPDATED);
				}) //
				.verifyComplete();
	}

	// --> UPDATE

	@Test // DATAES-488
	public void updateShouldHitEndpointCorrectly() {

		hostProvider.when(HOST) //
				.receiveUpdateOk();

		client.update(new UpdateRequest("twitter", "doc", "1").doc(Collections.singletonMap("user", "cstrobl"))).then() //
				.as(StepVerifier::create) //
				.verifyComplete();

		verify(hostProvider.client(HOST)).method(HttpMethod.POST);
		hostProvider.when(HOST).exchange(requestBodyUriSpec -> {

			verify(requestBodyUriSpec).uri(eq("/twitter/doc/1/_update"), any(Map.class));
			verify(requestBodyUriSpec).contentType(MediaType.APPLICATION_JSON);
		});
	}

	@Test // DATAES-488
	public void updateShouldEmitResponseCorrectly() {

		hostProvider.when(HOST) //
				.receiveUpdateOk();

		client.update(new UpdateRequest("twitter", "doc", "1").doc(Collections.singletonMap("user", "cstrobl")))
				.as(StepVerifier::create) //
				.consumeNextWith(updateResponse -> {

					assertThat(updateResponse.getResult()).isEqualTo(Result.UPDATED);
					assertThat(updateResponse.getVersion()).isEqualTo(2);
					assertThat(updateResponse.getId()).isEqualTo("1");
					assertThat(updateResponse.getIndex()).isEqualTo("twitter");
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void updateShouldEmitErrorWhenNotFound() {

		hostProvider.when(HOST) //
				.updateFail();

		client.update(new UpdateRequest("twitter", "doc", "1").doc(Collections.singletonMap("user", "cstrobl")))
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class) //
				.verify();
	}

	// --> SEARCH

	@Test // DATAES-488
	public void searchShouldHitSearchEndpoint() {

		hostProvider.when(HOST) //
				.receiveSearchOk();

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
				.receiveSearchOk();

		client.search(new SearchRequest("twitter")) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

}
