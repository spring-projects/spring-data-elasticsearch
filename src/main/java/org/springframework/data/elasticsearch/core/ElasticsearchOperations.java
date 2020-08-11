/*
 * Copyright 2013-2020 the original author or authors.
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
import java.util.Objects;

import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.lang.Nullable;

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

	/**
	 * get an {@link IndexOperations} that is bound to the given class
	 * 
	 * @return IndexOperations
	 */
	IndexOperations indexOps(Class<?> clazz);

	/**
	 * get an {@link IndexOperations} that is bound to the given index
	 * 
	 * @return IndexOperations
	 */
	IndexOperations indexOps(IndexCoordinates index);

	ElasticsearchConverter getElasticsearchConverter();

	IndexCoordinates getIndexCoordinatesFor(Class<?> clazz);

	/**
	 * gets the routing for an entity which might be defined by a join-type relation
	 * 
	 * @param entity the entity
	 * @return the routing, may be null if not set.
	 * @since 4.1
	 */
	@Nullable
	String getEntityRouting(Object entity);

	// region IndexOperations
	/**
	 * Create an index for given indexName .
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0, use {@link IndexOperations#create()}
	 */
	@Deprecated
	default boolean createIndex(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).create();
	}

	/**
	 * Create an index for given indexName and Settings.
	 *
	 * @param indexName the name of the index
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0, use {@link IndexOperations#create(Document)}
	 */
	@Deprecated
	default boolean createIndex(String indexName, Object settings) {
		return indexOps(IndexCoordinates.of(indexName)).create(getDocument(settings));
	}

	/**
	 * Create an index for a class if it does not already exist.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index was created
	 * @deprecated since 4.0, use {@link IndexOperations#create()}
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
	 * @deprecated since 4.0, use {@link IndexOperations#create(Document)}
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
	 * @deprecated since 4.0, use {@link IndexOperations#delete()}
	 */
	@Deprecated
	default boolean deleteIndex(Class<?> clazz) {
		return indexOps(clazz).delete();
	}

	/**
	 * Deletes an index.
	 *
	 * @param indexName the name of the index to delete
	 * @return {@literal true} if the index was deleted
	 * @deprecated since 4.0, use {@link IndexOperations#delete()}
	 */
	@Deprecated
	default boolean deleteIndex(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).delete();
	}

	/**
	 * Deletes an index for an IndexCoordinate
	 *
	 * @param index the index to delete
	 * @return {@literal true} if the index was deleted
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#delete()}
	 */
	@Deprecated
	default boolean deleteIndex(IndexCoordinates index) {
		return indexOps(index).delete();
	}

	/**
	 * check if index exists.
	 *
	 * @param indexName the name of the index
	 * @return {@literal true} if the index exists
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#exists()}
	 */
	@Deprecated
	default boolean indexExists(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).exists();
	}

	/**
	 * check if index is exists.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the index exists
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link IndexOperations#exists()}
	 */
	@Deprecated
	default boolean indexExists(Class<?> clazz) {
		return indexOps(clazz).exists();
	}

	/**
	 * Create mapping for a class and store it to the index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(Class)}, {@link IndexOperations#createMapping(Class)} and
	 *             {@link IndexOperations#putMapping(Document)}
	 */
	@Deprecated
	default boolean putMapping(Class<?> clazz) {
		IndexOperations indexOps = indexOps(clazz);
		return indexOps.putMapping(clazz);
	}

	/**
	 * Create mapping for the given class and put the mapping to the given index.
	 *
	 * @param index the index to store the mapping to
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)}, {@link IndexOperations#createMapping(Class)} and
	 *             {@link IndexOperations#putMapping(Document)}
	 */
	@Deprecated
	default boolean putMapping(IndexCoordinates index, Class<?> clazz) {
		IndexOperations indexOps = indexOps(index);
		return indexOps.putMapping(clazz);
	}

	/**
	 * Stores a mapping to an index.
	 *
	 * @param index the index to store the mapping to
	 * @param mappings can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#putMapping(Document)}
	 */
	@Deprecated
	default boolean putMapping(IndexCoordinates index, Object mappings) {
		return indexOps(index).putMapping(getDocument(mappings));
	}

	/**
	 * Create mapping for a class Stores a mapping to an index.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @param mappings can be a JSON String or a {@link Map}
	 * @return {@literal true} if the mapping could be stored
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link IndexOperations#putMapping(Document)}
	 */
	@Deprecated
	default boolean putMapping(Class<?> clazz, Object mappings) {
		return indexOps(clazz).putMapping(getDocument(mappings));
	}

	/**
	 * Get mapping for an index defined by a class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}.
	 * @return the mapping
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link IndexOperations#getMapping()}
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
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#getMapping()}
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
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#addAlias(AliasQuery)}
	 */
	@Deprecated
	default boolean addAlias(AliasQuery query, IndexCoordinates index) {
		return indexOps(index).addAlias(query);
	}

	/**
	 * Remove an alias.
	 *
	 * @param query query defining the alias
	 * @param index the index for which to remove an alias
	 * @return true if the alias was removed
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} {@link IndexOperations#removeAlias(AliasQuery)}
	 */
	@Deprecated
	default boolean removeAlias(AliasQuery query, IndexCoordinates index) {
		return indexOps(index).removeAlias(query);
	}

	/**
	 * Get the alias informations for a specified index.
	 *
	 * @param indexName the name of the index
	 * @return alias information
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#queryForAlias()}
	 */
	@Deprecated
	default List<AliasMetadata> queryForAlias(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).queryForAlias();
	}

	/**
	 * Get settings for a given indexName.
	 *
	 * @param indexName the name of the index
	 * @return the settings
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#getSettings()} )}
	 */
	@Deprecated
	default Map<String, Object> getSetting(String indexName) {
		return indexOps(IndexCoordinates.of(indexName)).getSettings();
	}

	/**
	 * Get settings for a given class.
	 *
	 * @param clazz The entity class, must be annotated with
	 *          {@link org.springframework.data.elasticsearch.annotations.Document}
	 * @return the settings
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link IndexOperations#getSettings()}
	 */
	@Deprecated
	default Map<String, Object> getSetting(Class<?> clazz) {
		return indexOps(clazz).getSettings();
	}

	/**
	 * Get settings for a given indexName.
	 *
	 * @param indexName the name of the index
	 * @param includeDefaults whether or not to include all the default settings
	 * @deprecated since 4.0 use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#getSettings(boolean)} ()}
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
	 * @param includeDefaults whether or not to include all the default settings
	 * @return the settings
	 * @deprecated since 4.0 use {@link #indexOps(Class)} and {@link IndexOperations#getSettings(boolean)} ()}
	 */
	default Map<String, Object> getSettings(Class<?> clazz, boolean includeDefaults) {
		return indexOps(clazz).getSettings(includeDefaults);
	}

	/**
	 * Refresh the index(es).
	 *
	 * @param index the index to refresh
	 * @deprecated since 4.0, use {@link #indexOps(IndexCoordinates)} and {@link IndexOperations#refresh()} instead}
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
	 * @deprecated since 4.0, use {@link #indexOps(Class)} and {@link IndexOperations#refresh()} instead}
	 */
	@Deprecated
	default void refresh(Class<?> clazz) {
		indexOps(clazz).refresh();
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
	} // endregion

	// region helper
	/**
	 * gets the String representation for an id.
	 * 
	 * @param id
	 * @return
	 * @since 4.0
	 */
	@Nullable
	default String stringIdRepresentation(@Nullable Object id) {
		return Objects.toString(id, null);
	}
	// endregion
}
