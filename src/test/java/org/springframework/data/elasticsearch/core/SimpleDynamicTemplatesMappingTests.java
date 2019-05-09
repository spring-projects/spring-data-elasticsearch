package org.springframework.data.elasticsearch.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.DynamicTemplates;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Dynamic templates tests
 *
 * @author Petr Kukral
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class SimpleDynamicTemplatesMappingTests extends MappingContextBaseTests {

	@Test // DATAES-568
	public void testCorrectDynamicTemplatesMappings() throws IOException {

		String mapping = getMappingBuilder().buildPropertyMapping(SampleDynamicTemplatesEntity.class);

		String EXPECTED_MAPPING_ONE = "{\"test-dynamictemplatestype\":{\"dynamic_templates\":"
				+ "[{\"with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"names.*\"}}]," + "\"properties\":{\"names\":{\"type\":\"object\"}}}}";

		assertEquals(EXPECTED_MAPPING_ONE, mapping);
	}

	@Test // DATAES-568
	public void testCorrectDynamicTemplatesMappingsTwo() throws IOException {

		String mapping = getMappingBuilder().buildPropertyMapping(SampleDynamicTemplatesEntityTwo.class);
		String EXPECTED_MAPPING_TWO = "{\"test-dynamictemplatestype\":{\"dynamic_templates\":"
				+ "[{\"with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"names.*\"}}," + "{\"participantA1_with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"participantA1.*\"}}]," + "\"properties\":{\"names\":{\"type\":\"object\"}}}}";

		assertEquals(EXPECTED_MAPPING_TWO, mapping);
	}

	/**
	 * @author Petr Kukral
	 */
	@Document(indexName = "test-dynamictemplates", type = "test-dynamictemplatestype", indexStoreType = "memory",
			shards = 1, replicas = 0, refreshInterval = "-1")
	@DynamicTemplates(mappingPath = "/mappings/test-dynamic_templates_mappings.json")
	static class SampleDynamicTemplatesEntity {

		@Id private String id;

		@Field(type = FieldType.Object) private Map<String, String> names = new HashMap<String, String>();
	}

	/**
	 * @author Petr Kukral
	 */
	@Document(indexName = "test-dynamictemplates", type = "test-dynamictemplatestype", indexStoreType = "memory",
			shards = 1, replicas = 0, refreshInterval = "-1")
	@DynamicTemplates(mappingPath = "/mappings/test-dynamic_templates_mappings_two.json")
	static class SampleDynamicTemplatesEntityTwo {

		@Id private String id;

		@Field(type = FieldType.Object) private Map<String, String> names = new HashMap<String, String>();
	}

}
