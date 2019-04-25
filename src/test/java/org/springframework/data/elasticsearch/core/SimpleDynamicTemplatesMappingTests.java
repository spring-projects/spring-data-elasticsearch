package org.springframework.data.elasticsearch.core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.elasticsearch.entities.SampleDynamicTemplatesEntity;
import org.springframework.data.elasticsearch.entities.SampleDynamicTemplatesEntityTwo;
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

	@Test
	public void testCorrectDynamicTemplatesMappings() throws IOException {
		String mapping = getMappingBuilder().buildMapping(SampleDynamicTemplatesEntity.class);

		String EXPECTED_MAPPING_ONE = "{\"test-dynamictemplatestype\":{\"dynamic_templates\":"
				+ "[{\"with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"names.*\"}}]," + "\"properties\":{\"names\":{\"type\":\"object\"}}}}";
		Assert.assertEquals(EXPECTED_MAPPING_ONE, mapping);
	}

	@Test
	public void testCorrectDynamicTemplatesMappingsTwo() throws IOException {
		String mapping = getMappingBuilder().buildMapping(SampleDynamicTemplatesEntityTwo.class);
		String EXPECTED_MAPPING_TWO = "{\"test-dynamictemplatestype\":{\"dynamic_templates\":"
				+ "[{\"with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"names.*\"}}," + "{\"participantA1_with_custom_analyzer\":{"
				+ "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"},"
				+ "\"path_match\":\"participantA1.*\"}}]," + "\"properties\":{\"names\":{\"type\":\"object\"}}}}";
		Assert.assertEquals(EXPECTED_MAPPING_TWO, mapping);
	}
}
