package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.elasticsearch.search.suggest.completion.context.ContextMapping;

/**
 * Based on reference doc - https://www.elastic.co/guide/en/elasticsearch/reference/current/suggester-context.html
 *
 * @author Robert Gruendler
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface CompletionContext {

	String name();

	ContextMapping.Type type();

	String precision() default "";

	String path() default "";
}
