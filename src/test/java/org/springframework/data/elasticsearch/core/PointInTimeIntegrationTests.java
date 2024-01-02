/*
 * Copyright 2022-2024 the original author or authors.
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

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.CriteriaQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Integration tests for the point in time API.
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class PointInTimeIntegrationTests {

	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;
	@Nullable IndexOperations indexOperations;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		indexOperations = operations.indexOps(SampleEntity.class);
		indexOperations.createWithMapping();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
	}

	@Test // #1684
	@DisplayName("should create pit search with it and delete it again")
	void shouldCreatePitSearchWithItAndDeleteItAgain() {

		// insert 2 records, one smith
		operations.save(new SampleEntity("1", "John", "Smith"), new SampleEntity("2", "Mike", "Cutter"));

		// seach for smith
		var searchQuery = new CriteriaQuery(Criteria.where("lastName").is("Smith"));
		var searchHits = operations.search(searchQuery, SampleEntity.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);

		// create pit
		var pit = operations.openPointInTime(IndexCoordinates.of(indexNameProvider.indexName()), Duration.ofMinutes(10));
		assertThat(StringUtils.hasText(pit)).isTrue();

		// add another smith
		operations.save(new SampleEntity("3", "Harry", "Smith"));

		// search with pit -> 1 smith
		var pitQuery = new CriteriaQueryBuilder(Criteria.where("lastName").is("Smith")) //
				.withPointInTime(new Query.PointInTime(pit, Duration.ofMinutes(10))) //
				.build();
		searchHits = operations.search(pitQuery, SampleEntity.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		var newPit = searchHits.getPointInTimeId();
		assertThat(StringUtils.hasText(newPit)).isTrue();

		// search without pit -> 2 smiths
		searchHits = operations.search(searchQuery, SampleEntity.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(2);

		// close pit
		var success = operations.closePointInTime(newPit);
		assertThat(success).isTrue();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	record SampleEntity( //
			@Nullable @Id String id, //
			@Field(type = FieldType.Text) String firstName, //
			@Field(type = FieldType.Text) String lastName //
	) {
	}
}
