/*
 * Copyright 2014 the original author or authors.
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
 * @author Akos Bordas
 */
public class RangeFacetRequest extends AbstractFacetRequest {

	private String field;
	private String keyField;
	private String valueField;

	private List<Entry> entries = new ArrayList<Entry>();

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
		entries.add(new StringEntry(from, to));
	}

	public void addRange(Double from, Double to) {
		entries.add(new DoubleEntry(from, to));
	}

	public void addRange(String from, String to) {
		entries.add(new StringEntry(from, to));
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

		for (Entry entry : entries) {
			if (entry instanceof DoubleEntry) {
				DoubleEntry doubleEntry = (DoubleEntry) entry;
				builder.addRange(validateValue(doubleEntry.getFrom(), Double.NEGATIVE_INFINITY), validateValue(doubleEntry.getTo(), Double.POSITIVE_INFINITY));
			} else {
				StringEntry stringEntry = (StringEntry) entry;
				builder.addRange(stringEntry.getFrom(), stringEntry.getTo());
			}
		}

		return builder;
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
