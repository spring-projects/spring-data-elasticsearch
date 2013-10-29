package org.springframework.data.elasticsearch.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Stuart Stevenson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class MappingBuilderTests {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void shouldNotFailOnCircularReference() {
        elasticsearchTemplate.createIndex(CircularObject.class);
        elasticsearchTemplate.putMapping(CircularObject.class);
    }

}
