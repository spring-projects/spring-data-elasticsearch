package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.*;

/**
 * Marks a property to be populated with the result of a scripted field retrieved from an Elasticsearch response.
 * @author Ryan Murfitt
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ScriptedField {

    /**
     * (Optional) The name of the scripted field. Defaults to
     * the field name.
     */
    String name() default "";

}
