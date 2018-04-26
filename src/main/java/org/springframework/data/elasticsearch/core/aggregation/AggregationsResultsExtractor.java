package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.core.ResultsExtractor;

/**
 * This Extractor will check error and only return the aggregation in response.
 */
public class AggregationsResultsExtractor implements ResultsExtractor<Aggregations> {
    @Override
    public Aggregations extract(SearchResponse response) {
        if (response.getFailedShards() > 0) {
            throw new UnsupportedOperationException(response.getShardFailures()[0].reason());
        }
        return response.getAggregations();
    }
}
