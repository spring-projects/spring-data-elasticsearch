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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 * @author Jakub Vavrik
 * @author Kevin Leturc
 * @author Peter-Josef Meisch
 * @author Xiao Yu
 * @author Aleksei Arsenev
 * @author Brian Kimmig
 * @author Morgan Lutz
 * @author Sascha Woo
 * @author Haibo Liu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Documented
@Inherited
public @interface Field {

	/**
	 * Alias for {@link #name}.
	 *
	 * @since 3.2
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The <em>name</em> to be used to store the field inside the document. If not set, the name of the annotated property
	 * is used.
	 *
	 * @since 3.2
	 */
	@AliasFor("value")
	String name() default "";

	FieldType type() default FieldType.Auto;

	boolean index() default true;

	DateFormat[] format() default { DateFormat.date_optional_time, DateFormat.epoch_millis };

	String[] pattern() default {};

	boolean store() default false;

	boolean fielddata() default false;

	String searchAnalyzer() default "";

	String analyzer() default "";

	String normalizer() default "";

	String[] ignoreFields() default {};

	boolean includeInParent() default false;

	String[] copyTo() default {};

	/**
	 * @since 4.0
	 */
	int ignoreAbove() default -1;

	/**
	 * @since 4.0
	 */
	boolean coerce() default true;

	/**
	 * @since 4.0
	 */
	boolean docValues() default true;

	/**
	 * @since 4.0
	 */
	boolean ignoreMalformed() default false;

	/**
	 * @since 4.0
	 */
	IndexOptions indexOptions() default IndexOptions.none;

	/**
	 * @since 4.0
	 */
	boolean indexPhrases() default false;

	/**
	 * implemented as array to enable the empty default value
	 *
	 * @since 4.0
	 */
	IndexPrefixes[] indexPrefixes() default {};

	/**
	 * @since 4.0
	 */
	boolean norms() default true;

	/**
	 * @since 4.0
	 */
	String nullValue() default "";

	/**
	 * @since 4.0
	 */
	int positionIncrementGap() default -1;

	/**
	 * @since 4.0
	 */
	String similarity() default Similarity.Default;

	/**
	 * @since 4.0
	 */
	TermVector termVector() default TermVector.none;

	/**
	 * @since 4.0
	 */
	double scalingFactor() default 1;

	/**
	 * @since 4.0
	 */
	int maxShingleSize() default -1;

	/**
	 * if true, the field will be stored in Elasticsearch even if it has a null value
	 *
	 * @since 4.1
	 */
	boolean storeNullValue() default false;

	/**
	 * to be used in combination with {@link FieldType#Rank_Feature}
	 *
	 * @since 4.1
	 */
	boolean positiveScoreImpact() default true;

	/**
	 * to be used in combination with {@link FieldType#Object}
	 *
	 * @since 4.1
	 */
	boolean enabled() default true;

	/**
	 * @since 4.1
	 */
	boolean eagerGlobalOrdinals() default false;

	/**
	 * @since 4.1
	 */
	NullValueType nullValueType() default NullValueType.String;

	/**
	 * to be used in combination with {@link FieldType#Dense_Vector}
	 *
	 * @since 4.2
	 */
	int dims() default -1;

	/**
	 * to be used in combination with {@link FieldType#Dense_Vector}
	 *
	 * @since 5.4
	 */
	String elementType() default FieldElementType.DEFAULT;

	/**
	 * to be used in combination with {@link FieldType#Dense_Vector}
	 *
	 * @since 5.4
	 */
	KnnSimilarity knnSimilarity() default KnnSimilarity.DEFAULT;

	/**
	 * to be used in combination with {@link FieldType#Dense_Vector}
	 *
	 * @since 5.4
	 */
	KnnIndexOptions[] knnIndexOptions() default {};

	/**
	 * Controls how Elasticsearch dynamically adds fields to the inner object within the document.<br>
	 * To be used in combination with {@link FieldType#Object} or {@link FieldType#Nested}
	 *
	 * @since 4.3
	 */
	Dynamic dynamic() default Dynamic.INHERIT;

	/**
	 * marks this field to be excluded from the _source in Elasticsearch
	 * (https://www.elastic.co/guide/en/elasticsearch/reference/7.15/mapping-source-field.html#include-exclude)
	 *
	 * @since 4.3
	 */
	boolean excludeFromSource() default false;

	/**
	 * when this field is a {{@link String}}, a {{@link java.util.Collection}} or a {{@link java.util.Map}} that is empty
	 * this property controlls whether the empty value is sent to Elasticsearch.
	 *
	 * @since 5.1
	 */
	boolean storeEmptyValue() default true;
}
