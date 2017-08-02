/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;

import java.util.List;
import java.util.Map;

/**
 * @author zzt
 */
public class HighlightMapper extends DefaultResultMapper {

	private final Highlight highlight;

	public HighlightMapper(Highlight highlight) {
		this.highlight = highlight;
	}

	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		AggregatedPage<T> res = super.mapResults(response, clazz, pageable);
		List<T> chunk = res.getContent();
		SearchHits hits = response.getHits();
		for (int i = 0; i < hits.getTotalHits(); i++) {
			SearchHit at = hits.getAt(i);
			T t = chunk.get(i);
			Map<String, HighlightField> highlightFields = at.getHighlightFields();
			for (org.springframework.data.elasticsearch.annotations.HighlightField field : highlight.fields()) {
				if (!highlightFields.containsKey(field.name())) {
					continue;
				}
				Text[] fragments = highlightFields.get(field.name()).fragments();
				if (fragments != null) {
					PropertyAccessor myAccessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
					String delimitedString = Strings.arrayToDelimitedString(fragments, field.fragmentSeparator());
					myAccessor.setPropertyValue(field.name(), delimitedString);
				}
			}
		}
		return res;
	}
}
