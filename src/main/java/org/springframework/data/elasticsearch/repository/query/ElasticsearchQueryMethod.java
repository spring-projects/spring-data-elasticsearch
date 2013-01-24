package org.springframework.data.elasticsearch.repository.query;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformationCreator;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;


public class ElasticsearchQueryMethod extends QueryMethod {

    private final ElasticsearchEntityInformation<?, ?> entityInformation;
    private Method method;

    public ElasticsearchQueryMethod(Method method, RepositoryMetadata metadata, ElasticsearchEntityInformationCreator elasticsearchEntityInformationCreator) {
        super(method, metadata);
        this.entityInformation = elasticsearchEntityInformationCreator.getEntityInformation(metadata.getReturnedDomainClass(method));
        this.method = method;
    }

    public boolean hasAnnotatedQuery() {
        return getQueryAnnotation() != null;
    }

    public String getAnnotatedQuery() {
        String query = (String) AnnotationUtils.getValue(getQueryAnnotation(), "value");
        return StringUtils.hasText(query) ? query : null;
    }

    private Query getQueryAnnotation() {
        return this.method.getAnnotation(Query.class);
    }

}
