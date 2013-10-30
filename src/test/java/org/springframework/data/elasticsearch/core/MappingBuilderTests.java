package org.springframework.data.elasticsearch.core;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.SampleTransientEntity;
import org.springframework.data.elasticsearch.SimpleRecursiveEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Stuart Stevenson
 * @author Jakub Vavrik
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class MappingBuilderTests {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void shouldNotFailOnCircularReference() {
        elasticsearchTemplate.createIndex(SimpleRecursiveEntity.class);
        elasticsearchTemplate.putMapping(SimpleRecursiveEntity.class);
    }

    @Test
    public void testInfiniteLoopAvoidance() throws IOException {
        final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\"" +
                "type\":\"string\",\"index\":\"not_analyzed\",\"search_analyzer\":\"standard\"," +
                "\"index_analyzer\":\"standard\"}}}}";

        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleTransientEntity.class, "mapping", "id");
        assertThat(xContentBuilder.string(), is(expected));
    }

}
