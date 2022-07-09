/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.core.indices;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.NewElasticsearchClientDevelopment;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class IndexOperationsIntegrationTests implements NewElasticsearchClientDevelopment {

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@Autowired protected IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {

		indexNameProvider.increment();
		indexOperations = operations.indexOps(EntityWithSettingsAndMappings.class);
		indexOperations.createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@DisabledIf(value = "newElasticsearchClient", disabledReason = "todo #2165, ES issue 286")
	@Test // #1646, #1718
	@DisplayName("should return a list of info for specific index")
	void shouldReturnInformationList() throws JSONException {

		String indexName = indexNameProvider.indexName();
		String aliasName = "testindexinformationindex";

		AliasActionParameters parameters = AliasActionParameters.builder().withAliases(aliasName).withIndices(indexName)
				.withIsHidden(false).withIsWriteIndex(false).withRouting("indexrouting").withSearchRouting("searchrouting")
				.build();
		indexOperations.alias(new AliasActions(new AliasAction.Add(parameters)));

		List<IndexInformation> indexInformationList = indexOperations.getInformation();

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(indexInformationList.size()).isEqualTo(1);
		IndexInformation indexInformation = indexInformationList.get(0);
		softly.assertThat(indexInformation.getName()).isEqualTo(indexName);
		Settings settings = indexInformation.getSettings();
		assertThat(settings).isNotNull();
		// old Elasticsearch client returns "1", the new one is typed and returns 1, so we check for Strings here
		softly.assertThat(settings.get("index.number_of_shards")).isEqualTo("1");
		softly.assertThat(settings.get("index.number_of_replicas")).isEqualTo("0");
		softly.assertThat(settings.get("index.analysis.analyzer.emailAnalyzer.type")).isEqualTo("custom");
		softly.assertAll();

		assertThat(indexInformation.getAliases()).hasSize(1);
		AliasData aliasData = indexInformation.getAliases().get(0);

		softly = new SoftAssertions();
		softly.assertThat(aliasData.getAlias()).isEqualTo(aliasName);
		softly.assertThat(aliasData.isHidden()).isEqualTo(false);
		softly.assertThat(aliasData.isWriteIndex()).isEqualTo(false);
		softly.assertThat(aliasData.getIndexRouting()).isEqualTo("indexrouting");
		softly.assertThat(aliasData.getSearchRouting()).isEqualTo("searchrouting");
		softly.assertAll();

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

	@Test // #2209
	@DisplayName("should return AliasData with getAliases method")
	void shouldReturnAliasDataWithGetAliasesMethod() {

		String indexName = indexNameProvider.indexName();
		String aliasName = "testindexinformationindex";

		AliasActionParameters parameters = AliasActionParameters.builder().withAliases(aliasName).withIndices(indexName)
				.withIsHidden(false).withIsWriteIndex(false).withRouting("indexrouting").withSearchRouting("searchrouting")
				.build();
		indexOperations.alias(new AliasActions(new AliasAction.Add(parameters)));

		Map<String, Set<AliasData>> aliases = indexOperations.getAliases(aliasName);

		assertThat(aliases).hasSize(1);
		Set<AliasData> aliasDataSet = aliases.get(indexName);
		assertThat(aliasDataSet).hasSize(1);
		AliasData aliasData = aliasDataSet.iterator().next();

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(aliasData.getAlias()).isEqualTo(aliasName);
		softly.assertThat(aliasData.isHidden()).isEqualTo(false);
		softly.assertThat(aliasData.isWriteIndex()).isEqualTo(false);
		softly.assertThat(aliasData.getIndexRouting()).isEqualTo("indexrouting");
		softly.assertThat(aliasData.getSearchRouting()).isEqualTo("searchrouting");
		softly.assertAll();
	}

	@Test // #2209
	@DisplayName("should return AliasData with getAliasesForIndex method")
	void shouldReturnAliasDataWithGetAliasesForIndexMethod() {

		String indexName = indexNameProvider.indexName();
		String aliasName = "testindexinformationindex";

		AliasActionParameters parameters = AliasActionParameters.builder().withAliases(aliasName).withIndices(indexName)
				.withIsHidden(false).withIsWriteIndex(false).withRouting("indexrouting").withSearchRouting("searchrouting")
				.build();
		indexOperations.alias(new AliasActions(new AliasAction.Add(parameters)));

		Map<String, Set<AliasData>> aliases = indexOperations.getAliasesForIndex(indexName);

		assertThat(aliases).hasSize(1);
		Set<AliasData> aliasDataSet = aliases.get(indexName);
		assertThat(aliasDataSet).hasSize(1);
		AliasData aliasData = aliasDataSet.iterator().next();

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(aliasData.getAlias()).isEqualTo(aliasName);
		softly.assertThat(aliasData.isHidden()).isEqualTo(false);
		softly.assertThat(aliasData.isWriteIndex()).isEqualTo(false);
		softly.assertThat(aliasData.getIndexRouting()).isEqualTo("indexrouting");
		softly.assertThat(aliasData.getSearchRouting()).isEqualTo("searchrouting");
		softly.assertAll();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
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
