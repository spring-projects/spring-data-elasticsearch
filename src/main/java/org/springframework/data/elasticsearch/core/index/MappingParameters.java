/*
 * Copyright 2019-2024 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * A class to hold the mapping parameters that might be set on
 * {@link org.springframework.data.elasticsearch.annotations.Field } or
 * {@link org.springframework.data.elasticsearch.annotations.InnerField} annotation.
 *
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 * @author Brian Kimmig
 * @author Morgan Lutz
 * @author Sascha Woo
 * @author Haibo Liu
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
	static final String FIELD_PARAM_IGNORE_Z_VALUE = "ignore_z_value";
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
	static final String FIELD_PARAM_ORIENTATION = "orientation";
	static final String FIELD_PARAM_POSITIVE_SCORE_IMPACT = "positive_score_impact";
	static final String FIELD_PARAM_DIMS = "dims";
	static final String FIELD_PARAM_ELEMENT_TYPE = "element_type";
	static final String FIELD_PARAM_M = "m";
	static final String FIELD_PARAM_EF_CONSTRUCTION = "ef_construction";
	static final String FIELD_PARAM_CONFIDENCE_INTERVAL = "confidence_interval";
	static final String FIELD_PARAM_SCALING_FACTOR = "scaling_factor";
	static final String FIELD_PARAM_SEARCH_ANALYZER = "search_analyzer";
	static final String FIELD_PARAM_STORE = "store";
	static final String FIELD_PARAM_SIMILARITY = "similarity";
	static final String FIELD_PARAM_TERM_VECTOR = "term_vector";
	static final String FIELD_PARAM_TYPE = "type";
	static final String FIELD_PARAM_PATH = "path";
	static final String FIELD_PARAM_TYPE_ALIAS = "alias";

	private final String analyzer;
	private final boolean coerce;
	@Nullable private final String[] copyTo;
	private final DateFormat[] dateFormats;
	private final String[] dateFormatPatterns;
	private final boolean docValues;
	private final boolean eagerGlobalOrdinals;
	private final boolean enabled;
	private final boolean fielddata;
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
	private final Integer dims;
	private final String elementType;
	private final KnnSimilarity knnSimilarity;
	@Nullable private final KnnIndexOptions knnIndexOptions;
	private final String searchAnalyzer;
	private final double scalingFactor;
	private final String similarity;
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

		if (annotation instanceof Field field) {
			return new MappingParameters(field);
		} else if (annotation instanceof InnerField innerField) {
			return new MappingParameters(innerField);
		} else {
			throw new IllegalArgumentException("annotation must be an instance of @Field or @InnerField");
		}
	}

	private MappingParameters(Field field) {
		index = field.index();
		store = field.store();
		fielddata = field.fielddata();
		type = field.type();
		dateFormats = field.format();
		dateFormatPatterns = field.pattern();
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
		dims = field.dims();
		if (type == FieldType.Dense_Vector) {
			Assert.isTrue(dims >= 1 && dims <= 4096,
					"Invalid required parameter! Dense_Vector value \"dims\" must be between 1 and 4096.");
		}
		elementType = field.elementType();
		knnSimilarity = field.knnSimilarity();
		knnIndexOptions = field.knnIndexOptions().length > 0 ? field.knnIndexOptions()[0] : null;
		Assert.isTrue(field.enabled() || type == FieldType.Object, "enabled false is only allowed for field type object");
		enabled = field.enabled();
		eagerGlobalOrdinals = field.eagerGlobalOrdinals();
	}

	private MappingParameters(InnerField field) {
		index = field.index();
		store = field.store();
		fielddata = field.fielddata();
		type = field.type();
		dateFormats = field.format();
		dateFormatPatterns = field.pattern();
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
		dims = field.dims();
		if (type == FieldType.Dense_Vector) {
			Assert.isTrue(dims >= 1 && dims <= 4096,
					"Invalid required parameter! Dense_Vector value \"dims\" must be between 1 and 4096.");
		}
		elementType = field.elementType();
		knnSimilarity = field.knnSimilarity();
		knnIndexOptions = field.knnIndexOptions().length > 0 ? field.knnIndexOptions()[0] : null;
		enabled = true;
		eagerGlobalOrdinals = field.eagerGlobalOrdinals();
	}

	public boolean isStore() {
		return store;
	}

	/**
	 * writes the different fields to an {@link ObjectNode}.
	 *
	 * @param objectNode must not be {@literal null}
	 */
	public void writeTypeAndParametersTo(ObjectNode objectNode) throws IOException {

		Assert.notNull(objectNode, "objectNode must not be null");

		if (fielddata) {
			objectNode.put(FIELD_PARAM_DATA, fielddata);
		}

		if (type != FieldType.Auto) {
			objectNode.put(FIELD_PARAM_TYPE, type.getMappedName());

			if (type == FieldType.Date || type == FieldType.Date_Nanos || type == FieldType.Date_Range) {
				List<String> formats = new ArrayList<>();

				// built-in formats
				for (DateFormat dateFormat : dateFormats) {
					formats.add(dateFormat.toString());
				}

				// custom date formats
				Collections.addAll(formats, dateFormatPatterns);

				if (!formats.isEmpty()) {
					objectNode.put(FIELD_PARAM_FORMAT, String.join("||", formats));
				}
			}
		}

		if (!index) {
			objectNode.put(FIELD_PARAM_INDEX, index);
		}

		if (StringUtils.hasLength(analyzer)) {
			objectNode.put(FIELD_PARAM_INDEX_ANALYZER, analyzer);
		}

		if (StringUtils.hasLength(searchAnalyzer)) {
			objectNode.put(FIELD_PARAM_SEARCH_ANALYZER, searchAnalyzer);
		}

		if (StringUtils.hasLength(normalizer)) {
			objectNode.put(FIELD_PARAM_NORMALIZER, normalizer);
		}

		if (copyTo != null && copyTo.length > 0) {
			objectNode.putArray(FIELD_PARAM_COPY_TO)
					.addAll(Arrays.stream(copyTo).map(TextNode::valueOf).collect(Collectors.toList()));
		}

		if (ignoreAbove != null) {
			Assert.isTrue(ignoreAbove >= 0, "ignore_above must be a positive value");
			objectNode.put(FIELD_PARAM_IGNORE_ABOVE, ignoreAbove);
		}

		if (!coerce) {
			objectNode.put(FIELD_PARAM_COERCE, coerce);
		}

		if (!docValues) {
			objectNode.put(FIELD_PARAM_DOC_VALUES, docValues);
		}

		if (ignoreMalformed) {
			objectNode.put(FIELD_PARAM_IGNORE_MALFORMED, ignoreMalformed);
		}

		if (indexOptions != IndexOptions.none) {
			objectNode.put(FIELD_PARAM_INDEX_OPTIONS, indexOptions.toString());
		}

		if (indexPhrases) {
			objectNode.put(FIELD_PARAM_INDEX_PHRASES, indexPhrases);
		}

		if (indexPrefixes != null) {
			ObjectNode prefixNode = objectNode.putObject(FIELD_PARAM_INDEX_PREFIXES);
			if (indexPrefixes.minChars() != IndexPrefixes.MIN_DEFAULT) {
				prefixNode.put(FIELD_PARAM_INDEX_PREFIXES_MIN_CHARS, indexPrefixes.minChars());
			}
			if (indexPrefixes.maxChars() != IndexPrefixes.MAX_DEFAULT) {
				prefixNode.put(FIELD_PARAM_INDEX_PREFIXES_MAX_CHARS, indexPrefixes.maxChars());
			}
		}

		if (!norms) {
			objectNode.put(FIELD_PARAM_NORMS, norms);
		}

		if (StringUtils.hasLength(nullValue)) {
			switch (nullValueType) {
				case Integer -> objectNode.put(FIELD_PARAM_NULL_VALUE, Integer.valueOf(nullValue));
				case Long -> objectNode.put(FIELD_PARAM_NULL_VALUE, Long.valueOf(nullValue));
				case Double -> objectNode.put(FIELD_PARAM_NULL_VALUE, Double.valueOf(nullValue));
				case String -> objectNode.put(FIELD_PARAM_NULL_VALUE, nullValue);
				default -> objectNode.put(FIELD_PARAM_NULL_VALUE, nullValue);
			}
		}

		if (positionIncrementGap != null && positionIncrementGap >= 0) {
			objectNode.put(FIELD_PARAM_POSITION_INCREMENT_GAP, positionIncrementGap);
		}

		if (!Similarity.Default.equals(similarity)) {
			objectNode.put(FIELD_PARAM_SIMILARITY, similarity);
			// similarity must have index explicitly set, otherwise Elasticsearch returns an error
			objectNode.put(FIELD_PARAM_INDEX, index);
		}

		if (termVector != TermVector.none) {
			objectNode.put(FIELD_PARAM_TERM_VECTOR, termVector.toString());
		}

		if (type == FieldType.Scaled_Float) {
			objectNode.put(FIELD_PARAM_SCALING_FACTOR, scalingFactor);
		}

		if (maxShingleSize != null) {
			objectNode.put(FIELD_PARAM_MAX_SHINGLE_SIZE, maxShingleSize);
		}

		if (!positiveScoreImpact) {
			objectNode.put(FIELD_PARAM_POSITIVE_SCORE_IMPACT, positiveScoreImpact);
		}

		if (type == FieldType.Dense_Vector) {
			objectNode.put(FIELD_PARAM_DIMS, dims);

			if (!FieldElementType.DEFAULT.equals(elementType)) {
				objectNode.put(FIELD_PARAM_ELEMENT_TYPE, elementType);
			}

			if (knnSimilarity != KnnSimilarity.DEFAULT) {
				objectNode.put(FIELD_PARAM_SIMILARITY, knnSimilarity.getSimilarity());
			}

			if (knnSimilarity != KnnSimilarity.DEFAULT) {
				Assert.isTrue(index, "knn similarity can only be specified when 'index' is true.");
				objectNode.put(FIELD_PARAM_SIMILARITY, knnSimilarity.getSimilarity());
			}

			if (knnIndexOptions != null) {
				Assert.isTrue(index, "knn index options can only be specified when 'index' is true.");
				ObjectNode indexOptionsNode = objectNode.putObject(FIELD_PARAM_INDEX_OPTIONS);
				KnnAlgorithmType algoType = knnIndexOptions.type();
				if (algoType != KnnAlgorithmType.DEFAULT) {
					if (algoType == KnnAlgorithmType.INT8_HNSW || algoType == KnnAlgorithmType.INT8_FLAT) {
						Assert.isTrue(!FieldElementType.BYTE.equals(elementType),
								"'element_type' can only be float when using vector quantization.");
					}
					indexOptionsNode.put(FIELD_PARAM_TYPE, algoType.getType());
				}
				if (knnIndexOptions.m() >= 0) {
					Assert.isTrue(algoType == KnnAlgorithmType.HNSW || algoType == KnnAlgorithmType.INT8_HNSW,
							"knn 'm' parameter can only be applicable to hnsw and int8_hnsw index types.");
					indexOptionsNode.put(FIELD_PARAM_M, knnIndexOptions.m());
				}
				if (knnIndexOptions.efConstruction() >= 0) {
					Assert.isTrue(algoType == KnnAlgorithmType.HNSW || algoType == KnnAlgorithmType.INT8_HNSW,
							"knn 'ef_construction' can only be applicable to hnsw and int8_hnsw index types.");
					indexOptionsNode.put(FIELD_PARAM_EF_CONSTRUCTION, knnIndexOptions.efConstruction());
				}
				if (knnIndexOptions.confidenceInterval() >= 0) {
					Assert.isTrue(algoType == KnnAlgorithmType.INT8_HNSW
							|| algoType == KnnAlgorithmType.INT8_FLAT,
							"knn 'confidence_interval' can only be applicable to int8_hnsw and int8_flat index types.");
					indexOptionsNode.put(FIELD_PARAM_CONFIDENCE_INTERVAL, knnIndexOptions.confidenceInterval());
				}
			}
		}

		if (!enabled) {
			objectNode.put(FIELD_PARAM_ENABLED, enabled);
		}

		if (eagerGlobalOrdinals) {
			objectNode.put(FIELD_PARAM_EAGER_GLOBAL_ORDINALS, eagerGlobalOrdinals);
		}
	}
}
