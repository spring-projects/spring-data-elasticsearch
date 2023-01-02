/*
 * Copyright 2018-2023 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveClusterOperations;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.core.script.ReactiveScriptOperations;
import org.springframework.lang.Nullable;

/**
 * Interface that specifies a basic set of Elasticsearch operations executed in a reactive way.
 * <p>
 * Implemented by {@link ReactiveElasticsearchTemplate}. Not often used but a useful option for extensibility and
 * testability (as it can be easily mocked, stubbed, or be the target of a JDK proxy). Command execution using
 * {@link ReactiveElasticsearchOperations} is deferred until a {@link org.reactivestreams.Subscriber} subscribes to the
 * {@link Publisher}.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
public interface ReactiveElasticsearchOperations
		extends ReactiveDocumentOperations, ReactiveSearchOperations, ReactiveScriptOperations {

	/**
	 * Execute within a {@link ClientCallback} managing resources and translating errors.
	 *
	 * @param callback must not be {@literal null}.
	 * @param <T> the type the Publisher emits
	 * @return the {@link Publisher} emitting results.
	 * @deprecated since 4.4, use the execute methods from the implementing classes (they are client specific)
	 */
	@Deprecated
	<T> Publisher<T> execute(ClientCallback<Publisher<T>> callback);

	/**
	 * Execute within a {@link IndicesClientCallback} managing resources and translating errors.
	 *
	 * @param callback must not be {@literal null}.
	 * @param <T> the type the Publisher emits
	 * @return the {@link Publisher} emitting results.
	 * @since 4.1
	 */
	<T> Publisher<T> executeWithIndicesClient(IndicesClientCallback<Publisher<T>> callback);

	/**
	 * Execute within a {@link ClusterClientCallback} managing resources and translating errors.
	 *
	 * @param callback must not be {@literal null}.
	 * @param <T> the type the Publisher emits
	 * @return the {@link Publisher} emitting results.
	 * @since 4.1
	 */
	<T> Publisher<T> executeWithClusterClient(ClusterClientCallback<Publisher<T>> callback);

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

	// region routing
	/**
	 * Returns a copy of this instance with the same configuration, but that uses a different {@link RoutingResolver} to
	 * obtain routing information.
	 *
	 * @param routingResolver the {@link RoutingResolver} value, must not be {@literal null}.
	 * @return DocumentOperations instance
	 */
	ReactiveElasticsearchOperations withRouting(RoutingResolver routingResolver);
	// endregion

	/**
	 * Callback interface to be used with {@link #execute(ClientCallback)} for operating directly on
	 * {@link ReactiveElasticsearchClient}.
	 *
	 * @param <T> result type of the callback
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	interface ClientCallback<T extends Publisher<?>> {

		T doWithClient(ReactiveElasticsearchClient client);
	}

	/**
	 * Callback interface to be used with {@link #executeWithIndicesClient(IndicesClientCallback)} for operating directly
	 * on {@link ReactiveElasticsearchClient.Indices}.
	 *
	 * @param <T> the return type
	 * @since 4.1
	 */
	interface IndicesClientCallback<T extends Publisher<?>> {
		T doWithClient(ReactiveElasticsearchClient.Indices client);
	}

	/**
	 * Callback interface to be used with {@link #executeWithClusterClient(ClusterClientCallback)} for operating directly
	 * on {@link ReactiveElasticsearchClient.Cluster}.
	 *
	 * @param <T> the return type
	 * @since 4.2
	 */
	interface ClusterClientCallback<T extends Publisher<?>> {
		T doWithClient(ReactiveElasticsearchClient.Cluster client);
	}
}
