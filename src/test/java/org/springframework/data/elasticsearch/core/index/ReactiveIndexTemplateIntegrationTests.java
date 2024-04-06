/*
 * Copyright 2018-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;

import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ReactiveIndexTemplateIntegrationTests {

	@Autowired private ReactiveElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;
	private ReactiveIndexOperations indexOperations;
	private IndexOperations blockingIndexOperations;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		indexOperations = operations.indexOps(IndexCoordinates.of(indexNameProvider.indexName()));
		blockingIndexOperations = blocking(indexOperations);
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
	}

	@Test // DATAES-612
	void shouldPutTemplate() {

		org.springframework.data.elasticsearch.core.document.Document mapping = blockingIndexOperations
				.createMapping(TemplateClass.class);
		Settings settings = blockingIndexOperations.createSettings(TemplateClass.class);

		AliasActions aliasActions = new AliasActions(
				new AliasAction.Add(AliasActionParameters.builderForTemplate().withAliases("alias1", "alias2").build()));
		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder("test-template", "log-*") //
				.withSettings(settings) //
				.withMappings(mapping) //
				.withAliasActions(aliasActions) //
				.withOrder(11) //
				.withVersion(42) //
				.build();

		Boolean acknowledged = blockingIndexOperations.putTemplate(putTemplateRequest);
		assertThat(acknowledged).isTrue();
	}

	@Test // #1458
	@DisplayName("should create component template")
	void shouldCreateComponentTemplate() {

		var blockingIndexOps = blocking(operations.indexOps(IndexCoordinates.of("dont-care")));

		org.springframework.data.elasticsearch.core.document.Document mapping = blockingIndexOps
				.createMapping(TemplateClass.class);
		Settings settings = blockingIndexOps.createSettings(TemplateClass.class);

		AliasActions aliasActions = new AliasActions( //
				new AliasAction.Add(AliasActionParameters.builderForTemplate() //
						.withAliases("alias1", "alias2") //
						.withFilterQuery(Query.findAll()) //
						.build())); //

		var putComponentTemplateRequest = PutComponentTemplateRequest.builder() //
				.withName("test-component-template") //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withAliasActions(aliasActions) //
						.withMapping(mapping) //
						.withSettings(settings) //
						.build() //
				).build();

		boolean acknowledged = blockingIndexOps.putComponentTemplate(putComponentTemplateRequest);

		assertThat(acknowledged).isTrue();
	}

	@Test // #1458
	@DisplayName("should get component template")
	void shouldGetComponentTemplate() throws JSONException {
		var blockingIndexOps = blocking(operations.indexOps(IndexCoordinates.of("dont-care")));

		org.springframework.data.elasticsearch.core.document.Document mapping = blockingIndexOps
				.createMapping(TemplateClass.class);
		Settings settings = blockingIndexOps.createSettings(TemplateClass.class);

		var filterQuery = CriteriaQuery.builder(Criteria.where("message").is("foo")).build();
		AliasActions aliasActions = new AliasActions(new AliasAction.Add(AliasActionParameters.builderForTemplate() //
				.withAliases("alias1", "alias2") //
				.withFilterQuery(filterQuery, TemplateClass.class)//
				.build()));

		PutComponentTemplateRequest putComponentTemplateRequest = PutComponentTemplateRequest.builder() //
				.withName("test-component-template") //
				.withVersion(42L) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withSettings(settings) //
						.withMapping(mapping) //
						.withAliasActions(aliasActions) //
						.build()) //
				.build();

		boolean acknowledged = blockingIndexOps.putComponentTemplate(putComponentTemplateRequest);
		assertThat(acknowledged).isTrue();

		GetComponentTemplateRequest getComponentTemplateRequest = new GetComponentTemplateRequest(
				putComponentTemplateRequest.name());
		var componentTemplates = blockingIndexOps.getComponentTemplate(getComponentTemplateRequest);

		assertThat(componentTemplates).isNotNull().hasSize(1);
		var returnedComponentTemplate = componentTemplates.iterator().next();
		assertThat(returnedComponentTemplate.version()).isEqualTo(putComponentTemplateRequest.version());
		var componentTemplateData = returnedComponentTemplate.templateData();

		assertEquals(mapping.toJson(), componentTemplateData.mapping().toJson(), false);
		assertEquals(settings.flatten().toJson(), componentTemplateData.settings().flatten().toJson(), false);
		var aliases = componentTemplateData.aliases();
		assertThat(aliases).hasSize(2);
		AliasData alias1 = aliases.get("alias1");
		assertThat(alias1.getAlias()).isEqualTo("alias1");
		assertThat(alias1.getFilterQuery()).isNotNull();
		AliasData alias2 = aliases.get("alias2");
		assertThat(alias2.getFilterQuery()).isNotNull();
		assertThat(alias2.getAlias()).isEqualTo("alias2");
	}

	@Test // #1458
	@DisplayName("should delete component template")
	void shouldDeleteComponentTemplate() {

		var blockingIndexOps = blocking(operations.indexOps(IndexCoordinates.of("dont-care")));
		String templateName = "template" + UUID.randomUUID();
		var putComponentTemplateRequest = PutComponentTemplateRequest.builder() //
				.withName(templateName) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withSettings(blockingIndexOps.createSettings(TemplateClass.class)) //
						.build() //
				).build();
		ExistsComponentTemplateRequest existsComponentTemplateRequest = new ExistsComponentTemplateRequest(templateName);

		boolean acknowledged = blockingIndexOps.putComponentTemplate(putComponentTemplateRequest);
		assertThat(acknowledged).isTrue();

		boolean exists = blockingIndexOps.existsComponentTemplate(existsComponentTemplateRequest);
		assertThat(exists).isTrue();

		acknowledged = blockingIndexOps.deleteComponentTemplate(new DeleteComponentTemplateRequest(templateName));
		assertThat(acknowledged).isTrue();

		exists = blockingIndexOps.existsComponentTemplate(existsComponentTemplateRequest);
		assertThat(exists).isFalse();
	}

	@Test // #1458
	@DisplayName("should put, get and delete index template with template")
	void shouldPutGetAndDeleteIndexTemplateWithTemplate() {

		var blockingIndexOps = blocking(operations.indexOps(IndexCoordinates.of("dont-care")));
		org.springframework.data.elasticsearch.core.document.Document mapping = blockingIndexOps
				.createMapping(TemplateClass.class);
		Settings settings = blockingIndexOps.createSettings(TemplateClass.class);

		AliasActions aliasActions = new AliasActions( //
				new AliasAction.Add(AliasActionParameters.builderForTemplate() //
						.withAliases("alias1", "alias2") //
						.withFilterQuery(Query.findAll()) //
						.build())); //
		var indexTemplateName = "test-index-template";
		var putIndexTemplateRequest = PutIndexTemplateRequest.builder() //
				.withName(indexTemplateName) //
				.withIndexPatterns("index-*", "endix-*") //
				.withSettings(settings) //
				.withMapping(mapping) //
				.withAliasActions(aliasActions) //
				.build();

		Boolean acknowledged = blockingIndexOps.putIndexTemplate(putIndexTemplateRequest);
		assertThat(acknowledged).isTrue();

		var exists = blockingIndexOps.existsIndexTemplate(indexTemplateName);
		assertThat(exists).isTrue();

		var indexTemplates = blockingIndexOps.getIndexTemplate(indexTemplateName);
		assertThat(indexTemplates).hasSize(1);

		// delete template
		acknowledged = blockingIndexOps.deleteIndexTemplate(indexTemplateName);
		assertThat(acknowledged).isTrue();

		exists = blockingIndexOps.existsIndexTemplate(indexTemplateName);
		assertThat(exists).isFalse();
	}

	@Test // #1458
	@DisplayName("should put, get and delete index template of components")
	void shouldPutGetAndDeleteIndexTemplateOfComponents() {

		var blockingIndexOps = blocking(operations.indexOps(IndexCoordinates.of("dont-care")));

		org.springframework.data.elasticsearch.core.document.Document mapping = blockingIndexOps
				.createMapping(TemplateClass.class);
		Settings settings = blockingIndexOps.createSettings(TemplateClass.class);

		var filterQuery = CriteriaQuery.builder(Criteria.where("message").is("foo")).build();
		AliasActions aliasActions = new AliasActions(new AliasAction.Add(AliasActionParameters.builderForTemplate() //
				.withAliases("alias1", "alias2") //
				.withFilterQuery(filterQuery, IndexTemplateIntegrationTests.TemplateClass.class)//
				.build()));

		PutComponentTemplateRequest putComponentTemplateRequestMapping = PutComponentTemplateRequest.builder() //
				.withName("test-component-template-mapping") //
				.withVersion(42L) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withMapping(mapping) //
						.build()) //
				.build();

		Boolean acknowledged = blockingIndexOps.putComponentTemplate(putComponentTemplateRequestMapping);
		assertThat(acknowledged).isTrue();

		PutComponentTemplateRequest putComponentTemplateRequestSettings = PutComponentTemplateRequest.builder() //
				.withName("test-component-template-settings") //
				.withVersion(42L) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withSettings(settings) //
						.build()) //
				.build();

		acknowledged = blockingIndexOps.putComponentTemplate(putComponentTemplateRequestSettings);
		assertThat(acknowledged).isTrue();

		PutComponentTemplateRequest putComponentTemplateRequestAliases = PutComponentTemplateRequest.builder() //
				.withName("test-component-template-aliases") //
				.withVersion(42L) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withAliasActions(aliasActions) //
						.build()) //
				.build();

		acknowledged = blockingIndexOps.putComponentTemplate(putComponentTemplateRequestAliases);
		assertThat(acknowledged).isTrue();

		var indexTemplateName = "test-index-template";
		var composedOf = List.of("test-component-template-mapping", "test-component-template-settings",
				"test-component-template-aliases");
		var putIndexTemplateRequest = PutIndexTemplateRequest.builder() //
				.withName(indexTemplateName) //
				.withIndexPatterns("index-*", "endix-*") //
				.withComposedOf(composedOf) //
				.build();

		acknowledged = blockingIndexOps.putIndexTemplate(putIndexTemplateRequest);
		assertThat(acknowledged).isTrue();

		var indexTemplates = blockingIndexOps.getIndexTemplate(indexTemplateName);
		assertThat(indexTemplates).hasSize(1);
		assertThat(indexTemplates.get(0).templateData().composedOf()).isEqualTo(composedOf);

		// delete template and components
		acknowledged = blockingIndexOps.deleteIndexTemplate(indexTemplateName);
		assertThat(acknowledged).isTrue();
		acknowledged = blockingIndexOps
				.deleteComponentTemplate(new DeleteComponentTemplateRequest("test-component-template-mapping"));
		assertThat(acknowledged).isTrue();
		acknowledged = blockingIndexOps
				.deleteComponentTemplate(new DeleteComponentTemplateRequest("test-component-template-settings"));
		assertThat(acknowledged).isTrue();
		acknowledged = blockingIndexOps
				.deleteComponentTemplate(new DeleteComponentTemplateRequest("test-component-template-aliases"));
		assertThat(acknowledged).isTrue();
	}

	@Test // DATAES-612
	void shouldReturnNullOnNonExistingGetTemplate() {

		String templateName = "template" + UUID.randomUUID();

		GetTemplateRequest getTemplateRequest = new GetTemplateRequest(templateName);
		indexOperations.getTemplate(getTemplateRequest) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-612
	void shouldGetTemplate() throws JSONException {

		var blockingIndexOperations = blocking(indexOperations);

		org.springframework.data.elasticsearch.core.document.Document mapping = blockingIndexOperations
				.createMapping(TemplateClass.class);
		Settings settings = blockingIndexOperations.createSettings(TemplateClass.class);

		AliasActions aliasActions = new AliasActions(
				new AliasAction.Add(AliasActionParameters.builderForTemplate().withAliases("alias1", "alias2").build()));
		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder("test-template", "log-*") //
				.withSettings(settings) //
				.withMappings(mapping) //
				.withAliasActions(aliasActions) //
				.withOrder(11) //
				.withVersion(42) //
				.build();

		Boolean acknowledged = blockingIndexOperations.putTemplate(putTemplateRequest);
		assertThat(acknowledged).isTrue();

		GetTemplateRequest getTemplateRequest = new GetTemplateRequest(putTemplateRequest.getName());
		TemplateData templateData = blockingIndexOperations.getTemplate(getTemplateRequest);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(templateData).isNotNull();
		softly.assertThat(templateData.getIndexPatterns()).containsExactlyInAnyOrder(putTemplateRequest.getIndexPatterns());
		assertEquals(settings.flatten().toJson(), templateData.getSettings().toJson(), false);
		assertEquals(mapping.toJson(), templateData.getMapping().toJson(), false);
		Map<String, AliasData> aliases = templateData.getAliases();
		softly.assertThat(aliases).hasSize(2);
		AliasData alias1 = aliases.get("alias1");
		softly.assertThat(alias1.getAlias()).isEqualTo("alias1");
		AliasData alias2 = aliases.get("alias2");
		softly.assertThat(alias2.getAlias()).isEqualTo("alias2");
		softly.assertThat(templateData.getOrder()).isEqualTo(putTemplateRequest.getOrder());
		softly.assertThat(templateData.getVersion()).isEqualTo(putTemplateRequest.getVersion());
		softly.assertAll();
	}

	@Test // DATAES-612
	void shouldCheckTemplateExists() {

		String templateName = "template" + UUID.randomUUID();
		ExistsTemplateRequest existsTemplateRequest = new ExistsTemplateRequest(templateName);

		boolean exists = blockingIndexOperations.existsTemplate(existsTemplateRequest);
		assertThat(exists).isFalse();

		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder(templateName, "log-*") //
				.withOrder(11) //
				.withVersion(42) //
				.build();

		boolean acknowledged = blockingIndexOperations.putTemplate(putTemplateRequest);
		assertThat(acknowledged).isTrue();

		exists = blockingIndexOperations.existsTemplate(existsTemplateRequest);
		assertThat(exists).isTrue();
	}

	@Test // DATAES-612
	void shouldDeleteTemplate() {

		String templateName = "template" + UUID.randomUUID();
		ExistsTemplateRequest existsTemplateRequest = new ExistsTemplateRequest(templateName);

		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder(templateName, "log-*") //
				.withOrder(11) //
				.withVersion(42) //
				.build();

		boolean acknowledged = blockingIndexOperations.putTemplate(putTemplateRequest);
		assertThat(acknowledged).isTrue();

		boolean exists = blockingIndexOperations.existsTemplate(existsTemplateRequest);
		assertThat(exists).isTrue();

		acknowledged = blockingIndexOperations.deleteTemplate(new DeleteTemplateRequest(templateName));
		assertThat(acknowledged).isTrue();

		exists = blockingIndexOperations.existsTemplate(existsTemplateRequest);
		assertThat(exists).isFalse();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Setting(refreshInterval = "5s")
	static class TemplateClass {
		@Id
		@Nullable private String id;
		@Field(type = FieldType.Text)
		@Nullable private String message;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

}
