package org.springframework.data.elasticsearch.annotations;

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

/**
 * SourceFilters
 *
 * @author Alexander Torres
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface SourceFilters {
    /**
     * Fields to include in the query search hits. (e.g. ["field1", "field2"])
     * @return
     */
    String includes() default "";

    /**
     * Fields to exclude from the query search hits. (e.g. ["field1", "field2"])
     * @return
     */
    String excludes() default "";
}
