/*
 * Copyright 2013-2020 the original author or authors.
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

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleElasticsearchRepository.class);

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

		try {
			if (shouldCreateIndexAndMapping() && !indexOperations.exists()) {
				indexOperations.create();
				indexOperations.putMapping(entityClass);
			}
		} catch (Exception exception) {
			LOGGER.warn("Cannot create index: {}", exception.getMessage());
		}
	}

	private boolean shouldCreateIndexAndMapping() {

		final ElasticsearchPersistentEntity<?> entity = operations.getElasticsearchConverter().getMappingContext()
				.getRequiredPersistentEntity(entityClass);
		return entity.isCreateIndexAndMapping();
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
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withPageable(pageable).build();
		SearchHits<T> searchHits = execute(operations -> operations.search(query, entityClass, getIndexCoordinates()));
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, query.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<T> findAll(Sort sort) {
		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPageable(PageRequest.of(0, itemCount, sort)).build();
		List<SearchHit<T>> searchHitList = execute(
				operations -> operations.search(query, entityClass, getIndexCoordinates()).getSearchHits());
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Assert.notNull(ids, "ids can't be null.");

		List<T> result = new ArrayList<>();
		Query idQuery = getIdQuery(ids);
		if (CollectionUtils.isEmpty(idQuery.getIds())) {
			return result;
		}

		List<T> multiGetEntities = execute(operations -> operations.multiGet(idQuery, entityClass, getIndexCoordinates()));

		if (multiGetEntities != null) {
			multiGetEntities.forEach(entity -> {

				if (entity != null) {
					result.add(entity);
				}
			});
		}

		return result;
	}

	@Override
	public long count() {
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		// noinspection ConstantConditions
		return execute(operations -> operations.count(query, entityClass, getIndexCoordinates()));
	}

	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Cannot save 'null' entity.");

		// noinspection ConstantConditions
		return executeAndRefresh(operations -> operations.save(entity, getIndexCoordinates()));
	}

	public <S extends T> List<S> save(List<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		return Streamable.of(saveAll(entities)).stream().collect(Collectors.toList());
	}

	@Override
	@Deprecated
	public <S extends T> S indexWithoutRefresh(S entity) {
		Assert.notNull(entity, "Cannot save 'null' entity.");
		// noinspection ConstantConditions
		return execute(operations -> operations.save(entity));
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		executeAndRefresh(operations -> operations.save(entities, indexCoordinates));

		return entities;
	}

	@Override
	public boolean existsById(ID id) {
		// noinspection ConstantConditions
		return execute(operations -> operations.exists(stringIdRepresentation(id), getIndexCoordinates()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<T> search(QueryBuilder query) {
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).build();
		long count = execute(operations -> operations.count(searchQuery, entityClass, getIndexCoordinates()));

		if (count == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		searchQuery.setPageable(PageRequest.of(0, (int) count));
		SearchHits<T> searchHits = execute(
				operations -> operations.search(searchQuery, entityClass, getIndexCoordinates()));
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, searchQuery.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<T> search(QueryBuilder query, Pageable pageable) {
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).withPageable(pageable).build();
		SearchHits<T> searchHits = execute(
				operations -> operations.search(searchQuery, entityClass, getIndexCoordinates()));
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, searchQuery.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<T> search(Query query) {
		SearchHits<T> searchHits = execute(operations -> operations.search(query, entityClass, getIndexCoordinates()));
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, query.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
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
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, pageable);
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@Override
	public void deleteById(ID id) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, null, getIndexCoordinates());
	}

	@Override
	public void delete(T entity) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		doDelete(extractIdFromBean(entity), operations.getEntityRouting(entity), getIndexCoordinates());
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {

		Assert.notNull(ids, "Cannot delete 'null' list.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		IdsQueryBuilder idsQueryBuilder = idsQuery();
		for (ID id : ids) {
			idsQueryBuilder.addIds(stringIdRepresentation(id));
		}

		if (idsQueryBuilder.ids().isEmpty()) {
			return;
		}

		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(new NativeSearchQueryBuilder().withQuery(idsQueryBuilder).build(), entityClass,
					indexCoordinates);
			return null;
		});
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {

		Assert.notNull(entities, "Cannot delete 'null' list.");

		List<ID> ids = new ArrayList<>();
		for (T entity : entities) {
			ID id = extractIdFromBean(entity);
			if (id != null) {
				ids.add(id);
			}
		}

		deleteAllById(ids);
	}

	private void doDelete(@Nullable ID id, @Nullable String routing, IndexCoordinates indexCoordinates) {

		if (id != null) {
			executeAndRefresh(operations -> operations.delete(stringIdRepresentation(id), routing, indexCoordinates));
		}
	}

	@Override
	public void deleteAll() {
		IndexCoordinates indexCoordinates = getIndexCoordinates();
		Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();

		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(query, entityClass, indexCoordinates);
			return null;
		});
	}

	@Override
	public void refresh() {
		indexOperations.refresh();
	}

	@Nullable
	protected ID extractIdFromBean(T entity) {
		return entityInformation.getId(entity);
	}

	private List<String> stringIdsRepresentation(Iterable<? extends ID> ids) {

		Assert.notNull(ids, "ids can't be null.");

		return StreamUtils.createStreamFromIterator(ids.iterator()).map(id -> stringIdRepresentation(id))
				.collect(Collectors.toList());
	}

	protected @Nullable String stringIdRepresentation(@Nullable ID id) {
		return operations.stringIdRepresentation(id);
	}

	private IndexCoordinates getIndexCoordinates() {
		return operations.getIndexCoordinatesFor(entityClass);
	}

	private Query getIdQuery(Iterable<? extends ID> ids) {
		List<String> stringIds = stringIdsRepresentation(ids);

		return new NativeSearchQueryBuilder().withIds(stringIds).build();
	}

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
		refresh();
		return result;
	}
	// endregion
}
