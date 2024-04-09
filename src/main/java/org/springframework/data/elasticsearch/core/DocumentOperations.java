/*
 * Copyright 2019-2024 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.lang.Nullable;

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs.html">Elasticsearch Document APIs</a>.
 *
 * @author Peter-Josef Meisch
 * @author Farid Faoudi
 * @author Sijia Liu
 * @author Haibo Liu
 * @since 4.0
 */
public interface DocumentOperations {

	/**
	 * Saves an entity to the index specified in the entity's Document annotation
	 *
	 * @param entity the entity to save, must not be {@literal null}
	 * @param <T> the entity type
	 * @return the saved entity
	 */
	<T> T save(T entity);

	/**
	 * Saves an entity to the index specified in the entity's Document annotation
	 *
	 * @param entity the entity to save, must not be {@literal null}
	 * @param index the index to save the entity in, must not be {@literal null}
	 * @param <T> the entity type
	 * @return the saved entity
	 */
	<T> T save(T entity, IndexCoordinates index);

	/**
	 * saves the given entities to the index retrieved from the entities' Document annotation
	 *
	 * @param entities must not be {@literal null}
	 * @param <T> the entity type
	 * @return the saved entites
	 */
	<T> Iterable<T> save(Iterable<T> entities);

	/**
	 * saves the given entities to the given index
	 *
	 * @param entities must not be {@literal null}
	 * @param index the index to save the entities in, must not be {@literal null}
	 * @param <T> the entity type
	 * @return the saved entities
	 */
	<T> Iterable<T> save(Iterable<T> entities, IndexCoordinates index);

	/**
	 * saves the given entities to the index retrieved from the entities' Document annotation
	 *
	 * @param entities must not be {@literal null}
	 * @param <T> the entity type
	 * @return the saved entities as Iterable
	 */
	<T> Iterable<T> save(T... entities);

	/**
	 * Index an object. Will do save or update.
	 *
	 * @param query the query defining the object
	 * @param index the index where the object is stored.
	 * @return returns the document id
	 */
	String index(IndexQuery query, IndexCoordinates index);

	/**
	 * Retrieves an object from the index specified in the entity's Document annotation.
	 *
	 * @param id the id of the object
	 * @param clazz the entity class,
	 * @param <T> the entity type
	 * @return the entity
	 */
	@Nullable
	<T> T get(String id, Class<T> clazz);

	/**
	 * Retrieves an object from the index specified in the entity's Document annotation.
	 *
	 * @param id the id of the object
	 * @param clazz the entity class,
	 * @param index the index from which the object is read.
	 * @return the entity
	 */
	@Nullable
	<T> T get(String id, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute a multiGet against elasticsearch for the given ids.
	 *
	 * @param query the query defining the ids of the objects to get
	 * @param clazz the type of the object to be returned
	 * @return list of {@link MultiGetItem}s
	 * @see Query#multiGetQuery(Collection)
	 * @see Query#multiGetQueryWithRouting(List)
	 * @since 4.1
	 */
	<T> List<MultiGetItem<T>> multiGet(Query query, Class<T> clazz);

	/**
	 * Execute a multiGet against elasticsearch for the given ids.
	 *
	 * @param query the query defining the ids of the objects to get
	 * @param clazz the type of the object to be returned
	 * @param index the index(es) from which the objects are read.
	 * @return list of {@link MultiGetItem}s
	 * @see Query#multiGetQuery(Collection)
	 * @see Query#multiGetQueryWithRouting(List)
	 */
	<T> List<MultiGetItem<T>> multiGet(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Check if an entity with given {@literal id} exists.
	 *
	 * @param id the {@literal _id} of the document to look for.
	 * @param clazz the domain type used.
	 * @return {@literal true} if a matching document exists, {@literal false} otherwise.
	 */
	boolean exists(String id, Class<?> clazz);

	/**
	 * Check if an entity with given {@literal id} exists.
	 *
	 * @param id the {@literal _id} of the document to look for.
	 * @param index the target index, must not be {@literal null}
	 * @return {@literal true} if a matching document exists, {@literal false} otherwise.
	 */
	boolean exists(String id, IndexCoordinates index);

	/**
	 * Bulk index all objects. Will do save or update.
	 *
	 * @param queries the queries to execute in bulk
	 * @param clazz the entity class
	 * @return the information about the indexed objects
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
	 * @since 4.1
	 */
	default List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, Class<?> clazz) {
		return bulkIndex(queries, BulkOptions.defaultOptions(), clazz);
	}

	/**
	 * Bulk index all objects. Will do save or update.
	 *
	 * @param queries the queries to execute in bulk
	 * @return the information about of the indexed objects
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
	 */
	default List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, IndexCoordinates index) {
		return bulkIndex(queries, BulkOptions.defaultOptions(), index);
	}

	/**
	 * Bulk index all objects. Will do save or update.
	 *
	 * @param queries the queries to execute in bulk
	 * @param bulkOptions options to be added to the bulk request
	 * @param clazz the entity class
	 * @return the information about of the indexed objects
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
	 * @since 4.1
	 */
	List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, Class<?> clazz);

	/**
	 * Bulk index all objects. Will do save or update.
	 *
	 * @param queries the queries to execute in bulk
	 * @param bulkOptions options to be added to the bulk request
	 * @return the information about of the indexed objects
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
	 */
	List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, IndexCoordinates index);

	/**
	 * Bulk update all objects. Will do update.
	 *
	 * @param queries the queries to execute in bulk
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
	 */
	default void bulkUpdate(List<UpdateQuery> queries, IndexCoordinates index) {
		bulkUpdate(queries, BulkOptions.defaultOptions(), index);
	}

	/**
	 * Bulk update all objects. Will do update.
	 *
	 * @param clazz the entity class
	 * @param queries the queries to execute in bulk
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
	 * @since 4.1
	 */
	void bulkUpdate(List<UpdateQuery> queries, Class<?> clazz);

	/**
	 * Bulk update all objects. Will do update.
	 *
	 * @param queries the queries to execute in bulk
	 * @param bulkOptions options to be added to the bulk request
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
	 */
	void bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index);

	/**
	 * Delete the one object with provided id.
	 *
	 * @param id the document to delete
	 * @param index the index from which to delete
	 * @return documentId of the document deleted
	 */
	String delete(String id, IndexCoordinates index);

	/**
	 * Delete the one object with provided id.
	 *
	 * @param id the document ot delete
	 * @param entityType must not be {@literal null}.
	 * @return documentId of the document deleted
	 */
	String delete(String id, Class<?> entityType);

	/**
	 * Deletes the given entity
	 *
	 * @param entity the entity to delete
	 * @return documentId of the document deleted
	 */
	String delete(Object entity);

	/**
	 * Deletes the given entity
	 *
	 * @param entity the entity to delete
	 * @param index the index from which to delete
	 * @return documentId of the document deleted
	 */
	String delete(Object entity, IndexCoordinates index);

	/**
	 * Delete all records matching the query.
	 *
	 * @param query query defining the objects
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return response with detailed information
	 * @since 4.1
	 * @deprecated since 5.3.0, use {@link #delete(DeleteQuery, Class)}
	 */
	@Deprecated
	ByQueryResponse delete(Query query, Class<?> clazz);

	/**
	 * Delete all records matching the query.
	 *
	 * @param query query defining the objects
	 * @param clazz The entity class must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return response with detailed information
	 * @since 5.3
	 */
	ByQueryResponse delete(DeleteQuery query, Class<?> clazz);

	/**
	 * Delete all records matching the query.
	 *
	 * @param query query defining the objects
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param index the index from which to delete
	 * @return response with detailed information
	 * @deprecated since 5.3.0, use {@link #delete(DeleteQuery, Class, IndexCoordinates)}
	 */
	@Deprecated
	ByQueryResponse delete(Query query, Class<?> clazz, IndexCoordinates index);

	/**
	 * Delete all records matching the query.
	 *
	 * @param query query defining the objects
	 * @param clazz The entity class must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param index the index from which to delete
	 * @return response with detailed information
	 * @since 5.3
	 */
	ByQueryResponse delete(DeleteQuery query, Class<?> clazz, IndexCoordinates index);

	/**
	 * Partially update a document by the given entity.
	 *
	 * @param entity the entity to update partially, must not be {@literal null}.
	 * @return the update response
	 * @param <T> the entity type
	 * @since 5.0
	 */
	<T> UpdateResponse update(T entity);

	/**
	 * Partially update a document by the given entity.
	 *
	 * @param entity the entity to update partially, must not be {@literal null}.
	 * @param index the index to use for the update instead of the one defined by the entity, must not be null
	 * @return the update response
	 * @param <T> the entity type
	 * @since 5.1
	 */
	<T> UpdateResponse update(T entity, IndexCoordinates index);

	/**
	 * Partial update of the document.
	 *
	 * @param updateQuery query defining the update
	 * @param index the index where to update the records
	 * @return the update response
	 */
	UpdateResponse update(UpdateQuery updateQuery, IndexCoordinates index);

	/**
	 * Update document(s) by query
	 *
	 * @param updateQuery query defining the update, must not be {@literal null}
	 * @param index the index where to update the records , must not be {@literal null}
	 * @return the update response
	 * @since 4.2
	 */
	ByQueryResponse updateByQuery(UpdateQuery updateQuery, IndexCoordinates index);

	/**
	 * Copies documents from a source to a destination. The source can be any existing index, alias, or data stream. The
	 * destination must differ from the source. For example, you cannot reindex a data stream into itself. (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)
	 *
	 * @param reindexRequest reindex request parameters
	 * @return the reindex response
	 * @since 4.4
	 */
	ReindexResponse reindex(ReindexRequest reindexRequest);

	/**
	 * Submits a reindex task. (@see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)
	 *
	 * @param reindexRequest reindex request parameters
	 * @return the task id
	 * @since 4.4
	 */
	String submitReindex(ReindexRequest reindexRequest);
}
