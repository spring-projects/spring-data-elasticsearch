/*
 * Copyright 2020-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.NewElasticsearchClientDevelopment;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class IndexTemplateIntegrationTests implements NewElasticsearchClientDevelopment {

	@Autowired ElasticsearchOperations operations;

	boolean rhlcWithCluster8() {
		var clusterVersion = ((AbstractElasticsearchTemplate) operations).getClusterVersion();
		return (oldElasticsearchClient() && clusterVersion != null && clusterVersion.startsWith("8"));
	}

	@DisabledIf(value = "rhlcWithCluster8", disabledReason = "RHLC fails to parse response from ES 8.2")
	@Test // DATAES-612
	void shouldCreateTemplate() {

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

	@Test // DATAES-612
	void shouldReturnNullOnNonExistingGetTemplate() {

		String templateName = "template" + UUID.randomUUID().toString();
		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("dont-care"));

		GetTemplateRequest getTemplateRequest = new GetTemplateRequest(templateName);
		TemplateData templateData = indexOps.getTemplate(getTemplateRequest);

		assertThat(templateData).isNull();
	}

	@DisabledIf(value = "rhlcWithCluster8", disabledReason = "RHLC fails to parse response from ES 8.2")
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

		String templateName = "template" + UUID.randomUUID().toString();
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

		String templateName = "template" + UUID.randomUUID().toString();
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
