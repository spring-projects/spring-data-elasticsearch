/*
 * Copyright 2020 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.search.join.ScoreMode;
import org.assertj.core.api.SoftAssertions;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.document.NestedMetaData;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * Testing the querying and parsing of inner_hits.
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { InnerHitsTests.Config.class })
public class InnerHitsTests {

	public static final String INDEX_NAME = "tests-inner-hits";

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	@Autowired ElasticsearchOperations operations;
	@Nullable IndexOperations indexOps;

	@BeforeEach
	void setUp() {
		indexOps = operations.indexOps(City.class);
		indexOps.create();
		indexOps.putMapping(City.class);

		Inhabitant john = new Inhabitant("John", "Smith");
		Inhabitant carla = new Inhabitant("Carla", "Miller");
		House cornerHouse = new House("Round the corner", "7", Arrays.asList(john, carla));
		City metropole = new City("Metropole", Arrays.asList(cornerHouse));

		Inhabitant jack = new Inhabitant("Jack", "Wayne");
		Inhabitant emmy = new Inhabitant("Emmy", "Stone");
		House mainStreet = new House("Main Street", "42", Arrays.asList(jack, emmy));
		City village = new City("Village", Arrays.asList(mainStreet));

		operations.save(Arrays.asList(metropole, village));
		indexOps.refresh();
	}

	@AfterEach
	void tearDown() {
		indexOps.delete();
	}

	@Test
	void shouldReturnInnerHits() {
		String innerHitName = "inner_hit_name";

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

		NestedQueryBuilder nestedQueryBuilder = nestedQuery("hou-ses.in-habi-tants",
				matchQuery("hou-ses.in-habi-tants.first-name", "Carla"), ScoreMode.Avg);
		nestedQueryBuilder.innerHit(new InnerHitBuilder(innerHitName));
		queryBuilder.withQuery(nestedQueryBuilder);

		NativeSearchQuery query = queryBuilder.build();

		SoftAssertions softly = new SoftAssertions();
		SearchHits<City> searchHits = operations.search(query, City.class);

		softly.assertThat(searchHits.getTotalHits()).isEqualTo(1);

		SearchHit<City> searchHit = searchHits.getSearchHit(0);
		softly.assertThat(searchHit.getInnerHits()).hasSize(1);

		SearchHits<?> innerHits = searchHit.getInnerHits(innerHitName);
		softly.assertThat(innerHits).hasSize(1);

		SearchHit<?> innerHit = innerHits.getSearchHit(0);
		Object content = innerHit.getContent();
		assertThat(content).isInstanceOf(Inhabitant.class);
		Inhabitant inhabitant = (Inhabitant) content;
		softly.assertThat(inhabitant.getFirstName()).isEqualTo("Carla");
		softly.assertThat(inhabitant.getLastName()).isEqualTo("Miller");

		NestedMetaData nestedMetaData = innerHit.getNestedMetaData();
		softly.assertThat(nestedMetaData.getField()).isEqualTo("houses");
		softly.assertThat(nestedMetaData.getOffset()).isEqualTo(0);
		softly.assertThat(nestedMetaData.getChild().getField()).isEqualTo("inhabitants");
		softly.assertThat(nestedMetaData.getChild().getOffset()).isEqualTo(1);

		softly.assertAll();
	}

	@Data
	@AllArgsConstructor
	@RequiredArgsConstructor
	@Document(indexName = INDEX_NAME)
	static class City {

		@Id private String name;

		// NOTE: using a custom names here to cover property name matching
		@Field(name = "hou-ses", type = FieldType.Nested) private Collection<House> houses = new ArrayList<>();
	}

	@Data
	@AllArgsConstructor
	@RequiredArgsConstructor
	static class House {

		@Field(type = FieldType.Text) private String street;

		@Field(type = FieldType.Text) private String streetNumber;

		// NOTE: using a custom names here to cover property name matching
		@Field(name = "in-habi-tants", type = FieldType.Nested) private List<Inhabitant> inhabitants = new ArrayList<>();
	}

	@Data
	@AllArgsConstructor
	@RequiredArgsConstructor
	static class Inhabitant {
		// NOTE: using a custom names here to cover property name matching

		@Field(name = "first-name", type = FieldType.Text) private String firstName;

		@Field(name = "last-name", type = FieldType.Text) private String lastName;
	}
}
