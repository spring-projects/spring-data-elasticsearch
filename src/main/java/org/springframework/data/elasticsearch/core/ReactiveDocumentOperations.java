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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.util.Assert;

/**
 * The reactive operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs.html">Elasticsearch Document APIs</a>.
 *
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 * @since 4.0
 */
public interface ReactiveDocumentOperations {
	/**
	 * Index the given entity, once available, extracting index and type from entity metadata.
	 *
	 * @param entityPublisher must not be {@literal null}.
	 * @param <T>
	 * @return a {@link Mono} emitting the saved entity.
	 */
	default <T> Mono<T> save(Mono<? extends T> entityPublisher) {

		Assert.notNull(entityPublisher, "EntityPublisher must not be null!");
		return entityPublisher.flatMap(this::save);
	}

	/**
	 * Index the given entity extracting index and type from entity metadata.
	 *
	 * @param entity must not be {@literal null}.
	 * @param <T>
	 * @return a {@link Mono} emitting the saved entity.
	 */
	<T> Mono<T> save(T entity);

	/**
	 * Index the entity, once available, under the given {@literal type} in the given {@literal index}. If the
	 * {@literal index} is {@literal null} or empty the index name provided via entity metadata is used. Same for the
	 * {@literal type}.
	 *
	 * @param entityPublisher must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return a {@link Mono} emitting the saved entity.
	 */
	default <T> Mono<T> save(Mono<? extends T> entityPublisher, IndexCoordinates index) {

		Assert.notNull(entityPublisher, "EntityPublisher must not be null!");
		return entityPublisher.flatMap(it -> save(it, index));
	}

	/**
	 * Index the entity under the given {@literal type} in the given {@literal index}. If the {@literal index} is
	 * {@literal null} or empty the index name provided via entity metadata is used. Same for the {@literal type}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return a {@link Mono} emitting the saved entity.
	 */
	<T> Mono<T> save(T entity, IndexCoordinates index);

	/**
	 * Index entities under the given {@literal type} in the given {@literal index}. If the {@literal index} is
	 * {@literal null} or empty the index name provided via entity metadata is used.
	 *
	 * @param entities must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return a {@link Flux} emitting saved entities.
	 * @since 4.0
	 */
	default <T> Flux<T> saveAll(Iterable<T> entities, IndexCoordinates index) {
		List<T> entityList = new ArrayList<>();
		entities.forEach(entityList::add);
		return saveAll(Mono.just(entityList), index);
	}

	/**
	 * Index entities under the given {@literal type} in the given {@literal index}. If the {@literal index} is
	 * {@literal null} or empty the index name provided via entity metadata is used.
	 *
	 * @param entities must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return a {@link Flux} emitting saved entities.
	 * @since 4.0
	 */
	<T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entities, IndexCoordinates index);

	/**
	 * Execute a multiGet against elasticsearch for the given ids.
	 *
	 * @param query the query defining the ids of the objects to get
	 * @param clazz the type of the object to be returned
	 * @param index the index(es) from which the objects are read.
	 * @return flux with list of nullable objects
	 * @since 4.0
	 */
	<T> Flux<T> multiGet(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Bulk update all objects. Will do update.
	 *
	 * @param queries the queries to execute in bulk
	 * @since 4.0
	 */
	default Mono<Void> bulkUpdate(List<UpdateQuery> queries, IndexCoordinates index) {
		return bulkUpdate(queries, BulkOptions.defaultOptions(), index);
	}

	/**
	 * Bulk update all objects. Will do update.
	 *
	 * @param queries the queries to execute in bulk
	 * @param bulkOptions options to be added to the bulk request
	 * @since 4.0
	 */
	Mono<Void> bulkUpdate(List<UpdateQuery> queries, BulkOptions bulkOptions, IndexCoordinates index);

	/**
	 * Find the document with the given {@literal id} mapped onto the given {@literal entityType}.
	 *
	 * @param id the {@literal _id} of the document to fetch.
	 * @param entityType the domain type used for mapping the document.
	 * @param <T>
	 * @return {@link Mono#empty()} if not found.
	 * @deprecated since 4.0 use {@link #get(String, Class)}
	 */
	@Deprecated
	default <T> Mono<T> findById(String id, Class<T> entityType) {
		return get(id, entityType);
	}

	/**
	 * Fetch the entity with given {@literal id}.
	 *
	 * @param id must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return the {@link Mono} emitting the entity or signalling completion if none found.
	 * @deprecated since 4.0, use {@link #get(String, Class, IndexCoordinates)}
	 */
	@Deprecated
	default <T> Mono<T> findById(String id, Class<T> entityType, IndexCoordinates index) {
		return get(id, entityType, index);
	}

	/**
	 * Find the document with the given {@literal id} mapped onto the given {@literal entityType}.
	 *
	 * @param id the {@literal _id} of the document to fetch.
	 * @param entityType the domain type used for mapping the document.
	 * @param <T>
	 * @return {@link Mono#empty()} if not found.
	 * @since 4.0
	 */
	<T> Mono<T> get(String id, Class<T> entityType);

	/**
	 * Fetch the entity with given {@literal id}.
	 *
	 * @param id must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return the {@link Mono} emitting the entity or signalling completion if none found.
	 */
	<T> Mono<T> get(String id, Class<T> entityType, IndexCoordinates index);

	/**
	 * Check if an entity with given {@literal id} exists.
	 *
	 * @param id the {@literal _id} of the document to look for.
	 * @param entityType the domain type used.
	 * @return a {@link Mono} emitting {@literal true} if a matching document exists, {@literal false} otherwise.
	 */
	Mono<Boolean> exists(String id, Class<?> entityType);

	/**
	 * Check if an entity with given {@literal id} exists.
	 *
	 * @param id the {@literal _id} of the document to look for.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting {@literal true} if a matching document exists, {@literal false} otherwise.
	 */
	Mono<Boolean> exists(String id, IndexCoordinates index);

	/**
	 * Check if an entity with given {@literal id} exists.
	 *
	 * @param id the {@literal _id} of the document to look for.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting {@literal true} if a matching document exists, {@literal false} otherwise.
	 * @deprecated since 4.0, use {@link #exists(String, Class)} or {@link #exists(String, IndexCoordinates)}
	 */
	@Deprecated
	Mono<Boolean> exists(String id, Class<?> entityType, IndexCoordinates index);

	/**
	 * Delete the given entity extracting index and type from entity metadata.
	 *
	 * @param entity must not be {@literal null}.
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 */
	Mono<String> delete(Object entity);

	/**
	 * Delete the given entity extracting index and type from entity metadata.
	 *
	 * @param entity must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 */
	Mono<String> delete(Object entity, IndexCoordinates index);

	/**
	 * Delete the entity with given {@literal id}.
	 *
	 * @param id must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 */
	Mono<String> delete(String id, IndexCoordinates index);

	/**
	 * Delete the entity with given {@literal id} extracting index and type from entity metadata.
	 *
	 * @param id must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 * @since 4.0
	 */
	Mono<String> delete(String id, Class<?> entityType);

	/**
	 * Delete the entity with given {@literal id} extracting index and type from entity metadata.
	 *
	 * @param id must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 * @deprecated since 4.0, use {@link #delete(String, Class)} or {@link #deleteById(String, IndexCoordinates)}
	 */
	@Deprecated
	default Mono<String> delete(String id, Class<?> entityType, IndexCoordinates index) {
		return delete(id, index);
	}

	/**
	 * Delete the documents matching the given {@link Query} extracting index and type from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the number of the removed documents.
	 */
	Mono<Long> delete(Query query, Class<?> entityType);

	/**
	 * Delete the documents matching the given {@link Query} extracting index and type from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the number of the removed documents.
	 */
	Mono<Long> delete(Query query, Class<?> entityType, IndexCoordinates index);
}
