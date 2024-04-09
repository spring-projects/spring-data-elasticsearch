/*
 * Copyright 2020-2024 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.NestedMetaData;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
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
 * @author Matt Gilene
 * @author Sascha Woo
 * @author Jakob Hoeper
 * @author Haibo Liu
 * @since 4.0
 */
public class SearchHitMapping<T> {

	private final Class<T> type;
	private final ElasticsearchConverter converter;
	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	private SearchHitMapping(Class<T> type, ElasticsearchConverter converter) {
		Assert.notNull(type, "type is null");
		Assert.notNull(converter, "converter is null");

		this.type = type;
		this.converter = converter;
		this.mappingContext = converter.getMappingContext();
	}

	public static <T> SearchHitMapping<T> mappingFor(Class<T> entityClass, ElasticsearchConverter converter) {
		return new SearchHitMapping<>(entityClass, converter);
	}

	public SearchHits<T> mapHits(SearchDocumentResponse searchDocumentResponse, List<T> contents) {
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
		SearchShardStatistics shardStatistics = searchDocumentResponse.getSearchShardStatistics();
		float maxScore = searchDocumentResponse.getMaxScore();
		String scrollId = searchDocumentResponse.getScrollId();
		String pointInTimeId = searchDocumentResponse.getPointInTimeId();

		List<SearchHit<T>> searchHits = new ArrayList<>();
		List<SearchDocument> searchDocuments = searchDocumentResponse.getSearchDocuments();
		for (int i = 0; i < searchDocuments.size(); i++) {
			SearchDocument document = searchDocuments.get(i);
			T content = contents.get(i);
			SearchHit<T> hit = mapHit(document, content);
			searchHits.add(hit);
		}
		AggregationsContainer<?> aggregations = searchDocumentResponse.getAggregations();
		TotalHitsRelation totalHitsRelation = TotalHitsRelation.valueOf(searchDocumentResponse.getTotalHitsRelation());

		Suggest suggest = searchDocumentResponse.getSuggest();
		mapHitsInCompletionSuggestion(suggest);

		return new SearchHitsImpl<>(totalHits, totalHitsRelation, maxScore, scrollId, pointInTimeId, searchHits,
				aggregations, suggest, shardStatistics);
	}

	@SuppressWarnings("unchecked")
	public void mapHitsInCompletionSuggestion(@Nullable Suggest suggest) {
		if (suggest != null) {
			for (Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestion : suggest
					.getSuggestions()) {
				if (suggestion instanceof CompletionSuggestion) {
					CompletionSuggestion<T> completionSuggestion = (CompletionSuggestion<T>) suggestion;
					for (CompletionSuggestion.Entry<T> entry : completionSuggestion.getEntries()) {
						for (CompletionSuggestion.Entry.Option<T> option : entry.getOptions()) {
							option.updateSearchHit(this::mapHit);
						}
					}
				}
			}
		}
	}

	public SearchHit<T> mapHit(SearchDocument searchDocument, T content) {

		Assert.notNull(searchDocument, "searchDocument is null");
		Assert.notNull(content, "content is null");

		return new SearchHit<>(searchDocument.getIndex(), //
				searchDocument.hasId() ? searchDocument.getId() : null, //
				searchDocument.getRouting(), //
				searchDocument.getScore(), //
				searchDocument.getSortValues(), //
				getHighlightsAndRemapFieldNames(searchDocument), //
				mapInnerHits(searchDocument), //
				searchDocument.getNestedMetaData(), //
				searchDocument.getExplanation(), //
				searchDocument.getMatchedQueries(), //
				content); //
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

	private Map<String, SearchHits<?>> mapInnerHits(SearchDocument searchDocument) {

		Map<String, SearchHits<?>> innerHits = new LinkedHashMap<>();
		Map<String, SearchDocumentResponse> documentInnerHits = searchDocument.getInnerHits();

		if (documentInnerHits != null && !documentInnerHits.isEmpty()) {

			SearchHitMapping<SearchDocument> searchDocumentSearchHitMapping = SearchHitMapping
					.mappingFor(SearchDocument.class, converter);

			for (Map.Entry<String, SearchDocumentResponse> entry : documentInnerHits.entrySet()) {
				SearchDocumentResponse searchDocumentResponse = entry.getValue();

				SearchHits<SearchDocument> searchHits = searchDocumentSearchHitMapping
						.mapHitsFromResponse(searchDocumentResponse, searchDocumentResponse.getSearchDocuments());

				// map Documents to real objects
				SearchHits<?> mappedSearchHits = mapInnerDocuments(searchHits, type);

				innerHits.put(entry.getKey(), mappedSearchHits);
			}

		}
		return innerHits;
	}

	/**
	 * try to convert the SearchDocument instances to instances of the inner property class.
	 *
	 * @param searchHits {@link SearchHits} containing {@link Document} instances
	 * @param type the class of the containing class
	 * @return a new {@link SearchHits} instance containing the mapped objects or the original inout if any error occurs
	 */
	private SearchHits<?> mapInnerDocuments(SearchHits<SearchDocument> searchHits, Class<T> type) {

		if (searchHits.isEmpty()) {
			return searchHits;
		}

		try {
			ElasticsearchPersistentEntity<?> persistentEntityForType = mappingContext.getPersistentEntity(type);
			NestedMetaData nestedMetaData = searchHits.getSearchHit(0).getContent().getNestedMetaData();
			ElasticsearchPersistentEntityWithNestedMetaData persistentEntityWithNestedMetaData = getPersistentEntity(
					persistentEntityForType, nestedMetaData);

			if (persistentEntityWithNestedMetaData.entity != null) {
				List<SearchHit<Object>> convertedSearchHits = new ArrayList<>();
				Class<?> targetType = persistentEntityWithNestedMetaData.entity.getType();

				// convert the list of SearchHit<SearchDocument> to list of SearchHit<Object>
				searchHits.getSearchHits().forEach(searchHit -> {
					SearchDocument searchDocument = searchHit.getContent();

					Object targetObject = converter.read(targetType, searchDocument);
					convertedSearchHits.add(new SearchHit<>(searchDocument.getIndex(), //
							searchDocument.getId(), //
							searchDocument.getRouting(), //
							searchDocument.getScore(), //
							searchDocument.getSortValues(), //
							searchDocument.getHighlightFields(), //
							searchHit.getInnerHits(), //
							getPersistentEntity(persistentEntityForType, //
									searchHit.getContent().getNestedMetaData()).nestedMetaData, //
							searchHit.getExplanation(), //
							searchHit.getMatchedQueries(), //
							targetObject));
				});

				String scrollId = null;
				if (searchHits instanceof SearchHitsImpl<?> searchHitsImpl) {
					scrollId = searchHitsImpl.getScrollId();
				}

				return new SearchHitsImpl<>(searchHits.getTotalHits(),
						searchHits.getTotalHitsRelation(),
						searchHits.getMaxScore(),
						scrollId,
						searchHits.getPointInTimeId(),
						convertedSearchHits,
						searchHits.getAggregations(),
						searchHits.getSuggest(),
						searchHits.getSearchShardStatistics());
			}
		} catch (Exception e) {
			throw new UncategorizedElasticsearchException("Unable to convert inner hits.", e);
		}

		return searchHits;
	}

	/**
	 * find a {@link ElasticsearchPersistentEntity} following the property chain defined by the nested metadata
	 *
	 * @param persistentEntity base entity
	 * @param nestedMetaData nested metadata
	 * @return A {@link ElasticsearchPersistentEntityWithNestedMetaData} containing the found entity or null together with
	 *         the {@link NestedMetaData} that has mapped field names.
	 */
	private ElasticsearchPersistentEntityWithNestedMetaData getPersistentEntity(
			@Nullable ElasticsearchPersistentEntity<?> persistentEntity, @Nullable NestedMetaData nestedMetaData) {

		NestedMetaData currentMetaData = nestedMetaData;
		List<NestedMetaData> mappedNestedMetaDatas = new LinkedList<>();

		while (persistentEntity != null && currentMetaData != null) {
			ElasticsearchPersistentProperty persistentProperty = persistentEntity
					.getPersistentPropertyWithFieldName(currentMetaData.getField());

			if (persistentProperty == null) {
				persistentEntity = null;
			} else {
				persistentEntity = mappingContext.getPersistentEntity(persistentProperty.getActualType());
				mappedNestedMetaDatas.add(0,
						NestedMetaData.of(persistentProperty.getName(), currentMetaData.getOffset(), null));
				currentMetaData = currentMetaData.getChild();
			}
		}

		NestedMetaData mappedNestedMetaData = mappedNestedMetaDatas.stream().reduce(null,
				(result, nmd) -> NestedMetaData.of(nmd.getField(), nmd.getOffset(), result));

		return new ElasticsearchPersistentEntityWithNestedMetaData(persistentEntity, mappedNestedMetaData);
	}

	private static class ElasticsearchPersistentEntityWithNestedMetaData {
		@Nullable private final ElasticsearchPersistentEntity<?> entity;
		private final NestedMetaData nestedMetaData;

		public ElasticsearchPersistentEntityWithNestedMetaData(@Nullable ElasticsearchPersistentEntity<?> entity,
				NestedMetaData nestedMetaData) {
			this.entity = entity;
			this.nestedMetaData = nestedMetaData;
		}
	}
}
