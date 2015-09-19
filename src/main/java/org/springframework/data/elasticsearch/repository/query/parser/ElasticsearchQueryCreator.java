/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.query.parser;

import java.util.Collection;
import java.util.Iterator;

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
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * ElasticsearchQueryCreator
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
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
		PersistentPropertyPath<ElasticsearchPersistentProperty> path = context
				.getPersistentPropertyPath(part.getProperty());
		return new CriteriaQuery(from(part,
				new Criteria(path.toDotPath(ElasticsearchPersistentProperty.PropertyToFieldNameConverter.INSTANCE)), iterator));
	}

	@Override
	protected CriteriaQuery and(Part part, CriteriaQuery base, Iterator<Object> iterator) {
		if (base == null) {
			return create(part, iterator);
		}
		PersistentPropertyPath<ElasticsearchPersistentProperty> path = context
				.getPersistentPropertyPath(part.getProperty());
		return base.addCriteria(from(part,
				new Criteria(path.toDotPath(ElasticsearchPersistentProperty.PropertyToFieldNameConverter.INSTANCE)), iterator));
	}

	@Override
	protected CriteriaQuery or(CriteriaQuery base, CriteriaQuery query) {
		return new CriteriaQuery(base.getCriteria().or(query.getCriteria()));
	}

	@Override
	protected CriteriaQuery complete(CriteriaQuery query, Sort sort) {
		if (query == null) {
			return null;
		}
		return query.addSort(sort);
	}

	private Criteria from(Part part, Criteria instance, Iterator<?> parameters) {
		Part.Type type = part.getType();

		Criteria criteria = instance;
		if (criteria == null) {
			criteria = new Criteria();
		}
		switch (type) {
			case TRUE:
				return criteria.is(true);
			case FALSE:
				return criteria.is(false);
			case NEGATING_SIMPLE_PROPERTY:
				return criteria.is(parameters.next()).not();
			case REGEX:
				return criteria.expression(parameters.next().toString());
			case LIKE:
			case STARTING_WITH:
				return criteria.startsWith(parameters.next().toString());
			case ENDING_WITH:
				return criteria.endsWith(parameters.next().toString());
			case CONTAINING:
				return criteria.contains(parameters.next().toString());
			case GREATER_THAN:
				return criteria.greaterThan(parameters.next());
			case AFTER:
			case GREATER_THAN_EQUAL:
				return criteria.greaterThanEqual(parameters.next());
			case LESS_THAN:
				return criteria.lessThan(parameters.next());
			case BEFORE:
			case LESS_THAN_EQUAL:
				return criteria.lessThanEqual(parameters.next());
			case BETWEEN:
				return criteria.between(parameters.next(), parameters.next());
			case IN:
				return criteria.in(asArray(parameters.next()));
			case NOT_IN:
				return criteria.notIn(asArray(parameters.next()));
			case SIMPLE_PROPERTY:
			case WITHIN: {
				Object firstParameter = parameters.next();
				Object secondParameter = null;
				if (type == Part.Type.SIMPLE_PROPERTY) {
					if (part.getProperty().getType() != GeoPoint.class)
						return criteria.is(firstParameter);
					else {
						// it means it's a simple find with exact geopoint matching (e.g. findByLocation)
						// and because Elasticsearch does not have any kind of query with just a geopoint
						// as argument we use a "geo distance" query with a distance of one meter.
						secondParameter = ".001km";
					}
				} else {
					secondParameter = parameters.next();
				}

				if (firstParameter instanceof GeoPoint && secondParameter instanceof String)
					return criteria.within((GeoPoint) firstParameter, (String) secondParameter);

				if (firstParameter instanceof Point && secondParameter instanceof Distance)
					return criteria.within((Point) firstParameter, (Distance) secondParameter);

				if (firstParameter instanceof String && secondParameter instanceof String)
					return criteria.within((String) firstParameter, (String) secondParameter);
			}
			case NEAR: {
				Object firstParameter = parameters.next();

				if (firstParameter instanceof GeoBox) {
					return criteria.boundedBy((GeoBox) firstParameter);
				}

				if (firstParameter instanceof Box) {
					return criteria.boundedBy(GeoBox.fromBox((Box) firstParameter));
				}

				Object secondParameter = parameters.next();

				// "near" query can be the same query as the "within" query
				if (firstParameter instanceof GeoPoint && secondParameter instanceof String)
					return criteria.within((GeoPoint) firstParameter, (String) secondParameter);

				if (firstParameter instanceof Point && secondParameter instanceof Distance)
					return criteria.within((Point) firstParameter, (Distance) secondParameter);

				if (firstParameter instanceof String && secondParameter instanceof String)
					return criteria.within((String) firstParameter, (String) secondParameter);
			}

			default:
				throw new InvalidDataAccessApiUsageException("Illegal criteria found '" + type + "'.");
		}
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
