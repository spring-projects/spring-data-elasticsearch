/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of RescorerQuery to be used for rescoring filtered search results.
 *
 * @author Peer Mueller
 * @since 4.2
 */
public class RescorerQuery {

	private final Query query;
	private ScoreMode scoreMode = ScoreMode.Default;
	@Nullable private Integer windowSize;
	@Nullable private Float queryWeight;
	@Nullable private Float rescoreQueryWeight;

	public RescorerQuery(Query query) {

		Assert.notNull(query, "query must not be null");

		this.query = query;
	}

	public Query getQuery() {
		return query;
	}

	public ScoreMode getScoreMode() {
		return scoreMode;
	}

	@Nullable
	public Integer getWindowSize() {
		return windowSize;
	}

	@Nullable
	public Float getQueryWeight() {
		return queryWeight;
	}

	@Nullable
	public Float getRescoreQueryWeight() {
		return rescoreQueryWeight;
	}

	public RescorerQuery withScoreMode(ScoreMode scoreMode) {

		Assert.notNull(scoreMode, "scoreMode must not be null");

		this.scoreMode = scoreMode;
		return this;
	}

	public RescorerQuery withWindowSize(int windowSize) {
		this.windowSize = windowSize;
		return this;
	}

	public RescorerQuery withQueryWeight(float queryWeight) {
		this.queryWeight = queryWeight;
		return this;
	}

	public RescorerQuery withRescoreQueryWeight(float rescoreQueryWeight) {
		this.rescoreQueryWeight = rescoreQueryWeight;
		return this;
	}

	public enum ScoreMode {
		Default, Avg, Max, Min, Total, Multiply
	}

}
