/*
 * Copyright 2020-2024 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class IndexTemplateIntegrationTests {

	@Autowired ElasticsearchOperations operations;

	@Test // DATAES-612
	void shouldPutTemplate() {

		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		org.springframework.data.elasticsearch.core.document.Document mapping = indexOps.createMapping(TemplateClass.class);
		Settings settings = indexOps.createSettings(TemplateClass.class);

		AliasActions aliasActions = new AliasActions(
				new AliasAction.Add(AliasActionParameters.builderForTemplate().withAliases("alias1", "alias2").build()));
		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder("test-template", "log-*") //
				.withSettings(settings) //
				.withMappings(mapping) //
				.withAliasActions(aliasActions) //
				.build();

		boolean acknowledged = indexOps.putTemplate(putTemplateRequest);

		assertThat(acknowledged).isTrue();
	}

	@Test // #1458
	@DisplayName("should create component template")
	void shouldCreateComponentTemplate() {

		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		org.springframework.data.elasticsearch.core.document.Document mapping = indexOps.createMapping(TemplateClass.class);
		Settings settings = indexOps.createSettings(TemplateClass.class);

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

		boolean acknowledged = indexOps.putComponentTemplate(putComponentTemplateRequest);

		assertThat(acknowledged).isTrue();
	}

	@Test // #1458
	@DisplayName("should get component template")
	void shouldGetComponentTemplate() throws JSONException {
		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		org.springframework.data.elasticsearch.core.document.Document mapping = indexOps.createMapping(TemplateClass.class);
		Settings settings = indexOps.createSettings(TemplateClass.class);

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

		boolean acknowledged = indexOps.putComponentTemplate(putComponentTemplateRequest);
		assertThat(acknowledged).isTrue();

		GetComponentTemplateRequest getComponentTemplateRequest = new GetComponentTemplateRequest(
				putComponentTemplateRequest.name());
		var componentTemplates = indexOps.getComponentTemplate(getComponentTemplateRequest);

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

		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));
		String templateName = "template" + UUID.randomUUID();
		var putComponentTemplateRequest = PutComponentTemplateRequest.builder() //
				.withName(templateName) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withSettings(indexOps.createSettings(TemplateClass.class)) //
						.build() //
				).build();
		ExistsComponentTemplateRequest existsComponentTemplateRequest = new ExistsComponentTemplateRequest(templateName);

		boolean acknowledged = indexOps.putComponentTemplate(putComponentTemplateRequest);
		assertThat(acknowledged).isTrue();

		boolean exists = indexOps.existsComponentTemplate(existsComponentTemplateRequest);
		assertThat(exists).isTrue();

		acknowledged = indexOps.deleteComponentTemplate(new DeleteComponentTemplateRequest(templateName));
		assertThat(acknowledged).isTrue();

		exists = indexOps.existsComponentTemplate(existsComponentTemplateRequest);
		assertThat(exists).isFalse();
	}

	@Test // #1458
	@DisplayName("should put, get and delete index template with template")
	void shouldPutGetAndDeleteIndexTemplateWithTemplate() {

		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));
		org.springframework.data.elasticsearch.core.document.Document mapping = indexOps.createMapping(TemplateClass.class);
		Settings settings = indexOps.createSettings(TemplateClass.class);

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

		boolean acknowledged = indexOps.putIndexTemplate(putIndexTemplateRequest);
		assertThat(acknowledged).isTrue();

		var exists = indexOps.existsIndexTemplate(indexTemplateName);
		assertThat(exists).isTrue();

		var indexTemplates = indexOps.getIndexTemplate(indexTemplateName);
		assertThat(indexTemplates).hasSize(1);

		// delete template
		acknowledged = indexOps.deleteIndexTemplate(indexTemplateName);
		assertThat(acknowledged).isTrue();

		exists = indexOps.existsIndexTemplate(indexTemplateName);
		assertThat(exists).isFalse();
	}

	@Test // #1458
	@DisplayName("should put, get and delete index template of components")
	void shouldPutGetAndDeleteIndexTemplateOfComponents() {

		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		org.springframework.data.elasticsearch.core.document.Document mapping = indexOps.createMapping(TemplateClass.class);
		Settings settings = indexOps.createSettings(TemplateClass.class);

		var filterQuery = CriteriaQuery.builder(Criteria.where("message").is("foo")).build();
		AliasActions aliasActions = new AliasActions(new AliasAction.Add(AliasActionParameters.builderForTemplate() //
				.withAliases("alias1", "alias2") //
				.withFilterQuery(filterQuery, TemplateClass.class)//
				.build()));

		PutComponentTemplateRequest putComponentTemplateRequestMapping = PutComponentTemplateRequest.builder() //
				.withName("test-component-template-mapping") //
				.withVersion(42L) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withMapping(mapping) //
						.build()) //
				.build();

		boolean acknowledged = indexOps.putComponentTemplate(putComponentTemplateRequestMapping);
		assertThat(acknowledged).isTrue();

		PutComponentTemplateRequest putComponentTemplateRequestSettings = PutComponentTemplateRequest.builder() //
				.withName("test-component-template-settings") //
				.withVersion(42L) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withSettings(settings) //
						.build()) //
				.build();

		acknowledged = indexOps.putComponentTemplate(putComponentTemplateRequestSettings);
		assertThat(acknowledged).isTrue();

		PutComponentTemplateRequest putComponentTemplateRequestAliases = PutComponentTemplateRequest.builder() //
				.withName("test-component-template-aliases") //
				.withVersion(42L) //
				.withTemplateData(ComponentTemplateRequestData.builder() //
						.withAliasActions(aliasActions) //
						.build()) //
				.build();

		acknowledged = indexOps.putComponentTemplate(putComponentTemplateRequestAliases);
		assertThat(acknowledged).isTrue();

		var indexTemplateName = "test-index-template";
		var composedOf = List.of("test-component-template-mapping", "test-component-template-settings",
				"test-component-template-aliases");
		var putIndexTemplateRequest = PutIndexTemplateRequest.builder() //
				.withName(indexTemplateName) //
				.withIndexPatterns("index-*", "endix-*") //
				.withComposedOf(composedOf) //
				.build();

		acknowledged = indexOps.putIndexTemplate(putIndexTemplateRequest);
		assertThat(acknowledged).isTrue();

		var indexTemplates = indexOps.getIndexTemplate(indexTemplateName);
		assertThat(indexTemplates).hasSize(1);
		assertThat(indexTemplates.get(0).templateData().composedOf()).isEqualTo(composedOf);

		// delete template and components
		acknowledged = indexOps.deleteIndexTemplate(indexTemplateName);
		assertThat(acknowledged).isTrue();
		acknowledged = indexOps
				.deleteComponentTemplate(new DeleteComponentTemplateRequest("test-component-template-mapping"));
		assertThat(acknowledged).isTrue();
		acknowledged = indexOps
				.deleteComponentTemplate(new DeleteComponentTemplateRequest("test-component-template-settings"));
		assertThat(acknowledged).isTrue();
		acknowledged = indexOps
				.deleteComponentTemplate(new DeleteComponentTemplateRequest("test-component-template-aliases"));
		assertThat(acknowledged).isTrue();
	}

	@Test // DATAES-612
	void shouldReturnNullOnNonExistingGetTemplate() {

		String templateName = "template" + UUID.randomUUID();
		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		GetTemplateRequest getTemplateRequest = new GetTemplateRequest(templateName);
		TemplateData templateData = indexOps.getTemplate(getTemplateRequest);

		assertThat(templateData).isNull();
	}

	@Test // DATAES-612, #2073
	void shouldGetTemplate() throws JSONException {
		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		org.springframework.data.elasticsearch.core.document.Document mapping = indexOps.createMapping(TemplateClass.class);
		Settings settings = indexOps.createSettings(TemplateClass.class);

		var filterQuery = CriteriaQuery.builder(Criteria.where("message").is("foo")).build();
		AliasActions aliasActions = new AliasActions(new AliasAction.Add(AliasActionParameters.builderForTemplate() //
				.withAliases("alias1", "alias2") //
				.withFilterQuery(filterQuery, TemplateClass.class)//
				.build()));

		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder("test-template", "log-*") //
				.withSettings(settings) //
				.withMappings(mapping) //
				.withAliasActions(aliasActions) //
				.withOrder(11) //
				.withVersion(42) //
				.build();

		boolean acknowledged = indexOps.putTemplate(putTemplateRequest);
		assertThat(acknowledged).isTrue();

		GetTemplateRequest getTemplateRequest = new GetTemplateRequest(putTemplateRequest.getName());
		TemplateData templateData = indexOps.getTemplate(getTemplateRequest);

		assertThat(templateData).isNotNull();
		assertThat(templateData.getIndexPatterns()).containsExactlyInAnyOrder(putTemplateRequest.getIndexPatterns());
		assertEquals(settings.flatten().toJson(), templateData.getSettings().toJson(), false);
		assertEquals(mapping.toJson(), templateData.getMapping().toJson(), false);
		Map<String, AliasData> aliases = templateData.getAliases();
		assertThat(aliases).hasSize(2);
		AliasData alias1 = aliases.get("alias1");
		assertThat(alias1.getAlias()).isEqualTo("alias1");
		assertThat(alias1.getFilterQuery()).isNotNull();
		AliasData alias2 = aliases.get("alias2");
		assertThat(alias2.getFilterQuery()).isNotNull();
		assertThat(alias2.getAlias()).isEqualTo("alias2");
		assertThat(templateData.getOrder()).isEqualTo(putTemplateRequest.getOrder());
		assertThat(templateData.getVersion()).isEqualTo(putTemplateRequest.getVersion());
	}

	@Test // DATAES-612
	void shouldCheckExists() {
		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		String templateName = "template" + UUID.randomUUID();
		ExistsTemplateRequest existsTemplateRequest = new ExistsTemplateRequest(templateName);

		boolean exists = indexOps.existsTemplate(existsTemplateRequest);
		assertThat(exists).isFalse();

		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder(templateName, "log-*") //
				.withOrder(11) //
				.withVersion(42) //
				.build();

		boolean acknowledged = indexOps.putTemplate(putTemplateRequest);
		assertThat(acknowledged).isTrue();

		exists = indexOps.existsTemplate(existsTemplateRequest);
		assertThat(exists).isTrue();
	}

	@Test // DATAES-612
	void shouldDeleteTemplate() {

		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		String templateName = "template" + UUID.randomUUID();
		ExistsTemplateRequest existsTemplateRequest = new ExistsTemplateRequest(templateName);

		PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder(templateName, "log-*") //
				.withOrder(11) //
				.withVersion(42) //
				.build();

		boolean acknowledged = indexOps.putTemplate(putTemplateRequest);
		assertThat(acknowledged).isTrue();

		boolean exists = indexOps.existsTemplate(existsTemplateRequest);
		assertThat(exists).isTrue();

		acknowledged = indexOps.deleteTemplate(new DeleteTemplateRequest(templateName));
		assertThat(acknowledged).isTrue();

		exists = indexOps.existsTemplate(existsTemplateRequest);
		assertThat(exists).isFalse();
	}

	@Document(indexName = "test-template")
	@Setting(shards = 3)
	static class TemplateClass {
		@Id
		@Nullable private String id;
		@Field(type = FieldType.Text)
		@Nullable private String message;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
