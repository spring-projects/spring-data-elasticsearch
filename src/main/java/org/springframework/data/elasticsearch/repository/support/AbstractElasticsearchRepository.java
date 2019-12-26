/*
 * Copyright 2013-2019 the original author or authors.
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

import org.elasticsearch.ElasticsearchException;
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
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.util.Streamable;
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
 */
public abstract class AbstractElasticsearchRepository<T, ID> implements ElasticsearchRepository<T, ID> {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticsearchRepository.class);

	protected ElasticsearchOperations operations;
	protected IndexOperations indexOperations;

	protected Class<T> entityClass;
	protected ElasticsearchEntityInformation<T, ID> entityInformation;

	public AbstractElasticsearchRepository() {}

	public AbstractElasticsearchRepository(ElasticsearchOperations operations) {
		Assert.notNull(operations, "ElasticsearchOperations must not be null.");
		this.operations = operations;
		this.indexOperations = operations.getIndexOperations();
	}

	public AbstractElasticsearchRepository(ElasticsearchEntityInformation<T, ID> metadata,
			ElasticsearchOperations operations) {
		this(operations);

		Assert.notNull(metadata, "ElasticsearchEntityInformation must not be null!");

		this.entityInformation = metadata;
		setEntityClass(this.entityInformation.getJavaType());
		try {
			if (createIndexAndMapping()) {
				createIndex();
				putMapping();
			}
		} catch (ElasticsearchException exception) {
			LOGGER.error("failed to load elasticsearch nodes : {}", exception.getDetailedMessage());
		}
	}

	private void createIndex() {
		indexOperations.createIndex(getEntityClass());
	}

	private void putMapping() {
		indexOperations.putMapping(getEntityClass());
	}

	private boolean createIndexAndMapping() {

		final ElasticsearchPersistentEntity<?> entity = operations.getElasticsearchConverter().getMappingContext()
				.getRequiredPersistentEntity(getEntityClass());
		return entity.isCreateIndexAndMapping();
	}

	@Override
	public Optional<T> findById(ID id) {
		GetQuery query = new GetQuery();
		query.setId(stringIdRepresentation(id));
		return Optional.ofNullable(operations.get(query, getEntityClass(), getIndexCoordinates()));
	}

	@Override
	public Iterable<T> findAll() {
		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.<T> emptyList());
		}
		return this.findAll(PageRequest.of(0, Math.max(1, itemCount)));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withPageable(pageable).build();
		SearchHits<T> searchHits = operations.search(query, getEntityClass(), getIndexCoordinates());
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, query.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.<T> emptyList());
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
		operations.index(createIndexQuery(entity), getIndexCoordinates());
		indexOperations.refresh(getIndexCoordinates());
		return entity;
	}

	public <S extends T> List<S> save(List<S> entities) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");
		return Streamable.of(saveAll(entities)).stream().collect(Collectors.toList());
	}

	@Override
	public <S extends T> S index(S entity) {
		return save(entity);
	}

	@Override
	public <S extends T> S indexWithoutRefresh(S entity) {
		Assert.notNull(entity, "Cannot save 'null' entity.");
		operations.index(createIndexQuery(entity), getIndexCoordinates());
		return entity;
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");
		List<IndexQuery> queries = Streamable.of(entities).stream().map(this::createIndexQuery)
				.collect(Collectors.toList());

		if (!queries.isEmpty()) {
			operations.bulkIndex(queries, getIndexCoordinates());
			indexOperations.refresh(getIndexCoordinates());
		}

		return entities;
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public Iterable<T> search(QueryBuilder query) {
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).build();
		int count = (int) operations.count(searchQuery, getEntityClass(), getIndexCoordinates());

		if (count == 0) {
			return new PageImpl<>(Collections.<T> emptyList());
		}
		searchQuery.setPageable(PageRequest.of(0, count));
		SearchHits<T> searchHits = operations.search(searchQuery, getEntityClass(), getIndexCoordinates());
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, searchQuery.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@Override
	public Page<T> search(QueryBuilder query, Pageable pageable) {
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).withPageable(pageable).build();
		SearchHits<T> searchHits = operations.search(searchQuery, getEntityClass(), getIndexCoordinates());
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, searchQuery.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@Override
	public Page<T> search(Query query) {
		SearchHits<T> searchHits = operations.search(query, getEntityClass(), getIndexCoordinates());
		AggregatedPage<SearchHit<T>> page = SearchHitSupport.page(searchHits, query.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@Override
	public Page<T> searchSimilar(T entity, String[] fields, Pageable pageable) {
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
		operations.delete(stringIdRepresentation(id), indexCoordinates);
		indexOperations.refresh(indexCoordinates);
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity, "Cannot delete 'null' entity.");
		deleteById(extractIdFromBean(entity));
		indexOperations.refresh(getIndexCoordinates());
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Cannot delete 'null' list.");
		for (T entity : entities) {
			delete(entity);
		}
	}

	@Override
	public void deleteAll() {
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(matchAllQuery());
		IndexCoordinates indexCoordinates = getIndexCoordinates();
		operations.delete(deleteQuery, indexCoordinates);
		indexOperations.refresh(indexCoordinates);
	}

	@Override
	public void refresh() {
		indexOperations.refresh(getEntityClass());
	}

	private IndexQuery createIndexQuery(T entity) {
		IndexQuery query = new IndexQuery();
		query.setObject(entity);
		query.setId(stringIdRepresentation(extractIdFromBean(entity)));
		query.setVersion(extractVersionFromBean(entity));
		query.setParentId(extractParentIdFromBean(entity));
		return query;
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

	@Override
	public Class<T> getEntityClass() {

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

	public final void setEntityClass(Class<T> entityClass) {
		Assert.notNull(entityClass, "EntityClass must not be null.");
		this.entityClass = entityClass;
	}

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

	protected abstract String stringIdRepresentation(ID id);

	private Long extractVersionFromBean(T entity) {
		return entityInformation.getVersion(entity);
	}

	private String extractParentIdFromBean(T entity) {
		return entityInformation.getParentId(entity);
	}

	private IndexCoordinates getIndexCoordinates() {
		return operations.getIndexCoordinatesFor(getEntityClass());
	}
}
