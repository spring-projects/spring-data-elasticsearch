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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Based on the reference doc - https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters-completion.html
 *
 * @author Mewes Kochheim
 * @author Robert Gruendler
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

	CompletionContext[] contexts() default {};
}
