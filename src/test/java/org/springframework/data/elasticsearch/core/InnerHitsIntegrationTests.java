/*
 * Copyright 2020-2021 the original author or authors.
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
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.backend.elasticsearch7.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.backend.elasticsearch7.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.document.NestedMetaData;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;

/**
 * Testing the querying and parsing of inner_hits.
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class InnerHitsIntegrationTests {

	public static final String INDEX_NAME = "tests-inner-hits";

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

	@Document(indexName = INDEX_NAME)
	static class City {
		@Nullable @Id private String name;
		// NOTE: using a custom names here to cover property name matching
		@Nullable @Field(name = "hou-ses", type = FieldType.Nested) private Collection<House> houses = new ArrayList<>();

		public City(@Nullable String name, @Nullable Collection<House> houses) {
			this.name = name;
			this.houses = houses;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public Collection<House> getHouses() {
			return houses;
		}

		public void setHouses(@Nullable Collection<House> houses) {
			this.houses = houses;
		}
	}

	static class House {
		@Nullable @Field(type = FieldType.Text) private String street;
		@Nullable @Field(type = FieldType.Text) private String streetNumber;
		// NOTE: using a custom names here to cover property name matching
		@Nullable @Field(name = "in-habi-tants",
				type = FieldType.Nested) private List<Inhabitant> inhabitants = new ArrayList<>();

		public House(@Nullable String street, @Nullable String streetNumber, @Nullable List<Inhabitant> inhabitants) {
			this.street = street;
			this.streetNumber = streetNumber;
			this.inhabitants = inhabitants;
		}

		@Nullable
		public String getStreet() {
			return street;
		}

		public void setStreet(@Nullable String street) {
			this.street = street;
		}

		@Nullable
		public String getStreetNumber() {
			return streetNumber;
		}

		public void setStreetNumber(@Nullable String streetNumber) {
			this.streetNumber = streetNumber;
		}

		@Nullable
		public List<Inhabitant> getInhabitants() {
			return inhabitants;
		}

		public void setInhabitants(@Nullable List<Inhabitant> inhabitants) {
			this.inhabitants = inhabitants;
		}
	}

	static class Inhabitant {
		// NOTE: using a custom names here to cover property name matching
		@Nullable @Field(name = "first-name", type = FieldType.Text) private String firstName;
		@Nullable @Field(name = "last-name", type = FieldType.Text) private String lastName;

		public Inhabitant(@Nullable String firstName, @Nullable String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}
	}
}
