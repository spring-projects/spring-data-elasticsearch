package org.springframework.data.elasticsearch.core;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
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
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class SimpleDynamicTemplatesMappingTests {

    @Test
    public void testCorrectDynamicTemplatesMappings() throws IOException {
        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleDynamicTemplatesEntity.class,
                "test-dynamictemplatestype", "id", null);
        String EXPECTED_MAPPING_ONE = "{\"test-dynamictemplatestype\":{\"dynamic_templates\":" +
                "[{\"with_custom_analyzer\":{" +
                "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"}," +
                "\"path_match\":\"names.*\"}}]," +
                "\"properties\":{\"names\":{\"type\":\"object\"}}}}";
        Assert.assertEquals(EXPECTED_MAPPING_ONE, xContentBuilder.string());
    }

    @Test
    public void testCorrectDynamicTemplatesMappingsTwo() throws IOException {
        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleDynamicTemplatesEntityTwo.class,
                "test-dynamictemplatestype", "id", null);
        String EXPECTED_MAPPING_TWO = "{\"test-dynamictemplatestype\":{\"dynamic_templates\":" +
                "[{\"with_custom_analyzer\":{" +
                "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"}," +
                "\"path_match\":\"names.*\"}}," +
                "{\"participantA1_with_custom_analyzer\":{" +
                "\"mapping\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"}," +
                "\"path_match\":\"participantA1.*\"}}]," +
                "\"properties\":{\"names\":{\"type\":\"object\"}}}}";
        Assert.assertEquals(EXPECTED_MAPPING_TWO, xContentBuilder.string());
    }
}
