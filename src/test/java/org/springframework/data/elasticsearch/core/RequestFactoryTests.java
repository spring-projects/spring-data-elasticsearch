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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.mockito.Mockito.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
@ExtendWith(MockitoExtension.class)
class RequestFactoryTests {

	@Nullable private static RequestFactory requestFactory;
	@Nullable private static MappingElasticsearchConverter converter;

	@Mock private Client client;

	@BeforeAll
	static void setUpAll() {
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(Person.class, EntityWithSeqNoPrimaryTerm.class)));
		mappingContext.afterPropertiesSet();

		converter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
		converter.afterPropertiesSet();

		requestFactory = new RequestFactory((converter));
	}

	@Test // FPI-734
	void shouldBuildSearchWithGeoSortSort() throws JSONException {
		CriteriaQuery query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
		Sort sort = Sort.by(new GeoDistanceOrder("location", new GeoPoint(49.0, 8.4)));
		query.addSort(sort);

		converter.updateQuery(query, Person.class);

		String expected = '{' + //
				"  \"query\": {" + //
				"    \"bool\": {" + //
				"      \"must\": [" + //
				"        {" + //
				"          \"query_string\": {" + //
				"            \"query\": \"Smith\"," + //
				"            \"fields\": [" + //
				"              \"last-name^1.0\"" + //
				"            ]" + //
				"          }" + //
				"        }" + //
				"      ]" + //
				"    }" + //
				"  }," + //
				"  \"sort\": [" + //
				"    {" + //
				"      \"_geo_distance\": {" + //
				"        \"current-location\": [" + //
				"          {" + //
				"            \"lat\": 49.0," + //
				"            \"lon\": 8.4" + //
				"          }" + //
				"        ]," + //
				"        \"unit\": \"m\"," + //
				"        \"distance_type\": \"arc\"," + //
				"        \"order\": \"asc\"," + //
				"        \"mode\": \"min\"," + //
				"        \"ignore_unmapped\": false" + //
				"      }" + //
				"    }" + //
				"  ]" + //
				'}';

		String searchRequest = requestFactory.searchRequest(query, Person.class, IndexCoordinates.of("persons")).source()
				.toString();

		assertEquals(expected, searchRequest, false);
	}

	@Test // DATAES-449
	void shouldAddRouting() throws JSONException {
		String route = "route66";
		CriteriaQuery query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
		query.setRoute(route);
		converter.updateQuery(query, Person.class);

		SearchRequest searchRequest = requestFactory.searchRequest(query, Person.class, IndexCoordinates.of("persons"));

		assertThat(searchRequest.routing()).isEqualTo(route);
	}

	@Test // DATAES-765
	void shouldAddMaxQueryWindowForUnpagedToRequest() {
		Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withPageable(Pageable.unpaged()).build();

		SearchRequest searchRequest = requestFactory.searchRequest(query, Person.class, IndexCoordinates.of("persons"));

		assertThat(searchRequest.source().from()).isEqualTo(0);
		assertThat(searchRequest.source().size()).isEqualTo(RequestFactory.INDEX_MAX_RESULT_WINDOW);
	}

	@Test // DATAES-765
	void shouldAddMaxQueryWindowForUnpagedToRequestBuilder() {
		when(client.prepareSearch(any())).thenReturn(new SearchRequestBuilder(client, SearchAction.INSTANCE));
		Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withPageable(Pageable.unpaged()).build();

		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, Person.class,
				IndexCoordinates.of("persons"));

		assertThat(searchRequestBuilder.request().source().from()).isEqualTo(0);
		assertThat(searchRequestBuilder.request().source().size()).isEqualTo(RequestFactory.INDEX_MAX_RESULT_WINDOW);
	}

	@Test // DATAES-799
	void shouldIncludeSeqNoAndPrimaryTermFromIndexQueryToIndexRequest() {
		IndexQuery query = new IndexQuery();
		query.setObject(new Person());
		query.setSeqNo(1L);
		query.setPrimaryTerm(2L);

		IndexRequest request = requestFactory.indexRequest(query, IndexCoordinates.of("persons"));

		assertThat(request.ifSeqNo()).isEqualTo(1L);
		assertThat(request.ifPrimaryTerm()).isEqualTo(2L);
	}

	@Test // DATAES-799
	void shouldIncludeSeqNoAndPrimaryTermFromIndexQueryToIndexRequestBuilder() {
		when(client.prepareIndex(anyString(), anyString()))
				.thenReturn(new IndexRequestBuilder(client, IndexAction.INSTANCE));

		IndexQuery query = new IndexQuery();
		query.setObject(new Person());
		query.setSeqNo(1L);
		query.setPrimaryTerm(2L);

		IndexRequestBuilder builder = requestFactory.indexRequestBuilder(client, query, IndexCoordinates.of("persons"));

		assertThat(builder.request().ifSeqNo()).isEqualTo(1L);
		assertThat(builder.request().ifPrimaryTerm()).isEqualTo(2L);
	}

	@Test // DATAES-799
	void shouldNotRequestSeqNoAndPrimaryTermViaSearchRequestWhenEntityClassDoesNotContainSeqNoPrimaryTermProperty() {
		Query query = new NativeSearchQueryBuilder().build();

		SearchRequest request = requestFactory.searchRequest(query, Person.class, IndexCoordinates.of("persons"));

		assertThat(request.source().seqNoAndPrimaryTerm()).isNull();
	}

	@Test // DATAES-799
	void shouldRequestSeqNoAndPrimaryTermViaSearchRequestWhenEntityClassContainsSeqNoPrimaryTermProperty() {
		Query query = new NativeSearchQueryBuilder().build();

		SearchRequest request = requestFactory.searchRequest(query, EntityWithSeqNoPrimaryTerm.class,
				IndexCoordinates.of("seqNoPrimaryTerm"));

		assertThat(request.source().seqNoAndPrimaryTerm()).isTrue();
	}

	@Test // DATAES-799
	void shouldNotRequestSeqNoAndPrimaryTermViaSearchRequestWhenEntityClassIsNull() {
		Query query = new NativeSearchQueryBuilder().build();

		SearchRequest request = requestFactory.searchRequest(query, null, IndexCoordinates.of("persons"));

		assertThat(request.source().seqNoAndPrimaryTerm()).isNull();
	}

	@Test // DATAES-799
	void shouldNotRequestSeqNoAndPrimaryTermViaSearchRequestBuilderWhenEntityClassDoesNotContainSeqNoPrimaryTermProperty() {
		when(client.prepareSearch(any())).thenReturn(new SearchRequestBuilder(client, SearchAction.INSTANCE));
		Query query = new NativeSearchQueryBuilder().build();

		SearchRequestBuilder builder = requestFactory.searchRequestBuilder(client, query, Person.class,
				IndexCoordinates.of("persons"));

		assertThat(builder.request().source().seqNoAndPrimaryTerm()).isNull();
	}

	@Test // DATAES-799
	void shouldRequestSeqNoAndPrimaryTermViaSearchRequestBuilderWhenEntityClassContainsSeqNoPrimaryTermProperty() {
		when(client.prepareSearch(any())).thenReturn(new SearchRequestBuilder(client, SearchAction.INSTANCE));
		Query query = new NativeSearchQueryBuilder().build();

		SearchRequestBuilder builder = requestFactory.searchRequestBuilder(client, query, EntityWithSeqNoPrimaryTerm.class,
				IndexCoordinates.of("seqNoPrimaryTerm"));

		assertThat(builder.request().source().seqNoAndPrimaryTerm()).isTrue();
	}

	@Test // DATAES-799
	void shouldNotRequestSeqNoAndPrimaryTermViaSearchRequestBuilderWhenEntityClassIsNull() {
		when(client.prepareSearch(any())).thenReturn(new SearchRequestBuilder(client, SearchAction.INSTANCE));
		Query query = new NativeSearchQueryBuilder().build();

		SearchRequestBuilder builder = requestFactory.searchRequestBuilder(client, query, null,
				IndexCoordinates.of("persons"));

		assertThat(builder.request().source().seqNoAndPrimaryTerm()).isNull();
	}

	@Test // DATAES-864
	void shouldBuildIndicesAliasRequest() throws IOException, JSONException {

		AliasActions aliasActions = new AliasActions();

		aliasActions.add(new AliasAction.Add(
				AliasActionParameters.builder().withIndices("index1", "index2").withAliases("alias1").build()));
		aliasActions.add(
				new AliasAction.Remove(AliasActionParameters.builder().withIndices("index3").withAliases("alias1").build()));

		aliasActions.add(new AliasAction.RemoveIndex(AliasActionParameters.builder().withIndices("index3").build()));

		aliasActions.add(new AliasAction.Add(AliasActionParameters.builder().withIndices("index4").withAliases("alias4")
				.withRouting("routing").withIndexRouting("indexRouting").withSearchRouting("searchRouting").withIsHidden(true)
				.withIsWriteIndex(true).build()));

		Query query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
		aliasActions.add(new AliasAction.Add(AliasActionParameters.builder().withIndices("index5").withAliases("alias5")
				.withFilterQuery(query, Person.class).build()));

		String expected = "{\n" + //
				"  \"actions\": [\n" + //
				"    {\n" + //
				"      \"add\": {\n" + //
				"        \"indices\": [\n" + //
				"          \"index1\",\n" + //
				"          \"index2\"\n" + //
				"        ],\n" + //
				"        \"aliases\": [\n" + //
				"          \"alias1\"\n" + //
				"        ]\n" + //
				"      }\n" + //
				"    },\n" + //
				"    {\n" + //
				"      \"remove\": {\n" + //
				"        \"indices\": [\n" + //
				"          \"index3\"\n" + //
				"        ],\n" + //
				"        \"aliases\": [\n" + //
				"          \"alias1\"\n" + //
				"        ]\n" + //
				"      }\n" + //
				"    },\n" + //
				"    {\n" + //
				"      \"remove_index\": {\n" + //
				"        \"indices\": [\n" + //
				"          \"index3\"\n" + //
				"        ]\n" + //
				"      }\n" + //
				"    },\n" + //
				"    {\n" + //
				"      \"add\": {\n" + //
				"        \"indices\": [\n" + //
				"          \"index4\"\n" + //
				"        ],\n" + //
				"        \"aliases\": [\n" + //
				"          \"alias4\"\n" + //
				"        ],\n" + //
				"        \"routing\": \"routing\",\n" + //
				"        \"index_routing\": \"indexRouting\",\n" + //
				"        \"search_routing\": \"searchRouting\",\n" + //
				"        \"is_write_index\": true,\n" + //
				"        \"is_hidden\": true\n" + //
				"      }\n" + //
				"    },\n" + //
				"    {\n" + //
				"      \"add\": {\n" + //
				"        \"indices\": [\n" + //
				"          \"index5\"\n" + //
				"        ],\n" + //
				"        \"aliases\": [\n" + //
				"          \"alias5\"\n" + //
				"        ],\n" + //
				"        \"filter\": {\n" + //
				"          \"bool\": {\n" + //
				"            \"must\": [\n" + //
				"              {\n" + //
				"                \"query_string\": {\n" + //
				"                  \"query\": \"Smith\",\n" + //
				"                  \"fields\": [\n" + //
				"                    \"last-name^1.0\"\n" + //
				"                  ],\n" + //
				"                  \"type\": \"best_fields\",\n" + //
				"                  \"default_operator\": \"and\",\n" + //
				"                  \"max_determinized_states\": 10000,\n" + //
				"                  \"enable_position_increments\": true,\n" + //
				"                  \"fuzziness\": \"AUTO\",\n" + //
				"                  \"fuzzy_prefix_length\": 0,\n" + //
				"                  \"fuzzy_max_expansions\": 50,\n" + //
				"                  \"phrase_slop\": 0,\n" + //
				"                  \"escape\": false,\n" + //
				"                  \"auto_generate_synonyms_phrase_query\": true,\n" + //
				"                  \"fuzzy_transpositions\": true,\n" + //
				"                  \"boost\": 1.0\n" + //
				"                }\n" + //
				"              }\n" + //
				"            ],\n" + //
				"            \"adjust_pure_negative\": true,\n" + //
				"            \"boost\": 1.0\n" + //
				"          }\n" + //
				"        }\n" + //
				"      }\n" + //
				"    }\n" + //
				"  ]\n" + //
				"}"; //

		IndicesAliasesRequest indicesAliasesRequest = requestFactory.indicesAliasesRequest(aliasActions);

		String json = requestToString(indicesAliasesRequest);

		assertEquals(expected, json, false);
	}

	@Test // DATAES-612
	void shouldCreatePutIndexTemplateRequest() throws JSONException, IOException {

		String expected = "{\n" + //
				"  \"index_patterns\": [\n" + //
				"    \"test-*\"\n" + //
				"  ],\n" + //
				"  \"order\": 42,\n" + //
				"  \"version\": 7,\n" + //
				"  \"settings\": {\n" + //
				"    \"index\": {\n" + //
				"      \"number_of_replicas\": \"2\",\n" + //
				"      \"number_of_shards\": \"3\",\n" + //
				"      \"refresh_interval\": \"7s\",\n" + //
				"      \"store\": {\n" + //
				"        \"type\": \"oops\"\n" + //
				"      }\n" + //
				"    }\n" + //
				"  },\n" + //
				"  \"mappings\": {\n" + //
				"    \"properties\": {\n" + //
				"      \"price\": {\n" + //
				"        \"type\": \"double\"\n" + //
				"      }\n" + //
				"    }\n" + //
				"  },\n" + //
				"  \"aliases\":{\n" + //
				"    \"alias1\": {},\n" + //
				"    \"alias2\": {},\n" + //
				"    \"alias3\": {\n" + //
				"      \"routing\": \"11\"\n" + //
				"    }\n" + //
				"  }\n" + //
				"}\n"; //

		org.springframework.data.elasticsearch.core.document.Document settings = org.springframework.data.elasticsearch.core.document.Document
				.create();
		settings.put("index.number_of_replicas", 2);
		settings.put("index.number_of_shards", 3);
		settings.put("index.refresh_interval", "7s");
		settings.put("index.store.type", "oops");

		org.springframework.data.elasticsearch.core.document.Document mappings = org.springframework.data.elasticsearch.core.document.Document
				.parse("{\"properties\":{\"price\":{\"type\":\"double\"}}}");

		AliasActions aliasActions = new AliasActions(
				new AliasAction.Add(AliasActionParameters.builderForTemplate().withAliases("alias1", "alias2").build()),
				new AliasAction.Add(
						AliasActionParameters.builderForTemplate().withAliases("alias3").withRouting("11").build()));

		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder("test-template", "test-*") //
				.withSettings(settings) //
				.withMappings(mappings) //
				.withAliasActions(aliasActions) //
				.withOrder(42) //
				.withVersion(7) //
				.build(); //

		PutIndexTemplateRequest putIndexTemplateRequest = requestFactory.putIndexTemplateRequest(putTemplateRequest);

		String json = requestToString(putIndexTemplateRequest);

		assertEquals(expected, json, false);
	}

	@Test // DATAES-247
	@DisplayName("should set op_type INDEX if not specified")
	void shouldSetOpTypeIndexIfNotSpecifiedAndIdIsSet() {

		IndexQuery indexQuery = new IndexQueryBuilder().withId("42").withObject(Person.builder().id("42").lastName("Smith"))
				.build();

		IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of("optype"));

		assertThat(indexRequest.opType()).isEqualTo(DocWriteRequest.OpType.INDEX);
	}

	@Test // DATAES-247
	@DisplayName("should set op_type CREATE if specified")
	void shouldSetOpTypeCreateIfSpecified() {

		IndexQuery indexQuery = new IndexQueryBuilder().withOpType(IndexQuery.OpType.CREATE).withId("42")
				.withObject(Person.builder().id("42").lastName("Smith")).build();

		IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of("optype"));

		assertThat(indexRequest.opType()).isEqualTo(DocWriteRequest.OpType.CREATE);
	}

	@Test // DATAES-247
	@DisplayName("should set op_type INDEX if specified")
	void shouldSetOpTypeIndexIfSpecified() {

		IndexQuery indexQuery = new IndexQueryBuilder().withOpType(IndexQuery.OpType.INDEX).withId("42")
				.withObject(Person.builder().id("42").lastName("Smith")).build();

		IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of("optype"));

		assertThat(indexRequest.opType()).isEqualTo(DocWriteRequest.OpType.INDEX);
	}

	@Test // DATAES-1003
	@DisplayName("should set timeout to request")
	void shouldSetTimeoutToRequest() {
		Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withTimeout(TimeValue.timeValueSeconds(1))
				.build();

		SearchRequest searchRequest = requestFactory.searchRequest(query, Person.class, IndexCoordinates.of("persons"));

		assertThat(searchRequest.source().timeout()).isEqualTo(TimeValue.timeValueSeconds(1));
	}

	@Test // DATAES-1003
	@DisplayName("should set timeout to requestbuilder")
	void shouldSetTimeoutToRequestBuilder() {
		when(client.prepareSearch(any())).thenReturn(new SearchRequestBuilder(client, SearchAction.INSTANCE));
		Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withTimeout(TimeValue.timeValueSeconds(1))
				.build();

		SearchRequestBuilder searchRequestBuilder = requestFactory.searchRequestBuilder(client, query, Person.class,
				IndexCoordinates.of("persons"));

		assertThat(searchRequestBuilder.request().source().timeout()).isEqualTo(TimeValue.timeValueSeconds(1));
	}

	private String requestToString(ToXContent request) throws IOException {
		return XContentHelper.toXContent(request, XContentType.JSON, true).utf8ToString();
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	static class Person {
		@Nullable @Id String id;
		@Nullable @Field(name = "last-name") String lastName;
		@Nullable @Field(name = "current-location") GeoPoint location;
	}

	static class EntityWithSeqNoPrimaryTerm {
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;
	}
}
