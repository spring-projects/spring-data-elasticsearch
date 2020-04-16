/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 * @author Roman Puchkovskiy
 * @since 4.0
 */
class SearchHitMapping<T> {
	private final Class<T> type;
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	private SearchHitMapping(Class<T> type,
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context) {

		Assert.notNull(type, "type is null");
		Assert.notNull(context, "context is null");

		this.type = type;
		this.mappingContext = context;
	}

	static <T> SearchHitMapping<T> mappingFor(Class<T> entityClass,
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context) {
		return new SearchHitMapping<>(entityClass, context);
	}

	SearchHit<T> mapHit(SearchDocument searchDocument, T content) {

		Assert.notNull(searchDocument, "searchDocument is null");
		Assert.notNull(content, "content is null");

		String id = searchDocument.hasId() ? searchDocument.getId() : null;
		float score = searchDocument.getScore();
		Object[] sortValues = searchDocument.getSortValues();
		Map<String, List<String>> highlightFields = getHighlightsAndRemapFieldNames(searchDocument);

		return new SearchHit<>(id, score, sortValues, highlightFields, content);
	}

	SearchHits<T> mapHits(SearchDocumentResponse searchDocumentResponse, List<T> contents) {
		return mapHitsFromResponse(searchDocumentResponse, contents);
	}

	SearchScrollHits<T> mapScrollHits(SearchDocumentResponse searchDocumentResponse, List<T> contents) {
		return mapHitsFromResponse(searchDocumentResponse, contents);
	}

	private SearchHitsImpl<T> mapHitsFromResponse(SearchDocumentResponse searchDocumentResponse, List<T> contents) {

		Assert.notNull(searchDocumentResponse, "searchDocumentResponse is null");
		Assert.notNull(contents, "contents is null");

		Assert.isTrue(searchDocumentResponse.getSearchDocuments().size() == contents.size(),
				"Count of documents must match the count of entities");

		long totalHits = searchDocumentResponse.getTotalHits();
		float maxScore = searchDocumentResponse.getMaxScore();
		String scrollId = searchDocumentResponse.getScrollId();

		List<SearchHit<T>> searchHits = new ArrayList<>();
		List<SearchDocument> searchDocuments = searchDocumentResponse.getSearchDocuments();
		for (int i = 0; i < searchDocuments.size(); i++) {
			SearchDocument document = searchDocuments.get(i);
			T content = contents.get(i);
			SearchHit<T> hit = mapHit(document, content);
			searchHits.add(hit);
		}
		Aggregations aggregations = searchDocumentResponse.getAggregations();
		TotalHitsRelation totalHitsRelation = TotalHitsRelation.valueOf(searchDocumentResponse.getTotalHitsRelation());

		return new SearchHitsImpl<>(totalHits, totalHitsRelation, maxScore, scrollId, searchHits, aggregations);
	}

	@Nullable
	private Map<String, List<String>> getHighlightsAndRemapFieldNames(SearchDocument searchDocument) {
		Map<String, List<String>> highlightFields = searchDocument.getHighlightFields();

		if (highlightFields == null) {
			return null;
		}

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(type);
		if (persistentEntity == null) {
			return highlightFields;
		}

		return highlightFields.entrySet().stream().collect(Collectors.toMap(entry -> {
			ElasticsearchPersistentProperty property = persistentEntity.getPersistentPropertyWithFieldName(entry.getKey());
			return property != null ? property.getName() : entry.getKey();
		}, Map.Entry::getValue));
	}
}
