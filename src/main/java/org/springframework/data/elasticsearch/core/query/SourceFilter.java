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

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.lang.Nullable;

/**
 * SourceFilter for providing includes and excludes. Using these helps in reducing the amount of data that is returned
 * from Elasticsearch especially when the stored docuements are large and only some fields from these documents are
 * needed. If the SourceFilter includes the name of a property that has a different name mapped in Elasticsearch (see
 * {@link Field#name()} this will automatically be mapped.
 *
 * @author Jon Tsiros
 * @author Peter-Josef Meisch
 */
public interface SourceFilter {

	/**
	 * @return the name of the fields to include in a response.
	 */
	@Nullable
	String[] getIncludes();

	/**
	 * @return the names of the fields to exclude from a response.
	 */
	@Nullable
	String[] getExcludes();
}
