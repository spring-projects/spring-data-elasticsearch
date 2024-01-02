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
package org.springframework.data.elasticsearch.repository.query.parser;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Iterator;

/**
 * ElasticsearchQueryCreator
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 * @author Junghoon Ban
 */
public class ElasticsearchQueryCreator extends AbstractQueryCreator<CriteriaQuery, CriteriaQuery> {

    private final MappingContext<?, ElasticsearchPersistentProperty> context;

    public ElasticsearchQueryCreator(PartTree tree, ParameterAccessor parameters,
                                     MappingContext<?, ElasticsearchPersistentProperty> context) {
        super(tree, parameters);
        this.context = context;
    }

    public ElasticsearchQueryCreator(PartTree tree, MappingContext<?, ElasticsearchPersistentProperty> context) {
        super(tree);
        this.context = context;
    }

    @Override
    protected CriteriaQuery create(Part part, Iterator<Object> iterator) {
        PersistentPropertyPath<ElasticsearchPersistentProperty> path = context.getPersistentPropertyPath(
                part.getProperty());
        return new CriteriaQuery(from(part,
                new Criteria(path.toDotPath(ElasticsearchPersistentProperty.QueryPropertyToFieldNameConverter.INSTANCE)),
                iterator));
    }

    @Override
    protected CriteriaQuery and(Part part, CriteriaQuery base, Iterator<Object> iterator) {
        if (base == null) {
            return create(part, iterator);
        }
        PersistentPropertyPath<ElasticsearchPersistentProperty> path = context.getPersistentPropertyPath(
                part.getProperty());
        return base.addCriteria(from(part,
                new Criteria(path.toDotPath(ElasticsearchPersistentProperty.QueryPropertyToFieldNameConverter.INSTANCE)),
                iterator));
    }

    @Override
    protected CriteriaQuery or(CriteriaQuery base, CriteriaQuery query) {
        return new CriteriaQuery(base.getCriteria().or(query.getCriteria()));
    }

    @Override
    protected CriteriaQuery complete(@Nullable CriteriaQuery query, Sort sort) {

        if (query == null) {
            // this is the case in a findAllByOrderByField method, add empty criteria
            query = new CriteriaQuery(new Criteria());
        }
        return query.addSort(sort);
    }

    private Criteria from(Part part, Criteria criteria, Iterator<?> parameters) {

        Part.Type type = part.getType();

        return switch (type) {
            case TRUE -> criteria.is(true);
            case FALSE -> criteria.is(false);
            case NEGATING_SIMPLE_PROPERTY -> criteria.is(parameters.next()).not();
            case REGEX -> criteria.expression(parameters.next().toString());
            case LIKE, STARTING_WITH -> criteria.startsWith(parameters.next().toString());
            case ENDING_WITH -> criteria.endsWith(parameters.next().toString());
            case CONTAINING -> criteria.contains(parameters.next().toString());
            case GREATER_THAN -> criteria.greaterThan(parameters.next());
            case AFTER, GREATER_THAN_EQUAL -> criteria.greaterThanEqual(parameters.next());
            case LESS_THAN -> criteria.lessThan(parameters.next());
            case BEFORE, LESS_THAN_EQUAL -> criteria.lessThanEqual(parameters.next());
            case BETWEEN -> criteria.between(parameters.next(), parameters.next());
            case IN -> criteria.in(asArray(parameters.next()));
            case NOT_IN -> criteria.notIn(asArray(parameters.next()));
            case SIMPLE_PROPERTY, WITHIN -> within(part, criteria, parameters);
            case NEAR -> near(criteria, parameters);
            case EXISTS, IS_NOT_NULL -> criteria.exists();
            case IS_NULL -> criteria.not().exists();
            case IS_EMPTY -> criteria.empty();
            case IS_NOT_EMPTY -> criteria.notEmpty();
            default -> throw new InvalidDataAccessApiUsageException("Illegal criteria found '" + type + "'.");
        };
    }

    private Criteria within(Part part, Criteria criteria, Iterator<?> parameters) {

        Object firstParameter = parameters.next();
        Object secondParameter;

        if (part.getType() == Part.Type.SIMPLE_PROPERTY) {
            if (part.getProperty().getType() != GeoPoint.class) {
                if (firstParameter != null) {
                    return criteria.is(firstParameter);
                } else {
                    // searching for null is a must_not (exists)
                    return criteria.exists().not();
                }
            } else {
                // it means it's a simple find with exact geopoint matching (e.g. findByLocation)
                // and because Elasticsearch does not have any kind of query with just a geopoint
                // as argument we use a "geo distance" query with a distance of one meter.
                secondParameter = ".001km";
            }
        } else {
            secondParameter = parameters.next();
        }

        return doWithinIfPossible(criteria, firstParameter, secondParameter);
    }

    private Criteria near(Criteria criteria, Iterator<?> parameters) {

        Object firstParameter = parameters.next();

        if (firstParameter instanceof GeoBox geoBox) {
            return criteria.boundedBy(geoBox);
        }

        if (firstParameter instanceof Box box) {
            return criteria.boundedBy(GeoBox.fromBox(box));
        }

        Object secondParameter = parameters.next();

        return doWithinIfPossible(criteria, firstParameter, secondParameter);
    }

    /**
     * Do a within query if possible, otherwise return the criteria unchanged.
     *
     * @param criteria        must not be {@literal null}
     * @param firstParameter  must not be {@literal null}
     * @param secondParameter must not be {@literal null}
     * @return the criteria with the within query applied if possible.
     * @author Junghoon Ban
     */
    private Criteria doWithinIfPossible(Criteria criteria, Object firstParameter, Object secondParameter) {

        if (firstParameter instanceof GeoPoint geoPoint && secondParameter instanceof String string) {
            return criteria.within(geoPoint, string);
        }

        if (firstParameter instanceof Point point && secondParameter instanceof Distance distance) {
            return criteria.within(point, distance);
        }

        if (firstParameter instanceof String firstString && secondParameter instanceof String secondString) {
            return criteria.within(firstString, secondString);
        }

        return criteria;
    }

    private Object[] asArray(Object o) {
        if (o instanceof Collection) {
            return ((Collection<?>) o).toArray();
        } else if (o.getClass().isArray()) {
            return (Object[]) o;
        }
        return new Object[]{o};
    }
}
