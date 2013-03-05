/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.query;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformationCreator;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 *  ElasticsearchQueryMethod
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
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
