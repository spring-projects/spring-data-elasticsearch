/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.4
 */
public class NativeQueryBuilder extends BaseQueryBuilder<NativeQuery, NativeQueryBuilder> {

	@Nullable private Query query;
	@Nullable private Query filter;
	private final Map<String, Aggregation> aggregations = new LinkedHashMap<>();
	@Nullable private Suggester suggester;
	@Nullable private FieldCollapse fieldCollapse;
	private final List<ScriptedField> scriptedFields = new ArrayList<>();
	private List<SortOptions> sortOptions = new ArrayList<>();
	private Map<String, JsonData> searchExtensions = new LinkedHashMap<>();

	public NativeQueryBuilder() {}

	@Nullable
	public Query getQuery() {
		return query;
	}

	@Nullable
	public Query getFilter() {
		return this.filter;
	}

	public Map<String, Aggregation> getAggregations() {
		return aggregations;
	}

	@Nullable
	public Suggester getSuggester() {
		return suggester;
	}

	@Nullable
	public FieldCollapse getFieldCollapse() {
		return fieldCollapse;
	}

	public List<ScriptedField> getScriptedFields() {
		return scriptedFields;
	}

	public List<SortOptions> getSortOptions() {
		return sortOptions;
	}

	public Map<String, JsonData> getSearchExtensions() {
		return this.searchExtensions;
	}

	public NativeQueryBuilder withQuery(Query query) {

		Assert.notNull(query, "query must not be null");

		this.query = query;
		return this;
	}

	public NativeQueryBuilder withQuery(Function<Query.Builder, ObjectBuilder<Query>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return withQuery(fn.apply(new Query.Builder()).build());
	}

	public NativeQueryBuilder withFilter(@Nullable Query filter) {
		this.filter = filter;
		return this;
	}

	public NativeQueryBuilder withFilter(Function<Query.Builder, ObjectBuilder<Query>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return withFilter(fn.apply(new Query.Builder()).build());
	}

	public NativeQueryBuilder withAggregation(String name, Aggregation aggregation) {

		Assert.notNull(name, "name must not be null");
		Assert.notNull(aggregation, "aggregation must not be null");

		this.aggregations.put(name, aggregation);
		return this;
	}

	public NativeQueryBuilder withSuggester(@Nullable Suggester suggester) {
		this.suggester = suggester;
		return this;
	}

	public NativeQueryBuilder withFieldCollapse(@Nullable FieldCollapse fieldCollapse) {
		this.fieldCollapse = fieldCollapse;
		return this;
	}

	public NativeQueryBuilder withScriptedField(ScriptedField scriptedField) {

		Assert.notNull(scriptedField, "scriptedField must not be null");

		this.scriptedFields.add(scriptedField);
		return this;
	}

	public NativeQueryBuilder withSort(List<SortOptions> values) {

		Assert.notEmpty(values, "values must not be empty");
		sortOptions.clear();
		sortOptions.addAll(values);

		return this;
	}

	public NativeQueryBuilder withSort(SortOptions value, SortOptions... values) {

		Assert.notNull(value, "value must not be null");
		sortOptions.add(value);
		if (values.length > 0) {
			sortOptions.addAll(Arrays.asList(values));
		}

		return this;
	}

	public NativeQueryBuilder withSort(Function<SortOptions.Builder, ObjectBuilder<SortOptions>> fn) {

		Assert.notNull(fn, "fn must not be null");
		withSort(fn.apply(new SortOptions.Builder()).build());

		return this;
	}

	public NativeQueryBuilder withSearchExtension(String key, JsonData value) {

		Assert.notNull(key, "key must not be null");
		Assert.notNull(value, "value must not be null");

		searchExtensions.put(key, value);
		return this;
	}

	public NativeQueryBuilder withSearchExtensions(Map<String, JsonData> searchExtensions) {

		Assert.notNull(searchExtensions, "searchExtensions must not be null");

		searchExtensions.putAll(searchExtensions);
		return this;
	}

	public NativeQuery build() {
		return new NativeQuery(this);
	}
}
