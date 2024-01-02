/*
 * Copyright 2013-2024 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.QueryAnnotation;

/**
 * Query
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 * @author Steven Pearce
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface Query {

	/**
	 * @return Elasticsearch query to be used when executing query. May contain placeholders eg. ?0. Alias for query.
	 */
	@AliasFor("query")
	String value() default "";

	/**
	 * @return Elasticsearch query to be used when executing query. May contain placeholders eg. ?0. Alias for value
	 * @since 5.0
	 */
	@AliasFor("value")
	String query() default "";

	/**
	 * Returns whether the query defined should be executed as count projection.
	 *
	 * @return {@literal false} by default.
	 * @since 4.2
	 */
	boolean count() default false;
}
