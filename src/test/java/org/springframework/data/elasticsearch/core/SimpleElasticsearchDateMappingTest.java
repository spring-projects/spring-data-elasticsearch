package org.springframework.data.elasticsearch.core;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.elasticsearch.SampleDateMappingEntity;

import java.beans.IntrospectionException;
import java.io.IOException;

/**
 * @author Jakub Vavrik
 */
public class SimpleElasticsearchDateMappingTest {
    private static final String EXPECTED_MAPPING = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true," +
            "\"type\":\"string\",\"index\":\"not_analyzed\",\"search_analyzer\":\"standard\",\"index_analyzer\"" +
            ":\"standard\"},\"customFormatDate\":{\"store\":false,\"type\":\"date\",\"format\":\"dd.MM.yyyy hh:mm\"}," +
            "\"defaultFormatDate\":{\"store\":false,\"type\":\"date\"},\"basicFormatDate\":{\"store\":false,\"" +
            "type\":\"date\",\"format\":\"basic_date\"}}}}";

    @Test
    public void testCorrectDateMappings() throws NoSuchFieldException, IntrospectionException, IOException {
        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleDateMappingEntity.class, "mapping", "id", null);
        Assert.assertEquals(EXPECTED_MAPPING, xContentBuilder.string());
    }
}
