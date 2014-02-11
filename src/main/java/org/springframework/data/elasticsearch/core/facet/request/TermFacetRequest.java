/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.facet.request;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;

/**
 * Term facet
 *
 * @author Artur Konczak
 */
public class TermFacetRequest extends AbstractFacetRequest {

	private String[] fields;
	private Object[] excludeTerms;
	private int size = 10;
	private TermFacetOrder order = TermFacetOrder.descCount;
	private boolean allTerms = false;
	private String regex = null;
	private int regexFlag = 0;

	public TermFacetRequest(String name) {
		super(name);
	}

	public void setFields(String... fields) {
		this.fields = fields;
	}

	public void setSize(int size) {
		Assert.isTrue(size >= 0, "Size should be bigger then zero !!!");
		this.size = size;
	}

	public void setOrder(TermFacetOrder order) {
		this.order = order;
	}

	public void setExcludeTerms(Object... excludeTerms) {
		this.excludeTerms = excludeTerms;
	}

	public void setAllTerms(boolean allTerms) {
		this.allTerms = allTerms;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public void setRegex(String regex, int regexFlag) {
		this.regex = regex;
		this.regexFlag = regexFlag;
	}

	@Override
	public FacetBuilder getFacet() {
		Assert.notEmpty(fields, "Please select at last one field !!!");
		TermsFacetBuilder builder = FacetBuilders.termsFacet(getName()).fields(fields).size(size);
		switch (order) {

			case descTerm:
				builder.order(TermsFacet.ComparatorType.REVERSE_TERM);
				break;
			case ascTerm:
				builder.order(TermsFacet.ComparatorType.TERM);
				break;
			case ascCount:
				builder.order(TermsFacet.ComparatorType.REVERSE_COUNT);
				break;
			default:
				builder.order(TermsFacet.ComparatorType.COUNT);
		}
		if (ArrayUtils.isNotEmpty(excludeTerms)) {
			builder.exclude(excludeTerms);
		}

		if (allTerms) {
			builder.allTerms(allTerms);
		}

		if (StringUtils.isNotBlank(regex)) {
			builder.regex(regex, regexFlag);
		}

		return builder;
	}
}
