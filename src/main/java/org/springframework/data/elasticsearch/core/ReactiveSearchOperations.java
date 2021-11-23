/*
 * Copyright 2019-2022 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;

/**
 * The reactive operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html">Elasticsearch Document
 * APIs</a>.
 *
 * @author Peter-Josef Meisch
 * @author Russell Parry
 * @author Thomas Geese
 * @since 4.0
 */
public interface ReactiveSearchOperations {
	/**
	 * Count the number of documents matching the given {@link Query}.
	 *
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the nr of matching documents.
	 */
	default Mono<Long> count(Class<?> entityType) {
		return count(matchAllQuery(), entityType);
	}

	/**
	 * Count the number of documents matching the given {@link Query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the nr of matching documents.
	 */
	Mono<Long> count(Query query, Class<?> entityType);

	/**
	 * Count the number of documents matching the given {@link Query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the nr of matching documents.
	 */
	Mono<Long> count(Query query, Class<?> entityType, IndexCoordinates index);

	/**
	 * Search the index for entities matching the given {@link Query query}. <br />
	 * {@link Pageable#isUnpaged() Unpaged} queries may overrule elasticsearch server defaults for page size by either
	 * delegating to the scroll API or using a max {@link org.elasticsearch.search.builder.SearchSourceBuilder#size(int)
	 * size}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param <T>
	 * @return a {@link Flux} emitting matching entities one by one wrapped in a {@link SearchHit}.
	 */
	default <T> Flux<SearchHit<T>> search(Query query, Class<T> entityType) {
		return search(query, entityType, entityType);
	}

	/**
	 * Search the index for entities matching the given {@link Query query}. <br />
	 * {@link Pageable#isUnpaged() Unpaged} queries may overrule elasticsearch server defaults for page size by either *
	 * delegating to the scroll API or using a max {@link org.elasticsearch.search.builder.SearchSourceBuilder#size(int) *
	 * size}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType The entity type for mapping the query. Must not be {@literal null}.
	 * @param returnType The mapping target type. Must not be {@literal null}. Th
	 * @param <T>
	 * @return a {@link Flux} emitting matching entities one by one wrapped in a {@link SearchHit}.
	 */
	<T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> returnType);

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return a {@link Flux} emitting matching entities one by one wrapped in a {@link SearchHit}.
	 */
	default <T> Flux<SearchHit<T>> search(Query query, Class<T> entityType, IndexCoordinates index) {
		return search(query, entityType, entityType, index);
	}

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param resultType the projection result type.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return a {@link Flux} emitting matching entities one by one wrapped in a {@link SearchHit}.
	 */
	<T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> resultType, IndexCoordinates index);

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param <T>
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting matching entities in a {@link SearchHits}.
	 * @since 4.1
	 */
	default <T> Mono<SearchPage<T>> searchForPage(Query query, Class<T> entityType) {
		return searchForPage(query, entityType, entityType);
	}

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param <T>
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param resultType the projection result type.
	 * @return a {@link Mono} emitting matching entities in a {@link SearchHits}.
	 * @since 4.1
	 */
	<T> Mono<SearchPage<T>> searchForPage(Query query, Class<?> entityType, Class<T> resultType);

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param <T>
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting matching entities in a {@link SearchHits}.
	 * @since 4.1
	 */
	default <T> Mono<SearchPage<T>> searchForPage(Query query, Class<T> entityType, IndexCoordinates index) {
		return searchForPage(query, entityType, entityType, index);
	}

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param <T>
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param resultType the projection result type.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting matching entities in a {@link SearchHits}.
	 * @since 4.1
	 */
	<T> Mono<SearchPage<T>> searchForPage(Query query, Class<?> entityType, Class<T> resultType, IndexCoordinates index);

	/**
	 * Perform a search and return the {@link ReactiveSearchHits} which contains information about the search results and
	 * which will provide the documents by the {@link ReactiveSearchHits#getSearchHits()} method.
	 *
	 * @param <T> the result type class
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the {@link ReactiveSearchHits} that contains the search result information
	 * @since 4.4
	 */
	default <T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<T> entityType) {
		return searchForHits(query, entityType, entityType);
	}

	/**
	 * Perform a search and return the {@link ReactiveSearchHits} which contains information about the search results and
	 * which will provide the documents by the {@link ReactiveSearchHits#getSearchHits()} method.
	 *
	 * @param <T> the result type class
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param resultType the projection result type.
	 * @return a {@link Mono} emitting the {@link ReactiveSearchHits} that contains the search result information
	 * @since 4.4
	 */
	<T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<?> entityType, Class<T> resultType);

	/**
	 * Perform a search and return the {@link ReactiveSearchHits} which contains information about the search results and
	 * which will provide the documents by the {@link ReactiveSearchHits#getSearchHits()} method.
	 *
	 * @param <T> the result type class
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the {@link ReactiveSearchHits} that contains the search result information
	 * @since 4.4
	 */
	default <T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<T> entityType, IndexCoordinates index) {
		return searchForHits(query, entityType, entityType, index);
	}

	/**
	 * Perform a search and return the {@link ReactiveSearchHits} which contains information about the search results and
	 * which will provide the documents by the {@link ReactiveSearchHits#getSearchHits()} method.
	 *
	 * @param <T> the result type class
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param resultType the projection result type.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the {@link ReactiveSearchHits} that contains the search result information
	 * @since 4.4
	 */
	<T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<?> entityType, Class<T> resultType,
			IndexCoordinates index);

	/**
	 * Perform an aggregation specified by the given {@link Query query}. <br />
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Flux} emitting matching aggregations one by one.
	 * @since 4.0
	 */
	Flux<AggregationContainer<?>> aggregate(Query query, Class<?> entityType);

	/**
	 * Perform an aggregation specified by the given {@link Query query}. <br />
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Flux} emitting matching aggregations one by one.
	 * @since 4.0
	 */
	Flux<AggregationContainer<?>> aggregate(Query query, Class<?> entityType, IndexCoordinates index);

	/**
	 * Does a suggest query
	 *
	 * @param suggestion the query
	 * @param entityType must not be {@literal null}.
	 * @return the suggest response (Elasticsearch library classes)
	 * @deprecated since 4.3, use {@link #suggest(Query, Class)}
	 */
	@Deprecated
	Flux<org.elasticsearch.search.suggest.Suggest> suggest(SuggestBuilder suggestion, Class<?> entityType);

	/**
	 * Does a suggest query
	 *
	 * @param suggestion the query
	 * @param index the index to run the query against
	 * @return the suggest response (Elasticsearch library classes)
	 * @deprecated since 4.3, use {@link #suggest(Query, Class, IndexCoordinates)}
	 */
	@Deprecated
	Flux<org.elasticsearch.search.suggest.Suggest> suggest(SuggestBuilder suggestion, IndexCoordinates index);

	/**
	 * Does a suggest query.
	 *
	 * @param query the Query containing the suggest definition. Must be currently a {@link NativeSearchQuery}, must not
	 *          be {@literal null}.
	 * @param entityType the type of the entities that might be returned for a completion suggestion, must not be
	 *          {@literal null}.
	 * @return suggest data
	 * @since 4.3
	 */
	Mono<Suggest> suggest(Query query, Class<?> entityType);

	/**
	 * Does a suggest query.
	 *
	 * @param query the Query containing the suggest definition. Must be currently a {@link NativeSearchQuery}, must not
	 *          be {@literal null}.
	 * @param entityType the type of the entities that might be returned for a completion suggestion, must not be
	 *          {@literal null}.
	 * @param index the index to run the query against, must not be {@literal null}.
	 * @return suggest data
	 * @since 4.3
	 */
	Mono<Suggest> suggest(Query query, Class<?> entityType, IndexCoordinates index);

	// region helper
	/**
	 * Creates a {@link Query} to find all documents. Must be implemented by the concrete implementations to provide an
	 * appropriate query using the respective client.
	 *
	 * @return a query to find all documents
	 * @since 4.3
	 */
	Query matchAllQuery();

	/**
	 * Creates a {@link Query} to find get all documents with given ids. Must be implemented by the concrete
	 * implementations to provide an appropriate query using the respective client.
	 *
	 * @param ids the list of ids must not be {@literal null}
	 * @return query returning the documents with the given ids
	 * @since 4.3
	 */
	Query idsQuery(List<String> ids);
	// endregion
}
