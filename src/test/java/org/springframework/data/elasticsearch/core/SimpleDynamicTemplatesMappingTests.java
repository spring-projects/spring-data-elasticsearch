/*
 * Copyright 2018-2019 the original author or authors.
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

		assertThat(mapping).isEqualTo(EXPECTED_MAPPING_ONE);
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

		assertThat(mapping).isEqualTo(EXPECTED_MAPPING_TWO);
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
