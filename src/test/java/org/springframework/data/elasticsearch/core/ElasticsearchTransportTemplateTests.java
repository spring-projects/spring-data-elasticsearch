/*
 * Copyright 2018-2019 the original author or authors.
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

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.Data;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.DocumentMissingException;
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
import org.springframework.data.elasticsearch.core.query.UpdateQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
public class ElasticsearchTransportTemplateTests extends ElasticsearchTemplateTests {

	@Autowired private Client client;

	@Test
	public void shouldThrowExceptionIfDocumentDoesNotExistWhileDoingPartialUpdate() {
		// when
		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("{}", XContentType.JSON);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(randomNumeric(5)).withIndexRequest(indexRequest).build();
		assertThatThrownBy(() -> {
			operations.update(updateQuery, index);
		}).isInstanceOf(DocumentMissingException.class);
	}

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

	@Data
	@Document(indexName = "test-index-sample-core-transport-template", type = "test-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
	}
}
