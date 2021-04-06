package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mikaellemos
 *
 * Annotation used to map the "id" field within _source
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IdSourceMap {
}
