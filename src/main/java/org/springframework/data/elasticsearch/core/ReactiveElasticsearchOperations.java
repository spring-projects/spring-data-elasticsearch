/*
 * Copyright 2018-2024 the original author or authors.
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

import org.springframework.data.elasticsearch.core.cluster.ReactiveClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.core.script.ReactiveScriptOperations;
import org.springframework.lang.Nullable;

/**
 * Interface that specifies a basic set of Elasticsearch operations executed in a reactive way.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public interface ReactiveElasticsearchOperations
		extends ReactiveDocumentOperations, ReactiveSearchOperations, ReactiveScriptOperations {

	/**
	 * Get the {@link ElasticsearchConverter} used.
	 *
	 * @return never {@literal null}
	 */
	ElasticsearchConverter getElasticsearchConverter();

	@Nullable
	ElasticsearchPersistentEntity<?> getPersistentEntityFor(Class<?> clazz);

	/**
	 * @param clazz
	 * @return the IndexCoordinates defined on the entity.
	 * @since 4.0
	 */
	IndexCoordinates getIndexCoordinatesFor(Class<?> clazz);

	/**
	 * Creates a {@link ReactiveIndexOperations} that is bound to the given index
	 *
	 * @param index IndexCoordinates specifying the index
	 * @return ReactiveIndexOperations implementation
	 * @since 4.1
	 */
	ReactiveIndexOperations indexOps(IndexCoordinates index);

	/**
	 * Creates a {@link ReactiveIndexOperations} that is bound to the given class
	 *
	 * @param clazz the entity clazz specifiying the index information
	 * @return ReactiveIndexOperations implementation
	 * @since 4.1
	 */
	ReactiveIndexOperations indexOps(Class<?> clazz);

	/**
	 * return a {@link ReactiveClusterOperations} instance that uses the same client communication setup as this
	 * ElasticsearchOperations instance.
	 *
	 * @return ClusterOperations implementation
	 * @since 4.2
	 */
	ReactiveClusterOperations cluster();

	/**
	 * gets the routing for an entity.
	 *
	 * @param entity the entity
	 * @return the routing, may be null if not set.
	 * @since 5.2
	 */
	@Nullable
	String getEntityRouting(Object entity);

	// region customizations
	/**
	 * Returns a copy of this instance with the same configuration, but that uses a different {@link RoutingResolver} to
	 * obtain routing information.
	 *
	 * @param routingResolver the {@link RoutingResolver} value, must not be {@literal null}.
	 * @return DocumentOperations instance
	 */
	ReactiveElasticsearchOperations withRouting(RoutingResolver routingResolver);

	/**
	 * Returns a copy of this instance with the same configuration, but that uses a different {@link RefreshPolicy}.
	 *
	 * @param refreshPolicy the {@link RefreshPolicy} value.
	 * @return {@link ReactiveElasticsearchOperations} instance.
	 * @since 5.2
	 */
	ReactiveElasticsearchOperations withRefreshPolicy(@Nullable RefreshPolicy refreshPolicy);
	// endregion
}
