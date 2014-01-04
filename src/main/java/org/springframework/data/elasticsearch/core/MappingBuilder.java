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
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.io.IOException;
import java.util.Arrays;
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
    public static final String FIELD_FORMAT = "format";
    public static final String FIELD_SEARCH_ANALYZER = "search_analyzer";
    public static final String FIELD_INDEX_ANALYZER = "index_analyzer";
    public static final String FIELD_PROPERTIES = "properties";

    public static final String INDEX_VALUE_NOT_ANALYZED = "not_analyzed";
    public static final String TYPE_VALUE_STRING = "string";
    public static final String TYPE_VALUE_GEO_POINT = "geo_point";

    private static SimpleTypeHolder SIMPLE_TYPE_HOLDER = new SimpleTypeHolder();

    static XContentBuilder buildMapping(Class clazz, String indexType, String idFieldName) throws IOException {
        XContentBuilder xContentBuilder = jsonBuilder().startObject().startObject(indexType).startObject(FIELD_PROPERTIES);

        mapEntity(xContentBuilder, clazz, true, idFieldName, EMPTY, false);

        return xContentBuilder.endObject().endObject().endObject();
    }

    private static void mapEntity(XContentBuilder xContentBuilder, Class clazz, boolean isRootObject, String idFieldName,
                                  String nestedObjectFieldName, boolean nestedAnnotaion) throws IOException {

        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

        if (!isRootObject && (isAnyPropertyAnnotatedAsField(fields) || nestedAnnotaion)) {
            String type = FieldType.Object.toString().toLowerCase();
            if(nestedAnnotaion){
                type =  FieldType.Nested.toString().toLowerCase();
            }
            xContentBuilder.startObject(nestedObjectFieldName).field(FIELD_TYPE, type).startObject(FIELD_PROPERTIES);
        }

        for (java.lang.reflect.Field field : fields) {

            if (field.isAnnotationPresent(Transient.class) || isInIgnoreFields(field)) {
                continue;
            }            
            
            boolean isGeoField = isGeoField(field);

            Field singleField = field.getAnnotation(Field.class);
            if (!isGeoField && isEntity(field) && !isAnnotated(field)) {
                if(singleField == null){
                    continue;
                }
                boolean nestedField = isNestedField(field);
                mapEntity(xContentBuilder, getFieldType(field), false, EMPTY, field.getName(), nestedField);
                if(nestedField){
                    continue;
                }
            }

            MultiField multiField = field.getAnnotation(MultiField.class);

            if (isGeoField) {
                applyGeoPointFieldMapping(xContentBuilder, field);
            }

            if (isRootObject && singleField != null && isIdField(field, idFieldName)) {
                applyDefaultIdFieldMapping(xContentBuilder, field);
            } else if (multiField != null) {
                addMultiFieldMapping(xContentBuilder, field, multiField);
            } else if (singleField != null) {
                addSingleFieldMapping(xContentBuilder, field, singleField);
            }
        }

        if (!isRootObject && isAnyPropertyAnnotatedAsField(fields)) {
            xContentBuilder.endObject().endObject();
        }

    }

    private static boolean isAnnotated(java.lang.reflect.Field field) {
        return field.getAnnotation(Field.class)==null && field.getAnnotation(MultiField.class)==null && field.getAnnotation(GeoPointField.class)==null;
    }

    private static void applyGeoPointFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field) throws IOException {
        xContentBuilder.startObject(field.getName());
        xContentBuilder.field(FIELD_TYPE, TYPE_VALUE_GEO_POINT)
                .endObject();
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
    private static void addSingleFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field,
                                              Field fieldAnnotation) throws IOException {
        xContentBuilder.startObject(field.getName());
        xContentBuilder.field(FIELD_STORE, fieldAnnotation.store());
        if (FieldType.Auto != fieldAnnotation.type()) {
            xContentBuilder.field(FIELD_TYPE, fieldAnnotation.type().name().toLowerCase());
            if (FieldType.Date == fieldAnnotation.type() && DateFormat.none != fieldAnnotation.format()) {
                xContentBuilder.field(FIELD_FORMAT, DateFormat.custom == fieldAnnotation.format()
                        ? fieldAnnotation.pattern() : fieldAnnotation.format());
            }
        }
        if (FieldIndex.not_analyzed == fieldAnnotation.index()) {
            xContentBuilder.field(FIELD_INDEX, fieldAnnotation.index().name().toLowerCase());
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
     * Apply mapping for a single nested @Field annotation
     *
     * @param builder
     * @param field
     * @param annotation
     * @throws IOException
     */
    private static void addNestedFieldMapping(XContentBuilder builder, java.lang.reflect.Field field,
                                              NestedField annotation) throws IOException {
        builder.startObject(field.getName() + "." + annotation.dotSuffix());
        builder.field(FIELD_STORE, annotation.store());
        if (FieldType.Auto != annotation.type()) {
            builder.field(FIELD_TYPE, annotation.type().name().toLowerCase());
        }
        if (FieldIndex.not_analyzed == annotation.index()) {
            builder.field(FIELD_INDEX, annotation.index().name().toLowerCase());
        }
        if (isNotBlank(annotation.searchAnalyzer())) {
            builder.field(FIELD_SEARCH_ANALYZER, annotation.searchAnalyzer());
        }
        if (isNotBlank(annotation.indexAnalyzer())) {
            builder.field(FIELD_INDEX_ANALYZER, annotation.indexAnalyzer());
        }
        builder.endObject();
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
                                             MultiField annotation) throws IOException {
        builder.startObject(field.getName());
        builder.field(FIELD_TYPE, "multi_field");
        builder.startObject("fields");
        //add standard field
        addSingleFieldMapping(builder, field, annotation.mainField());
        for (NestedField nestedField : annotation.otherFields()) {
            addNestedFieldMapping(builder, field, nestedField);
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

    protected static boolean isEntity(java.lang.reflect.Field field) {
        TypeInformation typeInformation = ClassTypeInformation.from(field.getType());
        Class<?> clazz = getFieldType(field);
        boolean isComplexType = !SIMPLE_TYPE_HOLDER.isSimpleType(clazz);
        return isComplexType && !Map.class.isAssignableFrom(typeInformation.getType());
    }

    protected static Class<?> getFieldType(java.lang.reflect.Field field) {
        Class<?> clazz = field.getType();
        TypeInformation typeInformation = ClassTypeInformation.from(clazz);
        if (typeInformation.isCollectionLike()) {
            clazz = GenericCollectionTypeResolver.getCollectionFieldType(field) != null ? GenericCollectionTypeResolver.getCollectionFieldType(field) : typeInformation.getComponentType().getType();
        }
        return clazz;
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

    private static boolean isInIgnoreFields(java.lang.reflect.Field field) {
        Field fieldAnnotation = field.getAnnotation(Field.class);
        if ( null != fieldAnnotation ) {
            String [] ignoreFields = fieldAnnotation.ignoreFields();
            return Arrays.asList(ignoreFields).contains(field.getName());
        }
        return false;
    }

    private static boolean isNestedField(java.lang.reflect.Field field) {
        Field fieldAnnotation = field.getAnnotation(Field.class);
        return fieldAnnotation != null && (FieldType.Nested == fieldAnnotation.type() || FieldType.Object == fieldAnnotation.type());
    }

    private static boolean isGeoField(java.lang.reflect.Field field) {
        return field.getType() == GeoPoint.class || field.getAnnotation(GeoPointField.class) != null;
    }

}
