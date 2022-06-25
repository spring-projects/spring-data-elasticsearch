/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.client.erhlc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 * @author Jens Schauder
 * @since 3.2
 */
public class SimpleReactiveElasticsearchRepository<T, ID> implements ReactiveElasticsearchRepository<T, ID> {

	private final ElasticsearchEntityInformation<T, ID> entityInformation;
	private final ReactiveElasticsearchOperations operations;
	private final ReactiveIndexOperations indexOperations;

	public SimpleReactiveElasticsearchRepository(ElasticsearchEntityInformation<T, ID> entityInformation,
			ReactiveElasticsearchOperations operations) {

		Assert.notNull(entityInformation, "EntityInformation must not be null!");
		Assert.notNull(operations, "ElasticsearchOperations must not be null!");

		this.entityInformation = entityInformation;
		this.operations = operations;
		this.indexOperations = operations.indexOps(entityInformation.getJavaType());

		createIndexAndMappingIfNeeded();
	}

	private void createIndexAndMappingIfNeeded() {

		if (shouldCreateIndexAndMapping()) {
			indexOperations.exists() //
					.flatMap(exists -> exists ? Mono.empty() : indexOperations.createWithMapping()) //
					.block();
		}
	}

	private boolean shouldCreateIndexAndMapping() {

		final ElasticsearchPersistentEntity<?> entity = operations.getElasticsearchConverter().getMappingContext()
				.getRequiredPersistentEntity(entityInformation.getJavaType());
		return entity.isCreateIndexAndMapping();
	}

	@Override
	public <S extends T> Mono<S> save(S entity) {

		Assert.notNull(entity, "Entity must not be null!");

		return operations.save(entity, entityInformation.getIndexCoordinates())
				.flatMap(saved -> doRefresh().thenReturn(saved));
	}

	@Override
	public <S extends T> Flux<S> saveAll(Iterable<S> entities) {

		Assert.notNull(entities, "Entities must not be null!");
		return saveAll(Flux.fromIterable(entities));
	}

	@Override
	public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {

		Assert.notNull(entityStream, "EntityStream must not be null!");

		return operations.saveAll(Flux.from(entityStream).collectList(), entityInformation.getIndexCoordinates())
				.concatWith(doRefresh().then(Mono.empty()));
	}

	@Override
	public Mono<T> findById(ID id) {

		Assert.notNull(id, "Id must not be null!");
		return operations.get(convertId(id), entityInformation.getJavaType(), entityInformation.getIndexCoordinates());
	}

	@Override
	public Mono<T> findById(Publisher<ID> id) {

		Assert.notNull(id, "Id must not be null!");
		return Mono.from(id).flatMap(this::findById);
	}

	@Override
	public Mono<Boolean> existsById(ID id) {

		Assert.notNull(id, "Id must not be null!");
		return operations.exists(convertId(id), entityInformation.getIndexCoordinates());
	}

	@Override
	public Mono<Boolean> existsById(Publisher<ID> id) {

		Assert.notNull(id, "Id must not be null!");
		return Mono.from(id).flatMap(this::existsById);
	}

	@Override
	public Flux<T> findAll() {

		return operations.search(Query.findAll().setPageable(Pageable.unpaged()), entityInformation.getJavaType(),
				entityInformation.getIndexCoordinates()).map(SearchHit::getContent);
	}

	@Override
	public Flux<T> findAll(Sort sort) {

		return operations.search(Query.findAll().addSort(sort).setPageable(Pageable.unpaged()),
				entityInformation.getJavaType(), entityInformation.getIndexCoordinates()).map(SearchHit::getContent);
	}

	@Override
	public Flux<T> findAllById(Iterable<ID> ids) {

		Assert.notNull(ids, "Ids must not be null!");

		return findAllById(Flux.fromIterable(ids));
	}

	@Override
	public Flux<T> findAllById(Publisher<ID> idStream) {

		Assert.notNull(idStream, "IdStream must not be null!");
		return Flux.from(idStream) //
				.map(this::convertId) //
				.collectList() //
				.map(ids -> new NativeSearchQueryBuilder().withIds(ids).build()) //
				.flatMapMany(query -> {
					IndexCoordinates index = entityInformation.getIndexCoordinates();
					return operations.multiGet(query, entityInformation.getJavaType(), index) //
							.filter(MultiGetItem::hasItem) //
							.map(MultiGetItem::getItem);
				});
	}

	@Override
	public Mono<Long> count() {

		return operations.count(Query.findAll(), entityInformation.getJavaType(), entityInformation.getIndexCoordinates());
	}

	@Override
	public Mono<Void> deleteById(ID id) {

		Assert.notNull(id, "Id must not be null!");
		return operations.delete(convertId(id), entityInformation.getIndexCoordinates()) //
				.then(doRefresh());
	}

	@Override
	public Mono<Void> deleteById(Publisher<ID> id) {

		Assert.notNull(id, "Id must not be null!");
		return Mono.from(id).flatMap(this::deleteById);
	}

	@Override
	public Mono<Void> delete(T entity) {

		Assert.notNull(entity, "Entity must not be null!");
		return operations.delete(entity, entityInformation.getIndexCoordinates()) //
				.then(doRefresh());
	}

	@Override
	public Mono<Void> deleteAllById(Iterable<? extends ID> ids) {

		Assert.notNull(ids, "Ids must not be null!");

		return Flux.fromIterable(ids) //
				.map(this::convertId) //
				.collectList() //
				.map(idList -> operations.idsQuery(idList)) //
				.flatMap(
						query -> operations.delete(query, entityInformation.getJavaType(), entityInformation.getIndexCoordinates())) //
				.then(doRefresh());
	}

	@Override
	public Mono<Void> deleteAll(Iterable<? extends T> entities) {

		Assert.notNull(entities, "Entities must not be null!");
		return deleteAll(Flux.fromIterable(entities));
	}

	@Override
	public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {

		Assert.notNull(entityStream, "EntityStream must not be null!");
		return Flux.from(entityStream) //
				.map(entityInformation::getRequiredId) //
				.map(this::convertId) //
				.collectList() //
				.map(idList -> operations.idsQuery(idList))
				.flatMap(
						query -> operations.delete(query, entityInformation.getJavaType(), entityInformation.getIndexCoordinates())) //
				.then(doRefresh());
	}

	@Override
	public Mono<Void> deleteAll() {

		return operations.delete(Query.findAll(), entityInformation.getJavaType(), entityInformation.getIndexCoordinates()) //
				.then(doRefresh());
	}

	private String convertId(Object id) {
		return operations.getElasticsearchConverter().convertId(id);
	}

	private Mono<Void> doRefresh() {
		RefreshPolicy refreshPolicy = null;

		if (operations instanceof ReactiveElasticsearchTemplate) {
			refreshPolicy = ((ReactiveElasticsearchTemplate) operations).getRefreshPolicy();
		}

		if (refreshPolicy == null) {
			return indexOperations.refresh();
		}

		return Mono.empty();
	}
}
