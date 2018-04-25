package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;

import java.util.Map;

public abstract class AggValue extends AggAssistant {
    public AggValue(String name) {
        super(name);
    }

    public Double readValue(Map<String, Object> subItems) {
        return (Double) subItems.get(this.getName());
    }

    public Double readValue(Map<String, Object> subItems, double defaultValue) {
        return (Double) subItems.getOrDefault(this.getName(), defaultValue);
    }

    @Override
    public Double collectValue(Aggregation aggregation) {
        if (aggregation instanceof NumericMetricsAggregation.SingleValue) {
            double value = ((NumericMetricsAggregation.SingleValue) aggregation).value();
            return Double.isNaN(value) ? 0 : value;
        }
        return 0d;
    }
}
