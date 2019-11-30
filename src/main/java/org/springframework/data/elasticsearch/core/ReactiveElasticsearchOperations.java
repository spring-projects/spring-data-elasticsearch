/*
 * Copyright 2018-2019 the original author or authors.
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

import org.elasticsearch.index.query.QueryBuilders;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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
public interface ReactiveElasticsearchOperations {

	/**
	 * Execute within a {@link ClientCallback} managing resources and translating errors.
	 *
	 * @param callback must not be {@literal null}.
	 * @param <T>
	 * @return the {@link Publisher} emitting results.
	 */
	<T> Publisher<T> execute(ClientCallback<Publisher<T>> callback);

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
	default <T> Mono<T> save(T entity) {
		return save(entity, getIndexCoordinatesFor(entity.getClass()));
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
	 * Find the document with the given {@literal id} mapped onto the given {@literal entityType}.
	 *
	 * @param id the {@literal _id} of the document to fetch.
	 * @param entityType the domain type used for mapping the document.
	 * @param <T>
	 * @return {@link Mono#empty()} if not found.
	 */
	default <T> Mono<T> findById(String id, Class<T> entityType) {
		return findById(id, entityType, getIndexCoordinatesFor(entityType));
	}

	/**
	 * Fetch the entity with given {@literal id}.
	 *
	 * @param id must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return the {@link Mono} emitting the entity or signalling completion if none found.
	 */
	<T> Mono<T> findById(String id, Class<T> entityType, IndexCoordinates index);

	/**
	 * Check if an entity with given {@literal id} exists.
	 *
	 * @param id the {@literal _id} of the document to look for.
	 * @param entityType the domain type used.
	 * @return a {@link Mono} emitting {@literal true} if a matching document exists, {@literal false} otherwise.
	 */
	default Mono<Boolean> exists(String id, Class<?> entityType) {
		return exists(id, entityType, getIndexCoordinatesFor(entityType));
	}

	/**
	 * Check if an entity with given {@literal id} exists.
	 *
	 * @param id the {@literal _id} of the document to look for.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting {@literal true} if a matching document exists, {@literal false} otherwise.
	 */
	Mono<Boolean> exists(String id, Class<?> entityType, IndexCoordinates index);

	/**
	 * Search the index for entities matching the given {@link Query query}. <br />
	 * {@link Pageable#isUnpaged() Unpaged} queries may overrule elasticsearch server defaults for page size by either
	 * delegating to the scroll API or using a max {@link org.elasticsearch.search.builder.SearchSourceBuilder#size(int)
	 * size}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param <T>
	 * @return a {@link Flux} emitting matching entities one by one.
	 */
	default <T> Flux<T> find(Query query, Class<T> entityType) {
		return find(query, entityType, entityType);
	}

	/**
	 * Search the index for entities matching the given {@link Query query}. <br />
	 * {@link Pageable#isUnpaged() Unpaged} queries may overrule elasticsearch server defaults for page size by either *
	 * delegating to the scroll API or using a max {@link org.elasticsearch.search.builder.SearchSourceBuilder#size(int) *
	 * size}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType The entity type for mapping the query. Must not be {@literal null}.
	 * @param returnType The mapping target type. Must not be {@literal null}. Th
	 * @param <T>
	 * @return a {@link Flux} emitting matching entities one by one.
	 */
	default <T> Flux<T> find(Query query, Class<?> entityType, Class<T> returnType) {
		return find(query, entityType, returnType, getIndexCoordinatesFor(entityType));
	}

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @returnm a {@link Flux} emitting matching entities one by one.
	 */
	default <T> Flux<T> find(Query query, Class<T> entityType, IndexCoordinates index) {
		return find(query, entityType, entityType, index);
	}

	/**
	 * Search the index for entities matching the given {@link Query query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param resultType the projection result type.
	 * @param index the target index, must not be {@literal null}
	 * @param <T>
	 * @return a {@link Flux} emitting matching entities one by one.
	 */
	<T> Flux<T> find(Query query, Class<?> entityType, Class<T> resultType, IndexCoordinates index);

	/**
	 * Count the number of documents matching the given {@link Query}.
	 *
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the nr of matching documents.
	 */
	default Mono<Long> count(Class<?> entityType) {
		return count(new StringQuery(QueryBuilders.matchAllQuery().toString()), entityType);
	}

	/**
	 * Count the number of documents matching the given {@link Query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the nr of matching documents.
	 */
	default Mono<Long> count(Query query, Class<?> entityType) {
		return count(query, entityType, getIndexCoordinatesFor(entityType));
	}

	/**
	 * Count the number of documents matching the given {@link Query}.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the nr of matching documents.
	 */
	Mono<Long> count(Query query, Class<?> entityType, IndexCoordinates index);

	/**
	 * Delete the given entity extracting index and type from entity metadata.
	 *
	 * @param entity must not be {@literal null}.
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 */
	default Mono<String> delete(Object entity) {
		return delete(entity, getIndexCoordinatesFor(entity.getClass()));
	}

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
	default Mono<String> deleteById(String id, IndexCoordinates index) {

		Assert.notNull(index, "Index must not be null!");

		return deleteById(id, Object.class, index);
	}

	/**
	 * Delete the entity with given {@literal id} extracting index and type from entity metadata.
	 *
	 * @param id must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 */
	default Mono<String> deleteById(String id, Class<?> entityType) {
		return deleteById(id, entityType, getIndexCoordinatesFor(entityType));
	}

	/**
	 * Delete the entity with given {@literal id} extracting index and type from entity metadata.
	 *
	 * @param id must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the {@literal id} of the removed document.
	 */
	Mono<String> deleteById(String id, Class<?> entityType, IndexCoordinates index);

	/**
	 * Delete the documents matching the given {@link Query} extracting index and type from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@link Mono} emitting the number of the removed documents.
	 */
	default Mono<Long> deleteBy(Query query, Class<?> entityType) {
		return deleteBy(query, entityType, getIndexCoordinatesFor(entityType));
	}

	/**
	 * Delete the documents matching the given {@link Query} extracting index and type from entity metadata.
	 *
	 * @param query must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @param index the target index, must not be {@literal null}
	 * @return a {@link Mono} emitting the number of the removed documents.
	 */
	Mono<Long> deleteBy(Query query, Class<?> entityType, IndexCoordinates index);

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
	default IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		ElasticsearchPersistentEntity entity = getPersistentEntityFor(clazz);
		return IndexCoordinates.of(entity.getIndexName()).withTypes(entity.getIndexType());
	}

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
