/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query.sort;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Order.Nested;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * Integration tests for nested sorts.
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class NestedSortIntegrationTests {

	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;
	@Nullable IndexOperations indexOperations;

	private final Actor marlonBrando = new Actor("Marlon Brando", "m", 1924);
	private final Actor robertDuvall = new Actor("RobertDuvall", "m", 1931);
	private final Actor jackNicholson = new Actor("Jack Nicholson", "m", 1937);
	private final Actor alPacino = new Actor("Al Pacino", "m", 1940);
	private final Actor ronalLeeErmey = new Actor("Ronal Lee Ermey", "m", 1944);
	private final Actor dianeKeaton = new Actor("Diane Keaton", "f", 1946);
	private final Actor shelleyDuval = new Actor("Shelley Duval", "f", 1949);
	private final Actor matthewModine = new Actor("Matthew Modine", "m", 1959);

	private final Movie theGodfather = new Movie("The Godfather", 1972, List.of(alPacino, dianeKeaton));
	private final Movie apocalypseNow = new Movie("Apocalypse Now", 1979, List.of(marlonBrando, robertDuvall));
	private final Movie theShining = new Movie("The Shining", 1980, List.of(jackNicholson, shelleyDuval));
	private final Movie fullMetalJacket = new Movie("Full Metal Jacket", 1987, List.of(matthewModine, ronalLeeErmey));

	private final Director stanleyKubrik = new Director("1", "Stanley Kubrik", 1928,
			List.of(fullMetalJacket, theShining));
	private final Director francisFordCoppola = new Director("2", "Francis Ford Coppola", 1939,
			List.of(apocalypseNow, theGodfather));

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		indexOperations = operations.indexOps(Director.class);
		indexOperations.createWithMapping();

		operations.save(francisFordCoppola, stanleyKubrik);
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
	}

	@Test // #1784
	@DisplayName("should sort directors by year of birth of actor in their movies ascending")
	void shouldSortDirectorsByYearOfBirthOfActorInTheirMoviesAscending() {

		var order = new org.springframework.data.elasticsearch.core.query.Order(Sort.Direction.ASC,
				"movies.actors.yearOfBirth") //
				.withNested(Nested.of("movies", //
						b -> b.withNested(Nested.of("movies.actors", Function.identity()))));
		var query = Query.findAll().addSort(Sort.by(order));

		var searchHits = operations.search(query, Director.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(2);

		assertThat(searchHits.getSearchHit(0).getContent().id).isEqualTo(francisFordCoppola.id);
		var sortValues = searchHits.getSearchHit(0).getSortValues();
		assertThat(sortValues).hasSize(1);
		assertThat(sortValues.get(0)).isEqualTo(1924L);

		assertThat(searchHits.getSearchHit(1).getContent().id).isEqualTo(stanleyKubrik.id);
		sortValues = searchHits.getSearchHit(1).getSortValues();
		assertThat(sortValues).hasSize(1);
		assertThat(sortValues.get(0)).isEqualTo(1937L);
	}

	@Test // #1784
	@DisplayName("should sort directors by year of birth of actor in their movies descending")
	void shouldSortDirectorsByYearOfBirthOfActorInTheirMoviesDescending() {

		var order = new org.springframework.data.elasticsearch.core.query.Order(Sort.Direction.DESC,
				"movies.actors.yearOfBirth") //
				.withNested( //
						Nested.builder("movies") //
								.withNested(Nested.builder("movies.actors") //
										.build()) //
								.build());

		var query = Query.findAll().addSort(Sort.by(order));

		var searchHits = operations.search(query, Director.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(2);

		assertThat(searchHits.getSearchHit(0).getContent().id).isEqualTo(stanleyKubrik.id);
		var sortValues = searchHits.getSearchHit(0).getSortValues();
		assertThat(sortValues).hasSize(1);
		assertThat(sortValues.get(0)).isEqualTo(1959L);

		assertThat(searchHits.getSearchHit(1).getContent().id).isEqualTo(francisFordCoppola.id);
		sortValues = searchHits.getSearchHit(1).getSortValues();
		assertThat(sortValues).hasSize(1);
		assertThat(sortValues.get(0)).isEqualTo(1946L);
	}

	@Test // #1784
	@DisplayName("should sort directors by year of birth of male actor in their movies descending")
	void shouldSortDirectorsByYearOfBirthOfMaleActorInTheirMoviesDescending() {

		var filter = StringQuery.builder("""
				{ "term": {"movies.actors.sex": "m"} }
				""").build();
		var order = new org.springframework.data.elasticsearch.core.query.Order(Sort.Direction.DESC,
				"movies.actors.yearOfBirth") //
				.withNested( //
						Nested.builder("movies") //
								.withNested( //
										Nested.builder("movies.actors") //
												.withFilter(filter) //
												.build()) //
								.build());

		var query = Query.findAll().addSort(Sort.by(order));

		var searchHits = operations.search(query, Director.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(2);

		assertThat(searchHits.getSearchHit(0).getContent().id).isEqualTo(stanleyKubrik.id);
		var sortValues = searchHits.getSearchHit(0).getSortValues();
		assertThat(sortValues).hasSize(1);
		assertThat(sortValues.get(0)).isEqualTo(1959L);

		assertThat(searchHits.getSearchHit(1).getContent().id).isEqualTo(francisFordCoppola.id);
		sortValues = searchHits.getSearchHit(1).getSortValues();
		assertThat(sortValues).hasSize(1);
		assertThat(sortValues.get(0)).isEqualTo(1940L);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	record Director( //
			@Nullable @Id String id, //
			@Field(type = FieldType.Text) String name, //
			@Field(type = FieldType.Integer) Integer yearOfBirth, //
			@Field(type = FieldType.Nested) List<Movie> movies //
	) {
	}

	record Movie( //
			@Field(type = FieldType.Text) String title, //
			@Field(type = FieldType.Integer) Integer year, //
			@Field(type = FieldType.Nested) List<Actor> actors //
	) {
	}

	record Actor( //
			@Field(type = FieldType.Text) String name, //
			@Field(type = FieldType.Keyword) String sex, //
			@Field(type = FieldType.Integer) Integer yearOfBirth //
	) {
	}

}
