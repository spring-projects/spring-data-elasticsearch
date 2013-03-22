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
package org.springframework.data.elasticsearch.core;


import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.data.elasticsearch.annotations.Field;
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
 */

class MappingBuilder {

    private static SimpleTypeHolder SIMPLE_TYPE_HOLDER = new SimpleTypeHolder();

    static XContentBuilder buildMapping(Class clazz, String indexType, String idFieldName) throws IOException {
        XContentBuilder xContentBuilder = jsonBuilder()
                .startObject().startObject(indexType).startObject("properties");

        mapEntity(xContentBuilder, clazz, true, idFieldName, EMPTY);

       return xContentBuilder.endObject().endObject().endObject();
    }

    private static void mapEntity(XContentBuilder xContentBuilder,
                                  Class clazz,
                                  boolean isRootObject,
                                  String idFieldName,
                                  String nestedObjectFieldName) throws IOException{

        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

        if(!isRootObject && isAnyPropertyAnnotatedAsField(fields)){
            xContentBuilder.startObject(nestedObjectFieldName)
                    .field("type", "object")
                    .startObject("properties");
        }

        for(java.lang.reflect.Field field : fields){
            if(isEntity(field)){
                mapEntity(xContentBuilder, field.getType(), false, EMPTY, field.getName());
            }
            Field fieldAnnotation = field.getAnnotation(Field.class);
            if(isRootObject && fieldAnnotation != null && isIdField(field, idFieldName)){
                applyDefaultIdFieldMapping(xContentBuilder, field);
            }else if(fieldAnnotation != null){
                applyFieldAnnotationMapping(xContentBuilder, field, fieldAnnotation);
            }
        }

        if(!isRootObject && isAnyPropertyAnnotatedAsField(fields)){
            xContentBuilder.endObject().endObject();
        }

    }

    private static void applyDefaultIdFieldMapping(XContentBuilder xContentBuilder, java.lang.reflect.Field field) throws IOException {
        xContentBuilder.startObject(field.getName())
                .field("type", "string")
                .field("index", "not_analyzed")
                .endObject();
    }

    private static void applyFieldAnnotationMapping(XContentBuilder xContentBuilder,
                                                    java.lang.reflect.Field field,
                                                    Field fieldAnnotation) throws IOException {
        xContentBuilder.startObject(field.getName());
        xContentBuilder.field("store", fieldAnnotation.store());
        if(isNotBlank(fieldAnnotation.type())){
            xContentBuilder.field("type", fieldAnnotation.type());
        }
        if(isNotBlank(fieldAnnotation.index())){
            xContentBuilder.field("index", fieldAnnotation.index());
        }
        if(isNotBlank(fieldAnnotation.searchAnalyzer())){
            xContentBuilder.field("search_analyzer", fieldAnnotation.searchAnalyzer());
        }
        if(isNotBlank(fieldAnnotation.indexAnalyzer())){
            xContentBuilder.field("index_analyzer", fieldAnnotation.indexAnalyzer());
        }
        xContentBuilder.endObject();
    }

    private static boolean isEntity(java.lang.reflect.Field field) {
        TypeInformation typeInformation = ClassTypeInformation.from(field.getType());
        TypeInformation<?> actualType = typeInformation.getActualType();
        boolean isComplexType = actualType == null ? false : !SIMPLE_TYPE_HOLDER.isSimpleType(actualType.getType());
        return isComplexType &&  !actualType.isCollectionLike() && !Map.class.isAssignableFrom(typeInformation.getType());
    }

    private static boolean isAnyPropertyAnnotatedAsField(java.lang.reflect.Field[] fields){
        if(fields != null){
            for(java.lang.reflect.Field field : fields){
                if (field.isAnnotationPresent(Field.class)){
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isIdField(java.lang.reflect.Field field, String idFieldName){
        return idFieldName.equals(field.getName());
    }
}
