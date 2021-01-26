/*
 * Copyright 2019-2021 the original author or authors.
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

import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.lang.Nullable;

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html">Elasticsearch Document
 * APIs</a>.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
public interface SearchOperations {
	/**
	 * Return number of elements found by given query.
	 *
	 * @param query the query to execute
	 * @param index the index to run the query against
	 * @return count
	 */
	default long count(Query query, IndexCoordinates index) {
		return count(query, null, index);
	}

	/**
	 * return number of elements found by given query
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping and index name extraction
	 * @return count
	 */
	long count(Query query, Class<?> clazz);

	/**
	 * return number of elements found by given query
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return count
	 */
	long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index);

	/**
	 * Does a suggest query
	 *
	 * @param suggestion the query
	 * @param the entity class
	 * @return the suggest response
	 * @since 4.1
	 */
	SearchResponse suggest(SuggestBuilder suggestion, Class<?> clazz);

	/**
	 * Does a suggest query
	 *
	 * @param suggestion the query
	 * @param index the index to run the query against
	 * @return the suggest response
	 */
	SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index);

	/**
	 * Execute the query against elasticsearch and return the first returned object.
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping and indexname extraction
	 * @return the first found object
	 */
	@Nullable
	default <T> SearchHit<T> searchOne(Query query, Class<T> clazz) {
		List<SearchHit<T>> content = search(query, clazz).getSearchHits();
		return content.isEmpty() ? null : content.get(0);
	}

	/**
	 * Execute the query against elasticsearch and return the first returned object.
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return the first found object
	 */
	@Nullable
	default <T> SearchHit<T> searchOne(Query query, Class<T> clazz, IndexCoordinates index) {
		List<SearchHit<T>> content = search(query, clazz, index).getSearchHits();
		return content.isEmpty() ? null : content.get(0);
	}

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List} of {@link SearchHits}.
	 *
	 * @param queries the queries to execute
	 * @param clazz the entity clazz
	 * @param <T> element return type
	 * @return list of SearchHits
	 * @since 4.1
	 */
	<T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz);

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List} of {@link SearchHits}.
	 *
	 * @param queries the queries to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @param <T> element return type
	 * @return list of SearchHits
	 */
	<T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List} of {@link SearchHits}.
	 *
	 * @param queries the queries to execute
	 * @param classes the entity classes
	 * @return list of SearchHits
	 * @since 4.1
	 */
	List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes);

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List} of {@link SearchHits}.
	 *
	 * @param queries the queries to execute
	 * @param classes the entity classes used for property mapping
	 * @param index the index to run the query against
	 * @return list of SearchHits
	 */
	List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes, IndexCoordinates index);

	/**
	 * Execute the criteria query against elasticsearch and return result as {@link SearchHits}
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping and index name extraction
	 * @return SearchHits containing the list of found objects
	 */
	<T> SearchHits<T> search(Query query, Class<T> clazz);

	/**
	 * Execute the criteria query against elasticsearch and return result as {@link SearchHits}
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return SearchHits containing the list of found objects
	 */
	<T> SearchHits<T> search(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * more like this query to search for documents that are "like" a specific document.
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping and index name extraction
	 * @return SearchHits containing the list of found objects
	 */
	<T> SearchHits<T> search(MoreLikeThisQuery query, Class<T> clazz);

	/**
	 * more like this query to search for documents that are "like" a specific document.
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return SearchHits containing the list of found objects
	 */
	<T> SearchHits<T> search(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Executes the given {@link Query} against elasticsearch and return result as {@link SearchHitsIterator}.
	 * <p>
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping and index name extraction
	 * @return a {@link SearchHitsIterator} that wraps an Elasticsearch scroll context that needs to be closed. The
	 *         try-with-resources construct should be used to ensure that the close method is invoked after the operations
	 *         are completed.
	 */
	<T> SearchHitsIterator<T> searchForStream(Query query, Class<T> clazz);

	/**
	 * Executes the given {@link Query} against elasticsearch and return result as {@link SearchHitsIterator}.
	 * <p>
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return a {@link SearchHitsIterator} that wraps an Elasticsearch scroll context that needs to be closed. The
	 *         try-with-resources construct should be used to ensure that the close method is invoked after the operations
	 *         are completed.
	 */
	<T> SearchHitsIterator<T> searchForStream(Query query, Class<T> clazz, IndexCoordinates index);
}
