package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.elasticsearch.core.SearchHit;

/**
 * Specifies that this field is used for storing the document score.
 * 
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 * @since 3.1
 * @deprecated since 4.0, use {@link SearchHit#getScore()} instead
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@ReadOnlyProperty
@Deprecated
public @interface Score {
}
