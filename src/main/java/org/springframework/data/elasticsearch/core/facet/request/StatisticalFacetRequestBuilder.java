/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.data.elasticsearch.core.facet.request;

import org.springframework.data.elasticsearch.core.facet.FacetRequest;


/**
 * @author Petar Tahchiev
 */
@Deprecated
public class StatisticalFacetRequestBuilder {

	StatisticalFacetRequest result;

	public StatisticalFacetRequestBuilder(String name) {
		result = new StatisticalFacetRequest(name);
	}

	public StatisticalFacetRequestBuilder field(String field) {
		result.setField(field);
		return this;
	}

	public StatisticalFacetRequestBuilder fields(String... fields) {
		result.setFields(fields);
		return this;
	}

	public StatisticalFacetRequestBuilder applyQueryFilter() {
		result.setApplyQueryFilter(true);
		return this;
	}

	public FacetRequest build() {
		return result;
	}
}
