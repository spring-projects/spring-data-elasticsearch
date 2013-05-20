/*
 * Copyright 2013 the original author or authors.
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

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.query.FacetRequest;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 */

class MappingBuilder {

    public static final String FIELD_STORE = "store";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_INDEX = "index";
    public static final String FIELD_SEARCH_ANALYZER = "search_analyzer";
    public static final String FIELD_INDEX_ANALYZER = "index_analyzer";
    public static final String FIELD_PROPERTIES = "properties";

    public static final String INDEX_VALUE_NOT_ANALYZED = "not_analyzed";
    public static final String TYPE_VALUE_STRING = "string";
    public static final String TYPE_VALUE_OBJECT = "object";

    private static SimpleTypeHolder SIMPLE_TYPE_HOLDER = new SimpleTypeHolder();

    static XContentBuilder buildMapping(Class clazz, String indexType, String idFieldName) throws IOException {
        XContentBuilder xContentBuilder = jsonBuilder().startObject().startObject(indexType).startObject(FIELD_PROPERTIES);

        mapEntity(xContentBuilder, clazz, true, idFieldName, EMPTY);

        return xContentBuilder.endObject().endObject().endObject();
    }

    private static void mapEntity(XContentBuilder xContentBuilder, Class clazz, boolean isRootObject, String idFieldName,
                                  String nestedObjectFieldName) throws IOException {

        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

        if (!isRootObject && isAnyPropertyAnnotatedAsField(fields)) {
            xContentBuilder.startObject(nestedObjectFieldName).field(FIELD_TYPE, TYPE_VALUE_OBJECT).startObject(FIELD_PROPERTIES);
        }

        for (java.lang.reflect.Field field : fields) {
            if (isEntity(field)) {
                mapEntity(xContentBuilder, field.getType(), false, EMPTY, field.getName());
            }
            Field fieldAnnotation = field.getAnnotation(Field.class);
            if (isRootObject && fieldAnnotation != null && isIdField(field, idFieldName)) {
                applyDefaultIdFieldMapping(xContentBuilder, field);
            } else if (fieldAnnotation != null) {
                if ((fieldAnnotation.sortable() || fieldAnnotation.facetable()) && TYPE_VALUE_STRING.equals(fieldAnnotation.type())) {
                    addMultiFieldMapping(xContentBuilder, field, fieldAnnotation);
                } else {
                    addSimpleFieldMapping(xContentBuilder, field, fieldAnnotation);
                }
            }
        }

        if (!isRootObject && isAnyPropertyAnnotatedAsField(fields)) {
            xContentBuilder.endObject().endObject();
        }

    }

    private static void applyDefaultIdFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field)
            throws IOException {
        xContentBuilder.startObject(field.getName())
                .field(FIELD_TYPE, TYPE_VALUE_STRING)
                .field(FIELD_INDEX, INDEX_VALUE_NOT_ANALYZED);
        xContentBuilder.endObject();
    }

    /**
     * Apply mapping for a single @Field annotation
     *
     * @param xContentBuilder
     * @param field
     * @param fieldAnnotation
     * @throws IOException
     */
    private static void addSimpleFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field,
                                              Field fieldAnnotation) throws IOException {
        xContentBuilder.startObject(field.getName());
        xContentBuilder.field(FIELD_STORE, fieldAnnotation.store());
        if (isNotBlank(fieldAnnotation.type())) {
            xContentBuilder.field(FIELD_TYPE, fieldAnnotation.type());
        }
        if (isNotBlank(fieldAnnotation.index())) {
            xContentBuilder.field(FIELD_INDEX, fieldAnnotation.index());
        }
        if (isNotBlank(fieldAnnotation.searchAnalyzer())) {
            xContentBuilder.field(FIELD_SEARCH_ANALYZER, fieldAnnotation.searchAnalyzer());
        }
        if (isNotBlank(fieldAnnotation.indexAnalyzer())) {
            xContentBuilder.field(FIELD_INDEX_ANALYZER, fieldAnnotation.indexAnalyzer());
        }
        xContentBuilder.endObject();
    }

    /**
     * Multi field mappings for string type fields, support for sorts and facets
     *
     * @param builder
     * @param field
     * @param annotation
     * @throws IOException
     */
    private static void addMultiFieldMapping(XContentBuilder builder, java.lang.reflect.Field field,
                                             Field annotation) throws IOException {
        builder.startObject(field.getName());
        builder.field(FIELD_TYPE, "multi_field");
        builder.startObject("fields");
        //add standard field
        addSimpleFieldMapping(builder, field, annotation);
        //facet field - untouched, not analise, stored
        if (annotation.facetable()) {
            addFacetMapping(builder, field, annotation);
        }
        //sort field - lowercase, not analise, stored
        if (annotation.sortable()) {
            addSortMapping(builder, field, annotation);
        }
        builder.endObject();
        builder.endObject();
    }

    /**
     * Facet field for string type, for other types we don't need it(long, int, double, float)
     *
     * @param builder
     * @param field
     * @param annotation
     * @throws IOException
     */
    private static void addFacetMapping(XContentBuilder builder, java.lang.reflect.Field field, Field annotation) throws IOException {
        builder.startObject(FacetRequest.FIELD_UNTOUCHED)
                .field(FIELD_TYPE, TYPE_VALUE_STRING)
                .field(FIELD_INDEX, INDEX_VALUE_NOT_ANALYZED)
                .field(FIELD_STORE, true);
        builder.endObject();
    }

    /**
     * Sort field for string type, for other types we don't need it(long, int, double, float)
     * value of the field should be converted to lowercase and not analise
     *
     * @param builder
     * @param field
     * @param annotation
     * @throws IOException
     */
    private static void addSortMapping(XContentBuilder builder, java.lang.reflect.Field field, Field annotation) throws IOException {
        builder.startObject(FacetRequest.FIELD_SORT)
                .field(FIELD_TYPE, TYPE_VALUE_STRING)
                .field(FIELD_INDEX, "keyword")
                .field(FIELD_STORE, true);
        builder.endObject();
    }

    private static boolean isEntity(java.lang.reflect.Field field) {
        TypeInformation typeInformation = ClassTypeInformation.from(field.getType());
        TypeInformation<?> actualType = typeInformation.getActualType();
        boolean isComplexType = actualType == null ? false : !SIMPLE_TYPE_HOLDER.isSimpleType(actualType.getType());
        return isComplexType && !actualType.isCollectionLike() && !Map.class.isAssignableFrom(typeInformation.getType());
    }

    private static boolean isAnyPropertyAnnotatedAsField(java.lang.reflect.Field[] fields) {
        if (fields != null) {
            for (java.lang.reflect.Field field : fields) {
                if (field.isAnnotationPresent(Field.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isIdField(java.lang.reflect.Field field, String idFieldName) {
        return idFieldName.equals(field.getName());
    }
}
