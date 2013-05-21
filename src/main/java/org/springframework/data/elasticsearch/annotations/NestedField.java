package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NestedField {

    String dotSuffix();

    FieldType type();

    FieldIndex index() default FieldIndex.analyzed;

    boolean store() default false;

    String searchAnalyzer() default "";

    String indexAnalyzer() default "";
}
