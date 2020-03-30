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

import java.lang.annotation.*;

/**
 * Based on reference doc - https://www.elastic.co/guide/en/elasticsearch/reference/current/search-as-you-type.html
 *
 * @author Aleksei Arsenev
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface SearchAsYouTypeField {

    int maxShingleSize() default 3;

    boolean index() default true;

    String searchAnalyzer() default "";

    String searchQuoteAnalyzer() default "";

    String analyzer() default "";

    IndexOptions indexOptions() default IndexOptions.positions;

    boolean norms() default true;

    boolean store() default false;

    Similarity similarity() default Similarity.Default;

    TermVector termVector() default TermVector.none;
}
