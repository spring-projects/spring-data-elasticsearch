/*
 * Copyright 2018-2020 the original author or authors.
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
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
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
public interface ReactiveElasticsearchOperations extends ReactiveDocumentOperations, ReactiveSearchOperations {

	/**
	 * Execute within a {@link ClientCallback} managing resources and translating errors.
	 *
	 * @param callback must not be {@literal null}.
	 * @param <T>
	 * @return the {@link Publisher} emitting results.
	 */
	<T> Publisher<T> execute(ClientCallback<Publisher<T>> callback);

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
	 * Callback interface to be used with {@link #execute(ClientCallback)} for operating directly on
	 * {@link ReactiveElasticsearchClient}.
	 *
	 * @param <T>
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	interface ClientCallback<T extends Publisher<?>> {

		T doWithClient(ReactiveElasticsearchClient client);
	}
}
