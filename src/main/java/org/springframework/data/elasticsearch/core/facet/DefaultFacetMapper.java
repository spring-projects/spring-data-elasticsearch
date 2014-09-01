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
package org.springframework.data.elasticsearch.core.facet;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.histogram.HistogramFacet;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.springframework.data.elasticsearch.core.facet.result.*;

/**
 * @author Artur Konczak
 * @author Petar Tahchiev
 */
public class DefaultFacetMapper {

	public static FacetResult parse(Facet facet) {
		if (facet instanceof TermsFacet) {
			return parseTerm((TermsFacet) facet);
		}

		if (facet instanceof RangeFacet) {
			return parseRange((RangeFacet) facet);
		}

		if (facet instanceof StatisticalFacet) {
			return parseStatistical((StatisticalFacet) facet);
		}

		if (facet instanceof HistogramFacet) {
			return parseHistogram((HistogramFacet) facet);
		}

		return null;
	}

	private static FacetResult parseTerm(TermsFacet facet) {
		List<Term> entries = new ArrayList<Term>();
		for (TermsFacet.Entry entry : facet.getEntries()) {
			entries.add(new Term(entry.getTerm().toString(), entry.getCount()));
		}
		return new TermResult(facet.getName(), entries, facet.getTotalCount(), facet.getOtherCount(), facet.getMissingCount());
	}

	private static FacetResult parseRange(RangeFacet facet) {
		List<Range> entries = new ArrayList<Range>();
		for (RangeFacet.Entry entry : facet.getEntries()) {
			entries.add(new Range(entry.getFrom() == Double.NEGATIVE_INFINITY ? null : entry.getFrom(), entry.getTo() == Double.POSITIVE_INFINITY ? null : entry.getTo(), entry.getCount(), entry.getTotal(), entry.getTotalCount(), entry.getMin(), entry.getMax()));
		}
		return new RangeResult(facet.getName(), entries);
	}

	private static FacetResult parseStatistical(StatisticalFacet facet) {
		return new StatisticalResult(facet.getName(), facet.getCount(), facet.getMax(), facet.getMin(), facet.getMean(), facet.getStdDeviation(), facet.getSumOfSquares(), facet.getTotal(), facet.getVariance());
	}

	private static FacetResult parseHistogram(HistogramFacet facet) {
		List<IntervalUnit> entries = new ArrayList<IntervalUnit>();
		for (HistogramFacet.Entry entry : facet.getEntries()) {
			entries.add(new IntervalUnit(entry.getKey(), entry.getCount(), entry.getTotalCount(), entry.getTotal(), entry.getMean(), entry.getMin(), entry.getMax()));
		}
		return new HistogramResult(facet.getName(), entries);
	}
}
