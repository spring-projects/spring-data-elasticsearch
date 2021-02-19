package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexInformation;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author George Popides
 */

@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
public class IndexOperationTransportTests extends IndexOperationTests {
	@Autowired
	protected ElasticsearchOperations operations;

	@Test // #1646
	@DisplayName("should return info of all indices using TRANSPORT template with no aliases")
	public void shouldReturnInformationListOfAllIndicesNoAliases() throws JSONException {
		String indexName = "test-index-information-list";
		IndexOperations indexOps = operations.indexOps(EntityWithSettingsAndMappings.class);

		indexOps.create();
		indexOps.putMapping();

		List<IndexInformation> indexInformationList = indexOps.getInformation();

		IndexInformation indexInformation = indexInformationList.get(0);

		assertThat(indexInformationList.size()).isEqualTo(1);

		assertThat(indexInformation.getName()).isEqualTo(indexName);
		assertThat(indexInformation.getSettings().get("index.number_of_shards")).isEqualTo("1");
		assertThat(indexInformation.getSettings().get("index.number_of_replicas")).isEqualTo("0");
		assertThat(indexInformation.getSettings().get("index.analysis.analyzer.emailAnalyzer.type")).isEqualTo("custom");
		assertThat(indexInformation.getAliases()).isEmpty();

		String expectedMappings = "{\"properties\":{\"email\":{\"type\":\"text\",\"analyzer\":\"emailAnalyzer\"}}}";

		JSONAssert.assertEquals(expectedMappings, indexInformation.getMappings().toJson(), false);
	}
}
