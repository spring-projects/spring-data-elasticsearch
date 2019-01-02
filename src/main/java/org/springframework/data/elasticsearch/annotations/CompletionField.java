/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Based on the reference doc - http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-suggesters-completion.html
 *
 * @author Mewes Kochheim
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface CompletionField {

	String searchAnalyzer() default "simple";

	String analyzer() default "simple";

	boolean preserveSeparators() default true;

	boolean preservePositionIncrements() default true;

	int maxInputLength() default 50;
}
