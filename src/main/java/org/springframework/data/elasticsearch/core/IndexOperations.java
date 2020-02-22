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

import java.util.List;
import java.util.Map;

import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.lang.Nullable;

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html">Elasticsearch Index APIs</a>.
 * <br/>
 * IndexOperations can be bound to an entity class or an IndexCoordinate (by {@link #indexOps(Class)} or
 * {@link #indexOps(IndexCoordinates)}), then it is not necessary to pass the class or IndexCoordinate in every call.
 * 
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
public interface IndexOperations {

	/**
	 * returns an {@link IndexOperations} bound to the given entity class.
	 * 
	 * @param clazz the entity class
	 * @return IndexOperations;
	 */
	IndexOperations indexOps(Class<?> clazz);

	/**
	 * returns an {@link IndexOperations} bound to the given IndexCoordinate.
	 * 
	 * @param index the IndexCoordinate
	 * @return IndexOperations;
	 */
	IndexOperations indexOps(IndexCoordinates index);

	/**
	 * Create an index.
	 *
	 * @return {@literal true} if the index was created
	 */
	boolean create();

	/**
	 * Create an index for given Settings.
	 *
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 */
	boolean create(Document settings);

	/**
	 * Deletes the index this {@link IndexOperations} is bound to
	 *
	 * @return {@literal true} if the index was deleted
	 * @throws IndexOperationsException if this object is not bound
	 */
	boolean delete();

	/**
	 * Checks if the index this IndexOperations is bound to exists
	 *
	 * @return {@literal true} if the index exists
	 */
	boolean exists();

	/**
	 * Refresh the index(es) this IndexOperations is bound to
	 */
	void refresh();

	/**
	 * Creates the index mapping for the entity this IndexOperations is bound to.
	 *
	 * @return mapping object
	 */
	Document createMapping();

	/**
	 * Creates the index mapping for the given class
	 *
	 * @param clazz the clazz to create a mapping for
	 * @return mapping object
	 */
	Document createMapping(Class<?> clazz);

	/**
	 * writes a mapping to the index
	 * 
	 * @param mapping the Document with the mapping definitions
	 * @return {@literal true} if the mapping could be stored
	 */
	boolean putMapping(Document mapping);

	/**
	 * Get mapping for an index defined by a class.
	 *
	 * @return the mapping
	 */
	Map<String, Object> getMapping();

	/**
	 * Add an alias.
	 *
	 * @param query query defining the alias
	 * @return true if the alias was created
	 */
	boolean addAlias(AliasQuery query);

	/**
	 * Get the alias informations for a specified index.
	 *
	 * @return alias information
	 */
	List<AliasMetaData> queryForAlias();

	/**
	 * Remove an alias.
	 *
	 * @param query query defining the alias
	 * @return true if the alias was removed
	 */
	boolean removeAlias(AliasQuery query);

	/**
	 * Get the index settings.
	 *
	 * @return the settings
	 */
	Map<String, Object> getSettings();

	/**
	 * Get settings for a given indexName.
	 *
	 * @param includeDefaults wehther or not to include all the default settings
	 * @return the settings
	 */
	Map<String, Object> getSettings(boolean includeDefaults);

	// region deprecated
	/**
	 * Create an index for given indexName.
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #create()} ()}
	 */
	@Deprecated
	default boolean createIndex(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).create();
	}

	/**
	 * Create an index for given indexName and Settings.
	 *
	 * @param indexName the name of the index
	 * @param settings the index settings, must be a JSON String or a Map<String, Object>
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #create(Document)} ()}
	 */
	@Deprecated
	default boolean createIndex(String indexName, Object settings) {
		return indexOps(IndexCoordinates.of(indexName)).create(getDocument(settings));
	}

	/**
	 * Create an index for a class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #create()} ()}
	 */
	@Deprecated
	default boolean createIndex(Class<?> clazz) {
		return indexOps(clazz).create();
	}

	/**
	 * Create an index for given class and Settings.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #create(Document)} ()}
	 */
	@Deprecated
	default boolean createIndex(Class<?> clazz, Object settings) {
		return indexOps(clazz).create(getDocument(settings));
	}

	/**
	 * Deletes an index for given entity.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index was deleted
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #delete()}
	 */
	@Deprecated
	default boolean deleteIndex(Class<?> clazz) {
		return indexOps(clazz).delete();
	}

	/**
	 * Deletes an index for an IndexCoordinate
	 *
	 * @param index the index to delete
	 * @return {@literal true} if the index was deleted
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link #delete()}
	 */
	@Deprecated
	default boolean deleteIndex(IndexCoordinates index) {
		return indexOps(index).delete();
	}

	/**
	 * Deletes an index.
	 *
	 * @param indexName the name of the index to delete
	 * @return {@literal true} if the index was deleted
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link #delete()}
	 */
	@Deprecated
	default boolean deleteIndex(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).delete();
	}

	/**
	 * check if index is exists.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index exists
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #exists()}
	 */
	@Deprecated
	default boolean indexExists(Class<?> clazz) {
		return indexOps(clazz).exists();
	}

	/**
	 * check if index exists.
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index exists
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link #exists()}
	 */
	@Deprecated
	default boolean indexExists(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).exists();
	}

	/**
	 * Refresh the index(es).
	 *
	 * @param index the index to refresh
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link #refresh()}
	 */
	@Deprecated
	default void refresh(IndexCoordinates index) {
		indexOps(index).refresh();
	}

	/**
	 * Refresh the index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #refresh()}
	 */
	@Deprecated
	default void refresh(Class<?> clazz) {
		indexOps(clazz).refresh();
	}

	/**
	 * Get settings for a given indexName.
	 *
	 * @param indexName the name of the index
	 * @return the settings
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link #getSettings()} ()}
	 */
	@Deprecated
	default Map<String, Object> getSettings(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).getSettings();
	}

	/**
	 * Get settings for a given indexName.
	 *
	 * @param indexName the name of the index
	 * @param includeDefaults whether or not to include all the default settings
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link #getSettings(boolean)} ()}
	 * @return the settings
	 */
	@Deprecated
	default Map<String, Object> getSettings(String indexName, boolean includeDefaults) {
		return indexOps(IndexCoordinates.of(indexName)).getSettings(includeDefaults);
	}

	/**
	 * Get settings for a given class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return the settings
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #getSettings()} ()}
	 */
	@Deprecated
	default Map<String, Object> getSettings(Class<?> clazz) {
		return indexOps(clazz).getSettings();
	}

	/**
	 * Get settings for a given class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param includeDefaults whether or not to include all the default settings
	 * @return the settings
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link #getSettings(boolean)} ()}
	 */
	default Map<String, Object> getSettings(Class<?> clazz, boolean includeDefaults) {
		return indexOps(clazz).getSettings(includeDefaults);
	}

	/**
	 * Stores a mapping to an index.
	 *
	 * @param clazz The entity class defining the index, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param mapping can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link #putMapping(Document)}
	 */
	@Deprecated
	default <T> boolean putMapping(Class<T> clazz, Object mapping) {
		Document document = getDocument(mapping);
		if (document == null) {
			throw new IllegalArgumentException("mapping cannot be converted to Document");
		}
		return indexOps(clazz).putMapping(document);
	}

	/**
	 * Create mapping for a class and store it to the index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(Class)}, {@link #createMapping(Class)} and
	 *             {@link #putMapping(Document)}
	 */
	@Deprecated
	default boolean putMapping(Class<?> clazz) {
		Document mapping = createMapping(clazz);
		return indexOps(clazz).putMapping(mapping);
	}

	/**
	 * Create mapping for the given class and put the mapping to the given index.
	 *
	 * @param index the index to store the mapping to
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)}, {@link #createMapping(Class)} and
	 *             {@link #putMapping(Document)}
	 */
	@Deprecated
	default boolean putMapping(IndexCoordinates index, Class<?> clazz) {
		Document mapping = createMapping(clazz);
		return indexOps(index).putMapping(mapping);
	}

	/**
	 * Stores a mapping to an index.
	 *
	 * @param index the index to store the mapping to
	 * @param mapping can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link #putMapping(Document)}
	 */
	@Deprecated
	default boolean putMapping(IndexCoordinates index, Object mapping) {
		Document document = getDocument(mapping);
		if (document == null) {
			throw new IllegalArgumentException("mapping cannot be converted to Document");
		}
		return indexOps(index).putMapping(document);
	}

	/**
	 * Get mapping for an index defined by a class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}.
	 * @return the mapping
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link #getMapping(IndexCoordinates)}.
	 */
	@Deprecated
	default Map<String, Object> getMapping(Class<?> clazz) {
		return indexOps(clazz).getMapping();
	}

	/**
	 * Get mapping for a given index.
	 *
	 * @param index the index to read the mapping from
	 * @return the mapping
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link #getMapping(IndexCoordinates)}.
	 */
	@Deprecated
	default Map<String, Object> getMapping(IndexCoordinates index) {
		return indexOps(index).getMapping();
	}

	/**
	 * Add an alias.
	 *
	 * @param query query defining the alias
	 * @param index the index for which to add an alias
	 * @return true if the alias was created
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link #addAlias(AliasQuery)}
	 */
	@Deprecated
	default boolean addAlias(AliasQuery query, IndexCoordinates index) {
		return indexOps(index).addAlias(query);
	}

	/**
	 * Get the alias informations for a specified index.
	 *
	 * @param indexName the name of the index
	 * @return alias information
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link #queryForAlias()}
	 */
	@Deprecated
	default List<AliasMetaData> queryForAlias(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).queryForAlias();
	}

	/**
	 * Remove an alias.
	 *
	 * @param query query defining the alias
	 * @param index the index for which to remove an alias
	 * @return true if the alias was removed
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link #removeAlias(AliasQuery)}
	 */
	default boolean removeAlias(AliasQuery query, IndexCoordinates index) {
		return indexOps(index).removeAlias(query);
	}

	/**
	 * converts an object to a Document
	 * 
	 * @param object
	 * @return
	 * @deprecated since 4.0, helper method for deprecated functions
	 */
	@Deprecated
	@Nullable
	default Document getDocument(Object object) {
		Document document = null;

		try {
			if (object instanceof String) {
				document = Document.parse((String) object);
			} else if (object instanceof Map) {
				document = Document.from((Map<String, Object>) object);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("object cannot be converted to Document", e);
		}
		return document;
	}

	// endregion

}
