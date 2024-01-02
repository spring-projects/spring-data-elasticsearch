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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;

/**
 * The operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html">Elasticsearch Index APIs</a>.
 * <br/>
 * IndexOperations are bound to an entity class or an IndexCoordinate by
 * {@link ElasticsearchOperations#indexOps(IndexCoordinates)} or {@link ElasticsearchOperations#indexOps(Class)}
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author George Popides
 * @since 4.0
 */
public interface IndexOperations {

	// region index management
	/**
	 * Create an index.
	 *
	 * @return {@literal true} if the index was created
	 */
	boolean create();

	/**
	 * Create an index for given settings.
	 *
	 * @param settings the index settings
	 * @return {@literal true} if the index was created
	 */
	boolean create(Map<String, Object> settings);

	/**
	 * Create an index for given settings and mapping.
	 *
	 * @param settings the index settings
	 * @param mapping the index mapping
	 * @return {@literal true} if the index was created
	 * @since 4.2
	 */
	boolean create(Map<String, Object> settings, Document mapping);

	/**
	 * Create an index with the settings and mapping defined for the entity this IndexOperations is bound to.
	 *
	 * @return {@literal true} if the index was created
	 * @since 4.2
	 */
	boolean createWithMapping();

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
	// endregion

	// region mapping
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
	 *
	 * @return {@literal true} if the mapping could be stored
	 * @since 4.1
	 */
	default boolean putMapping() {
		return putMapping(createMapping());
	}

	/**
	 * writes a mapping to the index
	 *
	 * @param mapping the Document with the mapping definitions
	 * @return {@literal true} if the mapping could be stored
	 */
	boolean putMapping(Document mapping);

	/**
	 * Creates the index mapping for the given class and writes it to the index.
	 *
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

	// endregion

	// region settings
	/**
	 * Creates the index settings for the entity this IndexOperations is bound to.
	 *
	 * @return a settings document.
	 * @since 4.1
	 */
	Settings createSettings();

	/**
	 * Creates the index settings from the annotations on the given class
	 *
	 * @param clazz the class to create the index settings from
	 * @return a settings document.
	 * @since 4.1
	 */
	Settings createSettings(Class<?> clazz);

	/**
	 * Get the index settings.
	 *
	 * @return the settings
	 */
	Settings getSettings();

	/**
	 * Get the index settings.
	 *
	 * @param includeDefaults whether or not to include all the default settings
	 * @return the settings
	 */
	Settings getSettings(boolean includeDefaults);
	// endregion

	// region aliases
	/**
	 * Executes the given {@link AliasActions}.
	 *
	 * @param aliasActions the actions to execute
	 * @return if the operation is acknowledged by Elasticsearch
	 * @since 4.1
	 */
	boolean alias(AliasActions aliasActions);

	/**
	 * gets information about aliases
	 *
	 * @param aliasNames alias names, must not be {@literal null}
	 * @return a {@link Map} from index names to {@link AliasData} for that index
	 * @since 4.1
	 */
	Map<String, Set<AliasData>> getAliases(String... aliasNames);

	/**
	 * gets information about aliases
	 *
	 * @param indexNames index names, must not be {@literal null}
	 * @return a {@link Map} from index names to {@link AliasData} for that index
	 * @since 4.1
	 */
	Map<String, Set<AliasData>> getAliasesForIndex(String... indexNames);
	// endregion

	// region templates
	/**
	 * Creates an index template using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html).
	 *
	 * @param putTemplateRequest template request parameters
	 * @return true if successful
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	boolean putTemplate(PutTemplateRequest putTemplateRequest);

	/**
	 * Creates an index template
	 *
	 * @param putIndexTemplateRequest template request parameters
	 * @return {@literal true} if successful
	 * @since 5.1
	 */
	boolean putIndexTemplate(PutIndexTemplateRequest putIndexTemplateRequest);

	/**
	 * Writes a component index template that can be used in a composable index template.
	 *
	 * @param putComponentTemplateRequest index template request parameters
	 * @return {@literal true} if successful
	 * @since 5.1
	 */
	boolean putComponentTemplate(PutComponentTemplateRequest putComponentTemplateRequest);

	/**
	 * Checks wether a component index template exists.
	 *
	 * @param existsComponentTemplateRequest the parameters for the request
	 * @return {@literal true} if the componentTemplate exists.
	 * @since 5.1
	 */
	boolean existsComponentTemplate(ExistsComponentTemplateRequest existsComponentTemplateRequest);

	/**
	 * Get a component template.
	 *
	 * @param getComponentTemplateRequest parameters for the request, may contain wildcard names
	 * @return the found {@link TemplateResponse}s, may be empty
	 * @since 5.1
	 */
	List<TemplateResponse> getComponentTemplate(GetComponentTemplateRequest getComponentTemplateRequest);

	/**
	 * Deletes the given component index template
	 *
	 * @param deleteComponentTemplateRequest request parameters
	 * @return {@literal true} if successful.
	 * @since 5.1
	 */
	boolean deleteComponentTemplate(DeleteComponentTemplateRequest deleteComponentTemplateRequest);

	/**
	 * gets an index template using the legacy Elasticsearch interface.
	 *
	 * @param templateName the template name
	 * @return TemplateData, {@literal null} if no template with the given name exists.
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	@Nullable
	default TemplateData getTemplate(String templateName) {
		return getTemplate(new GetTemplateRequest(templateName));
	}

	/**
	 * gets an index template using the legacy Elasticsearch interface.
	 *
	 * @param getTemplateRequest the request parameters
	 * @return TemplateData, {@literal null} if no template with the given name exists.
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	@Nullable
	TemplateData getTemplate(GetTemplateRequest getTemplateRequest);

	/**
	 * check if an index template exists using the legacy Elasticsearch interface.
	 *
	 * @param templateName the template name
	 * @return {@literal true} if the index exists
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	default boolean existsTemplate(String templateName) {
		return existsTemplate(new ExistsTemplateRequest(templateName));
	}

	/**
	 * check if an index template exists using the legacy Elasticsearch interface.
	 *
	 * @param existsTemplateRequest the request parameters
	 * @return {@literal true} if the index exists
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	boolean existsTemplate(ExistsTemplateRequest existsTemplateRequest);

	/**
	 * check if an index template exists.
	 *
	 * @param templateName the template name
	 * @return true if the index template exists
	 * @since 5.1
	 */
	default boolean existsIndexTemplate(String templateName) {
		return existsIndexTemplate(new ExistsIndexTemplateRequest(templateName));
	}

	/**
	 * check if an index template exists.
	 *
	 * @param existsTemplateRequest the request parameters
	 * @return true if the index template exists
	 * @since 5.1
	 */
	boolean existsIndexTemplate(ExistsIndexTemplateRequest existsTemplateRequest);

	/**
	 * Gets an index template.
	 *
	 * @param templateName template name
	 * @since 5.1
	 */
	default List<TemplateResponse> getIndexTemplate(String templateName) {
		return getIndexTemplate(new GetIndexTemplateRequest(templateName));
	}

	/**
	 * Gets an index template.
	 *
	 * @param getIndexTemplateRequest the request parameters
	 * @since 5.1
	 */
	List<TemplateResponse> getIndexTemplate(GetIndexTemplateRequest getIndexTemplateRequest);

	/**
	 * Deletes an index template.
	 *
	 * @param templateName template name
	 * @return true if successful
	 * @since 5.1
	 */
	default boolean deleteIndexTemplate(String templateName) {
		return deleteIndexTemplate(new DeleteIndexTemplateRequest(templateName));
	}

	/**
	 * Deletes an index template.
	 *
	 * @param deleteIndexTemplateRequest template request parameters
	 * @return true if successful
	 * @since 5.1
	 */
	boolean deleteIndexTemplate(DeleteIndexTemplateRequest deleteIndexTemplateRequest);

	/**
	 * Deletes an index template using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html).
	 *
	 * @param templateName the template name
	 * @return true if successful
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	default boolean deleteTemplate(String templateName) {
		return deleteTemplate(new DeleteTemplateRequest(templateName));
	}

	/**
	 * Deletes an index template using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html).
	 *
	 * @param deleteTemplateRequest template request parameters
	 * @return true if successful
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	boolean deleteTemplate(DeleteTemplateRequest deleteTemplateRequest);

	// endregion

	// region index information
	/**
	 * Gets the {@link IndexInformation} for the indices defined by {@link #getIndexCoordinates()}.
	 *
	 * @return a list of {@link IndexInformation}
	 * @since 4.2
	 */
	default List<IndexInformation> getInformation() {
		return getInformation(getIndexCoordinates());
	}

	/**
	 * Gets the {@link IndexInformation} for the indices defined by #index.
	 *
	 * @param index defines the index names to get the information for
	 * @return a list of {@link IndexInformation}
	 * @since 4.2
	 */
	List<IndexInformation> getInformation(IndexCoordinates index);
	// endregion

	// region helper functions
	/**
	 * get the current {@link IndexCoordinates}. These may change over time when the entity class has a SpEL constructed
	 * index name. When this IndexOperations is not bound to a class, the bound IndexCoordinates are returned.
	 *
	 * @return IndexCoordinates
	 * @since 4.1
	 */
	IndexCoordinates getIndexCoordinates();

	// endregion
}
