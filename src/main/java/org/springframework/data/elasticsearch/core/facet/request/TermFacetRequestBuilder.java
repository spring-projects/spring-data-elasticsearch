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
 * Basic term facet
 *
 * @author Artur Konczak
 */
@Deprecated
public class TermFacetRequestBuilder {

	private TermFacetRequest result;

	public TermFacetRequestBuilder(String name) {
		result = new TermFacetRequest(name);
	}

	public TermFacetRequestBuilder fields(String... fields) {
		result.setFields(fields);
		return this;
	}

	public TermFacetRequestBuilder size(int size) {
		result.setSize(size);
		return this;
	}

	public TermFacetRequestBuilder excludeTerms(String... terms) {
		result.setExcludeTerms(terms);
		return this;
	}

	public TermFacetRequestBuilder allTerms() {
		result.setAllTerms(true);
		return this;
	}

	public TermFacetRequestBuilder regex(String regex) {
		result.setRegex(regex);
		return this;
	}

	public TermFacetRequestBuilder ascTerm() {
		result.setOrder(TermFacetOrder.ascTerm);
		return this;
	}

	public TermFacetRequestBuilder descTerm() {
		result.setOrder(TermFacetOrder.descTerm);
		return this;
	}

	public TermFacetRequestBuilder ascCount() {
		result.setOrder(TermFacetOrder.ascCount);
		return this;
	}

	public TermFacetRequestBuilder descCount() {
		result.setOrder(TermFacetOrder.descCount);
		return this;
	}

	public TermFacetRequestBuilder applyQueryFilter() {
		result.setApplyQueryFilter(true);
		return this;
	}

	public FacetRequest build() {
		return result;
	}
}
