/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.completion;

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * @author Franck Marchand
 * @author Mohsin Husen
 * @author Mewes Kochheim
 */
public class CompletionEntityAnnotatedBuilder {

	private CompletionAnnotatedEntity result;

	public CompletionEntityAnnotatedBuilder(String id) {
		result = new CompletionAnnotatedEntity(id);
	}

	public CompletionEntityAnnotatedBuilder name(String name) {
		result.setName(name);
		return this;
	}

	public CompletionEntityAnnotatedBuilder suggest(String[] input) {
		return suggest(input, null);
	}

	public CompletionEntityAnnotatedBuilder suggest(String[] input, Integer weight) {
		Completion suggest = new Completion(input);
		if (weight != null) {
			suggest.setWeight(weight);
		}
		result.setSuggest(suggest);
		return this;
	}

	public CompletionAnnotatedEntity build() {
		return result;
	}

	public IndexQuery buildIndex() {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}
}
