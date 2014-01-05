/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;


import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.jackson.core.JsonEncoding;
import org.elasticsearch.common.jackson.core.JsonFactory;
import org.elasticsearch.common.jackson.core.JsonGenerator;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.facet.Facet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.facet.DefaultFacetMapper;
import org.springframework.data.elasticsearch.core.facet.FacetResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Artur Konczak
 */
public class DefaultResultMapper extends AbstractResultMapper {

    public DefaultResultMapper(){
        super(new DefaultEntityMapper());
    }

    public DefaultResultMapper(EntityMapper entityMapper) {
        super(entityMapper);
    }

    @Override
    public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        long totalHits = response.getHits().totalHits();
        List<T> results = new ArrayList<T>();
        for (SearchHit hit : response.getHits()) {
            if (hit != null) {
                if (!Strings.isNullOrEmpty(hit.sourceAsString())) {
                    results.add(mapEntity(hit.sourceAsString(), clazz));
                } else {
                    results.add(mapEntity(hit.getFields().values(), clazz));
                }
            }
        }
        List<FacetResult> facets = new ArrayList<FacetResult>();
        if (response.getFacets() != null) {
            for (Facet facet : response.getFacets()) {
                FacetResult facetResult = DefaultFacetMapper.parse(facet);
                if (facetResult != null) {
                    facets.add(facetResult);
                }
            }
        }

        return new FacetedPageImpl<T>(results, pageable, totalHits, facets);
    }

    private <T> T mapEntity(Collection<SearchHitField> values, Class<T> clazz) {
            return mapEntity(buildJSONFromFields(values), clazz);
    }

    private String buildJSONFromFields(Collection<SearchHitField> values) {
        JsonFactory nodeFactory = new JsonFactory();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            JsonGenerator generator = nodeFactory.createGenerator(stream, JsonEncoding.UTF8);
            generator.writeStartObject();
            for (SearchHitField value : values) {
                if (value.getValues().size() > 1) {
                    generator.writeArrayFieldStart(value.getName());
                    for (Object val : value.getValues()) {
                        generator.writeObject(val);
                    }
                    generator.writeEndArray();
                } else {
                    generator.writeObjectField(value.getName(), value.getValue());
                }
            }
            generator.writeEndObject();
            generator.flush();
            return new String(stream.toByteArray(), Charset.forName("UTF-8"));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public <T> T mapResult(GetResponse response, Class<T> clazz) {
        return mapEntity(response.getSourceAsString(),clazz);
    }
}
