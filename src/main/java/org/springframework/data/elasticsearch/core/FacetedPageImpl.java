/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStats;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.joda.time.DateTime;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.data.elasticsearch.core.facet.FacetResult;
import org.springframework.data.elasticsearch.core.facet.request.RangeFacetRequest;
import org.springframework.data.elasticsearch.core.facet.result.HistogramResult;
import org.springframework.data.elasticsearch.core.facet.result.IntervalUnit;
import org.springframework.data.elasticsearch.core.facet.result.RangeResult;
import org.springframework.data.elasticsearch.core.facet.result.StatisticalResult;
import org.springframework.data.elasticsearch.core.facet.result.Term;
import org.springframework.data.elasticsearch.core.facet.result.TermResult;

import static java.util.Optional.ofNullable;

/**
 * Container for query result and facet results
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 * @author Philipp Kräutli
 * @author Remco Zigterman
 */
@Deprecated
public abstract class FacetedPageImpl<T> extends PageImpl<T> implements FacetedPage<T>, AggregatedPage<T> {

	private List<FacetResult> facets;
	private Map<String, FacetResult> mapOfFacets = new HashMap<>();

	public FacetedPageImpl(List<T> content) {
		super(content);
	}

	public FacetedPageImpl(List<T> content, Pageable pageable, long total) {
		super(content, pageable, total);
	}

	@Override
	public boolean hasFacets() {
		processAggregations();
		return facets != null && facets.size() > 0;
	}

	@Override
	public List<FacetResult> getFacets() {
		processAggregations();
		return facets;
	}

	@Override
	public FacetResult getFacet(String name) {
		processAggregations();
		return mapOfFacets.get(name);
	}

	private void addFacet(FacetResult facetResult) {
		facets.add(facetResult);
		mapOfFacets.put(facetResult.getName(), facetResult);
	}

	/**
	 * Lazy conversion from aggregation to old facets
	 */
	private void processAggregations() {
		if (facets != null) {
			return;
		}
		facets = new ArrayList<>();
		Aggregations aggregations = getAggregations();
		if (aggregations == null) {
			return;
		}
		for (Aggregation agg : aggregations) {
			processAggregation(agg);
		}
	}

	private void processAggregation(Aggregation agg)
	{
		if (agg instanceof Terms) {
			processTermAggregation((Terms) agg);
		}
		if (agg instanceof Range) {
			processRangeAggregation((Range) agg);
		}
		if (agg instanceof ExtendedStats) {
			processExtendedStatsAggregation((ExtendedStats) agg);
		}
		if (agg instanceof Histogram) {
			processHistogramAggregation((Histogram) agg);
		}
	}

	private void processTermAggregation(Terms agg)
	{
		List<Term> terms = new ArrayList<>();
		for (Terms.Bucket t : agg.getBuckets()) {
			terms.add(new Term(t.getKeyAsString(), t.getDocCount()));
		}
		addFacet(new TermResult(agg.getName(), terms, terms.size(), agg.getSumOfOtherDocCounts(), 0));
	}

	private void processRangeAggregation(Range agg)
	{
		List<? extends Range.Bucket> buckets = ((Range) agg).getBuckets();
		List<org.springframework.data.elasticsearch.core.facet.result.Range> ranges = new ArrayList<>();
		for (Range.Bucket b : buckets) {
			ExtendedStats rStats = b.getAggregations().get(AbstractFacetRequest.INTERNAL_STATS);
			if (rStats != null) {
				Sum sum = b.getAggregations().get(RangeFacetRequest.RANGE_INTERNAL_SUM);
				ranges.add(new org.springframework.data.elasticsearch.core.facet.result.Range((Double) b.getFrom(), (Double) b.getTo(), b.getDocCount(), sum != null ? sum.getValue() : rStats.getSum(), rStats.getCount(), rStats.getMin(), rStats.getMax()));
			} else {
				ranges.add(new org.springframework.data.elasticsearch.core.facet.result.Range((Double) b.getFrom(), (Double) b.getTo(), b.getDocCount(), 0, 0, 0, 0));
			}
		}
		addFacet(new RangeResult(agg.getName(), ranges));
	}

	private void processExtendedStatsAggregation(ExtendedStats agg)
	{
		addFacet(new StatisticalResult(agg.getName(), agg.getCount(), agg.getMax(), agg.getMin(), agg.getAvg(), agg.getStdDeviation(), agg.getSumOfSquares(), agg.getSum(), agg.getVariance()));
	}

	private void processHistogramAggregation(Histogram agg)
	{
		List<IntervalUnit> intervals = new ArrayList<>();
		for (Histogram.Bucket h : agg.getBuckets()) {
			ExtendedStats hStats = h.getAggregations().get(AbstractFacetRequest.INTERNAL_STATS);
			if (hStats != null) {
				intervals.add(new IntervalUnit(((DateTime) h.getKey()).getMillis(), h.getDocCount(), h.getDocCount(), hStats.getSum(), hStats.getAvg(), hStats.getMin(), hStats.getMax()));
			} else {
				intervals.add(new IntervalUnit(((DateTime) h.getKey()).getMillis(), h.getDocCount(), h.getDocCount(), 0, 0, 0, 0));
			}
		}
		addFacet(new HistogramResult(agg.getName(), intervals));
	}
}
