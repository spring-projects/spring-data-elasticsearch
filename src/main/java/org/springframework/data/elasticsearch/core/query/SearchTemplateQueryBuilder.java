/*
 * Copyright 2022-2024 the original author or authors.
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

import java.util.Map;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public class SearchTemplateQueryBuilder extends BaseQueryBuilder<SearchTemplateQuery, SearchTemplateQueryBuilder> {

	@Nullable
	private String id;
	@Nullable String source;

	@Nullable
	Map<String, Object> params;

	@Nullable
	public String getId() {
		return id;
	}

	@Nullable
	public String getSource() {
		return source;
	}

	@Nullable
	public Map<String, Object> getParams() {
		return params;
	}

	public SearchTemplateQueryBuilder withId(@Nullable String id) {
		this.id = id;
		return this;
	}

	public SearchTemplateQueryBuilder withSource(@Nullable String source) {
		this.source = source;
		return this;
	}

	public SearchTemplateQueryBuilder withParams(@Nullable Map<String, Object> params) {
		this.params = params;
		return this;
	}

	@Override
	public SearchTemplateQuery build() {
		return new SearchTemplateQuery(this);
	}
}
