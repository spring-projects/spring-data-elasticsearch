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
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexInformation;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

import lombok.Data;

/**
 * @author George Popides
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class IndexOperationTests {
	@Autowired
	protected ElasticsearchOperations operations;

	@BeforeEach
	void setUp() {
		operations.indexOps(EntityWithSettingsAndMappings.class).delete();
	}

	@Test // #1646
	@DisplayName("should return a list of info for specific index")
	void shouldReturnInformationList() throws JSONException {
		IndexOperations indexOps = operations.indexOps(EntityWithSettingsAndMappings.class);

		String aliasName = "testindexinformationindex";
		String indexName = "test-index-information-list";

		indexOps.create();
		indexOps.putMapping();

		AliasActionParameters parameters = AliasActionParameters.builder()
				.withAliases(aliasName)
				.withIndices(indexName)
				.withIsHidden(false)
				.withIsWriteIndex(false)
				.withRouting("indexrouting")
				.withSearchRouting("searchrouting")
				.build();
		indexOps.alias(new AliasActions(new AliasAction.Add(parameters)));

		List<IndexInformation> indexInformationList = indexOps.getInformation();

		IndexInformation indexInformation = indexInformationList.get(0);

		assertThat(indexInformationList.size()).isEqualTo(1);
		assertThat(indexInformation.getName()).isEqualTo(indexName);
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

		String expectedMappings = "{\"properties\":{\"email\":{\"type\":\"text\",\"analyzer\":\"emailAnalyzer\"}}}";
		JSONAssert.assertEquals(expectedMappings, indexInformation.getMappings().toJson(), false);
	}

	@Data
	@Document(indexName = "test-index-information-list")
	@Setting(settingPath = "settings/test-settings.json")
	@Mapping(mappingPath = "mappings/test-mappings.json")
	protected static class EntityWithSettingsAndMappings {
		@Id
		String id;
	}
}
