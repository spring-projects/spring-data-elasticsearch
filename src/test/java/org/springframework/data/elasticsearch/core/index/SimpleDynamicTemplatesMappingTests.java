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

import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.DynamicTemplates;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.MappingContextBaseTests;
import org.springframework.lang.Nullable;

/**
 * Dynamic templates tests
 *
 * @author Petr Kukral
 * @author Peter-Josef Meisch
 */
public class SimpleDynamicTemplatesMappingTests extends MappingContextBaseTests {

	@Test // DATAES-568
	public void testCorrectDynamicTemplatesMappings() throws JSONException {

		String mapping = getMappingBuilder().buildPropertyMapping(SampleDynamicTemplatesEntity.class);

		String EXPECTED_MAPPING_ONE = "{\"dynamic_templates\":" + "[{\"with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"names.*\"}}]," + "\"properties\":{\"names\":{\"type\":\"object\"}}}";

		assertEquals(EXPECTED_MAPPING_ONE, mapping, false);
	}

	@Test // DATAES-568
	public void testCorrectDynamicTemplatesMappingsTwo() throws JSONException {

		String mapping = getMappingBuilder().buildPropertyMapping(SampleDynamicTemplatesEntityTwo.class);
		String EXPECTED_MAPPING_TWO = "{\"dynamic_templates\":" + "[{\"with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"names.*\"}}," + "{\"participantA1_with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"participantA1.*\"}}]," + "\"properties\":{\"names\":{\"type\":\"object\"}}}";

		assertEquals(EXPECTED_MAPPING_TWO, mapping, false);
	}

	/**
	 * @author Petr Kukral
	 */
	@Document(indexName = "test-dynamictemplates")
	@Setting(indexStoreType = "memory")
	@DynamicTemplates(mappingPath = "/mappings/test-dynamic_templates_mappings.json")
	static class SampleDynamicTemplatesEntity {

		@Nullable
		@Id private String id;

		@Nullable
		@Field(type = FieldType.Object) private final Map<String, String> names = new HashMap<>();
	}

	/**
	 * @author Petr Kukral
	 */
	@Document(indexName = "test-dynamictemplates")
	@Setting(indexStoreType = "memory")
	@DynamicTemplates(mappingPath = "/mappings/test-dynamic_templates_mappings_two.json")
	static class SampleDynamicTemplatesEntityTwo {

		@Nullable
		@Id private String id;

		@Nullable
		@Field(type = FieldType.Object) private final Map<String, String> names = new HashMap<>();
	}

}
