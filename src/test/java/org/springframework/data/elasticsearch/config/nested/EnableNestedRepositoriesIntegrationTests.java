/*
 * Copyright 2015-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.config.nested;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.lang.Double;
import java.lang.Long;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.repository.Repository;
import org.springframework.lang.Nullable;

/**
 * @author Kevin Leturc
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class EnableNestedRepositoriesIntegrationTests {

	@Autowired(required = false) private SampleRepository nestedRepository;
	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).delete();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test
	public void hasNestedRepository() {
		assertThat(nestedRepository).isNotNull();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Nullable
		@Field(type = Text, store = true, fielddata = true) private String message;
		@Nullable private int rate;
		@Nullable
		@ScriptedField private Double scriptedRate;
		@Nullable private boolean available;
		@Nullable private String highlightedMessage;
		@Nullable private GeoPoint location;
		@Nullable
		@Version private Long version;
	}

	interface SampleRepository extends Repository<SampleEntity, Long> {}
}
