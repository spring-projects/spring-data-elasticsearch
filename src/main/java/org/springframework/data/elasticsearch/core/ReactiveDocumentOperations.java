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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.util.Assert;

/**
 * The reactive operations for the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs.html">Elasticsearch Document APIs</a>.
 *
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 * @author Roman Puchkovskiy
 * @author Farid Faoudi
 * @author Sijia Liu
 * @since 4.0
 */
public interface ReactiveDocumentOperations {

	int FLUX_SAVE_BULK_SIZE = 500;

	/**
	 * Index the given entity, once available, extracting index from entity metadata.
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
	 * Index the given entity extracting index from entity metadata.
	 *
	 * @param entity must not be {@literal null}.
	 * @param <T>
	 * @return a {@link Mono} emitting the saved entity.
	 */
	<T> Mono<T> save(T entity);

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
	 * Indexes the entities into the index extracted from entity metadata.
	 *
	 * @param entities
	 * @param clazz the class to get the index name from
	 * @param <T> entity type
	 * @return a Flux emitting the saved entities
	 * @since 5.1
	 */
	default <T> Flux<T> save(Flux<T> entities, Class<?> clazz) {
		return save(entities, clazz, FLUX_SAVE_BULK_SIZE);
	}

	/**
	 * Indexes the entities into the index extracted from entity metadata. The entities are collected into batches of
	 * {bulkSize} with a maximal timeout of 200 ms, see
	 * {@link reactor.core.publisher.Flux#bufferTimeout(int, java.time .Duration)} and then sent in a bulk operation to
	 * Elasticsearch.
	 *
	 * @param entities
	 * @param clazz the class to get the index name from
	 * @param bulkSize number of entities to put in a bulk request
	 * @param <T> entity type
	 * @return a Flux emitting the saved entities
	 * @since 5.1
	 */
	<T> Flux<T> save(Flux<T> entities, Class<?> clazz, int bulkSize);

	/**
	 * Indexes the entities into the given index.
	 *
	 * @param entities the entities to save
	 * @param index the index to save to
	 * @param <T> entity type
	 * @return a Flux emitting the saved entities
	 * @since 5.1
	 */
	default <T> Flux<T> save(Flux<T> entities, IndexCoordinates index) {
		return save(entities, index, FLUX_SAVE_BULK_SIZE);
	}

	/**
	 * Indexes the entities into the given index. The entities are collected into batches of {bulkSize} with a maximal
	 * timeout of 200 ms, see {@link reactor.core.publisher.Flux#bufferTimeout(int, java.time * .Duration)} and then sent
	 * in a bulk operation to Elasticsearch.
	 *
	 * @param entities the entities to save
	 * @param index the index to save to
	 * @param bulkSize number of entities to put in a bulk request
	 * @param <T> entity type
	 * @return a Flux emitting the saved entities
	 * @since 5.1
	 */
	<T> Flux<T> save(Flux<T> entities, IndexCoordinates index, int bulkSize);

	/**
	 * Index entities the index extracted from entity metadata.
	 *
	 * @param entities must not be {@literal null}.
	 * @param clazz the entity class, used to determine the index
	 * @return a {@link Flux} emitting saved entities.
	 * @since 4.1
	 */
	default <T> Flux<T> saveAll(Iterable<T> entities, Class<T> clazz) {
		List<T> entityList = new ArrayList<>();
		entities.forEach(entityList::add);
		return saveAll(Mono.just(entityList), clazz);
	}

	/**
	 * Index entities in the given {@literal index}. If the {@literal index} is {@literal null} or empty the index name
	 * provided via entity metadata is used. On errors returns with
	 * {@link org.springframework.data.elasticsearch.BulkFailureException} with information about the failed operation
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
	 * Index entities in the index extracted from entity metadata.
	 *
	 * @param entities must not be {@literal null}.
	 * @param clazz the entity class, used to determine the index
	 * @return a {@link Flux} emitting saved entities.
	 * @since 4.1
	 */
	<T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entities, Class<T> clazz);

	/**
	 * Index entities in the given {@literal index}. If the {@literal index} is {@literal null} or empty the index name
	 * provided via entity metadata is used.
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
	 * @param clazz the type of the object to be returned, used to determine the index
	 * @return flux with list of {@link MultiGetItem}s that contain the entities
	 * @see Query#multiGetQuery(Collection)
	 * @see Query#multiGetQueryWithRouting(List)
	 * @since 4.1
	 */
	<T> Flux<MultiGetItem<T>> multiGet(Query query, Class<T> clazz);

	/**
	 * Execute a multiGet against elasticsearch for the given ids.
	 *
	 * @param query the query defining the ids of the objects to get
	 * @param clazz the type of the object to be returned
	 * @param index the index(es) from which the objects are read.
	 * @return flux with list of {@link MultiGetItem}s that contain the entities
	 * @see Query#multiGetQuery(Collection)
	 * @see Query#multiGetQueryWithRouting(List)
	 * @since 4.0
	 */
	<T> Flux<MultiGetItem<T>> multiGet(Query query, Class<T> clazz, IndexCoordinates index);

	/**
	 * Bulk update all objects. Will do update. On errors returns with
	 * {@link org.springframework.data.elasticsearch.BulkFailureException} with information about the failed operation
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
	 * @throws org.springframework.data.elasticsearch.BulkFailureException with information about the failed operation
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
	 * Delete the given entity extracting index from entity metadata.
	 *
	 * @param entity must not be {@literal null}.
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 */
	Mono<String> delete(Object entity);

	/**
	 * Delete the given entity extracting index from entity metadata.
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
	 * Delete the entity with given {@literal id} extracting index from entity metadata.
	 *
	 * @param id must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 * @since 4.0
	 */
	Mono<String> delete(String id, Class<?> entityType);

	/**
	 * Delete the documents matching the given {@link Query} extracting index from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the number of the removed documents.
	 * @deprecated since 5.3.0, use {@link #delete(DeleteQuery, Class)}
	 */
	@Deprecated
	Mono<ByQueryResponse> delete(Query query, Class<?> entityType);

	/**
	 * Delete the documents matching the given {@link Query} extracting index from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the number of the removed documents.
	 * @since 5.3
	 */
	Mono<ByQueryResponse> delete(DeleteQuery query, Class<?> entityType);

	/**
	 * Delete the documents matching the given {@link Query} extracting index from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the number of the removed documents.
	 * @deprecated since 5.3.0, use {@link #delete(DeleteQuery, Class, IndexCoordinates)}
	 */
	@Deprecated
	Mono<ByQueryResponse> delete(Query query, Class<?> entityType, IndexCoordinates index);

	/**
	 * Delete the documents matching the given {@link Query} extracting index from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the number of the removed documents.
	 * @since 5.3
	 */
	Mono<ByQueryResponse> delete(DeleteQuery query, Class<?> entityType, IndexCoordinates index);

	/**
	 * Partial update of the document.
	 *
	 * @param updateQuery query defining the update
	 * @param index the index where to update the records
	 * @return a {@link Mono} emitting the update response
	 * @since 4.1
	 */
	Mono<UpdateResponse> update(UpdateQuery updateQuery, IndexCoordinates index);

	/**
	 * Update document(s) by query.
	 *
	 * @param updateQuery query defining the update, must not be {@literal null}
	 * @param index the index where to update the records, must not be {@literal null}
	 * @return a {@link Mono} emitting the update response
	 * @since 4.2
	 */
	Mono<ByQueryResponse> updateByQuery(UpdateQuery updateQuery, IndexCoordinates index);

	/**
	 * Copies documents from a source to a destination. The source can be any existing index, alias, or data stream. The
	 * destination must differ from the source. For example, you cannot reindex a data stream into itself. (@see
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)
	 *
	 * @param reindexRequest reindex request parameters
	 * @return a {@link Mono} emitting the reindex response
	 * @since 4.4
	 */
	Mono<ReindexResponse> reindex(ReindexRequest reindexRequest);

	/**
	 * Submits a reindex task. (@see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)
	 *
	 * @param reindexRequest reindex request parameters
	 * @return a {@link Mono} emitting the {@literal task} id.
	 * @since 4.4
	 */
	Mono<String> submitReindex(ReindexRequest reindexRequest);
}
