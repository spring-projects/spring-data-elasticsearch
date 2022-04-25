/*
 * Copyright 2013-2021 the original author or authors.
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

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
	@Nullable
	private JsonNode valueParams;
	private String includes;
	private String excludes;
	private Boolean hasSourceFilter = false;

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String query) {
		super(queryMethod, elasticsearchOperations);
		Assert.notNull(query, "Query cannot be empty");
		this.query = query;
		this.includes = "";
		this.excludes = "";
		if (queryMethod.hasIncludes() || queryMethod.hasExcludes()) {
			this.includes = queryMethod.getIncludes();
			this.excludes = queryMethod.getExcludes();
			this.hasSourceFilter = true;
		}

		if (queryMethod.hasValueParams()) {
			try {
				this.valueParams = queryMethod.getValueParams();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}

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

		if (hasSourceFilter) {
			stringQuery.addSourceFilter(createSourceFilter(accessor));
		}

		if (valueParams != null) {
			try {
				addValueParams(stringQuery, accessor);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
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

	/**
	 * Parse the value parameters for elasticsearch and add them to the string query.
	 * @param stringQuery
	 * @throws JsonProcessingException if the json is not formatted properly
	 * @since 4.4
	 */
	private void addValueParams(StringQuery stringQuery, ParameterAccessor parameterAccessor) throws JsonProcessingException {
		StringQueryUtil stringQueryUtil = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService());
		ObjectMapper om = new ObjectMapper();
		if (valueParams == null) {
			return;
		}
		if (valueParams.has("_source")) {
			String _source = valueParams.get("_source").toString();
			String input = stringQueryUtil.replacePlaceholders(_source, parameterAccessor);
			FetchSourceFilterBuilder sourceFilterConfig = om.readValue(input, FetchSourceFilterBuilder.class);
			stringQuery.addSourceFilter(sourceFilterConfig.build());
		}
		final String AGGREGATION_KEYWORD = "aggs";
		if (valueParams.has(AGGREGATION_KEYWORD)) {
			String input = valueParams.get(AGGREGATION_KEYWORD).toString();
			String aggs = stringQueryUtil.replacePlaceholders(input, parameterAccessor);
			JsonNode customAggregationConfigs = om.readTree(aggs);
			Assert.isTrue(customAggregationConfigs.isObject(), "`aggs` should be a JSON object");

			List<TermsAggregationBuilder> aggregations = new ArrayList<>();
			Iterator<Map.Entry<String, JsonNode>> aggregationConfigs = customAggregationConfigs.fields();
			aggregationConfigs.forEachRemaining(it -> {
				String aggregationName = it.getKey();
				JsonNode aggregationConfig = it.getValue();
				try {
					aggregations.add(processAggregationConfig(aggregationName, aggregationConfig));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			});
			if (!aggregations.isEmpty()) {
				SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
				for(TermsAggregationBuilder aggregation: aggregations) {
					searchSourceBuilder.aggregation(aggregation);
				}
				stringQuery.setSearchSourceBuilder(searchSourceBuilder);
			}
		}
	}

	/**
	 * Process a configuration for an aggregation.
	 *
	 * @param aggName name of the aggregation
	 * @param aggConfig configuration of the aggregation
	 * @return the term aggregation builder
	 * @since 4.4
	 * @throws JsonProcessingException
	 */
	private TermsAggregationBuilder processAggregationConfig(String aggName, JsonNode aggConfig) throws JsonProcessingException {
		TermsAggregationBuilder aggregation = AggregationBuilders.terms(aggName);
		// parse the terms
		if (aggConfig.has("terms")) {
			JsonNode terms = aggConfig.get("terms");
			Assert.isTrue(terms.isObject(), "`terms` must be a JSON object");
			if (terms.has("field")) {
				String field = terms.get("field").textValue();
				aggregation.field(field);
			}
		}

		if (aggConfig.has("meta")) {
			Map meta = new ObjectMapper().readValue(aggConfig.get("meta").toString(), Map.class);
			aggregation.setMetadata(meta);
		}

		// Parse the sub-aggregations
		if (aggConfig.has("aggs")) {
			processAggregationConfig(aggConfig, aggregation);
		}

		return aggregation;
	}

	/**
	 * parse the sub aggregation configurations and add them to the parent
	 * @param aggConfig
	 * @param aggregation
	 * @since 4.4
	 */
	private void processAggregationConfig(JsonNode aggConfig, TermsAggregationBuilder aggregation) {
		Iterator<Map.Entry<String, JsonNode>> subAggregationConfigs = aggConfig.get("aggs").fields();
		subAggregationConfigs.forEachRemaining(it -> {
			try {
				aggregation.subAggregation(processAggregationConfig(it.getKey(), it.getValue()));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		});
	}

	protected StringQuery createQuery(ParametersParameterAccessor parameterAccessor) {
		String queryString = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService())
				.replacePlaceholders(this.query, parameterAccessor);
		return new StringQuery(queryString);
	}

	protected SourceFilter createSourceFilter(ParametersParameterAccessor parameterAccessor) {
		StringQueryUtil stringQueryUtil = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService());

		String[] includeList = new String[0];
		String[] excludeList = new String[0];

		final String sourceFilterDelim = ",";
		if (includes.length() != 0) {
			String includeFilter = stringQueryUtil.replacePlaceholders(includes, parameterAccessor);
			includeList = includeFilter.split(sourceFilterDelim);
		}
		if (excludes.length() != 0) {
			String excludeFilter = stringQueryUtil.replacePlaceholders(excludes, parameterAccessor);
			excludeList = excludeFilter.split(sourceFilterDelim);
		}

		return new FetchSourceFilterBuilder().withExcludes(excludeList).withIncludes(includeList).build();
	}

}
