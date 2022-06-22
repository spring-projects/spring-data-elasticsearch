/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.support;

import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class ScoreDoc {

	private final double score;
	@Nullable private final Integer doc;
	@Nullable private final Integer shardIndex;

	public ScoreDoc(double score, @Nullable Integer doc, @Nullable Integer shardIndex) {
		this.score = score;
		this.doc = doc;
		this.shardIndex = shardIndex;
	}

	public double getScore() {
		return score;
	}

	@Nullable
	public Integer getDoc() {
		return doc;
	}

	@Nullable
	public Integer getShardIndex() {
		return shardIndex;
	}
}
