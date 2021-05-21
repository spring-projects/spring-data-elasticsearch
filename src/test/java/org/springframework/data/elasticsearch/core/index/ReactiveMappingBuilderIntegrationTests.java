/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveElasticsearchRestTemplateConfiguration.class })
public class ReactiveMappingBuilderIntegrationTests {

	@Autowired private ReactiveElasticsearchOperations operations;

	@Test // #1822
	@DisplayName("should write runtime fields")
	void shouldWriteRuntimeFields() {

		ReactiveIndexOperations indexOps = operations.indexOps(RuntimeFieldEntity.class);

		indexOps.create().block();
		indexOps.putMapping().block();
		indexOps.delete().block();
	}

	// region entities
	@Document(indexName = "runtime-fields")
	@Mapping(runtimeFieldsPath = "/mappings/runtime-fields.json")
	private static class RuntimeFieldEntity {
		@Id @Nullable private String id;
		@Field(type = Date, format = DateFormat.epoch_millis, name = "@timestamp") @Nullable private Instant timestamp;
	}
	// endregion
}
