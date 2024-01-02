/*
 * Copyright 2013-2024 the original author or authors.
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

import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.core.script.ScriptOperations;
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
public interface ElasticsearchOperations extends DocumentOperations, SearchOperations, ScriptOperations {

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

	/**
	 * return a {@link ClusterOperations} instance that uses the same client communication setup as this
	 * ElasticsearchOperations instance.
	 *
	 * @return ClusterOperations implementation
	 * @since 4.2
	 */
	ClusterOperations cluster();

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

	// region helper
	/**
	 * Converts an idValue to a String representation. The default implementation calls
	 * {@link ElasticsearchConverter#convertId(Object)}
	 *
	 * @param idValue the value to convert
	 * @return the converted value or {@literal null} if idValue is null
	 * @since 5.0
	 */
	@Nullable
	default String convertId(@Nullable Object idValue) {
		return idValue != null ? getElasticsearchConverter().convertId(idValue) : null;
	}
	// endregion

	// region customizations
	/**
	 * Returns a copy of this instance with the same configuration, but that uses a different {@link RoutingResolver} to
	 * obtain routing information.
	 *
	 * @param routingResolver the {@link RoutingResolver} value, must not be {@literal null}.
	 * @return {@link ElasticsearchOperations} instance
	 * @since 4.2
	 */
	ElasticsearchOperations withRouting(RoutingResolver routingResolver);

	/**
	 * Returns a copy of this instance with the same configuration, but that uses a different {@link RefreshPolicy}.
	 *
	 * @param refreshPolicy the {@link RefreshPolicy} value.
	 * @return {@link ElasticsearchOperations} instance.
	 * @since 5.2
	 */
	ElasticsearchOperations withRefreshPolicy(@Nullable RefreshPolicy refreshPolicy);
	// endregion
}
