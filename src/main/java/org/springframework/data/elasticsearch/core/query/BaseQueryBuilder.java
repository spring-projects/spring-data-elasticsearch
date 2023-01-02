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
package org.springframework.data.elasticsearch.core.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * base class for query builders. The different implementations of {@link Query} should derive from this class and then
 * offer a constructor that takes their builder as argument and passes this on to the super class.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public abstract class BaseQueryBuilder<Q extends BaseQuery, SELF extends BaseQueryBuilder<Q, SELF>> {

	@Nullable private Pageable pageable;
	@Nullable private Sort sort;
	@Nullable private Integer maxResults;
	@Nullable private Collection<String> ids;
	private boolean trackScores;
	@Nullable protected IndicesOptions indicesOptions;
	private float minScore;
	@Nullable private String preference;
	@Nullable private SourceFilter sourceFilter;
	private List<String> fields = new ArrayList<>();
	@Nullable protected HighlightQuery highlightQuery;
	@Nullable private String route;
	@Nullable private List<IndexBoost> indicesBoost;

	@Nullable
	public Pageable getPageable() {
		return pageable;
	}

	@Nullable
	public Sort getSort() {
		return sort;
	}

	@Nullable
	public Integer getMaxResults() {
		return maxResults;
	}

	@Nullable
	public Collection<String> getIds() {
		return ids;
	}

	public boolean getTrackScores() {
		return trackScores;
	}

	@Nullable
	public IndicesOptions getIndicesOptions() {
		return indicesOptions;
	}

	public float getMinScore() {
		return minScore;
	}

	@Nullable
	public String getPreference() {
		return preference;
	}

	@Nullable
	public SourceFilter getSourceFilter() {
		return sourceFilter;
	}

	public List<String> getFields() {
		return fields;
	}

	@Nullable
	public HighlightQuery getHighlightQuery() {
		return highlightQuery;
	}

	@Nullable
	public String getRoute() {
		return route;
	}

	@Nullable
	public List<IndexBoost> getIndicesBoost() {
		return indicesBoost;
	}

	public SELF withPageable(Pageable pageable) {
		this.pageable = pageable;
		return self();
	}

	public SELF withSort(Sort sort) {
		if (this.sort == null) {
			this.sort = sort;
		} else {
			this.sort = this.sort.and(sort);
		}
		return self();
	}

	public SELF withMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
		return self();
	}

	public SELF withIds(String... ids) {
		this.ids = Arrays.asList(ids);
		return self();
	}

	public SELF withIds(Collection<String> ids) {
		this.ids = ids;
		return self();
	}

	public SELF withTrackScores(boolean trackScores) {
		this.trackScores = trackScores;
		return self();
	}

	public SELF withIndicesOptions(IndicesOptions indicesOptions) {
		this.indicesOptions = indicesOptions;
		return self();
	}

	public SELF withMinScore(float minScore) {
		this.minScore = minScore;
		return self();
	}

	public SELF withPreference(String preference) {
		this.preference = preference;
		return self();
	}

	public SELF withSourceFilter(SourceFilter sourceFilter) {
		this.sourceFilter = sourceFilter;
		return self();
	}

	public SELF withFields(String... fields) {
		Collections.addAll(this.fields, fields);
		return self();
	}

	public SELF withFields(Collection<String> fields) {
		this.fields.addAll(fields);
		return self();
	}

	public SELF withHighlightQuery(HighlightQuery highlightQuery) {
		this.highlightQuery = highlightQuery;
		return self();
	}

	public SELF withRoute(String route) {
		this.route = route;
		return self();
	}

	public SELF withIndicesBoost(List<IndexBoost> indicesBoost) {
		this.indicesBoost = indicesBoost;
		return self();
	}

	public SELF withIndicesBoost(IndexBoost... indicesBoost) {
		this.indicesBoost = Arrays.asList(indicesBoost);
		return self();
	}

	public abstract Q build();

	private SELF self() {
		// noinspection unchecked
		return (SELF) this;
	}
}
