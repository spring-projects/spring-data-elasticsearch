package org.springframework.data.elasticsearch.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.repositories.SampleElasticsearchRepository;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("namespace.xml")
public class ElasticsearchNamespaceHandlerTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void shouldCreatesNodeClient() {
        assertThat(context.getBean(NodeClientFactoryBean.class), is(notNullValue()));
        assertThat(context.getBean(NodeClientFactoryBean.class), is(instanceOf(NodeClientFactoryBean.class)));
    }

    @Test
    public void shouldCreateTransportClient() {
        assertThat(context.getBean(TransportClientFactoryBean.class), is(notNullValue()));
        assertThat(context.getBean(TransportClientFactoryBean.class), is(instanceOf(TransportClientFactoryBean.class)));
    }

    @Test
    public void shouldCreateRepository(){
        assertThat(context.getBean(TransportClientFactoryBean.class), is(notNullValue()));
        assertThat(context.getBean(SampleElasticsearchRepository.class), is(instanceOf(SampleElasticsearchRepository.class)));
    }

}
