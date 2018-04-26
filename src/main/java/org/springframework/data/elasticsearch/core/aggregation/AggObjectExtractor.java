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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.springframework.data.elasticsearch.core.ResultsExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The Extractor to extract data by multi AggAssistant
 * @author yushan gao
 * @param <T>
 */
public class AggObjectExtractor<T> implements ResultsExtractor<T> {
    private final Function<Map<String, Object>, T> valueCollector;
    private List<AggAssistant> aggAssistants = new ArrayList<>();

    public AggObjectExtractor(Function<Map<String, Object>, T> valueCollector,
                              AggAssistant... aggAssistants) {
        this.valueCollector = valueCollector;
        for (AggAssistant assistant : aggAssistants) {
            this.aggAssistants.add(assistant);
        }
    }

    @Override
    public T extract(SearchResponse response) {
        if (response.getFailedShards() > 0) {
            throw new RuntimeException(response.getShardFailures()[0].reason());
        }

        Map<String, Object> valueMap = new HashMap<>();
        Map<String, Aggregation> aggMap = response.getAggregations().asMap();
        for (AggAssistant assistant : aggAssistants) {
            Aggregation agg = aggMap.get(assistant.getName());
            valueMap.put(assistant.getName(), assistant.collectValue(agg));
        }
        return valueCollector.apply(valueMap);
    }
}
