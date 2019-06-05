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
 * Basic range facet
 *
 * @author Artur Konczak
 */
@Deprecated
public class RangeFacetRequestBuilder {

	RangeFacetRequest result;

	public RangeFacetRequestBuilder(String name) {
		result = new RangeFacetRequest(name);
	}

	public RangeFacetRequestBuilder field(String field) {
		result.setField(field);
		return this;
	}

	public RangeFacetRequestBuilder fields(String keyField, String valueField) {
		result.setFields(keyField, valueField);
		return this;
	}


	public RangeFacetRequestBuilder range(double from, double to) {
		result.range(from, to);
		return this;
	}

	public RangeFacetRequestBuilder range(String from, String to) {
		result.range(from, to);
		return this;
	}

	public RangeFacetRequestBuilder from(double from) {
		result.range(from, null);
		return this;
	}

	public RangeFacetRequestBuilder to(double to) {
		result.range(null, to);
		return this;
	}

	public RangeFacetRequestBuilder from(String from) {
		result.range(from, null);
		return this;
	}

	public RangeFacetRequestBuilder to(String to) {
		result.range(null, to);
		return this;
	}

	public RangeFacetRequestBuilder applyQueryFilter() {
		result.setApplyQueryFilter(true);
		return this;
	}

	public FacetRequest build() {
		return result;
	}
}
