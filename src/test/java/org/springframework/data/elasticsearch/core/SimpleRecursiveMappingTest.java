package org.springframework.data.elasticsearch.core;

import junit.framework.Assert;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;
import org.springframework.data.elasticsearch.SampleRecursiveMappingEntity;

import java.io.IOException;

/**
 * Test that classes that have fields of same type do not end in infinite loop when mapping.
 */
public class SimpleRecursiveMappingTest {

    private static final String EXPECTED = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\"" +
            "type\":\"string\",\"index\":\"not_analyzed\",\"search_analyzer\":\"standard\"," +
            "\"index_analyzer\":\"standard\"},\"nested\":{\"type\":\"object\",\"properties\":{\"" +
            "something\":{\"store\":false}}},\"nested\":{\"store\":false}}}}";

    @Test
    public void testInfiniteLoopAvoidance() throws IOException {
        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleRecursiveMappingEntity.class, "mapping", "id");
        Assert.assertEquals(EXPECTED, xContentBuilder.string());
    }
}
