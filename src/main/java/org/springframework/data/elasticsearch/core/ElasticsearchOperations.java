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

import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;

/**
 * ElasticsearchOperations. Since 4.0 this interface only contains common helper functions, the other methods have been
 * moved to the different interfaces that are extended by ElasticsearchOperations. The interfaces now reflect the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/rest-apis.html">REST API structure</a> of
 * Elasticsearch.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 * @author Zetang Zeng
 * @author Dmitriy Yakovlev
 * @author Peter-Josef Meisch
 */
public interface ElasticsearchOperations extends DocumentOperations, SearchOperations {

	IndexOperations getIndexOperations();

	ElasticsearchConverter getElasticsearchConverter();

	IndexCoordinates getIndexCoordinatesFor(Class<?> clazz);

	// region IndexOperations
	/**
	 * Create an index for given indexName if it does not already exist.
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0, use {@link IndexOperations#createIndex(String) instead}
	 */
	@Deprecated
	default boolean createIndex(String indexName) {
		return getIndexOperations().createIndex(indexName);
	}

	/**
	 * Create an index for given indexName and Settings.
	 *
	 * @param indexName the name of the index
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0, use {@link IndexOperations#createIndex(String, Object)} instead}
	 */
	@Deprecated
	default boolean createIndex(String indexName, Object settings) {
		return getIndexOperations().createIndex(indexName, settings);
	}

	/**
	 * Create an index for a class if it does not already exist.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0, use {@link IndexOperations#createIndex(Class)} instead}
	 */
	@Deprecated
	default boolean createIndex(Class<?> clazz) {
		return getIndexOperations().createIndex(clazz);
	}

	/**
	 * Create an index for given class and Settings.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0, use {@link IndexOperations#createIndex(Class, Object)} instead}
	 */
	@Deprecated
	default boolean createIndex(Class<?> clazz, Object settings) {
		return getIndexOperations().createIndex(clazz, settings);
	}

	/**
	 * Deletes an index for given entity.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index was deleted
	 * @deprecated since 4.0, use {@link IndexOperations#deleteIndex(Class)} instead}
	 */
	@Deprecated
	default boolean deleteIndex(Class<?> clazz) {
		return getIndexOperations().deleteIndex(clazz);
	}

	/**
	 * Deletes an index.
	 *
	 * @param indexName the name of the index to delete
	 * @return {@literal true} if the index was deleted
	 * @deprecated since 4.0, use {@link IndexOperations#deleteIndex(String)} instead}
	 */
	@Deprecated
	default boolean deleteIndex(String indexName) {
		return getIndexOperations().deleteIndex(indexName);
	}

	/**
	 * check if index exists.
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index exists
	 * @deprecated since 4.0, use {@link IndexOperations#indexExists(String)} instead}
	 */
	@Deprecated
	default boolean indexExists(String indexName) {
		return getIndexOperations().indexExists(indexName);
	}

	/**
	 * check if index is exists.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index exists
	 * @deprecated since 4.0, use {@link IndexOperations#indexExists(Class)} instead}
	 */
	@Deprecated
	default boolean indexExists(Class<?> clazz) {
		return getIndexOperations().indexExists(clazz);
	}

	/**
	 * Create mapping for a class and store it to the index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link IndexOperations#putMapping(Class)} instead}
	 */
	@Deprecated
	default boolean putMapping(Class<?> clazz) {
		return getIndexOperations().putMapping(clazz);
	}

	/**
	 * Create mapping for the given class and put the mapping to the given index.
	 *
	 * @param index the index to store the mapping to
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link IndexOperations#putMapping(IndexCoordinates, Class)} instead}
	 */
	@Deprecated
	default boolean putMapping(IndexCoordinates index, Class<?> clazz) {
		return getIndexOperations().putMapping(index, clazz);
	}

	/**
	 * Stores a mapping to an index.
	 *
	 * @param index the index to store the mapping to
	 * @param mappings can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link IndexOperations#putMapping(IndexCoordinates, Object)} instead}
	 */
	@Deprecated
	default boolean putMapping(IndexCoordinates index, Object mappings) {
		return getIndexOperations().putMapping(index, mappings);
	}

	/**
	 * Create mapping for a class Stores a mapping to an index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param mappings can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link IndexOperations#putMapping(Class, Object)} instead}
	 */
	@Deprecated
	default boolean putMapping(Class<?> clazz, Object mappings) {
		return getIndexOperations().putMapping(clazz, mappings);
	}

	/**
	 * Get mapping for an index defined by a class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}.
	 * @return the mapping
	 * @deprecated since 4.0, use {@link IndexOperations#getMapping(Class)} instead}
	 */
	@Deprecated
	default Map<String, Object> getMapping(Class<?> clazz) {
		return getIndexOperations().getMapping(clazz);
	}

	/**
	 * Get mapping for a given index.
	 *
	 * @param index the index to read the mapping from
	 * @return the mapping
	 * @deprecated since 4.0, use {@link IndexOperations#getMapping(IndexCoordinates)} instead}
	 */
	@Deprecated
	default Map<String, Object> getMapping(IndexCoordinates index) {
		return getIndexOperations().getMapping(index);
	}

	/**
	 * Add an alias.
	 *
	 * @param query query defining the alias
	 * @param index the index for which to add an alias
	 * @return true if the alias was created
	 * @deprecated since 4.0, use {@link IndexOperations#addAlias(AliasQuery, IndexCoordinates)} instead}
	 */
	@Deprecated
	default boolean addAlias(AliasQuery query, IndexCoordinates index) {
		return getIndexOperations().addAlias(query, index);
	}

	/**
	 * Remove an alias.
	 *
	 * @param query query defining the alias
	 * @param index the index for which to remove an alias
	 * @return true if the alias was removed
	 * @deprecated since 4.0, use {@link IndexOperations#removeAlias(AliasQuery, IndexCoordinates)} instead}
	 */
	@Deprecated
	default boolean removeAlias(AliasQuery query, IndexCoordinates index) {
		return getIndexOperations().removeAlias(query, index);
	}

	/**
	 * Get the alias informations for a specified index.
	 *
	 * @param indexName the name of the index
	 * @return alias information
	 * @deprecated since 4.0, use {@link IndexOperations#queryForAlias(String)} instead}
	 */
	@Deprecated
	default List<AliasMetaData> queryForAlias(String indexName) {
		return getIndexOperations().queryForAlias(indexName);
	}

	/**
	 * Get settings for a given indexName.
	 *
	 * @param indexName the name of the index
	 * @return the settings
	 * @deprecated since 4.0, use {@link IndexOperations#getSettings(String)} )} instead}
	 */
	@Deprecated
	default Map<String, Object> getSetting(String indexName) {
		return getIndexOperations().getSettings(indexName);
	}

	/**
	 * Get settings for a given class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return the settings
	 * @deprecated since 4.0, use {@link IndexOperations#getSettings(Class)} instead}
	 */
	@Deprecated
	default Map<String, Object> getSetting(Class<?> clazz) {
		return getIndexOperations().getSettings(clazz);
	}

	/**
	 * Refresh the index(es).
	 *
	 * @param index the index to refresh
	 * @deprecated since 4.0, use {@link IndexOperations#refresh(IndexCoordinates)} instead}
	 */
	@Deprecated
	default void refresh(IndexCoordinates index) {
		getIndexOperations().refresh(index);
	}

	/**
	 * Refresh the index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @deprecated since 4.0, use {@link IndexOperations#refresh(Class)} instead}
	 */
	@Deprecated
	default void refresh(Class<?> clazz) {
		getIndexOperations().refresh(clazz);
	}
	// endregion
}
