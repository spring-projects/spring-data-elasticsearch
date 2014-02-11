package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.*;

/**
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
