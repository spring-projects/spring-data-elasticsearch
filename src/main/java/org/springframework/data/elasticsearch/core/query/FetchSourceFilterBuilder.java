/*
 * Copyright 2016-2020 the original author or authors.
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

/**
 * SourceFilter builder for providing includes and excludes.
 *
 * @Author Jon Tsiros
 * @author Peter-Josef Meisch
 */
public class FetchSourceFilterBuilder {

	@Nullable private String[] includes;
	@Nullable private String[] excludes;

	public FetchSourceFilterBuilder withIncludes(String... includes) {
		this.includes = includes;
		return this;
	}

	public FetchSourceFilterBuilder withExcludes(String... excludes) {
		this.excludes = excludes;
		return this;
	}

	public SourceFilter build() {
		if (includes == null)
			includes = new String[0];
		if (excludes == null)
			excludes = new String[0];

		return new FetchSourceFilter(includes, excludes);
	}
}
