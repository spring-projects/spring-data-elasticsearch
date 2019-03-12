package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Persistent;

/**
 * Elasticsearch dynamic templates mapping.
 * This annotation is handy if you prefer apply dynamic templates on fields with annotation e.g. {@link Field}
 * with type = FieldType.Object etc. instead of static mapping on Document via {@link Mapping} annotation.
 * DynamicTemplates annotation is ommited if {@link Mapping} annotation is used.
 *
 * @author Petr Kukral
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DynamicTemplates {

	String mappingPath() default "";

}
