/*
 * Copyright 2021-present the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;
/*
 * Copyright 2024-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@TestPropertySource(properties = { "entity.indexName = index-property" })
public abstract class SimpleElasticsearchPersistentEntityIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
	}

	@Test // #3310
	@DisplayName("should evaluate static index name")
	void shouldEvaluateStaticIndexName() {

		var persistentEntity = getRequiredPersistentEntity(EntityWithStaticName.class);

		assertThat(persistentEntity.getIndexCoordinates().getIndexName()).isEqualTo("static-name");
	}

	@Test // #3310
	@DisplayName("should evaluate SpEL index name")
	void shouldEvaluateSpELIndexName() {

		var persistentEntity = getRequiredPersistentEntity(EntityWithSpel.class);

		assertThat(persistentEntity.getIndexCoordinates().getIndexName()).isEqualTo(indexNameProvider.indexName());
	}

	@Test // #3310
	@DisplayName("should evaluate property index name")
	void shouldEvaluatePropertyIndexName() {

		var persistentEntity = getRequiredPersistentEntity(EntityWithProperty.class);

		assertThat(persistentEntity.getIndexCoordinates().getIndexName()).isEqualTo("index-property");
	}

	private ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return operations.getElasticsearchConverter().getMappingContext().getRequiredPersistentEntity(clazz);
	}

	@Document(indexName = "static-name")
	static class EntityWithStaticName {}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class EntityWithSpel {}

	@Document(indexName = "${entity.indexName}")
	static class EntityWithProperty {}

}
