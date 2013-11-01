package org.springframework.data.elasticsearch.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repositories.ComplexElasticsearchRepositoryCustom;

/**
 * @author Artur Konczak
 */
public class ComplexElasticsearchRepositoryImpl implements ComplexElasticsearchRepositoryCustom {

    @Autowired
    private ElasticsearchTemplate template;

    @Override
    public String doSomethingSpecial() {
        assert(template.getElasticsearchConverter()!=null);
        return "2+2=4";
    }


}
