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

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html">Elasticsearch Index APIs</a>.
 * <br/>
 * IndexOperations are bound to an entity class or an IndexCoordinate by
 * {@link ElasticsearchOperations#indexOps(IndexCoordinates)} or {@link ElasticsearchOperations#indexOps(Class)}
 * 
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
public interface IndexOperations {

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
	 * Writes the mapping to the index for the class this IndexOperations is bound to.
	 * @return {@literal true} if the mapping could be stored
	 * @since 4.1
	 */
	default boolean putMapping() {
		return putMapping(createMapping());}

	/**
	 * writes a mapping to the index
	 * 
	 * @param mapping the Document with the mapping definitions
	 * @return {@literal true} if the mapping could be stored
	 */
	boolean putMapping(Document mapping);

	/**
	 * Creates the index mapping for the given class and writes it to the index.
	 * @param clazz the clazz to create a mapping for
	 * @return {@literal true} if the mapping could be stored
	 * @since 4.1
	 */
	default boolean putMapping(Class<?> clazz) {
		return putMapping(createMapping(clazz));
	}

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
	 * Get the index settings.
	 *
	 * @param includeDefaults whether or not to include all the default settings
	 * @return the settings
	 */
	Map<String, Object> getSettings(boolean includeDefaults);
}
