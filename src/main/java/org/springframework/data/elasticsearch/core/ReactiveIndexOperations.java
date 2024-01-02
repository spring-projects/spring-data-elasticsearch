/*
 * Copyright 2020-2024 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Interface defining operations on indexes for the reactive stack.
 *
 * @author Peter-Josef Meisch
 * @author George Popides
 * @since 4.1
 */
public interface ReactiveIndexOperations {

	// region index management
	/**
	 * Create an index.
	 *
	 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if eg.
	 *         the index already exist.
	 */
	Mono<Boolean> create();

	/**
	 * Create an index with the specified settings.
	 *
	 * @param settings index settings
	 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if eg.
	 *         the index already exist.
	 */
	Mono<Boolean> create(Map<String, Object> settings);

	/**
	 * Create an index for given settings and mapping.
	 *
	 * @param settings the index settings
	 * @param mapping the index mapping
	 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if eg.
	 *         the index already exist.
	 * @since 4.2
	 */
	Mono<Boolean> create(Map<String, Object> settings, Document mapping);

	/**
	 * Create an index with the settings and mapping defined for the entity this IndexOperations is bound to.
	 *
	 * @return a {@link Mono} signalling successful operation completion or an {@link Mono#error(Throwable) error} if eg.
	 *         the index already exist.
	 * @since 4.2
	 */
	Mono<Boolean> createWithMapping();

	/**
	 * Delete an index.
	 *
	 * @return a {@link Mono} signalling operation completion or an {@link Mono#error(Throwable) error}. If the index does
	 *         not exist, a value of {@literal false is emitted}.
	 */
	Mono<Boolean> delete();

	/**
	 * checks if an index exists
	 *
	 * @return a {@link Mono} with the result of exist check
	 */
	Mono<Boolean> exists();

	/**
	 * Refresh the index(es) this IndexOperations is bound to
	 *
	 * @return a {@link Mono} signalling operation completion.
	 */
	Mono<Void> refresh();
	// endregion

	// region mappings
	/**
	 * Creates the index mapping for the entity this IndexOperations is bound to.
	 *
	 * @return mapping object
	 */
	Mono<Document> createMapping();

	/**
	 * Creates the index mapping for the given class
	 *
	 * @param clazz the clazz to create a mapping for
	 * @return a {@link Mono} with the mapping document
	 */
	Mono<Document> createMapping(Class<?> clazz);

	/**
	 * Writes the mapping to the index for the class this IndexOperations is bound to.
	 *
	 * @return {@literal true} if the mapping could be stored
	 */
	default Mono<Boolean> putMapping() {
		return putMapping(createMapping());
	}

	/**
	 * writes a mapping to the index
	 *
	 * @param mapping the Document with the mapping definitions
	 * @return {@literal true} if the mapping could be stored
	 */
	Mono<Boolean> putMapping(Mono<Document> mapping);

	/**
	 * Creates the index mapping for the given class and writes it to the index.
	 *
	 * @param clazz the clazz to create a mapping for
	 * @return {@literal true} if the mapping could be stored
	 */
	default Mono<Boolean> putMapping(Class<?> clazz) {
		return putMapping(createMapping(clazz));
	}

	/**
	 * Get mapping for the index targeted defined by this {@link ReactiveIndexOperations}
	 *
	 * @return the mapping
	 */
	Mono<Document> getMapping();
	// endregion

	// region settings
	/**
	 * Creates the index settings for the entity this IndexOperations is bound to.
	 *
	 * @return a settings document.
	 * @since 4.1
	 */
	Mono<Settings> createSettings();

	/**
	 * Creates the index settings from the annotations on the given class
	 *
	 * @param clazz the class to create the index settings from
	 * @return a settings document.
	 * @since 4.1
	 */
	Mono<Settings> createSettings(Class<?> clazz);

	/**
	 * get the settings for the index
	 *
	 * @return a {@link Mono} with a {@link Document} containing the index settings
	 */
	default Mono<Settings> getSettings() {
		return getSettings(false);
	}

	/**
	 * get the settings for the index
	 *
	 * @param includeDefaults whether or not to include all the default settings
	 * @return a {@link Mono} with a {@link Document} containing the index settings
	 */
	Mono<Settings> getSettings(boolean includeDefaults);
	// endregion

	// region aliases
	/**
	 * Executes the given {@link AliasActions}.
	 *
	 * @param aliasActions the actions to execute
	 * @return if the operation is acknowledged by Elasticsearch
	 * @since 4.1
	 */
	Mono<Boolean> alias(AliasActions aliasActions);

	/**
	 * gets information about aliases
	 *
	 * @param aliasNames alias names, must not be {@literal null}
	 * @return a {@link Mono} of {@link Map} from index names to {@link AliasData} for that index
	 * @since 4.1
	 */
	Mono<Map<String, Set<AliasData>>> getAliases(String... aliasNames);

	/**
	 * gets information about aliases
	 *
	 * @param indexNames alias names, must not be {@literal null}
	 * @return a {@link Mono} of {@link Map} from index names to {@link AliasData} for that index
	 * @since 4.1
	 */
	Mono<Map<String, Set<AliasData>>> getAliasesForIndex(String... indexNames);
	// endregion

	// region templates
	/**
	 * Creates an index template using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html)
	 *
	 * @param putTemplateRequest template request parameters
	 * @return Mono of {@literal true} if the template could be stored
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	Mono<Boolean> putTemplate(PutTemplateRequest putTemplateRequest);

	/**
	 * Writes a component index template that can be used in a composable index template.
	 *
	 * @param putComponentTemplateRequest index template request parameters
	 * @return {@literal true} if successful
	 * @since 5.1
	 */
	Mono<Boolean> putComponentTemplate(PutComponentTemplateRequest putComponentTemplateRequest);

	/**
	 * Get component template(s).
	 *
	 * @param getComponentTemplateRequest the getComponentTemplateRequest parameters
	 * @return a {@link Flux} of {@link TemplateResponse}
	 * @since 5.1
	 */
	Flux<TemplateResponse> getComponentTemplate(GetComponentTemplateRequest getComponentTemplateRequest);

	/**
	 * Checks wether a component index template exists.
	 *
	 * @param existsComponentTemplateRequest the parameters for the request
	 * @return Mono with the value if the componentTemplate exists.
	 * @since 5.1
	 */
	Mono<Boolean> existsComponentTemplate(ExistsComponentTemplateRequest existsComponentTemplateRequest);

	/**
	 * Deletes a component index template.
	 *
	 * @param deleteComponentTemplateRequest the parameters for the request
	 * @return Mono with the value if the request was acknowledged.
	 * @since 5.1
	 */
	Mono<Boolean> deleteComponentTemplate(DeleteComponentTemplateRequest deleteComponentTemplateRequest);

	/**
	 * Creates an index template.
	 *
	 * @param putIndexTemplateRequest template request parameters
	 * @return {@literal true} if successful
	 * @since 5.1
	 */
	Mono<Boolean> putIndexTemplate(PutIndexTemplateRequest putIndexTemplateRequest);

	/**
	 * Checks if an index template exists.
	 *
	 * @param indexTemplateName the name of the index template
	 * @return Mono with the value if the index template exists.
	 * @since 5.1
	 */
	default Mono<Boolean> existsIndexTemplate(String indexTemplateName) {
		return existsIndexTemplate(new ExistsIndexTemplateRequest(indexTemplateName));
	}

	/**
	 * Checks if an index template exists.
	 *
	 * @param existsIndexTemplateRequest the parameters for the request
	 * @return Mono with the value if the index template exists.
	 * @since 5.1
	 */
	Mono<Boolean> existsIndexTemplate(ExistsIndexTemplateRequest existsIndexTemplateRequest);

	/**
	 * Get index template(s).
	 *
	 * @param indexTemplateName the name of the index template
	 * @return a {@link Flux} of {@link TemplateResponse}
	 * @since 5.1
	 */
	default Flux<TemplateResponse> getIndexTemplate(String indexTemplateName) {
		return getIndexTemplate(new GetIndexTemplateRequest(indexTemplateName));
	}

	/**
	 * Get index template(s).
	 *
	 * @param getIndexTemplateRequest
	 * @return a {@link Flux} of {@link TemplateResponse}
	 * @since 5.1
	 */
	Flux<TemplateResponse> getIndexTemplate(GetIndexTemplateRequest getIndexTemplateRequest);

	/**
	 * Deletes an index template.
	 *
	 * @param indexTemplateName the name of the index template
	 * @return Mono with the value if the request was acknowledged.
	 * @since 5.1
	 */
	default Mono<Boolean> deleteIndexTemplate(String indexTemplateName) {
		return deleteIndexTemplate(new DeleteIndexTemplateRequest(indexTemplateName));
	}

	/**
	 * Deletes an index template.
	 *
	 * @param deleteIndexTemplateRequest the parameters for the request
	 * @return Mono with the value if the request was acknowledged.
	 * @since 5.1
	 */
	Mono<Boolean> deleteIndexTemplate(DeleteIndexTemplateRequest deleteIndexTemplateRequest);

	/**
	 * gets an index template using the legacy Elasticsearch interface.
	 *
	 * @param templateName the template name
	 * @return Mono of TemplateData, {@literal Mono.empty()} if no template with the given name exists.
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	default Mono<TemplateData> getTemplate(String templateName) {
		return getTemplate(new GetTemplateRequest(templateName));
	}

	/**
	 * gets an index template using the legacy Elasticsearch interface.
	 *
	 * @param getTemplateRequest the request parameters
	 * @return Mono of TemplateData, {@literal Mono.empty()} if no template with the given name exists.
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	Mono<TemplateData> getTemplate(GetTemplateRequest getTemplateRequest);

	/**
	 * Checks if an index template exists using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html)
	 *
	 * @param templateName the template name
	 * @return Mono of {@literal true} if the template exists
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	default Mono<Boolean> existsTemplate(String templateName) {
		return existsTemplate(new ExistsTemplateRequest(templateName));
	}

	/**
	 * Checks if an index template exists using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html)
	 *
	 * @param existsTemplateRequest template request parameters
	 * @return Mono of {@literal true} if the template exists
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	Mono<Boolean> existsTemplate(ExistsTemplateRequest existsTemplateRequest);

	/**
	 * Deletes an index template using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html)
	 *
	 * @param templateName the template name
	 * @return Mono of {@literal true} if the template could be deleted
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	default Mono<Boolean> deleteTemplate(String templateName) {
		return deleteTemplate(new DeleteTemplateRequest(templateName));
	}

	/**
	 * Deletes an index template using the legacy Elasticsearch interface (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html)
	 *
	 * @param deleteTemplateRequest template request parameters
	 * @return Mono of {@literal true} if the template could be deleted
	 * @since 4.1
	 * @deprecated since 5.1, as the underlying Elasticsearch API is deprecated.
	 */
	@Deprecated
	Mono<Boolean> deleteTemplate(DeleteTemplateRequest deleteTemplateRequest);

	// endregion

	// region index information
	/**
	 * Gets the {@link IndexInformation} for the indices defined by {@link #getIndexCoordinates()}.
	 *
	 * @return a flux of {@link IndexInformation}
	 * @since 4.2
	 */
	default Flux<IndexInformation> getInformation() {
		return getInformation(getIndexCoordinates());
	}

	/**
	 * Gets the {@link IndexInformation} for the indices defined by {@link #getIndexCoordinates()}.
	 *
	 * @return a flux of {@link IndexInformation}
	 * @since 4.2
	 */
	Flux<IndexInformation> getInformation(IndexCoordinates index);

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
