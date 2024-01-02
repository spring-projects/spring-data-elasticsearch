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
package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.core.document.Explanation;
import org.springframework.data.elasticsearch.core.document.NestedMetaData;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Encapsulates the found data with additional information from the search.
 *
 * @param <T> the result data class.
 * @author Peter-Josef Meisch
 * @author Matt Gilene
 * @since 4.0
 */
public class SearchHit<T> {

	@Nullable private final String index;
	@Nullable private final String id;
	private final float score;
	private final List<Object> sortValues;
	private final T content;
	private final Map<String, List<String>> highlightFields = new LinkedHashMap<>();
	private final Map<String, SearchHits<?>> innerHits = new LinkedHashMap<>();
	@Nullable private final NestedMetaData nestedMetaData;
	@Nullable private final String routing;
	@Nullable private final Explanation explanation;
	private final List<String> matchedQueries = new ArrayList<>();

	public SearchHit(@Nullable String index, @Nullable String id, @Nullable String routing, float score,
			@Nullable Object[] sortValues, @Nullable Map<String, List<String>> highlightFields,
			@Nullable Map<String, SearchHits<?>> innerHits, @Nullable NestedMetaData nestedMetaData,
			@Nullable Explanation explanation, @Nullable List<String> matchedQueries, T content) {
		this.index = index;
		this.id = id;
		this.routing = routing;
		this.score = score;
		this.sortValues = (sortValues != null) ? Arrays.asList(sortValues) : new ArrayList<>();

		if (highlightFields != null) {
			this.highlightFields.putAll(highlightFields);
		}

		if (innerHits != null) {
			this.innerHits.putAll(innerHits);
		}

		this.nestedMetaData = nestedMetaData;
		this.explanation = explanation;
		this.content = content;

		if (matchedQueries != null) {
			this.matchedQueries.addAll(matchedQueries);
		}
	}

	/**
	 * @return the index name where the hit's document was found
	 * @since 4.1
	 */
	@Nullable
	public String getIndex() {
		return index;
	}

	@Nullable
	public String getId() {
		return id;
	}

	/**
	 * @return the score for the hit.
	 */
	public float getScore() {
		return score;
	}

	/**
	 * @return the object data from the search.
	 */
	public T getContent() {
		return content;
	}

	/**
	 * @return the sort values if the query had a sort criterion.
	 */
	public List<Object> getSortValues() {
		return Collections.unmodifiableList(sortValues);
	}

	/**
	 * @return the map from field names to highlight values, never {@literal null}
	 */
	public Map<String, List<String>> getHighlightFields() {
		return Collections.unmodifiableMap(highlightFields.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.unmodifiableList(entry.getValue()))));
	}

	/**
	 * gets the highlight values for a field.
	 *
	 * @param field must not be {@literal null}
	 * @return possibly empty List, never null
	 */
	public List<String> getHighlightField(String field) {

		Assert.notNull(field, "field must not be null");

		return Collections.unmodifiableList(highlightFields.getOrDefault(field, Collections.emptyList()));
	}

	/**
	 * returns the {@link SearchHits} for the inner hits with the given name. If the inner hits could be mapped to a
	 * nested entity class, the returned data will be of this type, otherwise
	 * {{@link org.springframework.data.elasticsearch.core.document.SearchDocument}} instances are returned in this
	 * {@link SearchHits} object.
	 *
	 * @param name the inner hits name
	 * @return {@link SearchHits} if available, otherwise {@literal null}
	 */
	@Nullable
	public SearchHits<?> getInnerHits(String name) {
		return innerHits.get(name);
	}

	/**
	 * @return the map from inner_hits names to inner hits, in a {@link SearchHits} object, never {@literal null}
	 * @since 4.1
	 */
	public Map<String, SearchHits<?>> getInnerHits() {
		return innerHits;
	}

	/**
	 * If this is a nested inner hit, return the nested metadata information
	 *
	 * @return {{@link NestedMetaData}
	 * @since 4.1
	 */
	@Nullable
	public NestedMetaData getNestedMetaData() {
		return nestedMetaData;
	}

	@Override
	public String toString() {
		return "SearchHit{" + "id='" + id + '\'' + ", score=" + score + ", sortValues=" + sortValues + ", content="
				+ content + ", highlightFields=" + highlightFields + '}';
	}

	/**
	 * @return the routing for this SearchHit, may be {@literal null}.
	 * @since 4.2
	 */
	@Nullable
	public String getRouting() {
		return routing;
	}

	/**
	 * @return the explanation for this SearchHit.
	 * @since 4.2
	 */
	@Nullable
	public Explanation getExplanation() {
		return explanation;
	}

	/**
	 * @return the matched queries for this SearchHit.
	 */
	@Nullable
	public List<String> getMatchedQueries() {
		return matchedQueries;
	}
}
