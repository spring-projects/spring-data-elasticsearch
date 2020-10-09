/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import java.io.IOException;
import java.lang.annotation.Annotation;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.IndexOptions;
import org.springframework.data.elasticsearch.annotations.IndexPrefixes;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.NullValueType;
import org.springframework.data.elasticsearch.annotations.Similarity;
import org.springframework.data.elasticsearch.annotations.TermVector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A class to hold the mapping parameters that might be set on
 * {@link org.springframework.data.elasticsearch.annotations.Field } or
 * {@link org.springframework.data.elasticsearch.annotations.InnerField} annotation.
 *
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 * @since 4.0
 */
public final class MappingParameters {

	static final String FIELD_PARAM_COERCE = "coerce";
	static final String FIELD_PARAM_COPY_TO = "copy_to";
	static final String FIELD_PARAM_DATA = "fielddata";
	static final String FIELD_PARAM_DOC_VALUES = "doc_values";
	static final String FIELD_PARAM_EAGER_GLOBAL_ORDINALS = "eager_global_ordinals";
	static final String FIELD_PARAM_ENABLED = "enabled";
	static final String FIELD_PARAM_FORMAT = "format";
	static final String FIELD_PARAM_IGNORE_ABOVE = "ignore_above";
	static final String FIELD_PARAM_IGNORE_MALFORMED = "ignore_malformed";
	static final String FIELD_PARAM_INDEX = "index";
	static final String FIELD_PARAM_INDEX_OPTIONS = "index_options";
	static final String FIELD_PARAM_INDEX_PHRASES = "index_phrases";
	static final String FIELD_PARAM_INDEX_PREFIXES = "index_prefixes";
	static final String FIELD_PARAM_INDEX_PREFIXES_MIN_CHARS = "min_chars";
	static final String FIELD_PARAM_INDEX_PREFIXES_MAX_CHARS = "max_chars";
	static final String FIELD_PARAM_INDEX_ANALYZER = "analyzer";
	static final String FIELD_PARAM_MAX_SHINGLE_SIZE = "max_shingle_size";
	static final String FIELD_PARAM_NORMALIZER = "normalizer";
	static final String FIELD_PARAM_NORMS = "norms";
	static final String FIELD_PARAM_NULL_VALUE = "null_value";
	static final String FIELD_PARAM_POSITION_INCREMENT_GAP = "position_increment_gap";
	static final String FIELD_PARAM_POSITIVE_SCORE_IMPACT = "positive_score_impact";
	static final String FIELD_PARAM_SCALING_FACTOR = "scaling_factor";
	static final String FIELD_PARAM_SEARCH_ANALYZER = "search_analyzer";
	static final String FIELD_PARAM_STORE = "store";
	static final String FIELD_PARAM_SIMILARITY = "similarity";
	static final String FIELD_PARAM_TERM_VECTOR = "term_vector";
	static final String FIELD_PARAM_TYPE = "type";

	private final String analyzer;
	private final boolean coerce;
	@Nullable private final String[] copyTo;
	private final String datePattern;
	private final boolean docValues;
	private final boolean eagerGlobalOrdinals;
	private final boolean enabled;
	private final boolean fielddata;
	private final DateFormat format;
	@Nullable private final Integer ignoreAbove;
	private final boolean ignoreMalformed;
	private final boolean index;
	private final IndexOptions indexOptions;
	private final boolean indexPhrases;
	@Nullable private final IndexPrefixes indexPrefixes;
	private final String normalizer;
	private final boolean norms;
	@Nullable private final Integer maxShingleSize;
	private final String nullValue;
	private final NullValueType nullValueType;
	private final Integer positionIncrementGap;
	private final boolean positiveScoreImpact;
	private final String searchAnalyzer;
	private final double scalingFactor;
	private final Similarity similarity;
	private final boolean store;
	private final TermVector termVector;
	private final FieldType type;

	/**
	 * extracts the mapping parameters from the relevant annotations.
	 *
	 * @param annotation must not be {@literal null}.
	 * @return empty Optional if the annotation does not have a conformant type.
	 */
	public static MappingParameters from(Annotation annotation) {

		Assert.notNull(annotation, "annotation must not be null!");

		if (annotation instanceof Field) {
			return new MappingParameters((Field) annotation);
		} else if (annotation instanceof InnerField) {
			return new MappingParameters((InnerField) annotation);
		} else {
			throw new IllegalArgumentException("annotation must be an instance of @Field or @InnerField");
		}
	}

	private MappingParameters(Field field) {
		index = field.index();
		store = field.store();
		fielddata = field.fielddata();
		type = field.type();
		format = field.format();
		datePattern = field.pattern();
		analyzer = field.analyzer();
		searchAnalyzer = field.searchAnalyzer();
		normalizer = field.normalizer();
		copyTo = field.copyTo();
		ignoreAbove = field.ignoreAbove() >= 0 ? field.ignoreAbove() : null;
		coerce = field.coerce();
		docValues = field.docValues();
		Assert.isTrue(!((type == FieldType.Text || type == FieldType.Nested) && !docValues),
				"docValues false is not allowed for field type text");
		ignoreMalformed = field.ignoreMalformed();
		indexOptions = field.indexOptions();
		indexPhrases = field.indexPhrases();
		indexPrefixes = field.indexPrefixes().length > 0 ? field.indexPrefixes()[0] : null;
		norms = field.norms();
		nullValue = field.nullValue();
		nullValueType = field.nullValueType();
		positionIncrementGap = field.positionIncrementGap();
		similarity = field.similarity();
		termVector = field.termVector();
		scalingFactor = field.scalingFactor();
		maxShingleSize = field.maxShingleSize() >= 0 ? field.maxShingleSize() : null;
		Assert.isTrue(type != FieldType.Search_As_You_Type //
				|| maxShingleSize == null //
				|| (maxShingleSize >= 2 && maxShingleSize <= 4), //
				"maxShingleSize must be in inclusive range from 2 to 4 for field type search_as_you_type");
		positiveScoreImpact = field.positiveScoreImpact();
		Assert.isTrue(field.enabled() || type == FieldType.Object, "enabled false is only allowed for field type object");
		enabled = field.enabled();
		eagerGlobalOrdinals = field.eagerGlobalOrdinals();
	}

	private MappingParameters(InnerField field) {
		index = field.index();
		store = field.store();
		fielddata = field.fielddata();
		type = field.type();
		format = field.format();
		datePattern = field.pattern();
		analyzer = field.analyzer();
		searchAnalyzer = field.searchAnalyzer();
		normalizer = field.normalizer();
		copyTo = null;
		ignoreAbove = field.ignoreAbove() >= 0 ? field.ignoreAbove() : null;
		coerce = field.coerce();
		docValues = field.docValues();
		Assert.isTrue(!((type == FieldType.Text || type == FieldType.Nested) && !docValues),
				"docValues false is not allowed for field type text");
		ignoreMalformed = field.ignoreMalformed();
		indexOptions = field.indexOptions();
		indexPhrases = field.indexPhrases();
		indexPrefixes = field.indexPrefixes().length > 0 ? field.indexPrefixes()[0] : null;
		norms = field.norms();
		nullValue = field.nullValue();
		nullValueType = field.nullValueType();
		positionIncrementGap = field.positionIncrementGap();
		similarity = field.similarity();
		termVector = field.termVector();
		scalingFactor = field.scalingFactor();
		maxShingleSize = field.maxShingleSize() >= 0 ? field.maxShingleSize() : null;
		Assert.isTrue(type != FieldType.Search_As_You_Type //
				|| maxShingleSize == null //
				|| (maxShingleSize >= 2 && maxShingleSize <= 4), //
				"maxShingleSize must be in inclusive range from 2 to 4 for field type search_as_you_type");
		positiveScoreImpact = field.positiveScoreImpact();
		enabled = true;
		eagerGlobalOrdinals = field.eagerGlobalOrdinals();
	}

	public boolean isStore() {
		return store;
	}

	/**
	 * writes the different fields to the builder.
	 *
	 * @param builder must not be {@literal null}.
	 */
	public void writeTypeAndParametersTo(XContentBuilder builder) throws IOException {

		Assert.notNull(builder, "builder must ot be null");

		if (fielddata) {
			builder.field(FIELD_PARAM_DATA, fielddata);
		}

		if (type != FieldType.Auto) {
			builder.field(FIELD_PARAM_TYPE, type.name().toLowerCase());
			if (type == FieldType.Date && format != DateFormat.none) {
				builder.field(FIELD_PARAM_FORMAT, format == DateFormat.custom ? datePattern : format.toString());
			}
		}

		if (!index) {
			builder.field(FIELD_PARAM_INDEX, index);
		}

		if (!StringUtils.isEmpty(analyzer)) {
			builder.field(FIELD_PARAM_INDEX_ANALYZER, analyzer);
		}

		if (!StringUtils.isEmpty(searchAnalyzer)) {
			builder.field(FIELD_PARAM_SEARCH_ANALYZER, searchAnalyzer);
		}

		if (!StringUtils.isEmpty(normalizer)) {
			builder.field(FIELD_PARAM_NORMALIZER, normalizer);
		}

		if (copyTo != null && copyTo.length > 0) {
			builder.field(FIELD_PARAM_COPY_TO, copyTo);
		}

		if (ignoreAbove != null) {
			Assert.isTrue(ignoreAbove >= 0, "ignore_above must be a positive value");
			builder.field(FIELD_PARAM_IGNORE_ABOVE, ignoreAbove);
		}

		if (!coerce) {
			builder.field(FIELD_PARAM_COERCE, coerce);
		}

		if (!docValues) {
			builder.field(FIELD_PARAM_DOC_VALUES, docValues);
		}

		if (ignoreMalformed) {
			builder.field(FIELD_PARAM_IGNORE_MALFORMED, ignoreMalformed);
		}

		if (indexOptions != IndexOptions.none) {
			builder.field(FIELD_PARAM_INDEX_OPTIONS, indexOptions);
		}

		if (indexPhrases) {
			builder.field(FIELD_PARAM_INDEX_PHRASES, indexPhrases);
		}

		if (indexPrefixes != null) {
			builder.startObject(FIELD_PARAM_INDEX_PREFIXES);
			if (indexPrefixes.minChars() != IndexPrefixes.MIN_DEFAULT) {
				builder.field(FIELD_PARAM_INDEX_PREFIXES_MIN_CHARS, indexPrefixes.minChars());
			}
			if (indexPrefixes.maxChars() != IndexPrefixes.MAX_DEFAULT) {
				builder.field(FIELD_PARAM_INDEX_PREFIXES_MAX_CHARS, indexPrefixes.maxChars());
			}
			builder.endObject();
		}

		if (!norms) {
			builder.field(FIELD_PARAM_NORMS, norms);
		}

		if (!StringUtils.isEmpty(nullValue)) {
			Object value;
			switch (nullValueType) {
				case Integer:
					value = Integer.valueOf(nullValue);
					break;
				case Long:
					value = Long.valueOf(nullValue);
					break;
				case Double:
					value = Double.valueOf(nullValue);
					break;
				case String:
				default:
					value = nullValue;
					break;
			}
			builder.field(FIELD_PARAM_NULL_VALUE, value);
		}

		if (positionIncrementGap != null && positionIncrementGap >= 0) {
			builder.field(FIELD_PARAM_POSITION_INCREMENT_GAP, positionIncrementGap);
		}

		if (similarity != Similarity.Default) {
			builder.field(FIELD_PARAM_SIMILARITY, similarity);
		}

		if (termVector != TermVector.none) {
			builder.field(FIELD_PARAM_TERM_VECTOR, termVector);
		}

		if (type == FieldType.Scaled_Float) {
			builder.field(FIELD_PARAM_SCALING_FACTOR, scalingFactor);
		}

		if (maxShingleSize != null) {
			builder.field(FIELD_PARAM_MAX_SHINGLE_SIZE, maxShingleSize);
		}

		if (!positiveScoreImpact) {
			builder.field(FIELD_PARAM_POSITIVE_SCORE_IMPACT, positiveScoreImpact);
		}

		if (!enabled) {
			builder.field(FIELD_PARAM_ENABLED, enabled);
		}

		if (eagerGlobalOrdinals) {
			builder.field(FIELD_PARAM_EAGER_GLOBAL_ORDINALS, eagerGlobalOrdinals);
		}
	}
}
