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

/**
 * The Assistant to create aggregation in query or read data from response.
 * @author yushan gao
 * @param <T> The result type to collect from response.
 */
public abstract class AggAssistant<T> {
    /**
     * The name of agg used to identify AggAssistant, should not conflict in the same aggregate level.
     */
    private String name;

    /**
     * The subAggs is low level aggregation of current aggregation. The name of subAggs should not conflict each other.
     */
    private AggAssistant[] subAggs;

    public AggAssistant(String name, AggAssistant... subAggs) {
        this.name = name;
        this.subAggs = subAggs;
    }

    /**
     * Create an AggregationBuilder only for this AggAssistant, not include its subAggs
     * @return
     */
    protected abstract AbstractAggregationBuilder createBuilder();

    /**
     * Read result from aggregation
     * @param aggregation This aggregation is exactly the aggregation of this AggAssistant.
     * @return The final result contains subAggs result
     */
    public abstract T collectValue(Aggregation aggregation);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AggAssistant[] getSubAggs() {
        return subAggs;
    }

    public void setSubAggs(AggAssistant[] subAggs) {
        this.subAggs = subAggs;
    }

    /**
     * Create an AggregationBuilder include its subAggs.This is the final AggregationBuilder.
     * @return
     */
    public AbstractAggregationBuilder toAggBuilder() {
        AbstractAggregationBuilder builder = createBuilder();
        if (subAggs != null) {
            for (AggAssistant assistant : subAggs) {
                builder.subAggregation(assistant.toAggBuilder());
            }
        }
        return builder;
    }
}
