/*
 * Copyright 2023-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ActiveShardCount;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.DocValueField;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

/**
 * @author Peter-Josef Meisch
 * @author Han Seungwoo
 * @author Steven Pearce
 * @author Ralph Ursprung
 */
class RequestConverterTest {

	private static final SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
	private static final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
	private final JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();
	private final RequestConverter requestConverter = new RequestConverter(converter, jsonpMapper);

	@Test // #2316
	@DisplayName("should add docvalue_fields")
	void shouldAddDocvalueFields() {

		var docValueFields = List.of( //
				new DocValueField("field1"), //
				new DocValueField("field2", "format2") //
		);
		// doesn't matter what type of query is used, the relevant part for docvalue_fields is in the base builder.
		var query = StringQuery.builder("""
				{
					"match_all":{}
				}
				""") //
				.withDocValueFields(docValueFields) //
				.build();

		var searchRequest = requestConverter.searchRequest(query, null, SampleEntity.class, IndexCoordinates.of("foo"),
				true);

		var fieldAndFormats = searchRequest.docvalueFields();
		assertThat(fieldAndFormats).hasSize(2);
		assertThat(fieldAndFormats.get(0).field()).isEqualTo("field1");
		assertThat(fieldAndFormats.get(0).format()).isNull();
		assertThat(fieldAndFormats.get(1).field()).isEqualTo("field2");
		assertThat(fieldAndFormats.get(1).format()).isEqualTo("format2");
	}

	@Test // #2973
	@DisplayName("should set refresh based on deleteRequest")
	void refreshSetByDeleteRequest() {
		var query = new CriteriaQuery(new Criteria("text").contains("test"));
		var deleteQuery = DeleteQuery.builder(query).withRefresh(true).build();

		var deleteByQueryRequest = requestConverter.documentDeleteByQueryRequest(deleteQuery, null, SampleEntity.class,
				IndexCoordinates.of("foo"),
				null);

		assertThat(deleteByQueryRequest.refresh()).isTrue();
	}

	@Document(indexName = "does-not-matter")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String text;
	}

	@Test // #3231
	@DisplayName("should not use updatequery id for script id")
	void shouldNotUseUpdatequeryIdForScriptId() {
		var updateQuery = UpdateQuery
				.builder("queryId")
				.withScript("script")
				.build();

		UpdateRequest<org.springframework.data.elasticsearch.core.document.Document, ?> updateRequest = requestConverter
				.documentUpdateRequest(updateQuery, IndexCoordinates.of("foo"), null, null);

		assertThat(updateRequest.id()).isEqualTo("queryId");
		assertThat(updateRequest.script().source().scriptString()).isEqualTo("script");
		assertThat(updateRequest.script().id()).isNull();
	}

	@Test // #3231
	@DisplayName("should use script name as update query id")
	void shouldUseScriptNameAsUpdateQueryId() {
		var updateQuery = UpdateQuery
				.builder("queryId")
				.withScript("script")
				.withScriptName("scriptName")
				.build();

		UpdateRequest<org.springframework.data.elasticsearch.core.document.Document, ?> updateRequest = requestConverter
				.documentUpdateRequest(updateQuery, IndexCoordinates.of("foo"), null, null);

		assertThat(updateRequest.id()).isEqualTo("queryId");
		assertThat(updateRequest.script()).isNotNull();
		assertThat(updateRequest.script().source().scriptString()).isEqualTo("script");
		assertThat(updateRequest.script().id()).isEqualTo("scriptName");
	}

	@Test // #3324
	@DisplayName("should allow update script with only an id and no script code")
	void shouldAllowUpdateScriptWithOnlyAnIdAndNoScriptCode() {
		var updateQuery = UpdateQuery
				.builder("queryId")
				.withScriptName("scriptName")
				.build();

		UpdateRequest<org.springframework.data.elasticsearch.core.document.Document, ?> updateRequest = requestConverter
				.documentUpdateRequest(updateQuery, IndexCoordinates.of("foo"), null, null);

		assertThat(updateRequest.id()).isEqualTo("queryId");
		assertThat(updateRequest.script()).isNotNull();
		assertThat(updateRequest.script().source()).isNull();
		assertThat(updateRequest.script().id()).isEqualTo("scriptName");
	}

	@Test // #2071
	@DisplayName("should convert bulk queries to operations in the given order")
	void shouldConvertBulkQueriesToOperationsInTheGivenOrder() {

		final var entity = new SampleEntity();
		final var indexQuery = new IndexQueryBuilder().withId("1").withObject(entity).build();
		final var createQuery = new IndexQueryBuilder().withId("2").withSource("{}")
				.withOpType(IndexQuery.OpType.CREATE).withIndex("other").build();
		final var updateQuery = UpdateQuery.builder("3").withDocument(
				org.springframework.data.elasticsearch.core.document.Document.create()).build();

		final var bulkRequest = requestConverter.documentBulkRequest(List.of(indexQuery, createQuery, updateQuery),
				BulkOptions.defaultOptions(), IndexCoordinates.of("foo"), null);

		final var operations = bulkRequest.operations();
		assertThat(operations).hasSize(3);
		assertThat(operations.get(0).isIndex()).isTrue();
		assertThat(operations.get(0).index().id()).isEqualTo("1");
		assertThat(operations.get(0).index().index()).isEqualTo("foo");
		assertThat(operations.get(1).isCreate()).isTrue();
		assertThat(operations.get(1).create().id()).isEqualTo("2");
		assertThat(operations.get(1).create().index()).isEqualTo("other");
		assertThat(operations.get(2).isUpdate()).isTrue();
		assertThat(operations.get(2).update().id()).isEqualTo("3");
		assertThat(operations.get(2).update().index()).isEqualTo("foo");
	}

	@Test // #2071
	@DisplayName("should reject unsupported query type for bulk operation")
	void shouldRejectUnsupportedQueryTypeForBulkOperation() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> requestConverter.bulkOperation("not a query", IndexCoordinates.of("foo")));
	}

	@Test // #2071
	@DisplayName("should use default refresh policy for bulk request if not set in options")
	void shouldUseDefaultRefreshPolicyForBulkRequestIfNotSetInOptions() {

		final var bulkRequest = requestConverter.bulkRequestBuilder(BulkOptions.defaultOptions(), RefreshPolicy.IMMEDIATE)
				.operations(List.of()).build();

		assertThat(bulkRequest.refresh()).isEqualTo(Refresh.True);
	}

	@Test // #2071
	@DisplayName("should apply bulk options to bulk request settings")
	void shouldApplyBulkOptionsToBulkRequestSettings() {

		final var bulkOptions = BulkOptions.builder() //
				.withTimeout(Duration.ofSeconds(2)) //
				.withRefreshPolicy(RefreshPolicy.WAIT_UNTIL) //
				.withWaitForActiveShards(new ActiveShardCount(2)) //
				.withPipeline("pipeline") //
				.withRoutingId("routing") //
				.build();

		final var bulkRequest = requestConverter.bulkRequestBuilder(bulkOptions, RefreshPolicy.IMMEDIATE)
				.operations(List.of()).build();

		assertThat(bulkRequest.timeout().time()).isEqualTo("2000ms");
		assertThat(bulkRequest.refresh()).isEqualTo(Refresh.WaitFor);
		assertThat(bulkRequest.waitForActiveShards().count()).isEqualTo(2);
		assertThat(bulkRequest.pipeline()).isEqualTo("pipeline");
		assertThat(bulkRequest.routing()).containsExactly("routing");
		assertThat(bulkRequest.operations()).isEmpty();
	}

	@Test // #3345
	@DisplayName("should create one bulk request if no limits are set")
	void shouldCreateOneBulkRequestIfNoLimitsAreSet() {

		final var bulkRequests = requestConverter.documentBulkRequests(sourceIndexQueries(5),
				BulkOptions.defaultOptions(), BulkOptions.defaultOptions(), IndexCoordinates.of("foo"),
				RefreshPolicy.IMMEDIATE);

		assertThat(bulkRequests).hasSize(1);
		assertThat(bulkRequests.get(0).operations()).hasSize(5);
		assertThat(bulkRequests.get(0).refresh()).isEqualTo(Refresh.True);
	}

	@Test // #3345
	@DisplayName("should split bulk requests and refresh only on the last one")
	void shouldSplitBulkRequestsAndRefreshOnlyOnTheLastOne() {

		final var bulkOptions = BulkOptions.builder() //
				.withMaxOperations(2) //
				.withRefreshPolicy(RefreshPolicy.WAIT_UNTIL) //
				.withPipeline("pipeline") //
				.build();

		final var bulkRequests = requestConverter.documentBulkRequests(sourceIndexQueries(5), bulkOptions,
				BulkOptions.defaultOptions(), IndexCoordinates.of("foo"), RefreshPolicy.IMMEDIATE);

		assertThat(bulkRequests).hasSize(3);
		assertThat(bulkRequests) //
				.flatExtracting(BulkRequest::operations) //
				.extracting(operation -> operation.index().id()) //
				.containsExactly("0", "1", "2", "3", "4");
		assertThat(bulkRequests).extracting(BulkRequest::refresh) //
				.containsExactly(Refresh.False, Refresh.False, Refresh.WaitFor);
		assertThat(bulkRequests).extracting(BulkRequest::pipeline).containsOnly("pipeline");
	}

	@Test // #3345
	@DisplayName("should use the limits of the default bulk options if not set in the options")
	void shouldUseTheLimitsOfTheDefaultBulkOptionsIfNotSetInTheOptions() {

		final var defaultBulkOptions = BulkOptions.builder().withMaxOperations(2).build();

		final var withDefaultLimit = requestConverter.documentBulkRequests(sourceIndexQueries(5),
				BulkOptions.defaultOptions(), defaultBulkOptions, IndexCoordinates.of("foo"), null);
		final var withOwnLimit = requestConverter.documentBulkRequests(sourceIndexQueries(5),
				BulkOptions.builder().withMaxOperations(4).build(), defaultBulkOptions, IndexCoordinates.of("foo"), null);

		assertThat(withDefaultLimit).extracting(bulkRequest -> bulkRequest.operations().size()).containsExactly(2, 2, 1);
		assertThat(withOwnLimit).extracting(bulkRequest -> bulkRequest.operations().size()).containsExactly(4, 1);
	}

	private static List<IndexQuery> sourceIndexQueries(int count) {
		return IntStream.range(0, count) //
				.mapToObj(i -> new IndexQueryBuilder().withId(String.valueOf(i)).withSource("{\"value\":" + i + '}').build()) //
				.toList();
	}

	@Test
	void getRouting() {

		assertTrue(requestConverter.getRouting(null).isEmpty());

		assertTrue(requestConverter.getRouting(null, null).isEmpty());
		assertTrue(requestConverter.getRouting("", null).isEmpty());

		assertEquals("1", requestConverter.getRouting("1", null).get());

		assertEquals("5", requestConverter.getRouting(null, "5").get());

	}
}
