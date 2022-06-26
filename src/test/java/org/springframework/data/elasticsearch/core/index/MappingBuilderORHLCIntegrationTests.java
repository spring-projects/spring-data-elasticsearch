/*
 * Copyright 2020-2022 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.EnabledIfOpensearch;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 */
@EnabledIfOpensearch
@ContextConfiguration(classes = { MappingBuilderORHLCIntegrationTests.Config.class })
public class MappingBuilderORHLCIntegrationTests extends MappingBuilderIntegrationTests {

	@Configuration
	@Import({ OpensearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("mappingbuilder-os");
		}
	}

	@Ignore
	@Override
	void shouldWriteDenseVectorFieldMapping() {
		// see please https://github.com/opensearch-project/OpenSearch/pull/3659
	}

	@Ignore
	@Override
	void shouldWriteRuntimeFields() {
		// Not supported by Opensearch
	}

	@Ignore
	@Override
	void shouldWriteWildcardFieldMapping() {
		// Not supported by Opensearch
	}

	@Override
	void shouldWriteDynamicMapping() {
		IndexOperations indexOps = operations.indexOps(DynamicMappingEntity.class);
		indexOps.createWithMapping();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}", dynamic = Dynamic.FALSE)
	static class DynamicMappingEntity {

		@Nullable
		@Field(type = FieldType.Object) //
		private Map<String, Object> objectInherit;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.FALSE) //
		private Map<String, Object> objectFalse;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.STRICT) //
		private Map<String, Object> objectStrict;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.RUNTIME) //
		private Map<String, Object> objectRuntime;
		@Nullable
		@Field(type = FieldType.Nested) //
		private List<Map<String, Object>> nestedObjectInherit;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.FALSE) //
		private List<Map<String, Object>> nestedObjectFalse;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.TRUE) //
		private List<Map<String, Object>> nestedObjectTrue;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.STRICT) //
		private List<Map<String, Object>> nestedObjectStrict;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.RUNTIME) //
		private List<Map<String, Object>> nestedObjectRuntime;
	}
}
