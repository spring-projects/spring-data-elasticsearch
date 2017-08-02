/*
 * Copyright 2017 the original author or authors.
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

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

/**
 * @author zzt
 */
public @interface HighlightField {

	String name() default "";

	/**
	 * @see HighlightBuilder#DEFAULT_PRE_TAGS
	 */
	String[] preTags() default {"<em>"};

	/**
	 * @see HighlightBuilder#DEFAULT_POST_TAGS
	 */
	String[] postTags() default {"</em>"};

	int fragmentSize() default HighlightBuilder.DEFAULT_FRAGMENT_CHAR_SIZE;

	int fragmentOffset() default -1;

	int numOfFragments() default HighlightBuilder.DEFAULT_NUMBER_OF_FRAGMENTS;

	/**
	 * used to concat multiple fragments
	 */
	String fragmentSeparator() default " ... ";
}
