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
import java.util.Map;

import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html">Elasticsearch Index APIs</a>.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
public interface IndexOperations {

	/**
	 * Create an index for given indexName.
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index was created
	 */
	boolean createIndex(String indexName);

	/**
	 * Create an index for given indexName and Settings.
	 *
	 * @param indexName the name of the index
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 */
	boolean createIndex(String indexName, Object settings);

	/**
	 * Create an index for a class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index was created
	 */
	boolean createIndex(Class<?> clazz);

	/**
	 * Create an index for given class and Settings.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 */
	boolean createIndex(Class<?> clazz, Object settings);

	/**
	 * Deletes an index for given entity.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index was deleted
	 */
	boolean deleteIndex(Class<?> clazz);

	/**
	 * Deletes an index.
	 *
	 * @param indexName the name of the index to delete
	 * @return {@literal true} if the index was deleted
	 */
	boolean deleteIndex(String indexName);

	/**
	 * check if index exists.
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index exists
	 */
	boolean indexExists(String indexName);

	/**
	 * check if index is exists.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index exists
	 */
	boolean indexExists(Class<?> clazz);

	/**
	 * Create mapping for a class and store it to the index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 */
	boolean putMapping(Class<?> clazz);

	/**
	 * Create mapping for the given class and put the mapping to the given index.
	 *
	 * @param index the index to store the mapping to
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 */
	boolean putMapping(IndexCoordinates index, Class<?> clazz);

	/**
	 * Stores a mapping to an index.
	 *
	 * @param index the index to store the mapping to
	 * @param mappings can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 */
	boolean putMapping(IndexCoordinates index, Object mappings);

	/**
	 * Create mapping for a class Stores a mapping to an index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param mappings can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 */
	<T> boolean putMapping(Class<T> clazz, Object mappings);

	/**
	 * Get mapping for an index defined by a class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}.
	 * @return the mapping
	 */
	Map<String, Object> getMapping(Class<?> clazz);

	/**
	 * Get mapping for a given index.
	 *
	 * @param index the index to read the mapping from
	 * @return the mapping
	 */
	Map<String, Object> getMapping(IndexCoordinates index);

	/**
	 * Add an alias.
	 *
	 * @param query query defining the alias
	 * @param index the index for which to add an alias
	 * @return true if the alias was created
	 */
	boolean addAlias(AliasQuery query, IndexCoordinates index);

	/**
	 * Remove an alias.
	 *
	 * @param query query defining the alias
	 * @param index the index for which to remove an alias
	 * @return true if the alias was removed
	 */
	boolean removeAlias(AliasQuery query, IndexCoordinates index);

	/**
	 * Get the alias informations for a specified index.
	 *
	 * @param indexName the name of the index
	 * @return alias information
	 */
	List<AliasMetaData> queryForAlias(String indexName);

	/**
	 * Get settings for a given indexName.
	 *
	 * @param indexName the name of the index
	 * @return the settings
	 */
	Map<String, Object> getSettings(String indexName);

	/**
	 * Get settings for a given indexName.
	 *
	 * @param indexName the name of the index
	 * @param includeDefaults whether or not to include all the default settings
	 * @return the settings
	 */
	Map<String, Object> getSettings(String indexName, boolean includeDefaults);

	/**
	 * Get settings for a given class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return the settings
	 */
	Map<String, Object> getSettings(Class<?> clazz);

	/**
	 * Get settings for a given class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param includeDefaults whether or not to include all the default settings
	 * @return the settings
	 */
	Map<String, Object> getSettings(Class<?> clazz, boolean includeDefaults);

	/**
	 * Refresh the index(es).
	 *
	 * @param index the index to refresh
	 */
	void refresh(IndexCoordinates index);

	/**
	 * Refresh the index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 */
	void refresh(Class<?> clazz);
}
