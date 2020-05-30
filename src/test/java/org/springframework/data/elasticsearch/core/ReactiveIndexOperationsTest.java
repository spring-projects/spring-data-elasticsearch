/*
 * Copyright 2018-2020 the original author or authors.
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
import static org.skyscreamer.jsonassert.JSONAssert.*;

import lombok.Data;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveIndexOperationsTest.Config.class })
public class ReactiveIndexOperationsTest {

	public static final String TESTINDEX = "reactive-index-operations-testindex";

	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class, ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	@Autowired private ReactiveElasticsearchOperations operations;

	@BeforeEach
	void setUp() {
		deleteIndices();
	}

	@AfterEach
	void tearDown() {
		deleteIndices();
	}

	private void deleteIndices() {
		operations.indexOps(IndexCoordinates.of(TESTINDEX + "*")).delete().block();
	}

	@Test // DATAES-678
	void shouldCreateIndexOpsForIndexCoordinates() {

		IndexCoordinates indexCoordinates = IndexCoordinates.of(TESTINDEX);

		ReactiveIndexOperations indexOperations = operations.indexOps(indexCoordinates);

		assertThat(indexOperations).isNotNull();
	}

	@Test // DATAES-678
	void shouldCreateIndexOpsForEntityClass() {

		ReactiveIndexOperations indexOperations = operations.indexOps(Entity.class);

		assertThat(indexOperations).isNotNull();
	}

	@Test // DATAES-678
	void shouldCreateIndexForName() {

		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-create"));

		indexOps.create() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldCreateIndexForEntity() {

		ReactiveIndexOperations indexOps = operations.indexOps(Entity.class);

		indexOps.create() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		// check the settings from the class annotation
		indexOps.getSettings().as(StepVerifier::create).consumeNextWith(settings -> {
			assertThat(settings.get("index.number_of_replicas")).isEqualTo("2");
			assertThat(settings.get("index.number_of_shards")).isEqualTo("3");
			assertThat(settings.get("index.refresh_interval")).isEqualTo("4s");
		}).verifyComplete();
	}

	@Test // DATAES-678
	void shouldCreateIndexWithGivenSettings() {

		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-create"));

		org.springframework.data.elasticsearch.core.document.Document requiredSettings = org.springframework.data.elasticsearch.core.document.Document
				.create();
		requiredSettings.put("index.number_of_replicas", 3);
		requiredSettings.put("index.number_of_shards", 4);
		requiredSettings.put("index.refresh_interval", "5s");

		indexOps.create(requiredSettings) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		indexOps.getSettings().as(StepVerifier::create).consumeNextWith(settings -> {
			assertThat(settings.get("index.number_of_replicas")).isEqualTo("3");
			assertThat(settings.get("index.number_of_shards")).isEqualTo("4");
			assertThat(settings.get("index.refresh_interval")).isEqualTo("5s");
		}).verifyComplete();
	}

	@Test // DATAES-678
	void shouldCreateIndexWithAnnotatedSettings() {

		ReactiveIndexOperations indexOps = operations.indexOps(EntityWithAnnotatedSettingsAndMappings.class);

		indexOps.create() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		indexOps.getSettings().as(StepVerifier::create).consumeNextWith(settings -> {
			assertThat(settings.get("index.number_of_replicas")).isEqualTo("0");
			assertThat(settings.get("index.number_of_shards")).isEqualTo("1");
			assertThat(settings.containsKey("index.analysis.analyzer.emailAnalyzer.tokenizer")).isTrue();
			assertThat(settings.get("index.analysis.analyzer.emailAnalyzer.tokenizer")).isEqualTo("uax_url_email");
		}).verifyComplete();
	}

	@Test // DATAES-678
	public void shouldCreateIndexUsingServerDefaultConfiguration() {

		ReactiveIndexOperations indexOps = operations.indexOps(EntityUseServerConfig.class);

		indexOps.create() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		// check the settings from the class annotation
		indexOps.getSettings().as(StepVerifier::create).consumeNextWith(settings -> {
			assertThat(settings.get("index.number_of_replicas")).isEqualTo("1");
			assertThat(settings.get("index.number_of_shards")).isEqualTo("1");
		}).verifyComplete();
	}

	@Test // DATAES-678
	void shouldDeleteIfItExists() {

		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-delete"));
		indexOps.create().block();

		indexOps.delete() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldReturnFalseOnDeleteIfItDoesNotExist() {

		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-delete"));

		indexOps.delete() //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldReturnExistsTrueIfIndexDoesExist() {

		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-exists"));
		indexOps.create().block();

		indexOps.exists() //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldReturnExistsFalseIfIndexDoesNotExist() {
		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-exists"));

		indexOps.exists() //
				.as(StepVerifier::create) //
				.expectNext(false) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldCreateMappingForEntityFromProperties() {
		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-mappings"));

		String expected = "{\n" + //
				"  \"properties\":{\n" + //
				"    \"text\": {\n" + //
				"      \"type\": \"text\"\n" + //
				"    },\n" + //
				"    \"publication-date\": {\n" + //
				"      \"type\": \"date\",\n" + //
				"      \"format\": \"basic_date\"\n" + //
				"    }\n" + //
				"  }\n" + //
				"}\n"; //

		indexOps.createMapping(Entity.class) //
				.as(StepVerifier::create) //
				.assertNext(document -> {
					try {
						assertEquals(expected, document.toJson(), JSONCompareMode.NON_EXTENSIBLE);
					} catch (JSONException e) {
						fail("", e);
					}
				}) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldCreateMappingForEntityFromMappingAnnotation() {
		ReactiveIndexOperations indexOps = operations.indexOps(IndexCoordinates.of(TESTINDEX + "-mappings"));

		String expected = "{\n" + //
				"  \"properties\": {\n" + //
				"    \"email\": {\n" + //
				"      \"type\": \"text\",\n" + //
				"      \"analyzer\": \"emailAnalyzer\"\n" + //
				"    }\n" + //
				"  }\n" + //
				"}\n"; //

		indexOps.createMapping(EntityWithAnnotatedSettingsAndMappings.class) //
				.as(StepVerifier::create) //
				.assertNext(document -> {
					try {
						assertEquals(expected, document.toJson(), JSONCompareMode.NON_EXTENSIBLE);
					} catch (JSONException e) {
						fail("", e);
					}
				}) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldCreateMappingBoundEntity() {
		ReactiveIndexOperations indexOps = operations.indexOps(Entity.class);

		String expected = "{\n" + //
				"  \"properties\":{\n" + //
				"    \"text\": {\n" + //
				"      \"type\": \"text\"\n" + //
				"    },\n" + //
				"    \"publication-date\": {\n" + //
				"      \"type\": \"date\",\n" + //
				"      \"format\": \"basic_date\"\n" + //
				"    }\n" + //
				"  }\n" + //
				"}\n"; //

		indexOps.createMapping() //
				.as(StepVerifier::create) //
				.assertNext(document -> {
					try {
						assertEquals(expected, document.toJson(), JSONCompareMode.NON_EXTENSIBLE);
					} catch (JSONException e) {
						fail("", e);
					}
				}) //
				.verifyComplete();
	}

	@Test // DATAES-678
	void shouldPutAndGetMapping() {
		ReactiveIndexOperations indexOps = operations.indexOps(Entity.class);

		String expected = "{\n" + //
				"  \"properties\":{\n" + //
				"    \"text\": {\n" + //
				"      \"type\": \"text\"\n" + //
				"    },\n" + //
				"    \"publication-date\": {\n" + //
				"      \"type\": \"date\",\n" + //
				"      \"format\": \"basic_date\"\n" + //
				"    }\n" + //
				"  }\n" + //
				"}\n"; //

		indexOps.create() //
				.then(indexOps.putMapping()) //
				.then(indexOps.getMapping()) //
				.as(StepVerifier::create) //
				.assertNext(document -> {
					try {
						assertEquals(expected, document.toJson(), JSONCompareMode.NON_EXTENSIBLE);
					} catch (JSONException e) {
						fail("", e);
					}
				}).verifyComplete();
	}

	@Data
	@Document(indexName = TESTINDEX, shards = 3, replicas = 2, refreshInterval = "4s")
	static class Entity {
		@Id private String id;
		@Field(type = FieldType.Text) private String text;
		@Field(name = "publication-date", type = FieldType.Date,
				format = DateFormat.basic_date) private LocalDate publicationDate;
	}

	@Data
	@Document(indexName = TESTINDEX, useServerConfiguration = true)
	static class EntityUseServerConfig {
		@Id private String id;
	}

	@Data
	@Document(indexName = TESTINDEX)
	@Setting(settingPath = "/settings/test-settings.json")
	@Mapping(mappingPath = "/mappings/test-mappings.json")
	static class EntityWithAnnotatedSettingsAndMappings {
		@Id private String id;
	}
}
