package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.springframework.data.elasticsearch.core.ResultsExtractor;

import java.util.List;

public class AggAssistantListExtractor<T> implements ResultsExtractor<List<T>> {
    private AggList<T> aggAssistant;
    public AggAssistantListExtractor(AggList<T> aggAssistant) {
        this.aggAssistant = aggAssistant;
    }
    @Override
    public List<T> extract(SearchResponse response) {
        if (response.getFailedShards() > 0) {
            throw new UnsupportedOperationException(response.getShardFailures()[0].reason());
        }
        Aggregation agg = response.getAggregations().asMap().get(aggAssistant.getName());
        return aggAssistant.collectValue(agg);
    }
}
