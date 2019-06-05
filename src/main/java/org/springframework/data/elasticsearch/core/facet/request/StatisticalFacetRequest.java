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

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Petar Tahchiev
 */
@Deprecated
public class StatisticalFacetRequest extends AbstractFacetRequest {

	private String field;

	private String[] fields;

	public StatisticalFacetRequest(String name) {
		super(name);
	}

	public void setField(String field) {
		this.field = field;
	}

	public void setFields(String... fields) {
		throw new UnsupportedOperationException("Native Facet are not supported in Elasticsearch 2.x - use Aggregation");
	}

	public AbstractAggregationBuilder getFacet() {
		Assert.notNull(getName(), "Facet name can't be a null !!!");
		Assert.isTrue(!StringUtils.isEmpty(field) && fields == null, "Please select field or fields on which to build the facets !!!");
		return AggregationBuilders.extendedStats(getName()).field(field);
	}
}
