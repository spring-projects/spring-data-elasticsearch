package org.springframework.data.elasticsearch.repository.support;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchRepositoryFactoryTest {

    @Mock
    private ElasticsearchOperations operations;
    private ElasticsearchConverter converter;
    private ElasticsearchRepositoryFactory factory;
    MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext= new SimpleElasticsearchMappingContext();

    @Before
    public void before(){
        converter = new MappingElasticsearchConverter(mappingContext);
        when(operations.getElasticsearchConverter()).thenReturn(converter);
        factory = new ElasticsearchRepositoryFactory(operations);
    }



    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionGivenQueryDslRepository(){
        //given
        RepositoryMetadata metadata = new DefaultRepositoryMetadata(QueryDslPredicateExecutor.class);
        //when
        factory.getRepositoryBaseClass(metadata);
    }

}
