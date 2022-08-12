/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import static org.springframework.data.elasticsearch.core.query.Criteria.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * CriteriaFilterProcessor
 *
 * @author Franck Marchand
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 * @deprecated since 5.0
 */
@Deprecated
class CriteriaFilterProcessor {

	@Nullable
	QueryBuilder createFilter(Criteria criteria) {

		List<QueryBuilder> filterBuilders = new ArrayList<>();

		for (Criteria chainedCriteria : criteria.getCriteriaChain()) {

			if (chainedCriteria.isOr()) {
				BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
				queriesForEntries(chainedCriteria).forEach(boolQuery::should);
				filterBuilders.add(boolQuery);
			} else if (chainedCriteria.isNegating()) {
				List<QueryBuilder> negationFilters = buildNegationFilter(criteria.getField().getName(),
						criteria.getFilterCriteriaEntries().iterator());

				filterBuilders.addAll(negationFilters);
			} else {
				filterBuilders.addAll(queriesForEntries(chainedCriteria));
			}
		}

		QueryBuilder filter = null;

		if (!filterBuilders.isEmpty()) {

			if (filterBuilders.size() == 1) {
				filter = filterBuilders.get(0);
			} else {
				BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
				filterBuilders.forEach(boolQuery::must);
				filter = boolQuery;
			}
		}

		return filter;
	}

	private List<QueryBuilder> queriesForEntries(Criteria criteria) {

		Assert.notNull(criteria.getField(), "criteria must have a field");
		String fieldName = criteria.getField().getName();
		Assert.notNull(fieldName, "Unknown field");

		return criteria.getFilterCriteriaEntries().stream()
				.map(entry -> queryFor(entry.getKey(), entry.getValue(), fieldName)).collect(Collectors.toList());
	}

	@Nullable
	private QueryBuilder queryFor(OperationKey key, Object value, String fieldName) {

		QueryBuilder filter = null;

		switch (key) {
			case WITHIN -> {
				Assert.isTrue(value instanceof Object[], "Value of a geo distance filter should be an array of two values.");
				filter = withinQuery(fieldName, (Object[]) value);
			}
			case BBOX -> {
				Assert.isTrue(value instanceof Object[],
						"Value of a boundedBy filter should be an array of one or two values.");
				filter = boundingBoxQuery(fieldName, (Object[]) value);
			}
			case GEO_INTERSECTS -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_INTERSECTS filter must be a GeoJson object");
				filter = geoJsonQuery(fieldName, (GeoJson<?>) value, "intersects");
			}
			case GEO_IS_DISJOINT -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_IS_DISJOINT filter must be a GeoJson object");
				filter = geoJsonQuery(fieldName, (GeoJson<?>) value, "disjoint");
			}
			case GEO_WITHIN -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_WITHIN filter must be a GeoJson object");
				filter = geoJsonQuery(fieldName, (GeoJson<?>) value, "within");
			}
			case GEO_CONTAINS -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_CONTAINS filter must be a GeoJson object");
				filter = geoJsonQuery(fieldName, (GeoJson<?>) value, "contains");
			}
		}

		return filter;
	}

	private QueryBuilder withinQuery(String fieldName, Object... valArray) {

		GeoDistanceQueryBuilder filter = QueryBuilders.geoDistanceQuery(fieldName);

		Assert.noNullElements(valArray, "Geo distance filter takes 2 not null elements array as parameter.");
		Assert.isTrue(valArray.length == 2, "Geo distance filter takes a 2-elements array as parameter.");
		Assert.isTrue(valArray[0] instanceof GeoPoint || valArray[0] instanceof String || valArray[0] instanceof Point,
				"First element of a geo distance filter must be a GeoPoint, a Point or a text");
		Assert.isTrue(valArray[1] instanceof String || valArray[1] instanceof Distance,
				"Second element of a geo distance filter must be a text or a Distance");

		StringBuilder dist = new StringBuilder();

		if (valArray[1] instanceof Distance) {
			extractDistanceString((Distance) valArray[1], dist);
		} else {
			dist.append((String) valArray[1]);
		}

		if (valArray[0]instanceof GeoPoint loc) {
			filter.point(loc.getLat(), loc.getLon()).distance(dist.toString()).geoDistance(GeoDistance.PLANE);
		} else if (valArray[0] instanceof Point) {
			GeoPoint loc = GeoPoint.fromPoint((Point) valArray[0]);
			filter.point(loc.getLat(), loc.getLon()).distance(dist.toString()).geoDistance(GeoDistance.PLANE);
		} else {
			String loc = (String) valArray[0];
			if (loc.contains(",")) {
				String[] c = loc.split(",");
				filter.point(Double.parseDouble(c[0]), Double.parseDouble(c[1])).distance(dist.toString())
						.geoDistance(GeoDistance.PLANE);
			} else {
				filter.geohash(loc).distance(dist.toString()).geoDistance(GeoDistance.PLANE);
			}
		}

		return filter;
	}

	private QueryBuilder boundingBoxQuery(String fieldName, Object... valArray) {

		Assert.noNullElements(valArray, "Geo boundedBy filter takes a not null element array as parameter.");

		GeoBoundingBoxQueryBuilder filter = QueryBuilders.geoBoundingBoxQuery(fieldName);

		if (valArray.length == 1) {
			// GeoEnvelop
			oneParameterBBox(filter, valArray[0]);
		} else if (valArray.length == 2) {
			// 2x GeoPoint
			// 2x text
			twoParameterBBox(filter, valArray);
		} else {
			throw new IllegalArgumentException(
					"Geo distance filter takes a 1-elements array(GeoBox) or 2-elements array(GeoPoints or Strings(format lat,lon or geohash)).");
		}

		return filter;
	}

	private QueryBuilder geoJsonQuery(String fieldName, GeoJson<?> geoJson, String relation) {
		return QueryBuilders.wrapperQuery(buildJsonQuery(fieldName, geoJson, relation));
	}

	private String buildJsonQuery(String fieldName, GeoJson<?> geoJson, String relation) {
		return "{\"geo_shape\": {\"" + fieldName + "\": {\"shape\": " + geoJson.toJson() + ", \"relation\": \"" + relation
				+ "\"}}}";
	}

	/**
	 * extract the distance string from a {@link org.springframework.data.geo.Distance} object.
	 *
	 * @param distance distance object to extract string from
	 * @param sb StringBuilder to build the distance string
	 */
	private void extractDistanceString(Distance distance, StringBuilder sb) {
		// handle Distance object
		sb.append((int) distance.getValue());

		Metrics metric = (Metrics) distance.getMetric();

		switch (metric) {
			case KILOMETERS -> sb.append("km");
			case MILES -> sb.append("mi");
		}
	}

	private void oneParameterBBox(GeoBoundingBoxQueryBuilder filter, Object value) {
		Assert.isTrue(value instanceof GeoBox || value instanceof Box,
				"single-element of boundedBy filter must be type of GeoBox or Box");

		GeoBox geoBBox;
		if (value instanceof Box) {
			geoBBox = GeoBox.fromBox((Box) value);
		} else {
			geoBBox = (GeoBox) value;
		}

		filter.setCorners(geoBBox.getTopLeft().getLat(), geoBBox.getTopLeft().getLon(), geoBBox.getBottomRight().getLat(),
				geoBBox.getBottomRight().getLon());
	}

	private static boolean isType(Object[] array, Class<?> clazz) {
		for (Object o : array) {
			if (!clazz.isInstance(o)) {
				return false;
			}
		}
		return true;
	}

	private void twoParameterBBox(GeoBoundingBoxQueryBuilder filter, Object... values) {
		Assert.isTrue(isType(values, GeoPoint.class) || isType(values, String.class),
				" both elements of boundedBy filter must be type of GeoPoint or text(format lat,lon or geohash)");
		if (values[0]instanceof GeoPoint topLeft) {
			GeoPoint bottomRight = (GeoPoint) values[1];
			filter.setCorners(topLeft.getLat(), topLeft.getLon(), bottomRight.getLat(), bottomRight.getLon());
		} else {
			String topLeft = (String) values[0];
			String bottomRight = (String) values[1];
			filter.setCorners(topLeft, bottomRight);
		}
	}

	private List<QueryBuilder> buildNegationFilter(String fieldName, Iterator<Criteria.CriteriaEntry> it) {
		List<QueryBuilder> notFilterList = new LinkedList<>();

		while (it.hasNext()) {
			Criteria.CriteriaEntry criteriaEntry = it.next();
			QueryBuilder notFilter = QueryBuilders.boolQuery()
					.mustNot(queryFor(criteriaEntry.getKey(), criteriaEntry.getValue(), fieldName));
			notFilterList.add(notFilter);
		}

		return notFilterList;
	}
}
