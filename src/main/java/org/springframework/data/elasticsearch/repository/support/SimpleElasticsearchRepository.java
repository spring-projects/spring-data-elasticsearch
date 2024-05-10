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
package org.springframework.data.elasticsearch.repository.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Elasticsearch specific repository implementation. Likely to be used as target within
 * {@link ElasticsearchRepositoryFactory}
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ryan Henszey
 * @author Kevin Leturc
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Michael Wirth
 * @author Sascha Woo
 * @author Murali Chevuri
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 * @author Jens Schauder
 */
public class SimpleElasticsearchRepository<T, ID> implements ElasticsearchRepository<T, ID> {

	protected ElasticsearchOperations operations;
	protected IndexOperations indexOperations;

	protected Class<T> entityClass;
	protected ElasticsearchEntityInformation<T, ID> entityInformation;

	public SimpleElasticsearchRepository(ElasticsearchEntityInformation<T, ID> metadata,
			ElasticsearchOperations operations) {
		this.operations = operations;

		Assert.notNull(metadata, "ElasticsearchEntityInformation must not be null!");

		this.entityInformation = metadata;
		this.entityClass = this.entityInformation.getJavaType();
		this.indexOperations = operations.indexOps(this.entityClass);

		if (!"true".equals(System.getenv("SPRING_DATA_ELASTICSEARCH_SKIP_REPOSITORY_INIT"))) {
			createIndexAndMappingIfNeeded();
		}
	}

	public void createIndexAndMappingIfNeeded() {

		if (shouldCreateIndexAndMapping() && !indexOperations.exists()) {
			indexOperations.createWithMapping();
		} else if (shouldAlwaysWriteMapping()) {
			indexOperations.putMapping();
		}
	}

	private boolean shouldCreateIndexAndMapping() {

		final ElasticsearchPersistentEntity<?> entity = operations.getElasticsearchConverter().getMappingContext()
				.getRequiredPersistentEntity(entityClass);
		return entity.isCreateIndexAndMapping();
	}

	private boolean shouldAlwaysWriteMapping() {
		return operations.getElasticsearchConverter().getMappingContext()
				.getRequiredPersistentEntity(entityClass).isAlwaysWriteMapping();
	}

	@Override
	public Optional<T> findById(ID id) {
		return Optional.ofNullable(
				execute(operations -> operations.get(stringIdRepresentation(id), entityClass, getIndexCoordinates())));
	}

	@Override
	public Iterable<T> findAll() {
		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		return this.findAll(PageRequest.of(0, Math.max(1, itemCount)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<T> findAll(Pageable pageable) {

		Assert.notNull(pageable, "pageable must not be null");

		Query query = Query.findAll();
		query.setPageable(pageable);
		SearchHits<T> searchHits = execute(operations -> operations.search(query, entityClass, getIndexCoordinates()));
		SearchPage<T> page = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
		// noinspection ConstantConditions
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<T> findAll(Sort sort) {

		Assert.notNull(sort, "sort must not be null");

		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		Pageable pageable = PageRequest.of(0, itemCount, sort);
		Query query = Query.findAll();
		query.setPageable(pageable);
		List<SearchHit<T>> searchHitList = execute(
				operations -> operations.search(query, entityClass, getIndexCoordinates()).getSearchHits());
		// noinspection ConstantConditions
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Assert.notNull(ids, "ids can't be null.");

		List<String> stringIds = stringIdsRepresentation(ids);
		Query query = getIdQuery(stringIds);
		if (!stringIds.isEmpty()) {
			query.setPageable(PageRequest.of(0, stringIds.size()));
		}
		List<SearchHit<T>> searchHitList = execute(
				operations -> operations.search(query, entityClass, getIndexCoordinates()).getSearchHits());
		// noinspection ConstantConditions
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public long count() {
		Query query = Query.findAll();
		((BaseQuery) query).setMaxResults(0);
		return execute(operations -> operations.count(query, entityClass, getIndexCoordinates()));
	}

	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Cannot save 'null' entity.");

		return executeAndRefresh(operations -> operations.save(entity, getIndexCoordinates()));
	}

	@Override
	public <S extends T> S save(S entity, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(entity, "entity must not be null");

		return executeAndRefresh(operations -> operations.save(entity, getIndexCoordinates()), refreshPolicy);
	}

	public <S extends T> List<S> save(List<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		return Streamable.of(saveAll(entities)).stream().collect(Collectors.toList());
	}

	public <S extends T> List<S> save(List<S> entities, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		return Streamable.of(saveAll(entities, refreshPolicy)).stream().collect(Collectors.toList());
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		executeAndRefresh(operations -> operations.save(entities, indexCoordinates));

		return entities;
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities, @Nullable RefreshPolicy refreshPolicy) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		executeAndRefresh(operations -> operations.save(entities, indexCoordinates), refreshPolicy);

		return entities;
	}

	@Override
	public boolean existsById(ID id) {
		// noinspection ConstantConditions
		return execute(operations -> operations.exists(stringIdRepresentation(id), getIndexCoordinates()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<T> searchSimilar(T entity, @Nullable String[] fields, Pageable pageable) {

		Assert.notNull(entity, "Cannot search similar records for 'null'.");
		Assert.notNull(pageable, "'pageable' cannot be 'null'");

		MoreLikeThisQuery query = new MoreLikeThisQuery();
		query.setId(stringIdRepresentation(extractIdFromBean(entity)));
		query.setPageable(pageable);

		if (fields != null) {
			query.addFields(fields);
		}

		SearchHits<T> searchHits = execute(operations -> operations.search(query, entityClass, getIndexCoordinates()));
		SearchPage<T> searchPage = SearchHitSupport.searchPageFor(searchHits, pageable);
		return (Page<T>) SearchHitSupport.unwrapSearchHits(searchPage);
	}

	@Override
	public void deleteById(ID id) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, null, getIndexCoordinates());
	}

	@Override
	public void deleteById(ID id, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, null, getIndexCoordinates(), refreshPolicy);
	}

	@Override
	public void delete(T entity) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		doDelete(extractIdFromBean(entity), operations.getEntityRouting(entity), getIndexCoordinates());
	}

	@Override
	public void delete(T entity, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		doDelete(extractIdFromBean(entity), operations.getEntityRouting(entity), getIndexCoordinates(), refreshPolicy);
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {

		// noinspection DuplicatedCode
		Assert.notNull(ids, "Cannot delete 'null' list.");

		List<String> idStrings = new ArrayList<>();
		for (ID id : ids) {
			idStrings.add(stringIdRepresentation(id));
		}

		if (idStrings.isEmpty()) {
			return;
		}

		Query query = operations.idsQuery(idStrings);
		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(DeleteQuery.builder(query).build(), entityClass, getIndexCoordinates());
			return null;
		});
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids, @Nullable RefreshPolicy refreshPolicy) {

		// noinspection DuplicatedCode
		Assert.notNull(ids, "Cannot delete 'null' list.");

		List<String> idStrings = new ArrayList<>();
		for (ID id : ids) {
			idStrings.add(stringIdRepresentation(id));
		}

		if (idStrings.isEmpty()) {
			return;
		}

		Query query = operations.idsQuery(idStrings);
		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(DeleteQuery.builder(query).build(), entityClass, getIndexCoordinates());
			return null;
		});
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		deleteAllById(getEntityIds(entities));
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities, @Nullable RefreshPolicy refreshPolicy) {
		deleteAllById(getEntityIds(entities), refreshPolicy);
	}

	@NotNull
	private List<ID> getEntityIds(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Cannot delete 'null' list.");

		List<ID> ids = new ArrayList<>();
		for (T entity : entities) {
			ID id = extractIdFromBean(entity);
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}

	private void doDelete(@Nullable ID id, @Nullable String routing, IndexCoordinates indexCoordinates) {

		if (id != null) {
			executeAndRefresh(operations -> {
				var ops = routing != null ? operations.withRouting(RoutingResolver.just(routing)) : operations;
				return ops.delete(stringIdRepresentation(id), indexCoordinates);
			});
		}
	}

	private void doDelete(@Nullable ID id, @Nullable String routing, IndexCoordinates indexCoordinates,
			@Nullable RefreshPolicy refreshPolicy) {

		if (id != null) {
			executeAndRefresh(operations -> {
				var ops = routing != null ? operations.withRouting(RoutingResolver.just(routing)) : operations;
				return ops.delete(stringIdRepresentation(id), indexCoordinates);
			}, refreshPolicy);
		}
	}

	@Override
	public void deleteAll() {

		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(Query.findAll(), entityClass, getIndexCoordinates());
			return null;
		});
	}

	@Override
	public void deleteAll(@Nullable RefreshPolicy refreshPolicy) {
		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(DeleteQuery.builder(Query.findAll()).build(), entityClass, getIndexCoordinates());
			return null;
		}, refreshPolicy);
	}

	private void doRefresh() {
		RefreshPolicy refreshPolicy = null;

		if (operations instanceof AbstractElasticsearchTemplate template) {
			refreshPolicy = template.getRefreshPolicy();
		}

		if (refreshPolicy == null) {
			indexOperations.refresh();
		}
	}

	// region helper functions
	@Nullable
	protected ID extractIdFromBean(T entity) {
		return entityInformation.getId(entity);
	}

	private List<String> stringIdsRepresentation(Iterable<? extends ID> ids) {

		Assert.notNull(ids, "ids can't be null.");

		return StreamUtils.createStreamFromIterator(ids.iterator()).map(this::stringIdRepresentation)
				.collect(Collectors.toList());
	}

	protected @Nullable String stringIdRepresentation(@Nullable ID id) {
		return operations.convertId(id);
	}

	private IndexCoordinates getIndexCoordinates() {
		return operations.getIndexCoordinatesFor(entityClass);
	}

	private Query getIdQuery(List<String> stringIds) {
		return operations.idsQuery(stringIds);
	}
	// endregion

	// region operations callback
	@FunctionalInterface
	public interface OperationsCallback<R> {
		@Nullable
		R doWithOperations(ElasticsearchOperations operations);
	}

	@Nullable
	public <R> R execute(OperationsCallback<R> callback) {
		return callback.doWithOperations(operations);
	}

	@Nullable
	public <R> R executeAndRefresh(OperationsCallback<R> callback) {
		R result = callback.doWithOperations(operations);
		doRefresh();
		return result;
	}

	@Nullable
	public <R> R executeAndRefresh(OperationsCallback<R> callback, @Nullable RefreshPolicy refreshPolicy) {
        return callback.doWithOperations(operations.withRefreshPolicy(refreshPolicy));
	}
	// endregion
}
