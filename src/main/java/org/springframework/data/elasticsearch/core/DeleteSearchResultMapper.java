package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

/**
 * DeleteSearchResultMapper
 *
 * @author withccm
 */
public class DeleteSearchResultMapper implements SearchResultMapper {
	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		List<String> result = new ArrayList<String>();
		for (SearchHit searchHit : response.getHits()) {
			String id = searchHit.getId();
			result.add(id);
		}
		if (result.size() > 0) {
			return new AggregatedPageImpl<T>((List<T>) result);
		}
		return null;
	}
}
