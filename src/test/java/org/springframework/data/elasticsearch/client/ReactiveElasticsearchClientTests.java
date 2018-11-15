/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.elasticsearch.TestUtils;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 */
public class ReactiveElasticsearchClientTests {

	static final String INDEX_I = "idx-1-reactive-client-tests";
	static final String INDEX_II = "idx-2-reactive-client-tests";

	static final String TYPE_I = "doc-type-1";
	static final String TYPE_II = "doc-type-2";

	static final Map<String, String> DOC_SOURCE;

	RestHighLevelClient syncClient;
	ReactiveElasticsearchClient client;

	static {

		Map<String, String> source = new LinkedHashMap<>();
		source.put("firstname", "chade");
		source.put("lastname", "fallstar");

		DOC_SOURCE = Collections.unmodifiableMap(source);
	}

	@Before
	public void setUp() {

		syncClient = TestUtils.restHighLevelClient();
		client = TestUtils.reactiveClient();
	}

	@After
	public void after() throws IOException {

		TestUtils.deleteIndex(INDEX_I, INDEX_II);

		syncClient.close();
	}

	@Test // DATAES-488
	public void pingForActiveHostShouldReturnTrue() {
		client.ping().as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void pingForUnknownHostShouldReturnFalse() {

		DefaultReactiveElasticsearchClient.create(HttpHeaders.EMPTY, "http://localhost:4711").ping() //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void infoShouldReturnClusterInformation() {

		client.info().as(StepVerifier::create) //
				.consumeNextWith(it -> {

					assertThat(it.isAvailable()).isTrue();
					assertThat(it.getVersion()).isGreaterThanOrEqualTo(Version.CURRENT);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void getShouldFetchDocumentById() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.get(new GetRequest(INDEX_I, TYPE_I, id)) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {

					assertThat(it.getId()).isEqualTo(id);
					assertThat(it.getSource()).containsAllEntriesOf(DOC_SOURCE);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void getShouldCompleteForNonExistingDocuments() {

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		String id = "this-one-does-not-exist";
		client.get(new GetRequest(INDEX_I, TYPE_I, id)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void getShouldCompleteForNonExistingType() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.get(new GetRequest(INDEX_I, "fantasy-books", id)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldReturnAllDocumentsFromSameCollection() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, TYPE_I, id1) //
				.add(INDEX_I, TYPE_I, id2);

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo(id1);
				}) //
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo(id2);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldReturnAllExistingDocumentsFromSameCollection() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, TYPE_I, id1) //
				.add(INDEX_I, TYPE_I, "this-one-does-not-exist");

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo(id1);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldSkipNonExistingDocuments() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, TYPE_I, id1) //
				.add(INDEX_I, TYPE_I, "this-one-does-not-exist") //
				.add(INDEX_I, TYPE_I, id2); //

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo(id1);
				}) //
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo(id2);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldCompleteIfNothingFound() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.multiGet(new MultiGetRequest().add(INDEX_II, TYPE_I, id1).add(INDEX_II, TYPE_I, id2)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldReturnAllExistingDocumentsFromDifferentCollection() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_II).to(INDEX_II);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, TYPE_I, id1) //
				.add(INDEX_II, TYPE_II, id2);

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo(id1);
				}) //
				.consumeNextWith(it -> {
					assertThat(it.getId()).isEqualTo(id2);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void existsReturnsTrueForExistingDocuments() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.exists(new GetRequest(INDEX_I, TYPE_I, id)) //
				.as(StepVerifier::create) //
				.expectNext(true)//
				.verifyComplete();
	}

	@Test // DATAES-488
	public void existsReturnsFalseForNonExistingDocuments() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.exists(new GetRequest(INDEX_II, TYPE_I, id)) //
				.as(StepVerifier::create) //
				.expectNext(false)//
				.verifyComplete();
	}

	@Test // DATAES-488
	public void indexShouldAddDocument() {

		IndexRequest request = indexRequest(DOC_SOURCE, INDEX_I, TYPE_I);

		client.index(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {

					assertThat(it.status()).isEqualTo(RestStatus.CREATED);
					assertThat(it.getId()).isEqualTo(request.id());
				})//
				.verifyComplete();
	}

	@Test // DATAES-488
	public void indexShouldErrorForExistingDocuments() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		IndexRequest request = indexRequest(DOC_SOURCE, INDEX_I, TYPE_I)//
				.id(id);

		client.index(request) //
				.as(StepVerifier::create) //
				.consumeErrorWith(error -> {
					assertThat(error).isInstanceOf(ElasticsearchStatusException.class);
				}) //
				.verify();
	}

	@Test // DATAES-488
	public void updateShouldUpsertNonExistingDocumentWhenUsedWithUpsert() {

		String id = UUID.randomUUID().toString();
		UpdateRequest request = new UpdateRequest(INDEX_I, TYPE_I, id) //
				.doc(DOC_SOURCE) //
				.docAsUpsert(true);

		client.update(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {

					assertThat(it.status()).isEqualTo(RestStatus.CREATED);
					assertThat(it.getId()).isEqualTo(id);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void updateShouldUpdateExistingDocument() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		UpdateRequest request = new UpdateRequest(INDEX_I, TYPE_I, id) //
				.doc(Collections.singletonMap("dutiful", "farseer"));

		client.update(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {

					assertThat(it.status()).isEqualTo(RestStatus.OK);
					assertThat(it.getId()).isEqualTo(id);
					assertThat(it.getVersion()).isEqualTo(2);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void updateShouldErrorNonExistingDocumentWhenNotUpserted() {

		String id = UUID.randomUUID().toString();
		UpdateRequest request = new UpdateRequest(INDEX_I, TYPE_I, id) //
				.doc(DOC_SOURCE);

		client.update(request) //
				.as(StepVerifier::create) //
				.consumeErrorWith(error -> {
					assertThat(error).isInstanceOf(ElasticsearchStatusException.class);
				}) //
				.verify();
	}

	@Test // DATAES-488
	public void deleteShouldRemoveExistingDocument() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		DeleteRequest request = new DeleteRequest(INDEX_I, TYPE_I, id);

		client.delete(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.status()).isEqualTo(RestStatus.OK);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void deleteShouldReturnNotFoundForNonExistingDocument() {

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		DeleteRequest request = new DeleteRequest(INDEX_I, TYPE_I, "this-one-does-not-exist");

		client.delete(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.status()).isEqualTo(RestStatus.NOT_FOUND);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void searchShouldFindExistingDocuments() {

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		SearchRequest request = new SearchRequest(INDEX_I).types(TYPE_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.search(request) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void searchShouldCompleteIfNothingFound() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I));

		SearchRequest request = new SearchRequest(INDEX_I).types(TYPE_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.search(request) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	AddToIndexOfType addSourceDocument() {
		return add(DOC_SOURCE);
	}

	AddToIndexOfType add(Map source) {
		return new AddDocument(source);
	}

	IndexRequest indexRequest(Map source, String index, String type) {

		return new IndexRequest(index, type) //
				.id(UUID.randomUUID().toString()) //
				.source(source) //
				.setRefreshPolicy(RefreshPolicy.IMMEDIATE) //
				.create(true);
	}

	String doIndex(Map source, String index, String type) {

		try {
			return syncClient.index(indexRequest(source, index, type)).getId();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	interface AddToIndexOfType extends AddToIndex {
		AddToIndex ofType(String type);
	}

	interface AddToIndex {
		String to(String index);
	}

	class AddDocument implements AddToIndexOfType {

		Map source;
		@Nullable String type;

		AddDocument(Map source) {
			this.source = source;
		}

		@Override
		public AddToIndex ofType(String type) {

			this.type = type;
			return this;
		}

		@Override
		public String to(String index) {
			return doIndex(new LinkedHashMap(source), index, type);
		}
	}

}
