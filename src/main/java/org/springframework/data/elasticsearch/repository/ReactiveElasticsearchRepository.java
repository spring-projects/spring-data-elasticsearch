/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License", @Nullable RefreshPolicy refreshPolicy);
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
package org.springframework.data.elasticsearch.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.lang.Nullable;

/**
 * Elasticsearch specific {@link org.springframework.data.repository.Repository} interface with reactive support.
 *
 * @author Christoph Strobl
 * @since 3.2
 */
@SuppressWarnings("unused")
@NoRepositoryBean
public interface ReactiveElasticsearchRepository<T, ID>
		extends ReactiveSortingRepository<T, ID>, ReactiveCrudRepository<T, ID> {
	/**
	 * @since 5.2
	 */
	<S extends T> Mono<S> save(S entity, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	<S extends T> Flux<S> saveAll(Iterable<S> entities, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	<S extends T> Flux<S> saveAll(Publisher<S> entityStream, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	Mono<Void> deleteById(ID id, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	Mono<Void> deleteById(Publisher<ID> id, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	Mono<Void> delete(T entity, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	Mono<Void> deleteAllById(Iterable<? extends ID> ids, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	Mono<Void> deleteAll(Iterable<? extends T> entities, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	Mono<Void> deleteAll(Publisher<? extends T> entityStream, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	Mono<Void> deleteAll(@Nullable RefreshPolicy refreshPolicy);
}
