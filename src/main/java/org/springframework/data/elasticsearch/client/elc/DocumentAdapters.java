/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.explain.ExplanationDetail;
import co.elastic.clients.elasticsearch.core.get.GetResult;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.NestedIdentity;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.Explanation;
import org.springframework.data.elasticsearch.core.document.NestedMetaData;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentAdapter;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class to adapt different Elasticsearch responses to a
 * {@link org.springframework.data.elasticsearch.core.document.Document}
 *
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 4.4
 */
final class DocumentAdapters {

	private static final Log LOGGER = LogFactory.getLog(DocumentAdapters.class);

	private DocumentAdapters() {}

	/**
	 * Creates a {@link SearchDocument} from a {@link Hit} returned by the Elasticsearch client.
	 *
	 * @param hit the hit object
	 * @param jsonpMapper to map JsonData objects
	 * @return the created {@link SearchDocument}
	 */
	public static SearchDocument from(Hit<?> hit, JsonpMapper jsonpMapper) {

		Assert.notNull(hit, "hit must not be null");

		Map<String, List<String>> highlightFields = hit.highlight();

		Map<String, SearchDocumentResponse> innerHits = new LinkedHashMap<>();
		hit.innerHits().forEach((name, innerHitsResult) -> {
			// noinspection ReturnOfNull
			innerHits.put(name, SearchDocumentResponseBuilder.from(innerHitsResult.hits(), null, null, null, null, null,
					searchDocument -> null, jsonpMapper));
		});

		NestedMetaData nestedMetaData = from(hit.nested());

		Explanation explanation = from(hit.explanation());

		List<String> matchedQueries = hit.matchedQueries();

		Function<Map<String, JsonData>, EntityAsMap> fromFields = fields -> {
			StringBuilder sb = new StringBuilder("{");
			final boolean[] firstField = { true };
			hit.fields().forEach((key, jsonData) -> {
				if (!firstField[0]) {
					sb.append(',');
				}
				sb.append('"').append(key).append("\":") //
						.append(jsonData.toJson(jsonpMapper).toString());
				firstField[0] = false;
			});
			sb.append('}');
			return new EntityAsMap().fromJson(sb.toString());
		};

		EntityAsMap hitFieldsAsMap = fromFields.apply(hit.fields());

		Map<String, List<Object>> documentFields = new LinkedHashMap<>();
		hitFieldsAsMap.forEach((key, value) -> {
			if (value instanceof List) {
				// noinspection unchecked
				documentFields.put(key, (List<Object>) value);
			} else {
				documentFields.put(key, Collections.singletonList(value));
			}
		});

		Document document;
		Object source = hit.source();
		if (source == null) {
			document = Document.from(hitFieldsAsMap);
		} else {
			if (source instanceof EntityAsMap entityAsMap) {
				document = Document.from(entityAsMap);
			} else if (source instanceof JsonData jsonData) {
				document = Document.from(jsonData.to(EntityAsMap.class));
			} else {

				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn(String.format("Cannot map from type " + source.getClass().getName()));
				}
				document = Document.create();
			}
		}
		document.setIndex(hit.index());
		document.setId(hit.id());

		if (hit.version() != null) {
			document.setVersion(hit.version());
		}
		document.setSeqNo(hit.seqNo() != null && hit.seqNo() >= 0 ? hit.seqNo() : -2); // -2 was the default value in the
		// old client
		document.setPrimaryTerm(hit.primaryTerm() != null && hit.primaryTerm() > 0 ? hit.primaryTerm() : 0);

		float score = hit.score() != null ? hit.score().floatValue() : Float.NaN;
		return new SearchDocumentAdapter(document, score, hit.sort().stream().map(TypeUtils::toObject).toArray(),
				documentFields, highlightFields, innerHits, nestedMetaData, explanation, matchedQueries, hit.routing());
	}

	public static SearchDocument from(CompletionSuggestOption<EntityAsMap> completionSuggestOption) {

		Document document = completionSuggestOption.source() != null ? Document.from(completionSuggestOption.source())
				: Document.create();
		document.setIndex(completionSuggestOption.index());

		if (completionSuggestOption.id() != null) {
			document.setId(completionSuggestOption.id());
		}

		float score = completionSuggestOption.score() != null ? completionSuggestOption.score().floatValue() : Float.NaN;
		return new SearchDocumentAdapter(document, score, new Object[] {}, Collections.emptyMap(), Collections.emptyMap(),
				Collections.emptyMap(), null, null, null, completionSuggestOption.routing());
	}

	@Nullable
	private static Explanation from(@Nullable co.elastic.clients.elasticsearch.core.explain.Explanation explanation) {

		if (explanation == null) {
			return null;
		}
		List<Explanation> details = explanation.details().stream().map(DocumentAdapters::from).collect(Collectors.toList());
		return new Explanation(true, (double) explanation.value(), explanation.description(), details);
	}

	private static Explanation from(ExplanationDetail explanationDetail) {

		List<Explanation> details = explanationDetail.details().stream().map(DocumentAdapters::from)
				.collect(Collectors.toList());
		return new Explanation(null, (double) explanationDetail.value(), explanationDetail.description(), details);
	}

	@Nullable
	private static NestedMetaData from(@Nullable NestedIdentity nestedIdentity) {

		if (nestedIdentity == null) {
			return null;
		}

		NestedMetaData child = from(nestedIdentity.nested());
		return NestedMetaData.of(nestedIdentity.field(), nestedIdentity.offset(), child);
	}

	/**
	 * Creates a {@link Document} from a {@link GetResponse} where the found document is contained as {@link EntityAsMap}.
	 *
	 * @param getResponse the response instance
	 * @return the Document
	 */
	@Nullable
	public static Document from(GetResult<EntityAsMap> getResponse) {

		Assert.notNull(getResponse, "getResponse must not be null");

		if (!getResponse.found()) {
			return null;
		}

		Document document = getResponse.source() != null ? Document.from(getResponse.source()) : Document.create();
		document.setIndex(getResponse.index());
		document.setId(getResponse.id());

		if (getResponse.version() != null) {
			document.setVersion(getResponse.version());
		}

		if (getResponse.seqNo() != null) {
			document.setSeqNo(getResponse.seqNo());
		}

		if (getResponse.primaryTerm() != null) {
			document.setPrimaryTerm(getResponse.primaryTerm());
		}

		return document;
	}

	/**
	 * Creates a list of {@link MultiGetItem}s from a {@link MgetResponse} where the data is contained as
	 * {@link EntityAsMap} instances.
	 *
	 * @param mgetResponse the response instance
	 * @return list of multiget items
	 */
	public static List<MultiGetItem<Document>> from(MgetResponse<EntityAsMap> mgetResponse) {

		Assert.notNull(mgetResponse, "mgetResponse must not be null");

		return mgetResponse.docs().stream() //
				.map(itemResponse -> MultiGetItem.of( //
						itemResponse.isFailure() ? null : from(itemResponse.result()), //
						ResponseConverter.getFailure(itemResponse)))
				.collect(Collectors.toList());
	}
}
