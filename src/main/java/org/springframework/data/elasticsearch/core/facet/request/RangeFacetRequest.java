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

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Range facet for numeric fields
 *
 * @author Artur Konczak
 * @author Akos Bordas
 */
@Deprecated
public class RangeFacetRequest extends AbstractFacetRequest {

	public static final String RANGE_INTERNAL_SUM = "range-internal-sum";
	private String field;
	private String keyField;
	private String valueField;

	private List<Entry> entries = new ArrayList<>();

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
		entries.add(new DoubleEntry(from, to));
	}

	public void range(String from, String to) {
		throw new UnsupportedOperationException("Native Facet are not supported in Elasticsearch 2.x - use Aggregation");
	}

	public void addRange(Double from, Double to) {
		entries.add(new DoubleEntry(from, to));
	}

	public void addRange(String from, String to) {
		throw new UnsupportedOperationException("Native Facet are not supported in Elasticsearch 2.x - use Aggregation");
	}

	@Override
	public AbstractAggregationBuilder getFacet() {
		Assert.notNull(getName(), "Facet name can't be a null !!!");

		RangeAggregationBuilder rangeBuilder = AggregationBuilders.range(getName());
		final String field = !StringUtils.isEmpty(keyField) ? keyField : this.field;
		rangeBuilder.field(field);

		for (Entry entry : entries) {
			DoubleEntry doubleEntry = (DoubleEntry) entry;
			rangeBuilder.addRange(validateValue(doubleEntry.getFrom(), Double.NEGATIVE_INFINITY), validateValue(doubleEntry.getTo(), Double.POSITIVE_INFINITY));
		}

		rangeBuilder.subAggregation(AggregationBuilders.extendedStats(INTERNAL_STATS).field(field));
		if(!StringUtils.isEmpty(valueField)){
			rangeBuilder.subAggregation(AggregationBuilders.sum(RANGE_INTERNAL_SUM).field(valueField));
		}

		return rangeBuilder;
	}

	private double validateValue(Double value, double defaultValue) {
		return value == null ? defaultValue : value;
	}

	static class DoubleEntry extends Entry<Double> {

		DoubleEntry(Double from, Double to) {
			super(from, to);
		}
	}

	static class StringEntry extends Entry<String> {

		StringEntry(String from, String to) {
			super(from, to);
		}
	}

	static class Entry<T> {

		T from;
		T to;

		Entry(T from, T to) {
			this.from = from;
			this.to = to;
		}

		public T getFrom() {
			return from;
		}

		public T getTo() {
			return to;
		}
	}
}

