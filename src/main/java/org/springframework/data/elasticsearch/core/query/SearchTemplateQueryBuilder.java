/*
 * Copyright 2022-2025 the original author or authors.
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

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public class SearchTemplateQueryBuilder extends BaseQueryBuilder<SearchTemplateQuery, SearchTemplateQueryBuilder> {

	@Nullable private String id;
	@Nullable String source;

	@Nullable Map<String, Object> params;

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
	public SearchTemplateQueryBuilder withSort(Sort sort) {
		throw new IllegalArgumentException(
				"sort is not supported in a searchtemplate query. Sort values must be defined in the stored template");
	}

	@Override
	public SearchTemplateQueryBuilder withPageable(Pageable pageable) {
		throw new IllegalArgumentException(
				"paging is not supported in a searchtemplate query. from and size values must be defined in the stored template");
	}

	@Override
	public SearchTemplateQuery build() {
		return new SearchTemplateQuery(this);
	}
}
