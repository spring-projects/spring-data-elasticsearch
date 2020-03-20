/*
 * Copyright 2019-2020 the original author or authors.
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
import java.util.Iterator;
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

	// region deprecated
	/**
	 * Execute the query against elasticsearch and return the first returned object.
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return the first matching object
	 * @deprecated since 4.0, use {@link #searchOne(Query, Class, IndexCoordinates)}.
	 */
	@Deprecated
	default <T> T queryForObject(Query query, Class<T> clazz, IndexCoordinates index) {
		return (T) SearchHitSupport.unwrapSearchHits(searchOne(query, clazz, index));
	}

	/**
	 * Execute the query against elasticsearch and return result as {@link Page}.
	 *
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return a page with aggregations
	 * @deprecated since 4.0, use {@link #search(Query, Class, IndexCoordinates)}.
	 */
	@Deprecated
	default <T> AggregatedPage<T> queryForPage(Query query, Class<T> clazz, IndexCoordinates index) {
		SearchHits<T> searchHits = search(query, clazz, index);
		AggregatedPage<SearchHit<T>> aggregatedPage = SearchHitSupport.page(searchHits, query.getPageable());
		return (AggregatedPage<T>) SearchHitSupport.unwrapSearchHits(aggregatedPage);
	}

	/**
	 * Execute the multi-search against elasticsearch and return result as {@link List} of {@link Page}
	 *
	 * @param queries the queries
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return list of pages with the results
	 * @deprecated since 4.0, use {@link #multiSearch(List, Class, IndexCoordinates)}.
	 */
	@Deprecated
	default <T> List<Page<T>> queryForPage(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index) {
		List<Page<T>> pageList = new ArrayList<>();
		List<SearchHits<T>> searchHitsList = multiSearch(queries, clazz, index);
		Iterator<? extends Query> qit = queries.iterator();
		searchHitsList.forEach(searchHits -> {
			AggregatedPage<SearchHit<T>> aggregatedPage = SearchHitSupport.page(searchHits, qit.next().getPageable());
			Page<T> page = (Page<T>) SearchHitSupport.unwrapSearchHits(aggregatedPage);
			pageList.add(page);
		});
		return pageList;
	}

	/**
	 * Execute the multi-search against elasticsearch and return result as {@link List} of {@link Page}
	 *
	 * @param queries the queries
	 * @param classes the entity classes used for the queries
	 * @param index the index to run the query against
	 * @return list of pages with the results
	 * @deprecated since 4.0, use {@link #multiSearch(List, List, IndexCoordinates)}.
	 */
	@Deprecated
	default List<AggregatedPage<?>> queryForPage(List<? extends Query> queries, List<Class<?>> classes,
			IndexCoordinates index) {
		List<AggregatedPage<?>> pageList = new ArrayList<>();
		List<SearchHits<?>> searchHitsList = multiSearch(queries, classes, index);
		Iterator<? extends Query> qit = queries.iterator();
		searchHitsList.forEach(searchHits -> {
			AggregatedPage<? extends SearchHit<?>> aggregatedPage = SearchHitSupport.page(searchHits,
					qit.next().getPageable());
			AggregatedPage<?> page = (AggregatedPage<?>) SearchHitSupport.unwrapSearchHits(aggregatedPage);
			pageList.add(page);
		});

		return pageList;
	}

	/**
	 * Executes the given {@link Query} against elasticsearch and return result as {@link CloseableIterator}.
	 * <p>
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return a {@link CloseableIterator} that wraps an Elasticsearch scroll context that needs to be closed. The
	 *         try-with-resources construct should be used to ensure that the close method is invoked after the operations
	 *         are completed.
	 * @deprecated since 4.0, use {@link #searchForStream(Query, Class, IndexCoordinates)}.
	 */
	@Deprecated
	<T> CloseableIterator<T> stream(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the criteria query against elasticsearch and return result as {@link List}
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return list of found objects
	 * @deprecated since 4.0, use {@link #search(Query, Class, IndexCoordinates)}.
	 */
	@Deprecated
	default <T> List<T> queryForList(Query query, Class<T> clazz, IndexCoordinates index) {
		return (List<T>) SearchHitSupport.unwrapSearchHits(search(query, clazz, index));
	}

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List}
	 *
	 * @param queries the queries to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @param <T> element return type
	 * @return list of found objects
	 * @deprecated since 4.0, use {@link #multiSearch(List, Class, IndexCoordinates)}.
	 */
	@Deprecated
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
	 * @deprecated since 4.0, use {@link #multiSearch(List, List, IndexCoordinates)}.
	 */
	@Deprecated
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
	 * @deprecated since 4.0 use {@link #search(Query, Class, IndexCoordinates)} and map the results.
	 */
	@Deprecated
	default List<String> queryForIds(Query query, Class<?> clazz, IndexCoordinates index) {
		return search(query, clazz, index).map(SearchHit::getId).toList();
	}

	/**
	 * more like this query to search for documents that are "like" a specific document.
	 *
	 * @param <T> element return type
	 * @param query the query to execute
	 * @param clazz the entity clazz used for property mapping
	 * @param index the index to run the query against
	 * @return page with the results
	 * @deprecated since 4.0, use {@link #search(MoreLikeThisQuery, Class, IndexCoordinates)}.
	 */
	@Deprecated
	default <T> AggregatedPage<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index) {
		SearchHits<T> searchHits = search(query, clazz, index);
		AggregatedPage<SearchHit<T>> aggregatedPage = SearchHitSupport.page(searchHits, query.getPageable());
		return (AggregatedPage<T>) SearchHitSupport.unwrapSearchHits(aggregatedPage);
	}

	// endregion

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
