/*
 * Copyright 2020-2020 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Peter-Josef Meisch
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface HighlightParameters {
	String boundaryChars() default "";

	int boundaryMaxScan() default -1;

	String boundaryScanner() default "";

	String boundaryScannerLocale() default "";

	/**
	 * only used for {@link Highlight}s.
	 */
	String encoder() default "";

	boolean forceSource() default false;

	String fragmenter() default "";

	/**
	 * only used for {@link HighlightField}s.
	 */
	int fragmentOffset() default -1;

	int fragmentSize() default -1;

	/**
	 * only used for {@link HighlightField}s.
	 */
	String[] matchedFields() default {};

	int noMatchSize() default -1;

	int numberOfFragments() default -1;

	String order() default "";

	int phraseLimit() default -1;

	String[] preTags() default {};

	String[] postTags() default {};

	boolean requireFieldMatch() default true;

	/**
	 * only used for {@link Highlight}s.
	 */
	String tagsSchema() default "";

	String type() default "";
}
