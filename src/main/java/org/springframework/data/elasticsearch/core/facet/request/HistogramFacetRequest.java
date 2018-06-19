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

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
 */
@Deprecated
public class HistogramFacetRequest extends AbstractFacetRequest {

	private String field;
	private long interval;
	private DateHistogramInterval timeUnit;

	public HistogramFacetRequest(String name) {
		super(name);
	}

	public void setField(String field) {
		this.field = field;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public void setTimeUnit(DateHistogramInterval timeUnit) {
		this.timeUnit = timeUnit;
	}

	public AbstractAggregationBuilder getFacet() {
		Assert.notNull(getName(), "Facet name can't be a null !!!");
		Assert.isTrue(!StringUtils.isEmpty(field), "Please select field on which to build the facet !!!");
		Assert.isTrue(interval > 0, "Please provide interval as positive value greater them zero !!!");

		DateHistogramAggregationBuilder dateHistogramBuilder = AggregationBuilders.dateHistogram(getName());
		dateHistogramBuilder.field(field);

		if (timeUnit != null) {
			dateHistogramBuilder.dateHistogramInterval(timeUnit);
		} else {
			dateHistogramBuilder.interval(interval);
		}

		dateHistogramBuilder.subAggregation(AggregationBuilders.extendedStats(INTERNAL_STATS).field(field));

		return dateHistogramBuilder;
	}
}