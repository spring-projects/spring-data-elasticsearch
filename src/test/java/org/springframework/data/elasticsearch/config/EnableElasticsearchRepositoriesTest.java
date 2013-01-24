package org.springframework.data.elasticsearch.config;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repositories.SampleElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableElasticsearchRepositoriesTest {

    @Configuration
    @EnableElasticsearchRepositories(basePackages = "org.springframework.data.elasticsearch.repositories")
    static class Config {

        @Bean
        public ElasticsearchOperations elasticsearchTemplate() {
            return new ElasticsearchTemplate(nodeBuilder().local(true).node().client());
        }
    }

    @Autowired
    private SampleElasticsearchRepository repository;

    @Test
    public void bootstrapsRepository() {
        assertThat(repository, is(notNullValue()));
    }

}
