package org.springframework.data.elasticsearch.annotations;

/**
 * @author zzt
 */
public @interface HighlightField {

	String name() default "";

	String[] preTags() default {};

	String[] postTags() default {};

	int fragmentSize() default -1;

	int fragmentOffset() default -1;

	int numOfFragments() default -1;

	/**
	 * used to concat multiple fragments
	 */
	String fragmentSeparator() default " ... ";
}
