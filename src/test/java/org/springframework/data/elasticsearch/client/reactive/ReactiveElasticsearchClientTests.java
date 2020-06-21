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

import static org.assertj.core.api.Assertions.*;

import lombok.SneakyThrows;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.Version;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.TestUtils;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.junit.junit4.ElasticsearchVersion;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Henrique Amaral
 * @author Russell Parry
 * @author Thomas Geese
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class ReactiveElasticsearchClientTests {

	static final String INDEX_I = "idx-1-reactive-client-tests";
	static final String INDEX_II = "idx-2-reactive-client-tests";

	static final String TYPE_I = "doc-type-1";
	static final String TYPE_II = "doc-type-2";

	// must be <String, Object> and not <String, String>, otherwise UpdateRequest.doc() will use the overload with
	// (Object...)
	static final Map<String, Object> DOC_SOURCE;

	RestHighLevelClient syncClient;
	ReactiveElasticsearchClient client;

	static {

		Map<String, String> source = new LinkedHashMap<>();
		source.put("firstname", "chade");
		source.put("lastname", "fallstar");

		DOC_SOURCE = Collections.unmodifiableMap(source);
	}

	@BeforeEach
	public void setUp() {

		syncClient = TestUtils.restHighLevelClient();
		client = TestUtils.reactiveClient();

		TestUtils.deleteIndex(INDEX_I, INDEX_II);
	}

	@AfterEach
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
				.consumeNextWith(it -> assertThat(it.getVersion()).isGreaterThanOrEqualTo(Version.CURRENT)) //
				.verifyComplete();
	}

	@Test // DATAES-519, DATAES-822
	public void getOnNonExistingIndexShouldThrowException() {

		client.get(new GetRequest(INDEX_I, "nonono")) //
				.as(StepVerifier::create) //
				.expectError(ElasticsearchStatusException.class) //
				.verify();
	}

	@Test // DATAES-488
	public void getShouldFetchDocumentById() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

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

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		String id = "this-one-does-not-exist";
		client.get(new GetRequest(INDEX_I, id)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldReturnAllDocumentsFromSameCollection() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_I, id2);

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo(id1)) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo(id2)) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldReturnAllExistingDocumentsFromSameCollection() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_I, "this-one-does-not-exist");

		client.multiGet(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getId()).isEqualTo(id1)) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldSkipNonExistingDocuments() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_I, "this-one-does-not-exist") //
				.add(INDEX_I, id2); //

		client.multiGet(request) //
				.map(GetResult::getId) //
				.as(StepVerifier::create) //
				.expectNext(id1, id2) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldCompleteIfNothingFound() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.multiGet(new MultiGetRequest() //
				.add(INDEX_II, id1).add(INDEX_II, id2)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void multiGetShouldReturnAllExistingDocumentsFromDifferentCollection() {

		String id1 = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String id2 = addSourceDocument().ofType(TYPE_II).to(INDEX_II);

		MultiGetRequest request = new MultiGetRequest() //
				.add(INDEX_I, id1) //
				.add(INDEX_II, id2);

		client.multiGet(request) //
				.map(GetResult::getId) //
				.as(StepVerifier::create) //
				.expectNext(id1, id2) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void existsReturnsTrueForExistingDocuments() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.exists(new GetRequest(INDEX_I, id)) //
				.as(StepVerifier::create) //
				.expectNext(true)//
				.verifyComplete();
	}

	@Test // DATAES-488
	public void existsReturnsFalseForNonExistingDocuments() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		client.exists(new GetRequest(INDEX_II, id)) //
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
				.verifyError(ElasticsearchStatusException.class);
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

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

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
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-488
	public void deleteShouldRemoveExistingDocument() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		DeleteRequest request = new DeleteRequest(INDEX_I, id);

		client.delete(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.status()).isEqualTo(RestStatus.OK)) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void deleteShouldReturnNotFoundForNonExistingDocument() {

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		DeleteRequest request = new DeleteRequest(INDEX_I, "this-one-does-not-exist");

		client.delete(request) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.status()).isEqualTo(RestStatus.NOT_FOUND)) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void searchShouldFindExistingDocuments() {

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.search(request) //
				.as(StepVerifier::create) //
				.expectNextCount(2) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void searchShouldCompleteIfNothingFound() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

		client.search(request) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	@ElasticsearchVersion(asOf = "6.5.0")
	public void deleteByShouldRemoveExistingDocument() {

		String id = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX_I) //
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("_id", id)));

		client.deleteBy(request) //
				.map(BulkByScrollResponse::getDeleted) //
				.as(StepVerifier::create) //
				.expectNext(1L) //
				.verifyComplete();
	}

	@Test // DATAES-488
	@ElasticsearchVersion(asOf = "6.5.0")
	public void deleteByEmitResultWhenNothingRemoved() {

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX_I) //
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("_id", "it-was-not-me")));

		client.deleteBy(request) //
				.map(BulkByScrollResponse::getDeleted) //
				.as(StepVerifier::create) //
				.expectNext(0L)//
				.verifyComplete();
	}

	@Test // DATAES-510
	public void scrollShouldReadWhileEndNotReached() {

		IntStream.range(0, 100).forEach(it -> add(Collections.singletonMap(it + "-foo", "bar")).ofType(TYPE_I).to(INDEX_I));

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

		IntStream.range(0, 100).forEach(it -> add(Collections.singletonMap(it + "-foo", "bar")).ofType(TYPE_I).to(INDEX_I));

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
	public void indexExistsShouldReturnTrueIfSo() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().existsIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-569
	public void indexExistsShouldReturnFalseIfNot() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().existsIndex(request -> request.indices(INDEX_II)) //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-678
	public void createIndex() throws IOException {

		client.indices().createIndex(request -> request.index(INDEX_I)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		assertThat(syncClient.indices().exists(new GetIndexRequest(INDEX_I), RequestOptions.DEFAULT)).isTrue();
	}

	@Test // DATAES-569
	public void createExistingIndexErrors() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().createIndex(request -> request.index(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-569, DATAES-678
	public void deleteExistingIndex() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().deleteIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		assertThat(syncClient.indices().exists(new GetIndexRequest(INDEX_I), RequestOptions.DEFAULT)).isFalse();
	}

	@Test // DATAES-569, DATAES-767
	public void deleteNonExistingIndexErrors() {

		client.indices().deleteIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-569
	public void openExistingIndex() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().openIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void openNonExistingIndex() {

		client.indices().openIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-569
	public void closeExistingIndex() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().openIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void closeNonExistingIndex() {

		client.indices().closeIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-569
	public void refreshIndex() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().refreshIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void refreshNonExistingIndex() {

		client.indices().refreshIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-569
	public void updateMapping() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		client.indices().updateMapping(request -> request.indices(INDEX_I).type(TYPE_I).source(jsonMap)) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void updateMappingNonExistingIndex() {

		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("message", Collections.singletonMap("type", "text")));

		client.indices().updateMapping(request -> request.indices(INDEX_I).type(TYPE_I).source(jsonMap)) //
				.as(StepVerifier::create) //
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-569
	public void flushIndex() throws IOException {

		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);

		client.indices().flushIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-569, DATAES-767
	public void flushNonExistingIndex() {

		client.indices().flushIndex(request -> request.indices(INDEX_I)) //
				.as(StepVerifier::create) //
				.verifyError(ElasticsearchStatusException.class);
	}

	@Test // DATAES-684
	public void bulkShouldUpdateExistingDocument() {
		String idFirstDoc = addSourceDocument().ofType(TYPE_I).to(INDEX_I);
		String idSecondDoc = addSourceDocument().ofType(TYPE_I).to(INDEX_I);

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
	public void aggregateReturnsAggregationResults() throws IOException {
		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);
		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("firstname", Collections.singletonMap("type", "keyword")));
		syncClient.indices().putMapping(new PutMappingRequest(INDEX_I).source(jsonMap), RequestOptions.DEFAULT);

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
		searchSourceBuilder.aggregation(AggregationBuilders.terms("terms").field("firstname"));

		SearchRequest request = new SearchRequest(INDEX_I) //
				.source(searchSourceBuilder);

		client.aggregate(request).as(StepVerifier::create)
				.expectNextMatches(aggregation -> aggregation.getType().equals(StringTerms.NAME)).verifyComplete();
	}

	@Test // DATAES-866
	public void suggestReturnsSuggestionResults() throws IOException {
		syncClient.indices().create(new CreateIndexRequest(INDEX_I), RequestOptions.DEFAULT);
		Map<String, Object> jsonMap = Collections.singletonMap("properties",
				Collections.singletonMap("firstname", Collections.singletonMap("type", "completion")));
		syncClient.indices().putMapping(new PutMappingRequest(INDEX_I).source(jsonMap), RequestOptions.DEFAULT);

		addSourceDocument().ofType(TYPE_I).to(INDEX_I);

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

	private AddToIndexOfType addSourceDocument() {
		return add(DOC_SOURCE);
	}

	private AddToIndexOfType add(Map<String, ?> source) {
		return new AddDocument(source);
	}

	private IndexRequest indexRequest(Map source, String index, String type) {

		return new IndexRequest(index) //
				.id(UUID.randomUUID().toString()) //
				.source(source) //
				.setRefreshPolicy(RefreshPolicy.IMMEDIATE) //
				.create(true);
	}

	@SneakyThrows
	private String doIndex(Map<?, ?> source, String index, String type) {
		return syncClient.index(indexRequest(source, index, type), RequestOptions.DEFAULT).getId();
	}

	interface AddToIndexOfType extends AddToIndex {
		AddToIndex ofType(String type);
	}

	interface AddToIndex {
		String to(String index);
	}

	class AddDocument implements AddToIndexOfType {

		Map<String, ?> source;
		@Nullable String type;

		AddDocument(Map<String, ?> source) {
			this.source = source;
		}

		@Override
		public AddToIndex ofType(String type) {

			this.type = type;
			return this;
		}

		@Override
		public String to(String index) {
			return doIndex(new LinkedHashMap<>(source), index, type);
		}
	}

}
