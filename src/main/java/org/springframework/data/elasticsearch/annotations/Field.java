/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 * @author Jakub Vavrik
 * @author Kevin Leturc
 * @author Peter-Josef Meisch
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface Field {

	/**
	 * Alias for {@link #name}.
	 * @since 3.2
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The <em>name</em> to be used to store the field inside the document.
	 * <p>If not set, the name of the annotated property is used.
	 * @since 3.2
	 */
	@AliasFor("value")
	String name() default "";

	FieldType type() default FieldType.Auto;

	boolean index() default true;

	DateFormat format() default DateFormat.none;

	String pattern() default "";

	boolean store() default false;

	boolean fielddata() default false;

	String searchAnalyzer() default "";

	String analyzer() default "";

	String normalizer() default "";

	String[] ignoreFields() default {};

	boolean includeInParent() default false;

	String[] copyTo() default {};
}
