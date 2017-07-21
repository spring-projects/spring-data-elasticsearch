package org.springframework.data.elasticsearch.core;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

/**
 * @author zzt
 */
public class HighlightMapper extends DefaultResultMapper {

	public HighlightMapper() {}

	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		AggregatedPage<T> res = super.mapResults(response, clazz, pageable);
		List<T> chunk = res.getContent();
		SearchHits hits = response.getHits();
		for (int i = 0; i < hits.getTotalHits(); i++) {
			SearchHit at = hits.getAt(i);
			T t = chunk.get(i);
			Map<String, HighlightField> highlightFields = at.getHighlightFields();
		}
		return res;
	}
}
