/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.SourceFilters;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repository.support.StringQueryUtil;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * ElasticsearchStringQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Taylor Ono
 * @author Peter-Josef Meisch
 * @author Alexander Torres
 */
public class ElasticsearchStringQuery extends AbstractElasticsearchRepositoryQuery {

	private String query;

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String query) {
		super(queryMethod, elasticsearchOperations);
		Assert.notNull(query, "Query cannot be empty");
		this.query = query;
	}

	@Override
	public boolean isCountQuery() {
		return queryMethod.hasCountQueryAnnotation();
	}

	@Override
	public Object execute(Object[] parameters) {

		Class<?> clazz = queryMethod.getResultProcessor().getReturnedType().getDomainType();
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);

		StringQuery stringQuery = createQuery(accessor);

		Assert.notNull(stringQuery, "unsupported query");

		if (queryMethod.hasAnnotatedHighlight()) {
			stringQuery.setHighlightQuery(queryMethod.getAnnotatedHighlightQuery());
		}

		if (queryMethod.hasSourceFilters()) {
			SourceFilter sourceFilter = processSourceFilterParams(queryMethod.getSourceFilters(), accessor);
			stringQuery.addSourceFilter(sourceFilter);
		}

		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		Object result = null;

		if (isCountQuery()) {
			result = elasticsearchOperations.count(stringQuery, clazz, index);
		} else if (queryMethod.isPageQuery()) {
			stringQuery.setPageable(accessor.getPageable());
			SearchHits<?> searchHits = elasticsearchOperations.search(stringQuery, clazz, index);
			if (queryMethod.isSearchPageMethod()) {
				result = SearchHitSupport.searchPageFor(searchHits, stringQuery.getPageable());
			} else {
				result = SearchHitSupport
						.unwrapSearchHits(SearchHitSupport.searchPageFor(searchHits, stringQuery.getPageable()));
			}
		} else if (queryMethod.isStreamQuery()) {
			stringQuery.setPageable(
					accessor.getPageable().isPaged() ? accessor.getPageable() : PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			result = StreamUtils.createStreamFromIterator(elasticsearchOperations.searchForStream(stringQuery, clazz, index));
		} else if (queryMethod.isCollectionQuery()) {
			stringQuery.setPageable(accessor.getPageable().isPaged() ? accessor.getPageable() : Pageable.unpaged());
			result = elasticsearchOperations.search(stringQuery, clazz, index);
		} else {
			result = elasticsearchOperations.searchOne(stringQuery, clazz, index);
		}

		return (queryMethod.isNotSearchHitMethod() && queryMethod.isNotSearchPageMethod())
				? SearchHitSupport.unwrapSearchHits(result)
				: result;
	}

	protected StringQuery createQuery(ParametersParameterAccessor parameterAccessor) {
		String queryString = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService())
				.replacePlaceholders(this.query, parameterAccessor);
		return new StringQuery(queryString);
	}

	/**
	 * Parse the {@link SourceFilters} attributes to construct a SourceFilter {@link SourceFilter}
	 * @param parameterAccessor the accessor with the query method parameter details
	 * @throws JsonProcessingException if the json is not formatted properly
	 * @return source filter with includes and excludes for a  query
	 * @since 4.4
	 */
	private SourceFilter processSourceFilterParams(SourceFilters sourceFilters, ParameterAccessor parameterAccessor) {
		StringQueryUtil stringQueryUtil = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService());
		ObjectMapper objectMapper = new ObjectMapper();
		FetchSourceFilterBuilder fetchSourceFilterBuilder = new FetchSourceFilterBuilder();
		String errorMessage = null;
		String includesInput = stringQueryUtil.replacePlaceholders(sourceFilters.includes(), parameterAccessor);
		String excludesInput = stringQueryUtil.replacePlaceholders(sourceFilters.excludes(), parameterAccessor);
		try {
			if (!includesInput.equals("")) {
				String[] includes = objectMapper.readValue(includesInput, String[].class);
				fetchSourceFilterBuilder.withIncludes(includes);
			}
			if (!excludesInput.equals("")) {
				String[] excludes = objectMapper.readValue(excludesInput, String[].class);
				fetchSourceFilterBuilder.withExcludes(excludes);
			}
		} catch (JsonProcessingException e) {
			errorMessage = e.getMessage();
		}

		SourceFilter sourceFilter = fetchSourceFilterBuilder.build();
		Assert.isTrue(sourceFilter.getIncludes().length > 0 || sourceFilter.getExcludes().length > 0,
				"At least one includes or excludes should be provided.\n Found error: " + errorMessage);
		return sourceFilter;
	}
}
