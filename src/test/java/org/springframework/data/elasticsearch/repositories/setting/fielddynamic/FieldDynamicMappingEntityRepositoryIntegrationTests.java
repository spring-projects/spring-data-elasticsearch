/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.setting.fielddynamic;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;

/**
 * FieldDynamicMappingEntityRepositoryIntegrationTests
 *
 * @author Ted Liang
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class FieldDynamicMappingEntityRepositoryIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(FieldDynamicMappingEntity.class).createWithMapping();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // DATAES-209
	public void shouldCreateMappingWithMappingAnnotationAtFieldLevel() {

		// given

		// then
		Map<String, Object> mapping = operations.indexOps(FieldDynamicMappingEntity.class).getMapping();
		assertThat(mapping).isNotNull();

		Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
		assertThat(properties).isNotNull();

		assertThat(properties.containsKey("file")).isTrue();
		Map<String, Object> file = (Map<String, Object>) properties.get("file");
		assertThat(file).isNotNull();
		assertThat(file.get("type")).isEqualTo("text");

		assertThat(file.containsKey("fields")).isTrue();
		Map<String, Object> fields = (Map<String, Object>) file.get("fields");
		assertThat(fields).isNotNull();

		assertThat(fields.containsKey("content")).isTrue();
		Map<String, Object> content = (Map<String, Object>) fields.get("content");
		assertThat(content).isNotNull();

		assertThat(content.get("type")).isEqualTo("text");
		assertThat(content.get("term_vector")).isEqualTo("with_positions_offsets");
		assertThat((Boolean) content.get("store")).isTrue();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class FieldDynamicMappingEntity {

		@Id private String id;

		@Mapping(mappingPath = "/mappings/test-field-mappings.json") private byte[] file;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public byte[] getFile() {
			return file;
		}

		public void setFile(byte[] file) {
			this.file = file;
		}
	}

	/**
	 * @author Ted Liang
	 */
	public interface FieldDynamicMappingEntityRepository
			extends ElasticsearchRepository<FieldDynamicMappingEntity, String> {}

}
