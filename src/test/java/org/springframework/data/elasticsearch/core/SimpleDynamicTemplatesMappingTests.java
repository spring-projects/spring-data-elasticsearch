package org.springframework.data.elasticsearch.core;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.elasticsearch.entities.SampleDynamicTemplatesEntity;
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
    private final String EXPECTED_MAPPING = "{\"test-dynamictemplatestype\":{\"dynamic_templates\":" +
            "[{\"with_custom_analyzer\":{\"mapping\":{\"type\":\"string\",\"indexAnalyzer\":" +
            "\"standard_lowercase_asciifolding\",\"searchAnalyzer\":\"standard_lowercase_asciifolding\"}," +
            "\"path_match\":\"names.*\"}}],\"properties\":{\"names\":{\"store\":false,\"type\":\"object\"}}}}";

    @Test
    public void testCorrectDynamicTemplatesMappings() throws IOException {
        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleDynamicTemplatesEntity.class,
                "test-dynamictemplatestype", "id", null);
        Assert.assertEquals(EXPECTED_MAPPING, xContentBuilder.string());
    }
}
