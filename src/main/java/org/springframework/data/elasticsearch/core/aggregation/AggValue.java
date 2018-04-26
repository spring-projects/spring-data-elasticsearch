package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;

import java.util.Map;
import java.util.function.Function;

/**
 * This Assistant will get a single double number from response.
 */
public abstract class AggValue extends AggAssistant <Double> {
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

    public static AggValue value(String name, String fieldCode, Function<String, ? extends ValuesSourceAggregationBuilder> creator) {
        return new AggValue(name) {
            @Override
            protected AbstractAggregationBuilder createBuilder() {
                return creator.apply(name).field(fieldCode);
            }
        };
    }
}
