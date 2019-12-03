/*
 * Copyright 2019 the original author or authors.
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
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html">Elasticsearch Document
 * APIs</a>.
 *
 * @author Peter-Josef Meisch
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
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return count
	 */
	long count(Query query, @Nullable Class<?> clazz, IndexCoordinates index);

	<T> T query(Query query, ResultsExtractor<T> resultsExtractor, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the query against elasticsearch and return the first returned object.
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return the first matching object
	 */
	default <T> T queryForObject(Query query, Class<T> clazz, IndexCoordinates index) {
		List<T> content = queryForPage(query, clazz, index).getContent();
		return content.isEmpty() ? null : content.get(0);
	}

	/**
	 * Execute the query against elasticsearch and return result as {@link Page}.
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return a page with aggregations
	 */
	<T> AggregatedPage<T> queryForPage(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the multi-search against elasticsearch and return result as {@link List} of {@link Page}
	 *
	 * @param queries the queries
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return list of pages with the results
	 */
	<T> List<Page<T>> queryForPage(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the multi-search against elasticsearch and return result as {@link List} of {@link Page}
	 *
	 * @param queries the queries
	 * @param classes the entity classes used for the queries
	 * @param index the index to run the query against
	 * @return list of pages with the results
	 */
	List<Page<?>> queryForPage(List<? extends Query> queries, List<Class<?>> classes, IndexCoordinates index);

	/**
	 * Executes the given {@link Query} against elasticsearch and return result as {@link CloseableIterator}.
	 * <p>
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return a {@link CloseableIterator} that wraps an Elasticsearch scroll context that needs to be closed in case of *
	 *         error.
	 */
	<T> CloseableIterator<T> stream(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the criteria query against elasticsearch and return result as {@link List}
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @param <T> element return type
	 * @return list of found objects
	 */
	<T> List<T> queryForList(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List}
	 *
	 * @param queries the queries to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @param <T> element return type
	 * @return list of found objects
	 */
	default <T> List<List<T>> queryForList(List<Query> queries, Class<T> clazz, IndexCoordinates index) {
		return queryForPage(queries, clazz, index).stream().map(Page::getContent).collect(Collectors.toList());
	}

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List}
	 *
	 * @param queries the queries to execute
	 * @param classes the entity classes used for property mapping
	 * @param index the index to run the query against
	 * @return list of list of found objects
	 */
	default List<List<?>> queryForList(List<Query> queries, List<Class<?>> classes, IndexCoordinates index) {
		return queryForPage(queries, classes, index).stream().map(Page::getContent).collect(Collectors.toList());
	}

	/**
	 * Execute the query against elasticsearch and return ids
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return list of found object ids
	 */
	List<String> queryForIds(Query query, Class<?> clazz, IndexCoordinates index);

	/**
	 * Returns scrolled page for given query
	 *
	 * @param scrollTimeInMillis duration of the scroll time
	 * @param query The search query.
	 * @param clazz The class of entity to retrieve.
	 * @param index the index to run the query against
	 * @return scrolled page result
	 */
	<T> ScrolledPage<T> startScroll(long scrollTimeInMillis, Query query, Class<T> clazz, IndexCoordinates index);

	<T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz);

	/**
	 * Clears the search contexts associated with specified scroll ids.
	 *
	 * @param scrollId the scroll id
	 */
	void clearScroll(String scrollId);

	/**
	 * more like this query to search for documents that are "like" a specific document.
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @param <T> element return type
	 * @return page with the results
	 */
	<T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Does a suggest query
	 * 
	 * @param suggestion the query
	 * @param index the index to run the query against
	 * @return the suggest response
	 */
	SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index);
}
