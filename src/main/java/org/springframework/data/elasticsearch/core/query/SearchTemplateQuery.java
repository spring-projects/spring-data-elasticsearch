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

import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public class SearchTemplateQuery extends BaseQuery {

	@Nullable final private String id;
	@Nullable final String source;
	@Nullable final Map<String, Object> params;

	public static SearchTemplateQueryBuilder builder() {
		return new SearchTemplateQueryBuilder();
	}

	public SearchTemplateQuery(SearchTemplateQueryBuilder builder) {
		super(builder);

		this.id = builder.getId();
		this.source = builder.getSource();
		this.params = builder.getParams();

		if (id == null && source == null) {
			throw new IllegalArgumentException("Either id or source must be set");
		}
	}

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
}
