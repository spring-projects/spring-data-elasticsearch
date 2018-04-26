/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;

import java.util.Map;
import java.util.function.Function;

/**
 * This Assistant will get a single double number from response.
 * @author yushan gao
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
