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
package org.springframework.data.elasticsearch.core.suggest;

import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Based on the reference doc -
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters-completion.html
 *
 * @author Mewes Kochheim
 * @author Robert Gruendler
 * @author Peter-Josef Meisch
 * @author Houtaroy
 */
public class Completion {

	private String[] input;
	@Nullable private Map<String, List<String>> contexts;
	@Nullable private Integer weight;

	public Completion() {
		this.input = new String[0];
	}

	public Completion(String[] input) {
		this.input = input;
	}

	public Completion(List<String> input) {
		this.input = input.toArray(new String[0]);
	}

	public String[] getInput() {
		return input;
	}

	public void setInput(String[] input) {
		this.input = input;
	}

	@Nullable
	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	@Nullable
	public Map<String, List<String>> getContexts() {
		return contexts;
	}

	public void setContexts(Map<String, List<String>> contexts) {
		this.contexts = contexts;
	}

}
