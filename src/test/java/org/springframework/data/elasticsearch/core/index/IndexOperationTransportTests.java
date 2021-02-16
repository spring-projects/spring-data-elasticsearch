package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexInformation;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

import lombok.Data;

/**
 * @author George Popides
 */

@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
@SpringIntegrationTest
public class IndexOperationTransportTests {
	@Autowired
	protected ElasticsearchOperations operations;

	@Test // #1646
	@DisplayName("should return info of all indices using transport template")
	public void shouldReturnInformationListOfAllIndices() {
		String indexName = "test-index-transport-information-list";
		IndexOperations indexOps = operations.indexOps(EntityWithSettingsAndMappingsTransport.class);

		indexOps.create();
		indexOps.putMapping();

		indexOps.alias(new AliasActions(
				new AliasAction.Add(AliasActionParameters.builder().withIndices(indexName)
						.withAliases("alias")
						.build())));

		List<IndexInformation> indexInformationList = indexOps.getInformation();

		IndexInformation indexInformation = indexInformationList.get(0);

		assertThat(indexInformationList.size()).isEqualTo(1);

		assertThat(indexInformation.getSettings().get("index.number_of_shards")).isEqualTo("1");
		assertThat(indexInformation.getSettings().get("index.number_of_replicas")).isEqualTo("0");
		assertThat(indexInformation.getSettings().get("index.analysis.analyzer.emailAnalyzer.type")).isEqualTo("custom");

		assertThat(indexInformation.getMappings()).containsKey("properties");

		assertThat(indexInformation.getName()).isEqualTo(indexName);
		assertThat(indexInformation.getMappings()).isInstanceOf(org.springframework.data.elasticsearch.core.document.Document.class);
		assertThat(indexInformation.getSettings()).isInstanceOf(org.springframework.data.elasticsearch.core.document.Document.class);
		assertThat(indexInformation.getAliases()).isInstanceOf(List.class);
	}



	@Data
	@Document(indexName = "test-index-transport-information-list", createIndex = false)
	@Setting(settingPath = "settings/test-settings.json")
	@Mapping(mappingPath = "mappings/test-mappings.json")
	private static class EntityWithSettingsAndMappingsTransport {
		@Id
		String id;
	}
}
