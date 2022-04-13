/*
 * Copyright 2021-2022 the original author or authors.
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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.elasticsearch.core.search.Suggester;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.RescorerQuery;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.lang.Nullable;

/**
 * A {@link org.springframework.data.elasticsearch.core.query.Query} implementation using query builders from the new
 * Elasticsearch Client library.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class NativeQuery extends BaseQuery {

	@Nullable private final Query query;
	@Nullable private Query filter;
	// note: the new client does not have pipeline aggs, these are just set up as normal aggs
	private final Map<String, Aggregation> aggregations = new LinkedHashMap<>();
	@Nullable private Suggester suggester;
	@Nullable private FieldCollapse fieldCollapse;
	private List<ScriptedField> scriptedFields = Collections.emptyList();
	private List<RescorerQuery> rescorerQueries = Collections.emptyList();

	public NativeQuery(NativeQueryBuilder builder) {
		super(builder);
		this.query = builder.getQuery();
		this.filter = builder.getFilter();
		this.aggregations.putAll(builder.getAggregations());
		this.suggester = builder.getSuggester();
		this.fieldCollapse = builder.getFieldCollapse();
		this.scriptedFields = builder.getScriptedFields();
		this.rescorerQueries = builder.getRescorerQueries();
	}

	public NativeQuery(@Nullable Query query) {
		this.query = query;
	}

	public static NativeQueryBuilder builder() {
		return new NativeQueryBuilder();
	}

	@Nullable
	public Query getQuery() {
		return query;
	}

	@Nullable
	public Query getFilter() {
		return filter;
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

	@Override
	public List<RescorerQuery> getRescorerQueries() {
		return rescorerQueries;
	}
}
