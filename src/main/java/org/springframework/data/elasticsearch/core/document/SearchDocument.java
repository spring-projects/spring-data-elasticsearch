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
package org.springframework.data.elasticsearch.core.document;

import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Extension to {@link Document} exposing search response related data.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Matt Gilene
 * @since 4.0
 * @see Document
 */
public interface SearchDocument extends Document {

	/**
	 * Return the search {@code score}.
	 *
	 * @return the search {@code score}.
	 */
	float getScore();

	/**
	 * @return the fields for the search result, not {@literal null}
	 */
	Map<String, List<Object>> getFields();

	/**
	 * The first value of the given field.
	 *
	 * @param name the field name
	 */
	@Nullable
	default <V> V getFieldValue(final String name) {
		List<Object> values = getFields().get(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return (V) values.get(0);
	}

	/**
	 * @return the sort values for the search hit
	 */
	@Nullable
	default Object[] getSortValues() {
		return null;
	}

	/**
	 * @return the highlightFields for the search hit.
	 */
	@Nullable
	default Map<String, List<String>> getHighlightFields() {
		return null;
	}

	/**
	 * @return the innerHits for the SearchHit
	 * @since 4.1
	 */
	@Nullable
	default Map<String, SearchDocumentResponse> getInnerHits() {
		return null;
	}

	/**
	 * @return the nested metadata in case this is a nested inner hit.
	 * @since 4.1
	 */
	@Nullable
	default NestedMetaData getNestedMetaData() {
		return null;
	}

	/**
	 * @return the routing value for the document
	 * @since 4.2
	 */
	@Nullable
	default String getRouting() {
		return getFieldValue("_routing");
	}

	/**
	 * @return the explanation for the SearchHit.
	 * @since 4.2
	 */
	@Nullable
	Explanation getExplanation();

	/**
	 * @return the matched queries for the SearchHit.
	 */
	@Nullable
	List<String> getMatchedQueries();
}
