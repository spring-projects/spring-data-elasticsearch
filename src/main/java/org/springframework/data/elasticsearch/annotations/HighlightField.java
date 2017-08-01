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
