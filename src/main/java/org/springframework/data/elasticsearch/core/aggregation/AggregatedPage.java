package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.core.ScoredPage;
import org.springframework.data.elasticsearch.core.ScrolledPage;
import org.springframework.lang.Nullable;

/**
 * @author Petar Tahchiev
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 * @deprecated since 4.0, use {@link org.springframework.data.elasticsearch.core.SearchHits} to return values.
 */
@Deprecated
public interface AggregatedPage<T> extends ScrolledPage<T>, ScoredPage<T> {

	boolean hasAggregations();

	@Nullable Aggregations getAggregations();

	@Nullable Aggregation getAggregation(String name);
}
