package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.*;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface MultiField {

    public Field mainField();

    public NestedField[] otherFields() default {};
}
