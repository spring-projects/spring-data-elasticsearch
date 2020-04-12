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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
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
 */
public abstract class AbstractElasticsearchRepository<T, ID> implements ElasticsearchRepository<T, ID> {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticsearchRepository.class);

	protected ElasticsearchOperations operations;
	protected IndexOperations indexOperations;

	protected Class<T> entityClass;
	protected @Nullable ElasticsearchEntityInformation<T, ID> entityInformation;

	public AbstractElasticsearchRepository(ElasticsearchEntityInformation<T, ID> metadata,
			ElasticsearchOperations operations) {
		this.operations = operations;

		Assert.notNull(metadata, "ElasticsearchEntityInformation must not be null!");

		this.entityInformation = metadata;
		this.entityClass = this.entityInformation.getJavaType();
		this.indexOperations = operations.indexOps(this.entityClass);
		try {
			if (createIndexAndMapping()) {
				createIndex();
				putMapping();
			}
		} catch (Exception exception) {
			LOGGER.warn("Cannot create index: {}", exception.getMessage());
		}
	}

	private void createIndex() {
		indexOperations.create();
	}

	private void putMapping() {
		indexOperations.putMapping(indexOperations.createMapping(entityClass));
	}

	private boolean createIndexAndMapping() {

		final ElasticsearchPersistentEntity<?> entity = operations.getElasticsearchConverter().getMappingContext()
				.getRequiredPersistentEntity(getEntityClass());
		return entity.isCreateIndexAndMapping();
	}

	@Override
	public Optional<T> findById(ID id) {
		return Optional.ofNullable(operations.get(stringIdRepresentation(id), getEntityClass(), getIndexCoordinates()));
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
		SearchHits<T> searchHits = operations.search(query, getEntityClass(), getIndexCoordinates());
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
		List<SearchHit<T>> searchHitList = operations.search(query, getEntityClass(), getIndexCoordinates())
				.getSearchHits();
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {
		Assert.notNull(ids, "ids can't be null.");
		NativeSearchQuery query = new NativeSearchQueryBuilder().withIds(stringIdsRepresentation(ids)).build();
		return operations.multiGet(query, getEntityClass(), getIndexCoordinates());
	}

	@Override
	public long count() {
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		return operations.count(query, getEntityClass(), getIndexCoordinates());
	}

	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Cannot save 'null' entity.");

		operations.save(entity, getIndexCoordinates());
		operations.indexOps(entity.getClass()).refresh();
		return entity;
	}

	public <S extends T> List<S> save(List<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		return Streamable.of(saveAll(entities)).stream().collect(Collectors.toList());
	}

	@Override
	@Deprecated
	public <S extends T> S indexWithoutRefresh(S entity) {
		Assert.notNull(entity, "Cannot save 'null' entity.");
		operations.save(entity);
		return entity;
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		operations.save(entities, indexCoordinates);
		operations.indexOps(indexCoordinates).refresh();

		return entities;
	}

	@Override
	public boolean existsById(ID id) {
		return operations.exists(stringIdRepresentation(id), getIndexCoordinates());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<T> search(QueryBuilder query) {
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).build();
		int count = (int) operations.count(searchQuery, getEntityClass(), getIndexCoordinates());

		if (count == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		searchQuery.setPageable(PageRequest.of(0, count));
		SearchHits<T> searchHits = operations.search(searchQuery, getEntityClass(), getIndexCoordinates());
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, searchQuery.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<T> search(QueryBuilder query, Pageable pageable) {
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).withPageable(pageable).build();
		SearchHits<T> searchHits = operations.search(searchQuery, getEntityClass(), getIndexCoordinates());
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, searchQuery.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<T> search(Query query) {
		SearchHits<T> searchHits = operations.search(query, getEntityClass(), getIndexCoordinates());
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

		SearchHits<T> searchHits = operations.search(query, getEntityClass(), getIndexCoordinates());
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, pageable);
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@Override
	public void deleteById(ID id) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		doDelete(id, indexCoordinates);
		indexOperations.refresh();
	}

	@Override
	public void delete(T entity) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		doDelete(extractIdFromBean(entity), indexCoordinates);
		indexOperations.refresh();
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {

		Assert.notNull(entities, "Cannot delete 'null' list.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		IdsQueryBuilder idsQueryBuilder = idsQuery();
		for (T entity : entities) {
			ID id = extractIdFromBean(entity);
			if (id != null) {
				idsQueryBuilder.addIds(stringIdRepresentation(id));
			}
		}

		if (idsQueryBuilder.ids().isEmpty()) {
			return;
		}

		Query query = new NativeSearchQueryBuilder().withQuery(idsQueryBuilder).build();

		operations.delete(query, getEntityClass(), indexCoordinates);
		indexOperations.refresh();
	}

	private void doDelete(@Nullable ID id, IndexCoordinates indexCoordinates) {
		if (id != null) {
			operations.delete(stringIdRepresentation(id), indexCoordinates);
		}
	}

	@Override
	public void deleteAll() {
		IndexCoordinates indexCoordinates = getIndexCoordinates();
		Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();

		operations.delete(query, getEntityClass(), indexCoordinates);
		indexOperations.refresh();
	}

	@Override
	public void refresh() {
		indexOperations.refresh();
	}

	@SuppressWarnings("unchecked")
	private Class<T> resolveReturnedClassFromGenericType() {
		ParameterizedType parameterizedType = resolveReturnedClassFromGenericType(getClass());
		return (Class<T>) parameterizedType.getActualTypeArguments()[0];
	}

	private ParameterizedType resolveReturnedClassFromGenericType(Class<?> clazz) {
		Object genericSuperclass = clazz.getGenericSuperclass();

		if (genericSuperclass instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
			Type rawtype = parameterizedType.getRawType();
			if (SimpleElasticsearchRepository.class.equals(rawtype)) {
				return parameterizedType;
			}
		}

		return resolveReturnedClassFromGenericType(clazz.getSuperclass());
	}

	protected Class<T> getEntityClass() {

		if (!isEntityClassSet()) {
			try {
				this.entityClass = resolveReturnedClassFromGenericType();
			} catch (Exception e) {
				throw new InvalidDataAccessApiUsageException("Unable to resolve EntityClass. Please use according setter!", e);
			}
		}
		return entityClass;
	}

	private boolean isEntityClassSet() {
		return entityClass != null;
	}

	@Nullable
	protected ID extractIdFromBean(T entity) {
		return entityInformation.getId(entity);
	}

	private List<String> stringIdsRepresentation(Iterable<ID> ids) {
		Assert.notNull(ids, "ids can't be null.");
		List<String> stringIds = new ArrayList<>();
		for (ID id : ids) {
			stringIds.add(stringIdRepresentation(id));
		}

		return stringIds;
	}

	protected abstract @Nullable String stringIdRepresentation(@Nullable ID id);

	private IndexCoordinates getIndexCoordinates() {
		return operations.getIndexCoordinatesFor(getEntityClass());
	}
}
