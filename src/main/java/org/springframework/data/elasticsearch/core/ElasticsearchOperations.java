/*
 * Copyright 2013-2019 the original author or authors.
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
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;

/**
 * ElasticsearchOperations
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 * @author Zetang Zeng
 * @author Dmitriy Yakovlev
 * @author Peter-Josef Meisch
 */
public interface ElasticsearchOperations {

	/**
	 * adding new alias
	 *
	 * @param query
	 * @param index
	 * @return
	 */
	boolean addAlias(AliasQuery query, IndexCoordinates index);

	/**
	 * removing previously created alias
	 *
	 * @param query
	 * @param index
	 * @return
	 */
	boolean removeAlias(AliasQuery query, IndexCoordinates index);

	/**
	 * Create an index for given indexName if it does not already exist
	 *
	 * @param indexName
	 */
	boolean createIndex(String indexName);

	/**
	 * Create an index for given indexName and Settings
	 *
	 * @param indexName
	 * @param settings
	 */
	boolean createIndex(String indexName, Object settings);

	/**
	 * Create an index for a class if it does not already exist
	 *
	 * @param clazz
	 * @param <T>
	 */
	<T> boolean createIndex(Class<T> clazz);

	/**
	 * Create an index for given class and Settings
	 *
	 * @param clazz
	 * @param settings
	 */
	<T> boolean createIndex(Class<T> clazz, Object settings);

	/**
	 * Create mapping for a class
	 *
	 * @param clazz
	 * @param <T>
	 */
	<T> boolean putMapping(Class<T> clazz);

	/**
	 * Create mapping for the given class and put the mapping to the given index
	 *
	 * @param index
	 * @param clazz
	 * @since 3.2
	 */
	<T> boolean putMapping(IndexCoordinates index, Class<T> clazz);

	/**
	 * Create mapping for a given index
	 *  @param index
	 * @param mappings
	 * @param index
	 */
	boolean putMapping(IndexCoordinates index, Object mappings);

	/**
	 * Create mapping for a class
	 *
	 * @param clazz
	 * @param mappings
	 */
	<T> boolean putMapping(Class<T> clazz, Object mappings);

	/**
	 * Get mapping for a class
	 *
	 * @param clazz
	 */
	default Map<String, Object> getMapping(Class<?> clazz) {
		return getMapping(getIndexCoordinatesFor(clazz));
	}

	/**
	 * Get mapping for a given index coordinates
	 *
	 * @param index
	 */
	Map<String, Object> getMapping(IndexCoordinates index);

	/**
	 * Get settings for a given indexName
	 *
	 * @param indexName
	 */
	Map<String, Object> getSetting(String indexName);

	/**
	 * Get settings for a given class
	 *
	 * @param clazz
	 */
	<T> Map<String, Object> getSetting(Class<T> clazz);

	/**
	 * get all the alias pointing to specified index
	 *
	 * @param indexName
	 * @return
	 */
	List<AliasMetaData> queryForAlias(String indexName);

	<T> T query(Query query, ResultsExtractor<T> resultsExtractor, Class<T> clazz, IndexCoordinates index);

	/**
	 * Retrieves an object from an index
	 *
	 * @param query the query defining the id of the object to get
	 * @param clazz the type of the object to be returned
	 * @param index the index from which the object is read.
	 * @return the found object
	 */
	<T> T get(GetQuery query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the query against elasticsearch and return the first returned object
	 *
	 * @param query
	 * @param clazz
	 * @param index
	 * @return the first matching object
	 */
	default <T> T queryForObject(Query query, Class<T> clazz, IndexCoordinates index) {
		List<T> content = queryForPage(query, clazz, index).getContent();
		return content.isEmpty() ? null : content.get(0);
	}

	/**
	 * Execute the query against elasticsearch and return result as {@link Page}
	 *
	 * @param query
	 * @param clazz
	 * @return
	 */
	<T> AggregatedPage<T> queryForPage(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the multi-search against elasticsearch and return result as {@link List} of {@link Page}
	 *
	 * @param queries
	 * @param clazz
	 * @param index
	 * @return
	 */
	<T> List<Page<T>> queryForPage(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the multi-search against elasticsearch and return result as {@link List} of {@link Page}
	 *
	 * @param queries
	 * @param classes
	 * @param index
	 * @return
	 */
	List<Page<?>> queryForPage(List<? extends Query> queries, List<Class<?>> classes, IndexCoordinates index);

	/**
	 * Executes the given {@link Query} against elasticsearch and return result as {@link CloseableIterator}.
	 * <p>
	 * Returns a {@link CloseableIterator} that wraps an Elasticsearch scroll context that needs to be closed in case of
	 * error.
	 *
	 * @param <T> element return type
	 * @param query
	 * @param clazz
	 * @param index
	 * @return
	 * @since 1.3
	 */
	<T> CloseableIterator<T> stream(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the criteria query against elasticsearch and return result as {@link List}
	 *
	 * @param query
	 * @param clazz
	 * @param index
	 * @param <T>
	 * @return
	 */
	<T> List<T> queryForList(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List}
	 *
	 * @param <T>
	 * @param queries
	 * @param clazz
	 * @param index
	 * @return
	 */
	default <T> List<List<T>> queryForList(List<Query> queries, Class<T> clazz, IndexCoordinates index) {
		return queryForPage(queries, clazz, index).stream().map(Page::getContent).collect(Collectors.toList());
	}

	/**
	 * Execute the multi search query against elasticsearch and return result as {@link List}
	 *
	 * @param queries
	 * @param classes
	 * @param index
	 * @return
	 */
	default List<List<?>> queryForList(List<Query> queries, List<Class<?>> classes, IndexCoordinates index) {
		return queryForPage(queries, classes, index).stream().map(Page::getContent).collect(Collectors.toList());
	}

	/**
	 * Execute the query against elasticsearch and return ids
	 *
	 * @param query
	 * @param clazz
	 * @param index
	 * @return
	 */
	List<String> queryForIds(Query query, Class<?> clazz, IndexCoordinates index);

	/**
	 * return number of elements found by given query
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

	/**
	 * Execute a multiGet against elasticsearch for the given ids
	 *
	 * @param query
	 * @param clazz
	 * @param index
	 * @return
	 */
	<T> List<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Index an object. Will do save or update
	 *
	 * @param query
	 * @return returns the document id
	 */
	String index(IndexQuery query, IndexCoordinates index);

	/**
	 * Partial update of the document
	 *
	 * @param updateQuery
	 * @return
	 */
	UpdateResponse update(UpdateQuery updateQuery, IndexCoordinates index);

	/**
	 * Bulk index all objects. Will do save or update.
	 *
	 * @param queries the queries to execute in bulk
	 */
	default void bulkIndex(List<IndexQuery> queries, IndexCoordinates index) {
		bulkIndex(queries, BulkOptions.defaultOptions(), index);
	}

	/**
	 * Bulk index all objects. Will do save or update.
	 *
	 * @param queries the queries to execute in bulk
	 * @param bulkOptions options to be added to the bulk request
	 * @since 3.2
	 */
	void bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, IndexCoordinates index);

	/**
	 * Bulk update all objects. Will do update
	 *
	 * @param queries the queries to execute in bulk
	 */
	default void bulkUpdate(List<UpdateQuery> queries, IndexCoordinates index) {
		bulkUpdate(queries, BulkOptions.defaultOptions(), index);
	}

	/**
	 * Bulk update all objects. Will do update
	 *
	 * @param queries the queries to execute in bulk
	 * @param bulkOptions options to be added to the bulk request
	 * @since 3.2
	 */
	void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index);

	/**
	 * Delete the one object with provided id.
	 *
	 * @param id the document ot delete
	 * @param index the index from which to delete
	 * @return documentId of the document deleted
	 */
	String delete(String id, IndexCoordinates index);

	/**
	 * Delete all records matching the criteria
	 * 
	 * @param query
	 * @param clazz
	 * @param index
	 */
	void delete(Query query, Class<?> clazz, IndexCoordinates index);

	/**
	 * Delete all records matching the query
	 *
	 * @param query
	 * @param index the index where to delete the records
	 */
	void delete(DeleteQuery query, IndexCoordinates index);

	/**
	 * Deletes an index for given entity
	 *
	 * @param clazz
	 * @return
	 */
	default boolean deleteIndex(Class<?> clazz) {
		return deleteIndex(getPersistentEntityFor(clazz).getIndexName());
	}

	/**
	 * Deletes an index for given indexName
	 *
	 * @param indexName
	 * @return
	 */
	boolean deleteIndex(String indexName);

	/**
	 * check if index is exists
	 *
	 * @param clazz
	 * @return
	 */
	default boolean indexExists(Class<?> clazz) {
		return indexExists(getIndexCoordinatesFor(clazz).getIndexName());
	}

	/**
	 * check if index is exists for given IndexName
	 *
	 * @param indexName
	 * @return
	 */
	boolean indexExists(String indexName);

	/**
	 * refresh the index(es)
	 *
	 * @param index
	 */
	void refresh(IndexCoordinates index);

	/**
	 * refresh the index
	 *
	 * @param clazz
	 */
	default <T> void refresh(Class<T> clazz) {
		refresh(getIndexCoordinatesFor(clazz));
	}

	/**
	 * Returns scrolled page for given query
	 *
	 * @param scrollTimeInMillis The time in millisecond for scroll feature
	 *          {@link org.elasticsearch.action.search.SearchRequestBuilder#setScroll(org.elasticsearch.common.unit.TimeValue)}.
	 * @param query The search query.
	 * @param clazz The class of entity to retrieve.
	 * @param index
	 * @return The scan id for input query.
	 */
	<T> ScrolledPage<T> startScroll(long scrollTimeInMillis, Query query, Class<T> clazz, IndexCoordinates index);

	<T> ScrolledPage<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz);

	/**
	 * Clears the search contexts associated with specified scroll ids.
	 *
	 * @param scrollId
	 */
	void clearScroll(String scrollId);

	/**
	 * more like this query to search for documents that are "like" a specific document.
	 *
	 * @param query
	 * @param clazz
	 * @param index
	 * @param <T>
	 * @return
	 */
	<T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index);

	ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz);

	/**
	 * @return Converter in use
	 */
	ElasticsearchConverter getElasticsearchConverter();

	/**
	 * @param clazz
	 * @return the IndexCoordinates defined on the entity.
	 * @since 4.0
	 */
	default IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		ElasticsearchPersistentEntity entity = getPersistentEntityFor(clazz);
		return IndexCoordinates.of(entity.getIndexName()).withTypes(entity.getIndexType());
	}

}
