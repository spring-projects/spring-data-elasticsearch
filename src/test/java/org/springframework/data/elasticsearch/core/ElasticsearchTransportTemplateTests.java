/*
 * Copyright 2018-2021 the original author or authors.
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
import static org.skyscreamer.jsonassert.JSONAssert.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import java.lang.Object;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.reindex.UpdateByQueryRequestBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndicesOptions;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author Farid Faoudi
 */
@ContextConfiguration(classes = { ElasticsearchTransportTemplateTests.Config.class })
@DisplayName("ElasticsearchTransportTemplate")
public class ElasticsearchTransportTemplateTests extends ElasticsearchTemplateTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("transport-template");
		}
	}

	@Autowired private Client client;

	@Test
	public void shouldThrowExceptionIfDocumentDoesNotExistWhileDoingPartialUpdate() {
		// when
		org.springframework.data.elasticsearch.core.document.Document document = org.springframework.data.elasticsearch.core.document.Document
				.create();
		UpdateQuery updateQuery = UpdateQuery.builder(nextIdAsString()).withDocument(document).build();
		assertThatThrownBy(() -> operations.update(updateQuery, IndexCoordinates.of(indexNameProvider.indexName())))
				.isInstanceOf(DocumentMissingException.class);
	}

	@Override
	@Test // DATAES-187
	public void shouldUsePageableOffsetToSetFromInSearchRequest() {

		// given
		Pageable pageable = new PageRequest(1, 10, Sort.unsorted()) {
			@Override
			public long getOffset() {
				return 30;
			}
		};

		NativeSearchQuery query = new NativeSearchQueryBuilder() //
				.withPageable(pageable) //
				.build();

		// when
		SearchRequestBuilder searchRequestBuilder = getRequestFactory().searchRequestBuilder(client, query, null,
				IndexCoordinates.of("test"));

		// then
		assertThat(searchRequestBuilder.request().source().from()).isEqualTo(30);
	}

	@Test // DATAES-768
	void shouldUseAllOptionsFromUpdateQuery() {
		Map<String, Object> doc = new HashMap<>();
		doc.put("id", "1");
		doc.put("message", "test");
		org.springframework.data.elasticsearch.core.document.Document document = org.springframework.data.elasticsearch.core.document.Document
				.from(doc);
		UpdateQuery updateQuery = UpdateQuery.builder("1") //
				.withDocument(document) //
				.withIfSeqNo(42) //
				.withIfPrimaryTerm(13) //
				.withScript("script")//
				.withLang("lang") //
				.withRefreshPolicy(RefreshPolicy.WAIT_UNTIL) //
				.withRetryOnConflict(7) //
				.withTimeout("4711s") //
				.withWaitForActiveShards("all").withFetchSourceIncludes(Collections.singletonList("incl")) //
				.withFetchSourceExcludes(Collections.singletonList("excl")) //
				.build();

		UpdateRequestBuilder request = getRequestFactory().updateRequestBuilderFor(client, updateQuery,
				IndexCoordinates.of("index"));

		assertThat(request).isNotNull();
		assertThat(request.request().ifSeqNo()).isEqualTo(42);
		assertThat(request.request().ifPrimaryTerm()).isEqualTo(13);
		assertThat(request.request().script().getIdOrCode()).isEqualTo("script");
		assertThat(request.request().script().getLang()).isEqualTo("lang");
		assertThat(request.request().getRefreshPolicy()).isEqualByComparingTo(WriteRequest.RefreshPolicy.WAIT_UNTIL);
		assertThat(request.request().retryOnConflict()).isEqualTo(7);
		assertThat(request.request().timeout()).isEqualByComparingTo(TimeValue.parseTimeValue("4711s", "test"));
		assertThat(request.request().waitForActiveShards()).isEqualTo(ActiveShardCount.ALL);
		FetchSourceContext fetchSourceContext = request.request().fetchSource();
		assertThat(fetchSourceContext).isNotNull();
		assertThat(fetchSourceContext.includes()).containsExactlyInAnyOrder("incl");
		assertThat(fetchSourceContext.excludes()).containsExactlyInAnyOrder("excl");
	}

	@Test // DATAES-782
	void shouldProvideClient() {
		Client client = ((ElasticsearchTemplate) operations).getClient();

		assertThat(client).isNotNull();
	}

	@Test // #1446
	void shouldUseAllOptionsFromUpdateByQuery() throws JSONException {

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()) //
				.withIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN) //
				.build(); //
		searchQuery.setScrollTime(Duration.ofMillis(1000));

		UpdateQuery updateQuery = UpdateQuery.builder(searchQuery) //
				.withAbortOnVersionConflict(true) //
				.withBatchSize(10) //
				.withMaxDocs(12) //
				.withMaxRetries(3) //
				.withPipeline("pipeline") //
				.withRequestsPerSecond(5F) //
				.withShouldStoreResult(false) //
				.withSlices(4) //
				.withScriptType(ScriptType.STORED) //
				.withScriptName("script_name") //
				.build(); //

		String expectedSearchRequest = '{' + //
				"  \"size\": 10," + //
				"  \"query\": {" + //
				"    \"match_all\": {" + //
				"      \"boost\": 1.0" + //
				"    }" + //
				"  }" + //
				'}'; //

		// when
		UpdateByQueryRequestBuilder request = getRequestFactory().updateByQueryRequestBuilder(client, updateQuery,
				IndexCoordinates.of("index"));

		// then
		assertThat(request).isNotNull();
		assertThat(request.request().getSearchRequest().indicesOptions()).usingRecursiveComparison()
				.isEqualTo(IndicesOptions.LENIENT_EXPAND_OPEN);
		assertThat(request.request().getScrollTime().getMillis()).isEqualTo(1000);
		assertEquals(request.request().getSearchRequest().source().toString(), expectedSearchRequest, false);
		assertThat(request.request().isAbortOnVersionConflict()).isTrue();
		assertThat(request.request().getBatchSize()).isEqualTo(10);
		assertThat(request.request().getMaxDocs()).isEqualTo(12);
		assertThat(request.request().getPipeline()).isEqualTo("pipeline");
		assertThat(request.request().getRequestsPerSecond()).isEqualTo(5F);
		assertThat(request.request().getShouldStoreResult()).isFalse();
		assertThat(request.request().getSlices()).isEqualTo(4);
		assertThat(request.request().getScript().getIdOrCode()).isEqualTo("script_name");
		assertThat(request.request().getScript().getType()).isEqualTo(org.elasticsearch.script.ScriptType.STORED);
	}

	@Document(indexName = "test-index-sample-core-transport-template")
	static class SampleEntity {
		@Nullable @Id private String id;
		@Nullable @Field(type = Text, store = true, fielddata = true) private String type;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}
}
