/*
 * Copyright 2024 the original author or authors.
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * Tests that need to check the data produced by the Elasticsearch client
 * @author Peter-Josef Meisch
 */
@SuppressWarnings("UastIncorrectHttpHeaderInspection")
@ExtendWith(SpringExtension.class)
public class ELCWiremockTests {

	@RegisterExtension static WireMockExtension wireMock = WireMockExtension.newInstance()
			.options(wireMockConfig()
					.dynamicPort()
					// needed, otherwise Wiremock goes to test/resources/mappings
					.usingFilesUnderDirectory("src/test/resources/wiremock-mappings"))
			.build();

	@Configuration
	static class Config extends ElasticsearchConfiguration {
		@Override
		public ClientConfiguration clientConfiguration() {
			return ClientConfiguration.builder()
					.connectedTo("localhost:" + wireMock.getPort())
					.build();
		}
	}

	@Autowired ElasticsearchOperations operations;

	@Test // #2839
	@DisplayName("should store null values if configured")
	void shouldStoreNullValuesIfConfigured() {

		wireMock.stubFor(put(urlPathEqualTo("/null-fields/_doc/42"))
				.withRequestBody(equalToJson("""
						{
							"_class": "org.springframework.data.elasticsearch.client.elc.ELCWiremockTests$EntityWithNullFields",
							"id": "42",
							"field1": null
							}
						"""))
				.willReturn(
						aResponse()
								.withStatus(200)
								.withHeader("X-elastic-product", "Elasticsearch")
								.withHeader("content-type", "application/vnd.elasticsearch+json;compatible-with=8")
								.withBody("""
										{
										  "_index": "null-fields",
										  "_id": "42",
										  "_version": 1,
										  "result": "created",
										  "forced_refresh": true,
										  "_shards": {
										    "total": 2,
										    "successful": 1,
										    "failed": 0
										  },
										  "_seq_no": 1,
										  "_primary_term": 1
										}
										""")));

		var entity = new EntityWithNullFields();
		entity.setId("42");

		operations.save(entity);
		// no need to assert anything, if the field1:null is not sent, we run into a 404 error
	}

	@Document(indexName = "null-fields")
	static class EntityWithNullFields {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(storeNullValue = true) private String field1;
		@Nullable
		@Field private String field2;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getField1() {
			return field1;
		}

		public void setField1(@Nullable String field1) {
			this.field1 = field1;
		}

		@Nullable
		public String getField2() {
			return field2;
		}

		public void setField2(@Nullable String field2) {
			this.field2 = field2;
		}
	}
}
