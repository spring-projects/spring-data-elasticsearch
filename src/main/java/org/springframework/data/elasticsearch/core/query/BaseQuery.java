/*
 * Copyright 2013-2024 the original author or authors.
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

import static java.util.Collections.*;
import static org.springframework.util.CollectionUtils.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * BaseQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Alen Turkovic
 * @author Sascha Woo
 * @author Farid Azaza
 * @author Peter-Josef Meisch
 * @author Peer Mueller
 * @author vdisk
 */
public class BaseQuery implements Query {

	private static final int DEFAULT_REACTIVE_BATCH_SIZE = 500;

	@Nullable protected Sort sort;
	protected Pageable pageable = DEFAULT_PAGE;
	protected List<String> fields = new ArrayList<>();
	@Nullable protected List<String> storedFields;
	@Nullable protected SourceFilter sourceFilter;
	protected float minScore;
	@Nullable protected Collection<String> ids;
	@Nullable protected String route;
	@Nullable protected SearchType searchType = SearchType.QUERY_THEN_FETCH;
	@Nullable protected IndicesOptions indicesOptions;
	protected boolean trackScores;
	@Nullable protected String preference;
	@Nullable protected Integer maxResults;
	@Nullable protected HighlightQuery highlightQuery;
	@Nullable private Boolean trackTotalHits;
	@Nullable protected Integer trackTotalHitsUpTo;
	@Nullable protected Duration scrollTime;
	@Nullable protected Duration timeout;
	private boolean explain = false;
	@Nullable protected List<Object> searchAfter;
	@Nullable protected List<IndexBoost> indicesBoost;
	protected List<RescorerQuery> rescorerQueries = new ArrayList<>();
	@Nullable protected Boolean requestCache;
	protected List<IdWithRouting> idsWithRouting = Collections.emptyList();
	protected List<RuntimeField> runtimeFields = new ArrayList<>();
	@Nullable protected PointInTime pointInTime;
	private boolean queryIsUpdatedByConverter = false;
	@Nullable private Integer reactiveBatchSize = null;
	@Nullable private Boolean allowNoIndices = null;
	private EnumSet<IndicesOptions.WildcardStates> expandWildcards;
	private List<DocValueField> docValueFields = new ArrayList<>();
	private List<ScriptedField> scriptedFields = new ArrayList<>();

	public BaseQuery() {}

	public <Q extends BaseQuery, B extends BaseQueryBuilder<Q, B>> BaseQuery(BaseQueryBuilder<Q, B> builder) {
		this.sort = builder.getSort();
		// do a setPageable after setting the sort, because the pageable may contain an additional sort
		this.setPageable(builder.getPageable() != null ? builder.getPageable() : DEFAULT_PAGE);
		this.fields = builder.getFields();
		this.storedFields = builder.getStoredFields();
		this.sourceFilter = builder.getSourceFilter();
		this.minScore = builder.getMinScore();
		this.ids = builder.getIds() == null ? null : builder.getIds();
		this.route = builder.getRoute();
		this.searchType = builder.getSearchType();
		this.indicesOptions = builder.getIndicesOptions();
		this.trackScores = builder.getTrackScores();
		this.preference = builder.getPreference();
		this.maxResults = builder.getMaxResults();
		this.highlightQuery = builder.getHighlightQuery();
		this.trackTotalHits = builder.getTrackTotalHits();
		this.trackTotalHitsUpTo = builder.getTrackTotalHitsUpTo();
		this.scrollTime = builder.getScrollTime();
		this.timeout = builder.getTimeout();
		this.explain = builder.getExplain();
		this.searchAfter = builder.getSearchAfter();
		this.indicesBoost = builder.getIndicesBoost();
		this.rescorerQueries = builder.getRescorerQueries();
		this.requestCache = builder.getRequestCache();
		this.idsWithRouting = builder.getIdsWithRouting();
		this.pointInTime = builder.getPointInTime();
		this.reactiveBatchSize = builder.getReactiveBatchSize();
		this.allowNoIndices = builder.getAllowNoIndices();
		this.expandWildcards = builder.getExpandWildcards();
		this.docValueFields = builder.getDocValueFields();
		this.scriptedFields = builder.getScriptedFields();
		this.runtimeFields = builder.getRuntimeFields();
	}

	/**
	 * @since 5.1
	 */
	public void setSort(@Nullable Sort sort) {
		this.sort = sort;
	}

	@Override
	@Nullable
	public Sort getSort() {
		return this.sort;
	}

	@Override
	public Pageable getPageable() {
		return this.pageable;
	}

	@Override
	public final <T extends Query> T setPageable(Pageable pageable) {

		Assert.notNull(pageable, "Pageable must not be null!");

		this.pageable = pageable;
		return this.addSort(pageable.getSort());
	}

	@Override
	public void addFields(String... fields) {
		addAll(this.fields, fields);
	}

	@Override
	public List<String> getFields() {
		return fields;
	}

	@Override
	public void setFields(List<String> fields) {

		Assert.notNull(fields, "fields must not be null");

		this.fields.clear();
		this.fields.addAll(fields);
	}

	@Override
	public void addStoredFields(String... storedFields) {

		if (storedFields.length == 0) {
			return;
		}

		if (this.storedFields == null) {
			this.storedFields = new ArrayList<>(storedFields.length);
		}
		addAll(this.storedFields, storedFields);
	}

	@Nullable
	@Override
	public List<String> getStoredFields() {
		return storedFields;
	}

	@Override
	public void setStoredFields(@Nullable List<String> storedFields) {
		this.storedFields = storedFields;
	}

	@Override
	public void addSourceFilter(SourceFilter sourceFilter) {
		this.sourceFilter = sourceFilter;
	}

	@Nullable
	@Override
	public SourceFilter getSourceFilter() {
		return sourceFilter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <T extends Query> T addSort(@Nullable Sort sort) {
		if (sort == null) {
			return (T) this;
		}

		if (this.sort == null) {
			this.sort = sort;
		} else {
			this.sort = this.sort.and(sort);
		}

		return (T) this;
	}

	@Override
	public float getMinScore() {
		return minScore;
	}

	public void setMinScore(float minScore) {
		this.minScore = minScore;
	}

	/**
	 * Set Ids for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param ids list of id values
	 */
	public void setIds(@Nullable Collection<String> ids) {
		this.ids = ids;
	}

	@Override
	@Nullable
	public Collection<String> getIds() {
		return ids;
	}

	@Override
	public List<IdWithRouting> getIdsWithRouting() {

		if (!isEmpty(idsWithRouting)) {
			return Collections.unmodifiableList(idsWithRouting);
		}

		if (!isEmpty(ids)) {
			return ids.stream().map(id -> new IdWithRouting(id, route)).collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	/**
	 * Set Ids with routing values for a multi-get request run with this query. Not used in any other searches.
	 *
	 * @param idsWithRouting list of id values, must not be {@literal null}
	 * @since 4.3
	 */
	public void setIdsWithRouting(List<IdWithRouting> idsWithRouting) {

		Assert.notNull(idsWithRouting, "idsWithRouting must not be null");

		this.idsWithRouting = idsWithRouting;
	}

	@Nullable
	@Override
	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public void setSearchType(@Nullable SearchType searchType) {
		this.searchType = searchType;
	}

	@Nullable
	@Override
	public SearchType getSearchType() {
		return searchType;
	}

	@Nullable
	@Override
	public IndicesOptions getIndicesOptions() {
		return indicesOptions;
	}

	public void setIndicesOptions(IndicesOptions indicesOptions) {
		this.indicesOptions = indicesOptions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.query.Query#getTrackScores()
	 */
	@Override
	public boolean getTrackScores() {
		return trackScores;
	}

	/**
	 * Configures whether to track scores.
	 *
	 * @since 3.1
	 */
	public void setTrackScores(boolean trackScores) {
		this.trackScores = trackScores;
	}

	@Nullable
	@Override
	public String getPreference() {
		return preference;
	}

	@Override
	public void setPreference(String preference) {
		this.preference = preference;
	}

	@Override
	public boolean isLimiting() {
		return maxResults != null;
	}

	@Nullable
	@Override
	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	@Override
	public void setHighlightQuery(HighlightQuery highlightQuery) {
		this.highlightQuery = highlightQuery;
	}

	@Override
	public Optional<HighlightQuery> getHighlightQuery() {
		return Optional.ofNullable(highlightQuery);
	}

	@Override
	public void setTrackTotalHits(@Nullable Boolean trackTotalHits) {
		this.trackTotalHits = trackTotalHits;
	}

	@Override
	@Nullable
	public Boolean getTrackTotalHits() {
		return trackTotalHits;
	}

	@Override
	public void setTrackTotalHitsUpTo(@Nullable Integer trackTotalHitsUpTo) {
		this.trackTotalHitsUpTo = trackTotalHitsUpTo;
	}

	@Override
	@Nullable
	public Integer getTrackTotalHitsUpTo() {
		return trackTotalHitsUpTo;
	}

	@Nullable
	@Override
	public Duration getScrollTime() {
		return scrollTime;
	}

	@Override
	public void setScrollTime(@Nullable Duration scrollTime) {
		this.scrollTime = scrollTime;
	}

	@Nullable
	@Override
	public Duration getTimeout() {
		return timeout;
	}

	/**
	 * set the query timeout
	 *
	 * @since 4.2
	 */
	public void setTimeout(@Nullable Duration timeout) {
		this.timeout = timeout;
	}

	@Override
	public boolean getExplain() {
		return explain;
	}

	/**
	 * @param explain the explain flag on the query.
	 */
	public void setExplain(boolean explain) {
		this.explain = explain;
	}

	@Override
	public void setSearchAfter(@Nullable List<Object> searchAfter) {
		this.searchAfter = searchAfter;
	}

	@Nullable
	@Override
	public List<Object> getSearchAfter() {
		return searchAfter;
	}

	@Override
	public void addRescorerQuery(RescorerQuery rescorerQuery) {

		Assert.notNull(rescorerQuery, "rescorerQuery must not be null");

		this.rescorerQueries.add(rescorerQuery);
	}

	@Override
	public void setRescorerQueries(List<RescorerQuery> rescorerQueryList) {

		Assert.notNull(rescorerQueries, "rescorerQueries must not be null");

		this.rescorerQueries.clear();
		this.rescorerQueries.addAll(rescorerQueryList);
	}

	@Override
	public List<RescorerQuery> getRescorerQueries() {
		return rescorerQueries;
	}

	@Override
	public void setRequestCache(@Nullable Boolean value) {
		this.requestCache = value;
	}

	@Override
	@Nullable
	public Boolean getRequestCache() {
		return this.requestCache;
	}

	@Override
	public void addRuntimeField(RuntimeField runtimeField) {

		Assert.notNull(runtimeField, "runtimeField must not be null");

		this.runtimeFields.add(runtimeField);
	}

	@Override
	public List<RuntimeField> getRuntimeFields() {
		return runtimeFields;
	}

	@Override
	@Nullable
	public List<IndexBoost> getIndicesBoost() {
		return indicesBoost;
	}

	/**
	 * @since 5.0
	 */
	@Nullable
	public PointInTime getPointInTime() {
		return pointInTime;
	}

	/**
	 * @since 5.0
	 */
	public void setPointInTime(@Nullable PointInTime pointInTime) {
		this.pointInTime = pointInTime;
	}

	/**
	 * used internally. Not considered part of the API.
	 *
	 * @since 5.0
	 */
	public boolean queryIsUpdatedByConverter() {
		return queryIsUpdatedByConverter;
	}

	/**
	 * used internally. Not considered part of the API.
	 *
	 * @since 5.0
	 */
	public void setQueryIsUpdatedByConverter(boolean queryIsUpdatedByConverter) {
		this.queryIsUpdatedByConverter = queryIsUpdatedByConverter;
	}

	@Override
	public Integer getReactiveBatchSize() {
		return reactiveBatchSize != null ? reactiveBatchSize : DEFAULT_REACTIVE_BATCH_SIZE;
	}

	/**
	 * @since 5.1
	 */
	public void setReactiveBatchSize(Integer reactiveBatchSize) {
		this.reactiveBatchSize = reactiveBatchSize;
	}

	@Nullable
	public Boolean getAllowNoIndices() {
		return allowNoIndices;
	}

	@Override
	public EnumSet<IndicesOptions.WildcardStates> getExpandWildcards() {
		return expandWildcards;
	}

	/**
	 * @since 5.1
	 */
	@Override
	public List<DocValueField> getDocValueFields() {
		return docValueFields;
	}

	/**
	 * @since 5.1
	 */
	public void setDocValueFields(List<DocValueField> docValueFields) {

		Assert.notNull(docValueFields, "getDocValueFields must not be null");

		this.docValueFields = docValueFields;
	}

	/**
	 * @since 5.2
	 */
	public void addScriptedField(ScriptedField scriptedField) {

		Assert.notNull(scriptedField, "scriptedField must not be null");

		this.scriptedFields.add(scriptedField);
	}

	@Override
	public List<ScriptedField> getScriptedFields() {
		return scriptedFields;
	}
}
