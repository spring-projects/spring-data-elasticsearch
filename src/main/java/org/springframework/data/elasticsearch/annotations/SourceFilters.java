/*
 * Copyright 2022 the original author or authors.
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

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

/**
 * SourceFilters
 *
 * @author Alexander Torres
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface SourceFilters {
    /**
     * Fields to include in the query search hits. (e.g. ["field1", "field2"])
     * @return
     */
    String includes() default "";

    /**
     * Fields to exclude from the query search hits. (e.g. ["field1", "field2"])
     * @return
     */
    String excludes() default "";
}
