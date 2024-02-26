/*
 * Copyright 2023-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.elasticsearch.repository.support.QueryStringProcessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.util.Assert;

/**
 * Convert {@link org.springframework.data.elasticsearch.annotations.Highlight} to {@link Highlight}.
 *
 * @author Haibo Liu
 */
public class HighlightConverter {

	private final ElasticsearchParametersParameterAccessor parameterAccessor;
	private final ConversionService conversionService;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final QueryMethod queryMethod;

	HighlightConverter(ElasticsearchParametersParameterAccessor parameterAccessor,
					   ConversionService conversionService,
					   QueryMethodEvaluationContextProvider evaluationContextProvider,
					   QueryMethod queryMethod) {

		Assert.notNull(parameterAccessor, "parameterAccessor must not be null");
		Assert.notNull(conversionService, "conversionService must not be null");
		Assert.notNull(evaluationContextProvider, "evaluationContextProvider must not be null");
		Assert.notNull(queryMethod, "queryMethod must not be null");

		this.parameterAccessor = parameterAccessor;
		this.conversionService = conversionService;
		this.evaluationContextProvider = evaluationContextProvider;
		this.queryMethod = queryMethod;
	}

	/**
	 * Creates a {@link Highlight} from an Annotation instance.
	 *
	 * @param highlight must not be {@literal null}
	 * @return highlight definition
	 */
	Highlight convert(org.springframework.data.elasticsearch.annotations.Highlight highlight) {

		Assert.notNull(highlight, "highlight must not be null");

		org.springframework.data.elasticsearch.annotations.HighlightParameters parameters = highlight.parameters();

		// replace placeholders in highlight query with actual parameters
		Query highlightQuery = null;
		if (!parameters.highlightQuery().value().isEmpty()) {
			String rawQuery = parameters.highlightQuery().value();
			String query = new QueryStringProcessor(rawQuery, queryMethod, conversionService, evaluationContextProvider)
					.createQuery(parameterAccessor);
			highlightQuery = new StringQuery(query);
		}

		HighlightParameters highlightParameters = HighlightParameters.builder() //
				.withBoundaryChars(parameters.boundaryChars()) //
				.withBoundaryMaxScan(parameters.boundaryMaxScan()) //
				.withBoundaryScanner(parameters.boundaryScanner()) //
				.withBoundaryScannerLocale(parameters.boundaryScannerLocale()) //
				.withEncoder(parameters.encoder()) //
				.withForceSource(parameters.forceSource()) //
				.withFragmenter(parameters.fragmenter()) //
				.withFragmentSize(parameters.fragmentSize()) //
				.withNoMatchSize(parameters.noMatchSize()) //
				.withNumberOfFragments(parameters.numberOfFragments()) //
				.withHighlightQuery(highlightQuery) //
				.withOrder(parameters.order()) //
				.withPhraseLimit(parameters.phraseLimit()) //
				.withPreTags(parameters.preTags()) //
				.withPostTags(parameters.postTags()) //
				.withRequireFieldMatch(parameters.requireFieldMatch()) //
				.withTagsSchema(parameters.tagsSchema()) //
				.withType(parameters.type()) //
				.build();

		List<HighlightField> highlightFields = Arrays.stream(highlight.fields()) //
				.map(HighlightField::of) //
				.toList();

		return new Highlight(highlightParameters, highlightFields);
	}
}
