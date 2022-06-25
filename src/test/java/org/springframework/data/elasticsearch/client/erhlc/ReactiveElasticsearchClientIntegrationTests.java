/*
 * Copyright 2018-2022 the original author or authors.
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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.RestStatusException;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Henrique Amaral
 * @author Russell Parry
 * @author Thomas Geese
 * @author Farid Faoudi
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveElasticsearchClientIntegrationTests.Config.class })
public class ReactiveElasticsearchClientIntegrationTests {

	@Configuration
	static class Config extends ReactiveElasticsearchRestTemplateConfiguration {
		@Override
		@Bean
		public ReactiveElasticsearchClient reactiveElasticsearchClient() {
			return super.reactiveElasticsearchClient();
		}

		@Override
		protected org.springframework.data.elasticsearch.core.RefreshPolicy refreshPolicy() {
			return org.springframework.data.elasticsearch.core.RefreshPolicy.IMMEDIATE;
		}
	}

	static final String INDEX_I = "idx-1-reactive-client-tests";
	static final String INDEX_II = "idx-2-reactive-client-tests";

	// must be <String, Object> and not <String, String>, otherwise UpdateRequest.doc() will use the overload with
	// (Object...)
	static final Map<String, Object> DOC_SOURCE;

	@Autowired ReactiveElasticsearchClient client;
	@Autowired ReactiveElasticsearchOperations operations;

	static {

		Map<String, String> source = new LinkedHashMap<>();
		source.put("firstname", "chade");
		source.put("lastname", "fallstar");

		DOC_SOURCE = Collections.unmodifiableMap(source);
	}

	@BeforeEach
	public void setUp() {
		operations.indexOps(IndexCoordinates.of(INDEX_I)).delete().block();
		operations.indexOps(IndexCoordinates.of(INDEX_II)).delete().block();
	}

	@AfterEach
	public void after() {
		operations.indexOps(IndexCoordinates.of(INDEX_I)).delete().block();
		operations.indexOps(IndexCoordinates.of(INDEX_II)).delete().block();
	}

	@Test // DATAES-488
	public void pingForActiveHostShouldReturnTrue() {
		client.ping().as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void pingForUnknownHostShouldReturnFalse() {

		DefaultReactiveElasticsearchClient
				.create(ClientConfiguration.builder().connectedTo("localhost:4711").withConnectTimeout(Duration.ofSeconds(2))
						.build())
				.ping() //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void infoShouldReturnClusterInformation() {

		client.info().as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getVersion()).isNotNull()) //
				.verifyComplete();
	}

	@Test // DATAES-519, DATAES-822
	public void getOnNonExistingIndexShouldThrowException() {

		client.get(new GetRequest(INDEX_I, "nonono")) //
				.as(StepVerifier::create) //
				.expectError(RestStatusException.class) //
				.verify();
	}

	@Test // DATAES-488
	public void getShouldFetchDocumentById() {

		String id = addSourceDocument().to(INDEX_I);

		client.get(new GetRequest(INDEX_I, id)) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {

					assertThat(it.getId()).isEqualTo(id);
					assertThat(it.getSource()).containsAllEntriesOf(DOC_SOURCE);
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void getShouldCompleteForNonExistingDocuments() {

		addSourceDocument().to(INDEX_I);

		String id = "this-one-does-not-exist";
		client.get(new GetRequest(INDEX_I, id)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488, #1678
	public void multiGetShouldReturnAllDocumentsFromSameCollection() {

		String id1 = addSourceDocument().to(INDEX_I);
		String id2 = addSourceDocument().to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_I, id2);

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.getResponse().isExists()).isTrue(); //
					assertThat(it.getId()).isEqualTo(id1); //
				}) //
				.consumeNextWith(it -> {
					assertThat(it.getResponse().isExists()).isTrue(); //
					assertThat(it.getId()).isEqualTo(id2); //
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488, #1678
	public void multiGetShouldReturnAllExistingDocumentsFromSameCollection() {

		String id1 = addSourceDocument().to(INDEX_I);
		addSourceDocument().to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_I, "this-one-does-not-exist");

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.isFailed()).isFalse(); //
					assertThat(it.getResponse().isExists()).isTrue(); //
					assertThat(it.getId()).isEqualTo(id1); //
				}) //
				.consumeNextWith(it -> {
					assertThat(it.isFailed()).isFalse(); //
					assertThat(it.getResponse().isExists()).isFalse(); //
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488, #1678
	public void multiGetShouldNotSkipNonExistingDocuments() {

		String id1 = addSourceDocument().to(INDEX_I);
		String id2 = addSourceDocument().to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_I, "this-one-does-not-exist") //
				.add(INDEX_I, id2); //

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.isFailed()).isFalse(); //
					assertThat(it.getResponse().isExists()).isTrue(); //
					assertThat(it.getId()).isEqualTo(id1); //
				}) //
				.consumeNextWith(it -> {
					assertThat(it.isFailed()).isFalse(); //
					assertThat(it.getResponse().isExists()).isFalse();
				}) //
				.consumeNextWith(it -> {
					assertThat(it.isFailed()).isFalse(); //
					assertThat(it.getResponse().isExists()).isTrue(); //
					assertThat(it.getId()).isEqualTo(id2); //
				}) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldCompleteIfNothingFound() {

		String id1 = addSourceDocument().to(INDEX_I);
		String id2 = addSourceDocument().to(INDEX_I);

		client.multiGet(new MultiGetRequest() //
				.add(INDEX_II, id1).add(INDEX_II, id2)) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.isFailed()).isTrue()) //
				.consumeNextWith(it -> assertThat(it.isFailed()).isTrue()) //
				.verifyComplete();
	}

	@Test // DATAES-488, #1678
	public void multiGetShouldReturnAllExistingDocumentsFromDifferentCollection() {

		String id1 = addSourceDocument().to(INDEX_I);
		String id2 = addSourceDocument().to(INDEX_II);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_II, id2);

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> { //
					assertThat(it.isFailed()).isFalse(); //
					assertThat(it.getId()).isEqualTo(id1); //
				}) //
				.consumeNextWith(it -> { //
					assertThat(it.isFailed()).isFalse(); //
					assertThat(it.getId()).isEqualTo(id2); //
				}) //
				.verifyComplete(); //
	}

	@Test // DATAES-488
	public void existsReturnsTrueForExistingDocuments() {

		String id = addSourceDocument().to(INDEX_I);

		client.exists(new GetRequest(INDEX_I, id)) //
				.as(StepVerifier::create) //
				.expectNext(true)//
				.verifyComplete();
	}

	@Test // DATAES-488
	public void existsReturnsFalseForNonExistingDocuments() {

		String id = addSourceDocument().to(INDEX_I);

		client.exists(new GetRequest(INDEX_II, id)) //
				.as(StepVerifier::create) //
				.expectNext(false)//
				.verifyComplete();
	}

	@Test // DATAES-488
	public void indexShouldAddDocument() {

		IndexRequest request = indexRequest();

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

		String id = addSourceDocument().to(INDEX_I);

		IndexRequest request = indexRequest()//
				.id(id);

		client.index(request) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-488
	public void updateShouldUpsertNonExistingDocumentWhenUsedWithUpsert() {

		String id = UUID.randomUUID().toString();
		UpdateRequest request = new UpdateRequest(INDEX_I, id) //
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

		String id = addSourceDocument().to(INDEX_I);

		UpdateRequest request = new UpdateRequest(INDEX_I, id) //
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
		UpdateRequest request = new UpdateRequest(INDEX_I, id) //
				.doc(DOC_SOURCE);

		client.update(request) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-488
	public void deleteShouldRemoveExistingDocument() {

		String id = addSourceDocument().to(INDEX_I);

		DeleteRequest request = new DeleteRequest(INDEX_I, id);

		client.delete(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.status()).isEqualTo(RestStatus.OK)) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void deleteShouldReturnNotFoundForNonExistingDocument() {

		addSourceDocument().to(INDEX_I);

		DeleteRequest request = new DeleteRequest(INDEX_I, "this-one-does-not-exist");

		client.delete(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.status()).isEqualTo(RestStatus.NOT_FOUND)) //
				.verifyComplete();
	}

	@Test // #1725
	public void inlineSearchTemplateShouldFindMatchingDocuments() {

		addSourceDocument().to(INDEX_I);
		addSourceDocument().to(INDEX_I);

		Map<String, Object> testDoc = new LinkedHashMap<>();
		testDoc.put("firstname", "inline");
		testDoc.put("lastname", "template");
		add(testDoc).to(INDEX_I);

		SearchTemplateRequest request = new SearchTemplateRequest(new SearchRequest(INDEX_I));
		request.setScriptType(ScriptType.INLINE);
		request.setScript("{\"query\":{\"match\":{\"firstname\":\"{{firstname}}\"}}}");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("firstname", "inline");
		request.setScriptParams(params);

		client.searchTemplate(request).as(StepVerifier::create).expectNextCount(1).verifyComplete();
	}

	@Test // #1725
	public void storedSearchTemplateShouldFindMatchingDocuments() {

		addSourceDocument().to(INDEX_I);
		addSourceDocument().to(INDEX_I);

		Map<String, Object> testDoc = new LinkedHashMap<>();
		testDoc.put("firstname", "stored");
		testDoc.put("lastname", "template");
		add(testDoc).to(INDEX_I);

		client.execute(c -> c.post().uri(builder -> builder.path("_scripts/searchbyfirstname").build())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(
						"{\"script\":{\"lang\":\"mustache\",\"source\":{\"query\":{\"match\":{\"firstname\":\"{{firstname}}\"}}}}}")
				.retrieve().bodyToMono(Void.class)).block();

		client.indices().refreshIndex(request -> request.indices(INDEX_I)).block();

		SearchTemplateRequest request = new SearchTemplateRequest(new SearchRequest(INDEX_I));
		request.setScriptType(ScriptType.STORED);
		request.setScript("searchbyfirstname");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("firstname", "stored");
		request.setScriptParams(params);

		client.searchTemplate(request) //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete(); //
	}

	@Test // DATAES-488
	public void searchShouldFindExistingDocuments() {

		addSourceDocument().to(INDEX_I);
		addSourceDocument().to(INDEX_I);

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.search(request) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void searchShouldCompleteIfNothingFound() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.search(request) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void deleteByShouldRemoveExistingDocument() {

		String id = addSourceDocument().to(INDEX_I);

		DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX_I) //
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("_id", id)));

		client.deleteBy(request) //
				.map(BulkByScrollResponse::getDeleted) //
				.as(StepVerifier::create) //
				.expectNext(1L) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void deleteByEmitResultWhenNothingRemoved() {

		addSourceDocument().to(INDEX_I);

		DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX_I) //
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("_id", "it-was-not-me")));

		client.deleteBy(request) //
				.map(BulkByScrollResponse::getDeleted) //
				.as(StepVerifier::create) //
				.expectNext(0L)//
				.verifyComplete();
	}

	@Test // #1446
	void updateByEmitResultWhenNothingUpdated() {
		addSourceDocument().to(INDEX_I);
		addSourceDocument().to(INDEX_I);

		Map<String, String> source = new LinkedHashMap<>();
		source.put("firstname", "crow");
		source.put("lastname", "cat");

		final Map<String, String> documentToNotUpdate = Collections.unmodifiableMap(source);
		add(documentToNotUpdate).to(INDEX_I);

		final String script = "ctx._source['firstname'] = params['newFirstname']";
		final Map<String, Object> params = Collections.singletonMap("newFirstname", "arrow");

		final UpdateByQueryRequest request = new UpdateByQueryRequest(INDEX_I) //
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("lastname", "fallstar"))) //
				.setAbortOnVersionConflict(true) //
				.setRefresh(true) //
				.setScript(new Script(ScriptType.INLINE, "painless", script, params)); //

		client.updateBy(request) //
				.map(ByQueryResponse::getUpdated) //
				.as(StepVerifier::create) //
				.expectNext(2L) //
				.verifyComplete(); //

		final SearchRequest searchUpdatedRequest = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder() //
						.query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("firstname", "arrow"))) //
				);

		client.search(searchUpdatedRequest) //
				.collectList() //
				.map(List::size) //
				.as(StepVerifier::create) //
				.expectNext(2) //
				.verifyComplete(); //
	}

	@Test // #1446
	void updateByShouldUpdateExistingDocument() {
		addSourceDocument().to(INDEX_I);

		String script = "ctx._source['firstname'] = params['newFirstname']";
		Map<String, Object> params = Collections.singletonMap("newFirstname", "arrow");

		UpdateByQueryRequest request = new UpdateByQueryRequest(INDEX_I) //
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("lastname", "non_existing_lastname"))) //
				.setAbortOnVersionConflict(true) //
				.setRefresh(true) //
				.setScript(new Script(ScriptType.INLINE, "painless", script, params)); //

		client.updateBy(request) //
				.map(ByQueryResponse::getUpdated) //
				.as(StepVerifier::create) //
				.expectNext(0L) //
				.verifyComplete(); //

		SearchRequest searchUpdatedRequest = new SearchRequest(INDEX_I) // //
				.source(new SearchSourceBuilder() //
						.query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("firstname", "arrow"))) //
				); //

		client.search(searchUpdatedRequest) //
				.collectList() //
				.map(List::size) //
				.as(StepVerifier::create) //
				.expectNext(0) //
				.verifyComplete(); //
	}

	@Test // DATAES-510
	public void scrollShouldReadWhileEndNotReached() {

		IntStream.range(0, 100).forEach(it -> add(Collections.singletonMap(it + "-foo", "bar")).to(INDEX_I));

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		request = request.scroll(TimeValue.timeValueMinutes(1));

		client.scroll(HttpHeaders.EMPTY, request) //
				.as(StepVerifier::create) //
				.expectNextCount(100) //
				.verifyComplete();
	}

	@Test // DATAES-510
	public void scrollShouldReadWhileTakeNotReached() {

		IntStream.range(0, 100).forEach(it -> add(Collections.singletonMap(it + "-foo", "bar")).to(INDEX_I));

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		request = request.scroll(TimeValue.timeValueMinutes(1));

		client.scroll(HttpHeaders.EMPTY, request) //
				.take(73) //
				.as(StepVerifier::create) //
				.expectNextCount(73) //
				.verifyComplete();
	}

	@Test // DATAES-569
	public void indexExistsShouldReturnTrueIfSo() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().existsIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-569
	public void indexExistsShouldReturnFalseIfNot() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().existsIndex(request -> request.indices(INDEX_II)) //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // #1658
	public void indexExistsShouldReturnTrueIfExists() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().existsIndex(new GetIndexRequest(INDEX_I)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // #1658
	public void indexExistsShouldReturnFalseIfNotExists() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().existsIndex(new GetIndexRequest(INDEX_II)) //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-678
	public void createIndex() {

		client.indices().createIndex(request -> request.index(INDEX_I)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		operations.indexOps(IndexCoordinates.of(INDEX_I)).exists() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-569
	public void createExistingIndexErrors() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().createIndex(request -> request.index(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // #1658
	public void createIndex_() {

		client.indices().createIndex(new CreateIndexRequest(INDEX_I)).as(StepVerifier::create).expectNext(true)
				.verifyComplete();

		operations.indexOps(IndexCoordinates.of(INDEX_I)).exists() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // #1658
	public void createExistingIndexErrors_() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().createIndex(new CreateIndexRequest(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // #1658
	public void getIndex() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().getIndex(new GetIndexRequest(INDEX_I)).as(StepVerifier::create).consumeNextWith(it -> {
			assertThat(it.getIndices().length).isOne();
			assertThat(it.getIndices()[0]).isEqualTo(INDEX_I);
		}).verifyComplete();
	}

	@Test // #1658
	public void getIndexError() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().getIndex(new GetIndexRequest(INDEX_II)).as(StepVerifier::create)
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-569, DATAES-678
	public void deleteExistingIndex() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().deleteIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		operations.indexOps(IndexCoordinates.of(INDEX_I)) //
				.exists() //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void deleteNonExistingIndexErrors() {

		client.indices().deleteIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-569
	public void openExistingIndex() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().openIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void openNonExistingIndex() {

		client.indices().openIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-569
	public void closeExistingIndex() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().openIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void closeNonExistingIndex() {

		client.indices().closeIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-569
	public void refreshIndex() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().refreshIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void refreshNonExistingIndex() {

		client.indices().refreshIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // #1640
	void putMapping() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		org.elasticsearch.client.indices.PutMappingRequest putMappingRequest = new org.elasticsearch.client.indices.PutMappingRequest(
				INDEX_I).source(jsonMap);

		client.indices().putMapping(putMappingRequest) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // #1640
	void putMappingError() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		org.elasticsearch.client.indices.PutMappingRequest putMappingRequest = new org.elasticsearch.client.indices.PutMappingRequest(
				INDEX_II).source(jsonMap);

		client.indices().putMapping(putMappingRequest) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // #1640
	void getMapping() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		org.elasticsearch.client.indices.PutMappingRequest putMappingRequest = new org.elasticsearch.client.indices.PutMappingRequest(
				INDEX_I).source(jsonMap);

		client.indices().putMapping(putMappingRequest).block();

		final GetMappingsRequest getMappingsRequest = new GetMappingsRequest().indices(INDEX_I);

		client.indices().getMapping(getMappingsRequest) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.mappings().get(INDEX_I).getSourceAsMap()).isEqualTo(jsonMap);
				}).verifyComplete();
	}

	@Test // #1640
	void getMappingError() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		org.elasticsearch.client.indices.PutMappingRequest putMappingRequest = new org.elasticsearch.client.indices.PutMappingRequest(
				INDEX_I).source(jsonMap);

		client.indices().putMapping(putMappingRequest).block();

		final GetMappingsRequest getMappingsRequest = new GetMappingsRequest().indices(INDEX_II);

		client.indices().getMapping(getMappingsRequest) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-569
	public void updateMapping() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		client.indices().putMapping(request -> request.indices(INDEX_I).source(jsonMap)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void updateMappingNonExistingIndex() {

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		client.indices().putMapping(request -> request.indices(INDEX_I).source(jsonMap)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-569
	public void flushIndex() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		client.indices().flushIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void flushNonExistingIndex() {

		client.indices().flushIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-684
	public void bulkShouldUpdateExistingDocument() {
		String idFirstDoc = addSourceDocument().to(INDEX_I);
		String idSecondDoc = addSourceDocument().to(INDEX_I);

		UpdateRequest requestFirstDoc = new UpdateRequest(INDEX_I, idFirstDoc) //
				.doc(Collections.singletonMap("dutiful", "farseer"));
		UpdateRequest requestSecondDoc = new UpdateRequest(INDEX_I, idSecondDoc) //
				.doc(Collections.singletonMap("secondDocUpdate", "secondDocUpdatePartTwo"));

		BulkRequest bulkRequest = new BulkRequest();
		bulkRequest.add(requestFirstDoc);
		bulkRequest.add(requestSecondDoc);

		client.bulk(bulkRequest).as(StepVerifier::create) //
				.consumeNextWith(it -> {
					assertThat(it.status()).isEqualTo(RestStatus.OK);
					assertThat(it.hasFailures()).isFalse();

					Arrays.stream(it.getItems()).forEach(itemResponse -> {
						assertThat(itemResponse.status()).isEqualTo(RestStatus.OK);
						assertThat(itemResponse.getVersion()).isEqualTo(2);
					});
				}).verifyComplete();
	}

	@Test // DATAES-567
	public void aggregateReturnsAggregationResults() {
		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(INDEX_I));
		indexOps.create().block();

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("firstname", Collections.singletonMap("type", "keyword")));
		indexOps.putMapping(Mono.just(Document.from(jsonMap))).block();

		addSourceDocument().to(INDEX_I);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
		searchSourceBuilder.aggregation(AggregationBuilders.terms("terms").field("firstname"));

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(searchSourceBuilder);

		client.aggregate(request).as(StepVerifier::create)
				.expectNextMatches(aggregation -> aggregation.getType().equals(StringTerms.NAME)).verifyComplete();
	}

	@Test // DATAES-866
	public void suggestReturnsSuggestionResults() {
		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(INDEX_I));
		indexOps.create().block();
		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("firstname", Collections.singletonMap("type", "completion")));
		indexOps.putMapping(Mono.just(Document.from(jsonMap))).block();

		addSourceDocument().to(INDEX_I);

		SuggestBuilder suggestBuilder = new SuggestBuilder().addSuggestion("firstname",
				new CompletionSuggestionBuilder("firstname").prefix("ch"));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
		searchSourceBuilder.suggest(suggestBuilder);

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(searchSourceBuilder);

		client
				.suggest(request).as(StepVerifier::create).expectNextMatches(suggestions -> suggestions
						.getSuggestion("firstname").getEntries().get(0).getOptions().get(0).getText().string().equals("chade"))
				.verifyComplete();
	}

	@Test // #1640
	void getFieldMapping() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		Map<String, Object> properties = new HashMap<>();
		properties.put("message1", Collections.singletonMap("type", "text"));
		properties.put("message2", Collections.singletonMap("type", "keyword"));

		Map<String, Object> jsonMap = Collections.singletonMap("properties", properties);

		PutMappingRequest putMappingRequest = new PutMappingRequest(INDEX_I).source(jsonMap);

		client.indices().putMapping(putMappingRequest).block();

		client.indices().getFieldMapping(request -> request.indices(INDEX_I).fields("message1", "message2"))
				.as(StepVerifier::create).consumeNextWith(it -> {
					assertThat(it.mappings().get(INDEX_I).keySet().size()).isEqualTo(2);
					assertThat(it.mappings().get(INDEX_I).get("message1").sourceAsMap())
							.isEqualTo(Collections.singletonMap("message1", Collections.singletonMap("type", "text")));
					assertThat(it.mappings().get(INDEX_I).get("message2").sourceAsMap())
							.isEqualTo(Collections.singletonMap("message2", Collections.singletonMap("type", "keyword")));
				}).verifyComplete();
	}

	@Test // #1640
	void getFieldMappingNonExistingField() {

		operations.indexOps(IndexCoordinates.of(INDEX_I)).create().block();

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		PutMappingRequest putMappingRequest = new PutMappingRequest(INDEX_I).source(jsonMap);

		client.indices().putMapping(putMappingRequest).block();

		client.indices().getFieldMapping(request -> request.indices(INDEX_I).fields("message1")).as(StepVerifier::create)
				.consumeNextWith(it -> {
					assertThat(it.mappings().get(INDEX_I).keySet().size()).isZero();
				}).verifyComplete();
	}

	@Test // #1640
	void getFieldMappingNonExistingIndex() {

		client.indices().getFieldMapping(request -> request.indices(INDEX_I).fields("message1")).as(StepVerifier::create)
				.verifyError(RestStatusException.class);
	}

	@Test // DATAES-796
	@DisplayName("should return the whole SearchResponse")
	void shouldReturnTheWholeSearchResponse() {
		addSourceDocument().to(INDEX_I);
		addSourceDocument().to(INDEX_I);

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.searchForResponse(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(searchResponse -> assertThat(searchResponse.getHits().getTotalHits().value).isEqualTo(2))
				.verifyComplete();
	}

	private AddToIndex addSourceDocument() {
		return add(DOC_SOURCE);
	}

	private AddToIndex add(Map<String, ?> source) {
		return new AddDocument(source);
	}

	private IndexRequest indexRequest() {

		return new IndexRequest(ReactiveElasticsearchClientIntegrationTests.INDEX_I) //
				.id(UUID.randomUUID().toString()) //
				.source(ReactiveElasticsearchClientIntegrationTests.DOC_SOURCE) //
				.create(true);
	}

	private String doIndex(Map<String, ?> source, String index) {
		return operations.save(source, IndexCoordinates.of(index)).block().get("id").toString();
	}

	interface AddToIndex {
		String to(String index);
	}

	class AddDocument implements AddToIndex {

		Map<String, ?> source;

		AddDocument(Map<String, ?> source) {
			this.source = source;
		}

		@Override
		public String to(String index) {
			return doIndex(new LinkedHashMap<>(source), index);
		}
	}

}
