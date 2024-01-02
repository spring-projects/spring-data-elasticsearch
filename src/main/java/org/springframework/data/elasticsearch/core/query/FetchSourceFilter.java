/*
 * Copyright 2016-2024 the original author or authors.
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

import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * SourceFilter implementation for providing includes and excludes.
 *
 * @author Jon Tsiros
 * @author Peter-Josef Meisch
 */
public class FetchSourceFilter implements SourceFilter {

	@Nullable private final String[] includes;
	@Nullable private final String[] excludes;

	/**
	 * @since 5.2
	 */
	public static SourceFilter of(@Nullable final String[] includes, @Nullable final String[] excludes) {
		return new FetchSourceFilter(includes, excludes);
	}

	/**
	 * @since 5.2
	 */
	public static SourceFilter of(Function<FetchSourceFilterBuilder, FetchSourceFilterBuilder> builderFunction) {

		Assert.notNull(builderFunction, "builderFunction must not be null");

		return builderFunction.apply(new FetchSourceFilterBuilder()).build();
	}

	public FetchSourceFilter(@Nullable final String[] includes, @Nullable final String[] excludes) {
		this.includes = includes;
		this.excludes = excludes;
	}

	@Override
	public String[] getIncludes() {
		return includes;
	}

	@Override
	public String[] getExcludes() {
		return excludes;
	}
}
