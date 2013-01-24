package org.springframework.data.elasticsearch.annotations;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Query {

    /**
     * Elasticsearch query to be used when executing query. May contain placeholders eg. ?0
     *
     * @return
     */
    String value() default "";

    /**
     * Named Query Named looked up by repository.
     *
     * @return
     */
    String name() default "";

}
