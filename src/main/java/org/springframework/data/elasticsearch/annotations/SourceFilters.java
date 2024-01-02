/*
 * Copyright 2022-2024 the original author or authors.
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

/**
 * This annotation can be placed on repository methods to define the properties that should be requested from
 * Elasticsearch when the method is run.
 *
 * @author Alexander Torres
 * @author Peter-Josef Meisch
 * @since 5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface SourceFilters {

	/**
	 * Properties to be requested from Elasticsearch to be included in the response. These can be passed in as literals
	 * like
	 *
	 * <pre>
	 * {@code @SourceFilters(includes = {"property1", "property2"})}
	 * </pre>
	 *
	 * or as a parameterized value
	 *
	 * <pre>
	 * {@code @SourceFilters(includes = "?0")}
	 * </pre>
	 *
	 * when the list of properties is passed as a function parameter.
	 */
	String[] includes() default "";

	/**
	 * Properties to be requested from Elasticsearch to be excluded in the response. These can be passed in as literals
	 * like
	 *
	 * <pre>
	 * {@code @SourceFilters(excludes = {"property1", "property2"})}
	 * </pre>
	 *
	 * or as a parameterized value
	 *
	 * <pre>
	 * {@code @SourceFilters(excludes = "?0")}
	 * </pre>
	 *
	 * when the list of properties is passed as a function parameter.
	 */
	String[] excludes() default "";
}
