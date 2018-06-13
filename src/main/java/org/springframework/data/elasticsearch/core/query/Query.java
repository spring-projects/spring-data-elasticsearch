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
package org.springframework.data.elasticsearch.core.query;

import java.util.Collection;
import java.util.List;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.IndicesOptions;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Query
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Alen Turkovic
 * @author Sascha Woo
 */
public interface Query {

	int DEFAULT_PAGE_SIZE = 10;
	Pageable DEFAULT_PAGE = PageRequest.of(0, DEFAULT_PAGE_SIZE);

	/**
	 * restrict result to entries on given page. Corresponds to the 'start' and 'rows' parameter in elasticsearch
	 *
	 * @param pageable
	 * @return
	 */
	<T extends Query> T setPageable(Pageable pageable);

	/**
	 * Get page settings if defined
	 *
	 * @return
	 */
	Pageable getPageable();

	/**
	 * Add {@link org.springframework.data.domain.Sort} to query
	 *
	 * @param sort
	 * @return
	 */
	<T extends Query> T addSort(Sort sort);

	/**
	 * @return null if not set
	 */
	Sort getSort();

	/**
	 * Get Indices to be searched
	 *
	 * @return
	 */
	List<String> getIndices();

	/**
	 * Add Indices to be added as part of search request
	 *
	 * @param indices
	 */
	void addIndices(String... indices);

	/**
	 * Add types to be searched
	 *
	 * @param types
	 */
	void addTypes(String... types);

	/**
	 * Get types to be searched
	 *
	 * @return
	 */
	List<String> getTypes();

	/**
	 * Add fields to be added as part of search request
	 *
	 * @param fields
	 */
	void addFields(String... fields);

	/**
	 * Get fields to be returned as part of search request
	 *
	 * @return
	 */
	List<String> getFields();

	/**
	 * Add source filter to be added as part of search request
	 *
	 * @param sourceFilter
	 */
	void addSourceFilter(SourceFilter sourceFilter);

	/**
	 * Get SourceFilter to be returned to get include and exclude source fields as part of search request.
	 *
	 * @return SourceFilter
	 */
	SourceFilter getSourceFilter();

	/**
	 * Get minimum score
	 *
	 * @return
	 */
	float getMinScore();

	/**
	 * Get if scores will be computed and tracked, regardless of whether sorting on a field. Defaults to <tt>false</tt>.
	 * 
	 * @return
	 * @since 3.1
	 */
	boolean getTrackScores();

	/**
	 * Get Ids
	 *
	 * @return
	 */
	Collection<String> getIds();

	/**
	 * Get route
	 *
	 * @return
	 */
	String getRoute();

	/**
	 * Type of search
	 *
	 * @return
	 */
	SearchType getSearchType();

	/**
	 * Get indices options
	 *
	 * @return null if not set
	 */
	IndicesOptions getIndicesOptions();
}
