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

import org.elasticsearch.action.update.UpdateResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs.html">Elasticsearch Document APIs</a>.
 *
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public interface DocumentOperations {

	/**
	 * Index an object. Will do save or update.
	 *
	 * @param query the query defining the object
	 * @param index the index from which the object is read.
	 * @return returns the document id
	 */
	String index(IndexQuery query, IndexCoordinates index);

	/**
	 * Retrieves an object from an index.
	 *
	 * @param query the query defining the id of the object to get
	 * @param clazz the type of the object to be returned
	 * @param index the index from which the object is read.
	 * @return the found object
	 */
	<T> T get(GetQuery query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Execute a multiGet against elasticsearch for the given ids.
	 *
	 * @param query the query defining the ids of the objects to get
	 * @param clazz the type of the object to be returned
	 * @param index the index(es) from which the objects are read.
	 * @return list of objects
	 */
	<T> List<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index);

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
	 */
	void bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, IndexCoordinates index);

	/**
	 * Bulk update all objects. Will do update.
	 *
	 * @param queries the queries to execute in bulk
	 */
	default void bulkUpdate(List<UpdateQuery> queries, IndexCoordinates index) {
		bulkUpdate(queries, BulkOptions.defaultOptions(), index);
	}

	/**
	 * Bulk update all objects. Will do update.
	 *
	 * @param queries the queries to execute in bulk
	 * @param bulkOptions options to be added to the bulk request
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
	 * Delete all records matching the query.
	 *
	 * @param query query defining the objects
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param index the index from which to delete
	 */
	void delete(Query query, Class<?> clazz, IndexCoordinates index);

	/**
	 * Delete all records matching the query.
	 *
	 * @param query query defining the objects
	 * @param index the index where to delete the records
	 */
	void delete(DeleteQuery query, IndexCoordinates index);

	/**
	 * Partial update of the document.
	 *
	 * @param updateQuery query defining the update
	 * @param index the index where to update the records
	 * @return the update response
	 */
	UpdateResponse update(UpdateQuery updateQuery, IndexCoordinates index);
}
