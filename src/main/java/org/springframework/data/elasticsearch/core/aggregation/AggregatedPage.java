package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.core.ScoredPage;
import org.springframework.data.elasticsearch.core.ScrolledPage;

/**
 * @author Petar Tahchiev
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 */
public interface AggregatedPage<T> extends ScrolledPage<T>, ScoredPage<T> {

	boolean hasAggregations();

	Aggregations getAggregations();

	Aggregation getAggregation(String name);
}
