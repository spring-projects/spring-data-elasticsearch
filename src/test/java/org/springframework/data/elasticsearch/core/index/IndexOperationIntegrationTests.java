/*
 * Copyright 2021 the original author or authors.
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

import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author George Popides
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class IndexOperationIntegrationTests {

	public static final String INDEX_NAME = "test-index-information-list";

	@Autowired protected ElasticsearchOperations operations;

	@BeforeEach
	void setUp() {
		operations.indexOps(EntityWithSettingsAndMappings.class).delete();
	}

	@Test // #1646, #1718
	@DisplayName("should return a list of info for specific index")
	void shouldReturnInformationList() throws JSONException {
		IndexOperations indexOps = operations.indexOps(EntityWithSettingsAndMappings.class);

		String aliasName = "testindexinformationindex";

		indexOps.createWithMapping();

		AliasActionParameters parameters = AliasActionParameters.builder().withAliases(aliasName).withIndices(INDEX_NAME)
				.withIsHidden(false).withIsWriteIndex(false).withRouting("indexrouting").withSearchRouting("searchrouting")
				.build();
		indexOps.alias(new AliasActions(new AliasAction.Add(parameters)));

		List<IndexInformation> indexInformationList = indexOps.getInformation();

		IndexInformation indexInformation = indexInformationList.get(0);

		assertThat(indexInformationList.size()).isEqualTo(1);
		assertThat(indexInformation.getName()).isEqualTo(INDEX_NAME);
		assertThat(indexInformation.getSettings().get("index.number_of_shards")).isEqualTo("1");
		assertThat(indexInformation.getSettings().get("index.number_of_replicas")).isEqualTo("0");
		assertThat(indexInformation.getSettings().get("index.analysis.analyzer.emailAnalyzer.type")).isEqualTo("custom");
		assertThat(indexInformation.getAliases()).hasSize(1);

		AliasData aliasData = indexInformation.getAliases().get(0);

		assertThat(aliasData.getAlias()).isEqualTo(aliasName);
		assertThat(aliasData.isHidden()).isEqualTo(false);
		assertThat(aliasData.isWriteIndex()).isEqualTo(false);
		assertThat(aliasData.getIndexRouting()).isEqualTo("indexrouting");
		assertThat(aliasData.getSearchRouting()).isEqualTo("searchrouting");

		String expectedMappings = "{\n" + //
				"  \"properties\": {\n" + //
				"    \"email\": {\n" + //
				"      \"type\": \"text\",\n" + //
				"      \"analyzer\": \"emailAnalyzer\"\n" + //
				"    }\n" + //
				"  }\n" + //
				"}"; //
		JSONAssert.assertEquals(expectedMappings, indexInformation.getMapping().toJson(), false);
	}

	@Document(indexName = INDEX_NAME)
	@Setting(settingPath = "settings/test-settings.json")
	@Mapping(mappingPath = "mappings/test-mappings.json")
	protected static class EntityWithSettingsAndMappings {
		@Nullable private @Id String id;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}
	}
}
