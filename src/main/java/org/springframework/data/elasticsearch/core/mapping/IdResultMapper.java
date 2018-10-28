/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.util.*;

import org.elasticsearch.action.search.*;
import org.elasticsearch.search.*;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.aggregation.*;
import org.springframework.data.elasticsearch.core.aggregation.impl.*;

/**
 * @author Nikita Guchakov
 */
public class IdResultMapper implements SearchResultMapper {

	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		List<String> result = Arrays.stream(response.getHits().getHits()).map(SearchHit::getId).collect(toList());
		result = result.isEmpty() ? emptyList() : result;
		return new AggregatedPageImpl<>((List<T>) result, response.getScrollId());
	}
}
