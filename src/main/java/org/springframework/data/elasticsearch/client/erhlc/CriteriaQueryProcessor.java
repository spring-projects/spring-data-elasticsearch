/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import static org.elasticsearch.index.query.Operator.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.core.query.Criteria.*;
import static org.springframework.util.StringUtils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.Field;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * CriteriaQueryProcessor
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
 * @author Rasmus Faber-Espensen
 * @author James Bodkin
 * @author Peter-Josef Meisch
 * @deprecated since 5.0
 */
@Deprecated
class CriteriaQueryProcessor {

	@Nullable
	QueryBuilder createQuery(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		List<QueryBuilder> shouldQueryBuilders = new ArrayList<>();
		List<QueryBuilder> mustNotQueryBuilders = new ArrayList<>();
		List<QueryBuilder> mustQueryBuilders = new ArrayList<>();

		QueryBuilder firstQuery = null;
		boolean negateFirstQuery = false;

		for (Criteria chainedCriteria : criteria.getCriteriaChain()) {
			QueryBuilder queryFragment = queryForEntries(chainedCriteria);

			if (queryFragment != null) {

				if (firstQuery == null) {
					firstQuery = queryFragment;
					negateFirstQuery = chainedCriteria.isNegating();
					continue;
				}

				if (chainedCriteria.isOr()) {
					shouldQueryBuilders.add(queryFragment);
				} else if (chainedCriteria.isNegating()) {
					mustNotQueryBuilders.add(queryFragment);
				} else {
					mustQueryBuilders.add(queryFragment);
				}
			}
		}

		for (Criteria subCriteria : criteria.getSubCriteria()) {

			QueryBuilder subQuery = createQuery(subCriteria);

			if (subQuery != null) {
				if (criteria.isOr()) {
					shouldQueryBuilders.add(subQuery);
				} else if (criteria.isNegating()) {
					mustNotQueryBuilders.add(subQuery);
				} else {
					mustQueryBuilders.add(subQuery);
				}
			}
		}

		if (firstQuery != null) {

			if (!shouldQueryBuilders.isEmpty() && mustNotQueryBuilders.isEmpty() && mustQueryBuilders.isEmpty()) {
				shouldQueryBuilders.add(0, firstQuery);
			} else {

				if (negateFirstQuery) {
					mustNotQueryBuilders.add(0, firstQuery);
				} else {
					mustQueryBuilders.add(0, firstQuery);
				}
			}
		}

		BoolQueryBuilder query = null;

		if (!shouldQueryBuilders.isEmpty() || !mustNotQueryBuilders.isEmpty() || !mustQueryBuilders.isEmpty()) {

			query = boolQuery();

			for (QueryBuilder qb : shouldQueryBuilders) {
				query.should(qb);
			}
			for (QueryBuilder qb : mustNotQueryBuilders) {
				query.mustNot(qb);
			}
			for (QueryBuilder qb : mustQueryBuilders) {
				query.must(qb);
			}
		}

		return query;
	}

	@Nullable
	private QueryBuilder queryForEntries(Criteria criteria) {

		Field field = criteria.getField();

		if (field == null || criteria.getQueryCriteriaEntries().isEmpty())
			return null;

		String fieldName = field.getName();
		Assert.notNull(fieldName, "Unknown field " + fieldName);

		Iterator<Criteria.CriteriaEntry> it = criteria.getQueryCriteriaEntries().iterator();
		QueryBuilder query;

		if (criteria.getQueryCriteriaEntries().size() == 1) {
			query = queryFor(it.next(), field);
		} else {
			query = boolQuery();
			while (it.hasNext()) {
				Criteria.CriteriaEntry entry = it.next();
				((BoolQueryBuilder) query).must(queryFor(entry, field));
			}
		}

		addBoost(query, criteria.getBoost());

		if (hasText(field.getPath())) {
			query = nestedQuery(field.getPath(), query, ScoreMode.Avg);
		}

		return query;
	}

	@Nullable
	private QueryBuilder queryFor(Criteria.CriteriaEntry entry, Field field) {

		QueryBuilder query = null;
		String fieldName = field.getName();
		boolean isKeywordField = FieldType.Keyword == field.getFieldType();

		OperationKey key = entry.getKey();

		// operations without a value
		switch (key) {
			case EXISTS -> query = existsQuery(fieldName);
			case EMPTY -> query = boolQuery().must(existsQuery(fieldName)).mustNot(wildcardQuery(fieldName, "*"));
			case NOT_EMPTY -> query = wildcardQuery(fieldName, "*");
			default -> {}
		}

		if (query != null) {
			return query;
		}

		// now operation keys with a value
		Object value = entry.getValue();
		String searchText = QueryParserUtil.escape(value.toString());

		switch (key) {
			case EQUALS:
				query = queryStringQuery(searchText).field(fieldName).defaultOperator(AND);
				break;
			case CONTAINS:
				query = queryStringQuery('*' + searchText + '*').field(fieldName).analyzeWildcard(true);
				break;
			case STARTS_WITH:
				query = queryStringQuery(searchText + '*').field(fieldName).analyzeWildcard(true);
				break;
			case ENDS_WITH:
				query = queryStringQuery('*' + searchText).field(fieldName).analyzeWildcard(true);
				break;
			case EXPRESSION:
				query = queryStringQuery(value.toString()).field(fieldName);
				break;
			case LESS_EQUAL:
				query = rangeQuery(fieldName).lte(value);
				break;
			case GREATER_EQUAL:
				query = rangeQuery(fieldName).gte(value);
				break;
			case BETWEEN:
				Object[] ranges = (Object[]) value;
				query = rangeQuery(fieldName).from(ranges[0]).to(ranges[1]);
				break;
			case LESS:
				query = rangeQuery(fieldName).lt(value);
				break;
			case GREATER:
				query = rangeQuery(fieldName).gt(value);
				break;
			case FUZZY:
				query = fuzzyQuery(fieldName, searchText);
				break;
			case MATCHES:
				query = matchQuery(fieldName, value).operator(org.elasticsearch.index.query.Operator.OR);
				break;
			case MATCHES_ALL:
				query = matchQuery(fieldName, value).operator(org.elasticsearch.index.query.Operator.AND);
				break;
			case IN:
				if (value instanceof Iterable<?> iterable) {
					if (isKeywordField) {
						query = boolQuery().must(termsQuery(fieldName, toStringList(iterable)));
					} else {
						query = queryStringQuery(orQueryString(iterable)).field(fieldName);
					}
				}
				break;
			case NOT_IN:
				if (value instanceof Iterable<?> iterable) {
					if (isKeywordField) {
						query = boolQuery().mustNot(termsQuery(fieldName, toStringList(iterable)));
					} else {
						query = queryStringQuery("NOT(" + orQueryString(iterable) + ')').field(fieldName);
					}
				}
				break;
		}
		return query;
	}

	private static List<String> toStringList(Iterable<?> iterable) {
		List<String> list = new ArrayList<>();
		for (Object item : iterable) {
			list.add(item != null ? item.toString() : null);
		}
		return list;
	}

	private static String orQueryString(Iterable<?> iterable) {
		StringBuilder sb = new StringBuilder();

		for (Object item : iterable) {

			if (item != null) {

				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append('"');
				sb.append(QueryParserUtil.escape(item.toString()));
				sb.append('"');
			}
		}

		return sb.toString();
	}

	private void addBoost(@Nullable QueryBuilder query, float boost) {

		if (query == null || Float.isNaN(boost)) {
			return;
		}

		query.boost(boost);
	}
}
