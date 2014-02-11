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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;

/**
 * Range facet for numeric fields
 *
 * @author Artur Konczak
 */
public class RangeFacetRequest extends AbstractFacetRequest {

	private String field;
	private String keyField;
	private String valueField;

	private List<Double> from = new ArrayList<Double>();
	private List<Double> to = new ArrayList<Double>();

	public RangeFacetRequest(String name) {
		super(name);
	}

	public void setField(String field) {
		this.field = field;
	}

	public void setFields(String keyField, String valueField) {
		this.keyField = keyField;
		this.valueField = valueField;
	}

	public void range(Double from, Double to) {
		if (from == null) {
			this.from.add(Double.NEGATIVE_INFINITY);
		} else {
			this.from.add(from);
		}

		if (to == null) {
			this.to.add(Double.POSITIVE_INFINITY);
		} else {
			this.to.add(to);
		}
	}

	@Override
	public FacetBuilder getFacet() {
		Assert.notNull(getName(), "Facet name can't be a null !!!");
		Assert.isTrue(StringUtils.isNotBlank(field) || StringUtils.isNotBlank(keyField) && StringUtils.isNotBlank(valueField), "Please select field or key field and value field !!!");

		RangeFacetBuilder builder = FacetBuilders.rangeFacet(getName());
		if (StringUtils.isNotBlank(keyField)) {
			builder.keyField(keyField).valueField(valueField);
		} else {
			builder.field(field);
		}
		Assert.notEmpty(from, "Please select at last one range");
		Assert.notEmpty(to, "Please select at last one range");
		for (int i = 0; i < from.size(); i++) {
			builder.addRange(from.get(i), to.get(i));
		}
		return builder;
	}
}
