package org.springframework.data.elasticsearch.core.domain;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;

/**
 * @author Petar Tahchiev
 */
public interface AggregatedPage<T> {

	boolean hasAggregations();

	Aggregations getAggregations();

	Aggregation getAggregation(String name);
}
