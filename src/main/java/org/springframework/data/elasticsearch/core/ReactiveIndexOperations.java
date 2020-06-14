/*
 * Copyright 2020 the original author or authors.
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

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Interface defining operations on indexes for the reactive stack.
 *
 * @author Peter-Josef Meisch
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
	Mono<Boolean> create(Document settings);

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
	 * get the settings for the index
	 * 
	 * @return a {@link Mono} with a {@link Document} containing the index settings
	 */
	default Mono<Document> getSettings() {
		return getSettings(false);
	}

	/**
	 * get the settings for the index
	 *
	 * @param includeDefaults whether or not to include all the default settings
	 * @return a {@link Mono} with a {@link Document} containing the index settings
	 */
	Mono<Document> getSettings(boolean includeDefaults);
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

	// region helper functions
	/**
	 * get the current {@link IndexCoordinates}. These may change over time when the entity class has a SpEL constructed
	 * index name. When this IndexOperations is not bound to a class, the bound IndexCoordinates are returned.
	 *
	 * @return IndexCoordinates
	 * @ince 4.1
	 */
	IndexCoordinates getIndexCoordinates();

	// endregion
}
