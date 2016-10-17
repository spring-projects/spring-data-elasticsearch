package org.springframework.data.elasticsearch.core.aggregation.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.FacetedPageImpl;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

/**
 * @author Petar Tahchiev
 * @author Artur Konczak
 * @author Mohsin Husen
 */
public class AggregatedPageImpl<T> extends FacetedPageImpl<T> implements AggregatedPage<T> {

	private Aggregations aggregations;
	private Map<String, Aggregation> mapOfAggregations = new HashMap<String, Aggregation>();

	public AggregatedPageImpl(List<T> content) {
		super(content);
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total) {
		super(content, pageable, total);
	}

	public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations) {
		super(content, pageable, total);
		this.aggregations = aggregations;
		if (aggregations != null) {
			for (Aggregation aggregation : aggregations) {
				mapOfAggregations.put(aggregation.getName(), aggregation);
			}
		}
	}

	@Override
	public boolean hasAggregations() {
		return aggregations != null && mapOfAggregations.size() > 0;
	}

	@Override
	public Aggregations getAggregations() {
		return aggregations;
	}

	@Override
	public Aggregation getAggregation(String name) {
		return aggregations == null ? null : aggregations.get(name);
	}
}
