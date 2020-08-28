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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import lombok.Data;
import lombok.val;

import java.lang.Object;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
@DisplayName("ElasticsearchTransportTemplate")
public class ElasticsearchTransportTemplateTests extends ElasticsearchTemplateTests {

	@Autowired private Client client;

	@Test
	public void shouldThrowExceptionIfDocumentDoesNotExistWhileDoingPartialUpdate() {
		// when
		org.springframework.data.elasticsearch.core.document.Document document = org.springframework.data.elasticsearch.core.document.Document
				.create();
		UpdateQuery updateQuery = UpdateQuery.builder(nextIdAsString()).withDocument(document).build();
		assertThatThrownBy(() -> operations.update(updateQuery, index)).isInstanceOf(DocumentMissingException.class);
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
				.withRefresh(UpdateQuery.Refresh.Wait_For) //
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
		val fetchSourceContext = request.request().fetchSource();
		assertThat(fetchSourceContext).isNotNull();
		assertThat(fetchSourceContext.includes()).containsExactlyInAnyOrder("incl");
		assertThat(fetchSourceContext.excludes()).containsExactlyInAnyOrder("excl");
	}

	@Test // DATAES-782
	void shouldProvideClient() {
		Client client = ((ElasticsearchTemplate) operations).getClient();

		assertThat(client).isNotNull();
	}

	@Data
	@Document(indexName = "test-index-sample-core-transport-template", replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
	}
}
