/*
 * Copyright 2019 the original author or authors.
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
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Encapsulates the found data with additional information from the search.
 * 
 * @param <T> the result data class.
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public class SearchHit<T> {

	private final String id;
	private final float score;
	private final List<Object> sortValues;
	private final T content;

	public SearchHit(@Nullable String id, float score, Object[] sortValues, T content) {
		this.id = id;
		this.score = score;
		this.sortValues = (sortValues != null) ? Arrays.asList(sortValues) : new ArrayList<>();
		this.content = content;
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

	@Override
	public String toString() {
		return "SearchHit{" + "id='" + id + '\'' + ", score=" + score + ", sortValues=" + sortValues + ", content="
				+ content + '}';
	}
}
