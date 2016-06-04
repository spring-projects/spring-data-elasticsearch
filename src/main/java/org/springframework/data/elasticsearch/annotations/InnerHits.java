package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.*;

/**
 * Created by flefebure on 06/04/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface InnerHits {
    String path();
}
