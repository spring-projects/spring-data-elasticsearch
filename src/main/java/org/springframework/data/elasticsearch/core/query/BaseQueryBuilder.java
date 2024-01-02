/*
 * Copyright 2022-2024 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * base class for query builders. The different implementations of {@link Query} should derive from this class and then
 * offer a constructor that takes their builder as argument and passes this on to the super class.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public abstract class BaseQueryBuilder<Q extends BaseQuery, SELF extends BaseQueryBuilder<Q, SELF>> {

	@Nullable private Sort sort;
	@Nullable private Pageable pageable;
	private final List<String> fields = new ArrayList<>();
	@Nullable private List<String> storedFields;
	@Nullable private SourceFilter sourceFilter;
	private float minScore;
	private final Collection<String> ids = new ArrayList<>();
	@Nullable private String route;
	@Nullable private Query.SearchType searchType = Query.SearchType.QUERY_THEN_FETCH;
	@Nullable private IndicesOptions indicesOptions;
	private boolean trackScores;
	@Nullable private String preference;
	@Nullable private Integer maxResults;
	@Nullable private HighlightQuery highlightQuery;
	@Nullable private Boolean trackTotalHits;
	@Nullable private Integer trackTotalHitsUpTo;
	@Nullable private Duration scrollTime;
	@Nullable private Duration timeout;
	boolean explain = false;
	@Nullable private List<Object> searchAfter;

	@Nullable private List<IndexBoost> indicesBoost;
	protected final List<RescorerQuery> rescorerQueries = new ArrayList<>();

	@Nullable private Boolean requestCache;
	private final List<Query.IdWithRouting> idsWithRouting = new ArrayList<>();
	private final List<RuntimeField> runtimeFields = new ArrayList<>();
	@Nullable private Query.PointInTime pointInTime;
	@Nullable private Boolean allowNoIndices;
	private EnumSet<IndicesOptions.WildcardStates> expandWildcards = EnumSet.noneOf(IndicesOptions.WildcardStates.class);

	@Nullable Integer reactiveBatchSize;
	private final List<DocValueField> docValueFields = new ArrayList<>();
	private final List<ScriptedField> scriptedFields = new ArrayList<>();

	@Nullable
	public Sort getSort() {
		return sort;
	}

	@Nullable
	public Pageable getPageable() {
		return pageable;
	}

	public List<String> getFields() {
		return fields;
	}

	@Nullable
	public List<String> getStoredFields() {
		return storedFields;
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

	@Nullable
	public Query.SearchType getSearchType() {
		return searchType;
	}

	@Nullable
	public Boolean getTrackTotalHits() {
		return trackTotalHits;
	}

	@Nullable
	public Integer getTrackTotalHitsUpTo() {
		return trackTotalHitsUpTo;
	}

	@Nullable
	public Duration getScrollTime() {
		return scrollTime;
	}

	@Nullable
	public Duration getTimeout() {
		return timeout;
	}

	public boolean getExplain() {
		return explain;
	}

	@Nullable
	public List<Object> getSearchAfter() {
		return searchAfter;
	}

	@Nullable
	public Boolean getRequestCache() {
		return requestCache;
	}

	public List<Query.IdWithRouting> getIdsWithRouting() {
		return idsWithRouting;
	}

	public List<RuntimeField> getRuntimeFields() {
		return runtimeFields;
	}

	public List<RescorerQuery> getRescorerQueries() {
		return rescorerQueries;
	}

	/**
	 * @since 5.0
	 */
	@Nullable
	public Query.PointInTime getPointInTime() {
		return pointInTime;
	}

	/**
	 * @since 5.1
	 */
	public Integer getReactiveBatchSize() {
		return reactiveBatchSize;
	}

	/**
	 * @since 5.1
	 */
	@Nullable
	public Boolean getAllowNoIndices() {
		return allowNoIndices;
	}

	/**
	 * @since 5.1
	 */
	public EnumSet<IndicesOptions.WildcardStates> getExpandWildcards() {
		return expandWildcards;
	}

	/**
	 * @since 5.1
	 */
	public List<DocValueField> getDocValueFields() {
		return docValueFields;
	}

	public List<ScriptedField> getScriptedFields() {
		return scriptedFields;
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

	/**
	 * Set Ids for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param ids list of id values
	 */
	public SELF withIds(String... ids) {

		this.ids.clear();
		this.ids.addAll(Arrays.asList(ids));
		return self();
	}

	/**
	 * Set Ids for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param ids list of id values
	 */
	public SELF withIds(Collection<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		this.ids.clear();
		this.ids.addAll(ids);
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

		this.fields.clear();
		Collections.addAll(this.fields, fields);
		return self();
	}

	public SELF withFields(Collection<String> fields) {

		Assert.notNull(fields, "fields must not be null");

		this.fields.clear();
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

	public SELF withIndicesBoost(@Nullable List<IndexBoost> indicesBoost) {
		this.indicesBoost = indicesBoost;
		return self();
	}

	public SELF withStoredFields(@Nullable List<String> storedFields) {
		this.storedFields = storedFields;
		return self();
	}

	public SELF withIndicesBoost(IndexBoost... indicesBoost) {
		this.indicesBoost = Arrays.asList(indicesBoost);
		return self();
	}

	public SELF withSearchType(@Nullable Query.SearchType searchType) {
		this.searchType = searchType;
		return self();
	}

	public SELF withTrackTotalHits(@Nullable Boolean trackTotalHits) {
		this.trackTotalHits = trackTotalHits;
		return self();
	}

	public SELF withTrackTotalHitsUpTo(@Nullable Integer trackTotalHitsUpTo) {
		this.trackTotalHitsUpTo = trackTotalHitsUpTo;
		return self();
	}

	public SELF withTimeout(@Nullable Duration timeout) {
		this.timeout = timeout;
		return self();
	}

	public SELF withScrollTime(@Nullable Duration scrollTime) {
		this.scrollTime = scrollTime;
		return self();
	}

	public SELF withExplain(boolean explain) {
		this.explain = explain;
		return self();
	}

	public SELF withSearchAfter(@Nullable List<Object> searchAfter) {
		this.searchAfter = searchAfter;
		return self();
	}

	public SELF withRequestCache(@Nullable Boolean requestCache) {
		this.requestCache = requestCache;
		return self();
	}

	/**
	 * Set Ids with routing values for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param idsWithRouting list of id values, must not be {@literal null}
	 * @since 4.3
	 */
	public SELF withIdsWithRouting(List<Query.IdWithRouting> idsWithRouting) {

		Assert.notNull(idsWithRouting, "idsWithRouting must not be null");

		this.idsWithRouting.clear();
		this.idsWithRouting.addAll(idsWithRouting);
		return self();
	}

	public SELF withRuntimeFields(List<RuntimeField> runtimeFields) {

		Assert.notNull(runtimeFields, "runtimeFields must not be null");

		this.runtimeFields.clear();
		this.runtimeFields.addAll(runtimeFields);
		return self();
	}

	public SELF withRescorerQueries(List<RescorerQuery> rescorerQueries) {

		Assert.notNull(rescorerQueries, "rescorerQueries must not be null");

		this.rescorerQueries.clear();
		this.rescorerQueries.addAll(rescorerQueries);
		return self();
	}

	public SELF withRescorerQuery(RescorerQuery rescorerQuery) {

		Assert.notNull(rescorerQuery, "rescorerQuery must not be null");

		this.rescorerQueries.add(rescorerQuery);
		return self();
	}

	/**
	 * @since 5.0
	 */
	public SELF withPointInTime(@Nullable Query.PointInTime pointInTime) {
		this.pointInTime = pointInTime;
		return self();
	}

	/**
	 * @since 5.1
	 */
	public SELF withReactiveBatchSize(@Nullable Integer reactiveBatchSize) {
		this.reactiveBatchSize = reactiveBatchSize;
		return self();
	}

	public SELF withAllowNoIndices(@Nullable Boolean allowNoIndices) {
		this.allowNoIndices = allowNoIndices;
		return self();
	}

	public SELF withExpandWildcards(EnumSet<IndicesOptions.WildcardStates> expandWildcards) {

		Assert.notNull(expandWildcards, "expandWildcards must not be null");

		this.expandWildcards = expandWildcards;
		return self();
	}

	/**
	 * @since 5.1
	 */
	public SELF withDocValueFields(List<DocValueField> docValueFields) {

		Assert.notNull(docValueFields, "docValueFields must not be null");

		this.docValueFields.clear();
		this.docValueFields.addAll(docValueFields);
		return self();
	}

	public SELF withScriptedField(ScriptedField scriptedField) {

		Assert.notNull(scriptedField, "scriptedField must not be null");

		this.scriptedFields.add(scriptedField);
		return self();
	}

	public abstract Q build();

	private SELF self() {
		// noinspection unchecked
		return (SELF) this;
	}
}
