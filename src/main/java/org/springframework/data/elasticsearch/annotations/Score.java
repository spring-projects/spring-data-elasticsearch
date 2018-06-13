package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.ReadOnlyProperty;

/**
 * Specifies that this field is used for storing the document score.
 * 
 * @author Sascha Woo
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@ReadOnlyProperty
public @interface Score {}
