/*
 * Copyright 2014-2020 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Sascha Woo
 * @author Xiao Yu
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InnerField {

	String suffix();

	FieldType type();

	boolean index() default true;

	DateFormat format() default DateFormat.none;

	String pattern() default "";

	boolean store() default false;

	boolean fielddata() default false;

	String searchAnalyzer() default "";

	String analyzer() default "";

	String normalizer() default "";

	/**
	 * @since 4.0
	 */
	int ignoreAbove() default -1;

	/**
	 * @since 4.0
	 */
	boolean coerce() default true;

	/**
	 * @since 4.0
	 */
	boolean docValues() default true;

	/**
	 * @since 4.0
	 */
	boolean ignoreMalformed() default false;

	/**
	 * @since 4.0
	 */
	IndexOptions indexOptions() default IndexOptions.none;

	/**
	 * @since 4.0
	 */
	boolean indexPhrases() default false;

	/**
	 * implemented as array to enable the empty default value
	 *
	 * @since 4.0
	 */
	IndexPrefixes[] indexPrefixes() default {};

	/**
	 * @since 4.0
	 */
	boolean norms() default true;

	/**
	 * @since 4.0
	 */
	String nullValue() default "";

	/**
	 * @since 4.0
	 */
	int positionIncrementGap() default -1;

	/**
	 * @since 4.0
	 */
	Similarity similarity() default Similarity.Default;

	/**
	 * @since 4.0
	 */
	TermVector termVector() default TermVector.none;

	/**
	 * @since 4.0
	 */
	double scalingFactor() default 1;

	/**
	 * @since 4.0
	 */
	int maxShingleSize() default -1;
}
