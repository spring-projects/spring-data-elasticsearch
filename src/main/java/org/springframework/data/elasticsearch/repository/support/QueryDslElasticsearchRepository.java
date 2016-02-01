/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.support;

import java.io.Serializable;

import com.querydsl.elasticsearch.ElasticsearchQuery;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.util.Assert;

/**
 * Special QueryDsl based repository implementation that allows execution {@link Predicate}s in various forms.
 *
 * @author Kevin Leturc
 */
public class QueryDslElasticsearchRepository<T, ID extends Serializable> extends AbstractElasticsearchRepository<T, ID>
        implements QueryDslPredicateExecutor<T> {

    private final PathBuilder<T> builder;

    /**
     * Creates a new {@link QueryDslElasticsearchRepository} for the given {@link ElasticsearchEntityInformation} and {@link ElasticsearchOperations}. Uses
     * the {@link org.springframework.data.querydsl.SimpleEntityPathResolver} to create an {@link com.querydsl.core.types.EntityPath} for the given domain class.
     *
     * @param entityInformation The elasticsearch entity information.
     * @param elasticsearchOperations The elasticsearch operations.
     */
    public QueryDslElasticsearchRepository(ElasticsearchEntityInformation<T, ID> entityInformation, ElasticsearchOperations elasticsearchOperations) {
        this(entityInformation, elasticsearchOperations, SimpleEntityPathResolver.INSTANCE);
    }

    /**
     * Creates a new {@link QueryDslElasticsearchRepository} for the given {@link ElasticsearchEntityInformation}, {@link ElasticsearchOperations}
     * and {@link org.springframework.data.querydsl.EntityPathResolver}.
     *
     * @param entityInformation The elasticsearch entity information.
     * @param elasticsearchOperations The elasticsearch operations.
     * @param resolver The query dsl path resolver.
     */
    public QueryDslElasticsearchRepository(ElasticsearchEntityInformation<T, ID> entityInformation, ElasticsearchOperations elasticsearchOperations,
                                   EntityPathResolver resolver) {

        super(entityInformation, elasticsearchOperations);
        Assert.notNull(resolver);
        EntityPath<T> path = resolver.createPath(entityInformation.getJavaType());
        this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findOne(com.querydsl.core.types.Predicate)
     */
    @Override
    public T findOne(Predicate predicate) {
        return createQueryFor(predicate).fetchFirst();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate)
     */
    @Override
    public Iterable<T> findAll(Predicate predicate) {
        return createQueryFor(predicate).fetch();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, org.springframework.data.domain.Sort)
     */
    @Override
    public Iterable<T> findAll(Predicate predicate, Sort sort) {
        return applySorting(createQueryFor(predicate), sort).fetch();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, com.querydsl.core.types.OrderSpecifier<?>[])
     */
    @Override
    public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
        return createQueryFor(predicate).orderBy(orders).fetch();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.OrderSpecifier<?>[])
     */
    @Override
    public Iterable<T> findAll(OrderSpecifier<?>... orders) {
        return createQuery().orderBy(orders).fetch();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.querydsl.core.types.Predicate, org.springframework.data.domain.Pageable)
     */
    @Override
    public Page<T> findAll(Predicate predicate, Pageable pageable) {
        ElasticsearchQuery<T> countQuery = createQueryFor(predicate);
        ElasticsearchQuery<T> query = createQueryFor(predicate);

        return new PageImpl<T>(applyPagination(query, pageable).fetch(), pageable, countQuery.fetchCount());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#count(com.querydsl.core.types.Predicate)
     */
    @Override
    public long count(Predicate predicate) {
        return createQueryFor(predicate).fetchCount();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#exists(com.querydsl.core.types.Predicate)
     */
    @Override
    public boolean exists(Predicate predicate) {
        return createQueryFor(predicate).fetchCount() > 0;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#stringIdRepresentation(ID)
     */
    @Override
    protected String stringIdRepresentation(ID id) {
        // Handle String and Number ids
        return String.valueOf(id);
    }

    /**
     * Creates an {@link ElasticsearchQuery} for the given {@link Predicate}.
     *
     * @param predicate The predicate.
     * @return The querydsl query.
     */
    private ElasticsearchQuery<T> createQueryFor(Predicate predicate) {
        return createQuery().where(predicate);
    }

    /**
     * Creates an {@link ElasticsearchQuery}.
     *ate.
     * @return The querydsl query.
     */
    private ElasticsearchQuery<T> createQuery() {
        return new SpringDataElasticsearchQuery<T, ID>(elasticsearchOperations, entityInformation);
    }

    /**
     * Applies the given {@link Pageable} to the given {@link ElasticsearchQuery}.
     *
     * @param query The query to apply pagination.
     * @param pageable The pageable to apply.
     * @return The querydsl query.
     */
    private ElasticsearchQuery<T> applyPagination(ElasticsearchQuery<T> query, Pageable pageable) {

        if (pageable == null) {
            return query;
        }

        query = query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        return applySorting(query, pageable.getSort());
    }

    /**
     * Applies the given {@link Sort} to the given {@link ElasticsearchQuery}.
     *
     * @param query The query to apply sort.
     * @param sort The sort to appy.
     * @return The querydsl query.
     */
    private ElasticsearchQuery<T> applySorting(ElasticsearchQuery<T> query, Sort sort) {

        if (sort == null) {
            return query;
        }

        for (Sort.Order order : sort) {
            query.orderBy(toOrder(order));
        }

        return query;
    }

    /**
     * Transforms a plain {@link Sort.Order} into a QueryDsl specific {@link OrderSpecifier}.
     *
     * @param order The order to convert.
     * @return The querydsl order.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private OrderSpecifier<?> toOrder(Sort.Order order) {

        Expression<Object> property = builder.get(order.getProperty());

        return new OrderSpecifier(order.isAscending() ? com.querydsl.core.types.Order.ASC
                : com.querydsl.core.types.Order.DESC, property);
    }
}
