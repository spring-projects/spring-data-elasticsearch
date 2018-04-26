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
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.core.ResultsExtractor;

/**
 * This Extractor will check error and only return the aggregation in response.
 * @author yushan gao
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
