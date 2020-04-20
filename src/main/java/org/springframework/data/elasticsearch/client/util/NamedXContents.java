/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.client.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ContextParser;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.adjacency.AdjacencyMatrixAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.adjacency.ParsedAdjacencyMatrix;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoTileGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoTileGrid;
import org.elasticsearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.ParsedGlobal;
import org.elasticsearch.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedAutoDateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.elasticsearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.ParsedMissing;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedReverseNested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.GeoDistanceAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.IpRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.ParsedBinaryRange;
import org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.elasticsearch.search.aggregations.bucket.range.ParsedGeoDistance;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.sampler.InternalSampler;
import org.elasticsearch.search.aggregations.bucket.sampler.ParsedSampler;
import org.elasticsearch.search.aggregations.bucket.significant.ParsedSignificantLongTerms;
import org.elasticsearch.search.aggregations.bucket.significant.ParsedSignificantStringTerms;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantLongTerms;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.DoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedDoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.*;
import org.elasticsearch.search.aggregations.pipeline.*;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestion;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;

/**
 * <p>
 * Original implementation source {@link org.elasticsearch.client.RestHighLevelClient#getDefaultNamedXContents()} by
 * {@literal Elasticsearch} (<a href="https://www.elastic.co">https://www.elastic.co</a>) licensed under the Apache
 * License, Version 2.0.
 * </p>
 * Modified for usage with {@link ReactiveElasticsearchClient}.
 * <p>
 * Only intended for internal use.
 *
 * @author Russell Parry
 * @since 4.0
 */
public class NamedXContents {

	private NamedXContents() {
		// contains only utility methods
	}

	public static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
		Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
		map.put(CardinalityAggregationBuilder.NAME, (p, c) -> ParsedCardinality.fromXContent(p, (String) c));
		map.put(InternalHDRPercentiles.NAME, (p, c) -> ParsedHDRPercentiles.fromXContent(p, (String) c));
		map.put(InternalHDRPercentileRanks.NAME, (p, c) -> ParsedHDRPercentileRanks.fromXContent(p, (String) c));
		map.put(InternalTDigestPercentiles.NAME, (p, c) -> ParsedTDigestPercentiles.fromXContent(p, (String) c));
		map.put(InternalTDigestPercentileRanks.NAME, (p, c) -> ParsedTDigestPercentileRanks.fromXContent(p, (String) c));
		map.put(PercentilesBucketPipelineAggregationBuilder.NAME,
				(p, c) -> ParsedPercentilesBucket.fromXContent(p, (String) c));
		map.put(MedianAbsoluteDeviationAggregationBuilder.NAME,
				(p, c) -> ParsedMedianAbsoluteDeviation.fromXContent(p, (String) c));
		map.put(MinAggregationBuilder.NAME, (p, c) -> ParsedMin.fromXContent(p, (String) c));
		map.put(MaxAggregationBuilder.NAME, (p, c) -> ParsedMax.fromXContent(p, (String) c));
		map.put(SumAggregationBuilder.NAME, (p, c) -> ParsedSum.fromXContent(p, (String) c));
		map.put(AvgAggregationBuilder.NAME, (p, c) -> ParsedAvg.fromXContent(p, (String) c));
		map.put(WeightedAvgAggregationBuilder.NAME, (p, c) -> ParsedWeightedAvg.fromXContent(p, (String) c));
		map.put(ValueCountAggregationBuilder.NAME, (p, c) -> ParsedValueCount.fromXContent(p, (String) c));
		map.put(InternalSimpleValue.NAME, (p, c) -> ParsedSimpleValue.fromXContent(p, (String) c));
		map.put(DerivativePipelineAggregationBuilder.NAME, (p, c) -> ParsedDerivative.fromXContent(p, (String) c));
		map.put(InternalBucketMetricValue.NAME, (p, c) -> ParsedBucketMetricValue.fromXContent(p, (String) c));
		map.put(StatsAggregationBuilder.NAME, (p, c) -> ParsedStats.fromXContent(p, (String) c));
		map.put(StatsBucketPipelineAggregationBuilder.NAME, (p, c) -> ParsedStatsBucket.fromXContent(p, (String) c));
		map.put(ExtendedStatsAggregationBuilder.NAME, (p, c) -> ParsedExtendedStats.fromXContent(p, (String) c));
		map.put(ExtendedStatsBucketPipelineAggregationBuilder.NAME,
				(p, c) -> ParsedExtendedStatsBucket.fromXContent(p, (String) c));
		map.put(GeoBoundsAggregationBuilder.NAME, (p, c) -> ParsedGeoBounds.fromXContent(p, (String) c));
		map.put(GeoCentroidAggregationBuilder.NAME, (p, c) -> ParsedGeoCentroid.fromXContent(p, (String) c));
		map.put(HistogramAggregationBuilder.NAME, (p, c) -> ParsedHistogram.fromXContent(p, (String) c));
		map.put(DateHistogramAggregationBuilder.NAME, (p, c) -> ParsedDateHistogram.fromXContent(p, (String) c));
		map.put(AutoDateHistogramAggregationBuilder.NAME, (p, c) -> ParsedAutoDateHistogram.fromXContent(p, (String) c));
		map.put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c));
		map.put(LongTerms.NAME, (p, c) -> ParsedLongTerms.fromXContent(p, (String) c));
		map.put(DoubleTerms.NAME, (p, c) -> ParsedDoubleTerms.fromXContent(p, (String) c));
		map.put(MissingAggregationBuilder.NAME, (p, c) -> ParsedMissing.fromXContent(p, (String) c));
		map.put(NestedAggregationBuilder.NAME, (p, c) -> ParsedNested.fromXContent(p, (String) c));
		map.put(ReverseNestedAggregationBuilder.NAME, (p, c) -> ParsedReverseNested.fromXContent(p, (String) c));
		map.put(GlobalAggregationBuilder.NAME, (p, c) -> ParsedGlobal.fromXContent(p, (String) c));
		map.put(FilterAggregationBuilder.NAME, (p, c) -> ParsedFilter.fromXContent(p, (String) c));
		map.put(InternalSampler.PARSER_NAME, (p, c) -> ParsedSampler.fromXContent(p, (String) c));
		map.put(GeoHashGridAggregationBuilder.NAME, (p, c) -> ParsedGeoHashGrid.fromXContent(p, (String) c));
		map.put(GeoTileGridAggregationBuilder.NAME, (p, c) -> ParsedGeoTileGrid.fromXContent(p, (String) c));
		map.put(RangeAggregationBuilder.NAME, (p, c) -> ParsedRange.fromXContent(p, (String) c));
		map.put(DateRangeAggregationBuilder.NAME, (p, c) -> ParsedDateRange.fromXContent(p, (String) c));
		map.put(GeoDistanceAggregationBuilder.NAME, (p, c) -> ParsedGeoDistance.fromXContent(p, (String) c));
		map.put(FiltersAggregationBuilder.NAME, (p, c) -> ParsedFilters.fromXContent(p, (String) c));
		map.put(AdjacencyMatrixAggregationBuilder.NAME, (p, c) -> ParsedAdjacencyMatrix.fromXContent(p, (String) c));
		map.put(SignificantLongTerms.NAME, (p, c) -> ParsedSignificantLongTerms.fromXContent(p, (String) c));
		map.put(SignificantStringTerms.NAME, (p, c) -> ParsedSignificantStringTerms.fromXContent(p, (String) c));
		map.put(ScriptedMetricAggregationBuilder.NAME, (p, c) -> ParsedScriptedMetric.fromXContent(p, (String) c));
		map.put(IpRangeAggregationBuilder.NAME, (p, c) -> ParsedBinaryRange.fromXContent(p, (String) c));
		map.put(TopHitsAggregationBuilder.NAME, (p, c) -> ParsedTopHits.fromXContent(p, (String) c));
		map.put(CompositeAggregationBuilder.NAME, (p, c) -> ParsedComposite.fromXContent(p, (String) c));
		List<NamedXContentRegistry.Entry> entries = map.entrySet().stream().map(
				entry -> new NamedXContentRegistry.Entry(Aggregation.class, new ParseField(entry.getKey()), entry.getValue()))
				.collect(Collectors.toList());
		entries.add(
				new NamedXContentRegistry.Entry(Suggest.Suggestion.class, new ParseField(TermSuggestionBuilder.SUGGESTION_NAME),
						(parser, context) -> TermSuggestion.fromXContent(parser, (String) context)));
		entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class,
				new ParseField(PhraseSuggestionBuilder.SUGGESTION_NAME),
				(parser, context) -> PhraseSuggestion.fromXContent(parser, (String) context)));
		entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class,
				new ParseField(CompletionSuggestionBuilder.SUGGESTION_NAME),
				(parser, context) -> CompletionSuggestion.fromXContent(parser, (String) context)));
		return entries;
	}
}
