/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoBoundingBoxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoDistanceQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoShapeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.core.convert.GeoConverters;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.utils.geohash.Geohash;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * Class to convert a {@link org.springframework.data.elasticsearch.core.query.CriteriaQuery} into an Elasticsearch
 * filter.
 *
 * @author Peter-Josef Meisch
 * @author Junghoon Ban
 * @since 4.4
 */
class CriteriaFilterProcessor {
	/**
	 * Creates a filter query from the given criteria.
	 *
	 * @param criteria the criteria to process
	 * @return the optional query, empty if the criteria did not contain filter relevant elements
	 */
	public static Optional<Query> createQuery(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		List<Query> filterQueries = new ArrayList<>();

		for (Criteria chainedCriteria : criteria.getCriteriaChain()) {

			if (chainedCriteria.isOr()) {
				Collection<? extends Query> queriesForEntries = queriesForEntries(chainedCriteria);

				if (!queriesForEntries.isEmpty()) {
					BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
					queriesForEntries.forEach(boolQueryBuilder::should);
					filterQueries.add(new Query(boolQueryBuilder.build()));
				}
			} else if (chainedCriteria.isNegating()) {

				Assert.notNull(criteria.getField(), "criteria must have a field");

				Collection<? extends Query> negatingFilters = buildNegatingFilter(criteria.getField().getName(),
						criteria.getFilterCriteriaEntries());
				filterQueries.addAll(negatingFilters);
			} else {
				filterQueries.addAll(queriesForEntries(chainedCriteria));
			}
		}

		if (filterQueries.isEmpty()) {
			return Optional.empty();
		} else {

			if (filterQueries.size() == 1) {
				return Optional.of(filterQueries.get(0));
			} else {
				BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
				filterQueries.forEach(boolQueryBuilder::must);
				BoolQuery boolQuery = boolQueryBuilder.build();
				return Optional.of(new Query(boolQuery));
			}
		}
	}

	private static Collection<? extends Query> buildNegatingFilter(String fieldName,
			Set<Criteria.CriteriaEntry> filterCriteriaEntries) {

		List<Query> negationFilters = new ArrayList<>();

		filterCriteriaEntries.forEach(criteriaEntry -> {
			Optional<Query> query = queryFor(criteriaEntry.getKey(), criteriaEntry.getValue(), fieldName);

			if (query.isPresent()) {
				BoolQuery negatingFilter = QueryBuilders.bool().mustNot(query.get()).build();
				negationFilters.add(new Query(negatingFilter));
			}
		});

		return negationFilters;
	}

	private static Collection<? extends Query> queriesForEntries(Criteria criteria) {

		Assert.notNull(criteria.getField(), "criteria must have a field");

		String fieldName = criteria.getField().getName();
		Assert.notNull(fieldName, "Unknown field");

		return criteria.getFilterCriteriaEntries().stream()
				.map(entry -> queryFor(entry.getKey(), entry.getValue(), fieldName)) //
				.filter(Optional::isPresent) //
				.map(Optional::get) //
				.collect(Collectors.toList());
	}

	private static Optional<Query> queryFor(Criteria.OperationKey key, Object value, String fieldName) {

		ObjectBuilder<? extends QueryVariant> queryBuilder = null;

		switch (key) {
			case WITHIN -> {
				Assert.isTrue(value instanceof Object[], "Value of a geo distance filter should be an array of two values.");
				queryBuilder = withinQuery(fieldName, (Object[]) value);
			}
			case BBOX -> {
				Assert.isTrue(value instanceof Object[],
						"Value of a boundedBy filter should be an array of one or two values.");
				queryBuilder = boundingBoxQuery(fieldName, (Object[]) value);
			}
			case GEO_INTERSECTS -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_INTERSECTS filter must be a GeoJson object");
				queryBuilder = geoJsonQuery(fieldName, (GeoJson<?>) value, "intersects");
			}
			case GEO_IS_DISJOINT -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_IS_DISJOINT filter must be a GeoJson object");
				queryBuilder = geoJsonQuery(fieldName, (GeoJson<?>) value, "disjoint");
			}
			case GEO_WITHIN -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_WITHIN filter must be a GeoJson object");
				queryBuilder = geoJsonQuery(fieldName, (GeoJson<?>) value, "within");
			}
			case GEO_CONTAINS -> {
				Assert.isTrue(value instanceof GeoJson<?>, "value of a GEO_CONTAINS filter must be a GeoJson object");
				queryBuilder = geoJsonQuery(fieldName, (GeoJson<?>) value, "contains");
			}
		}

		return Optional.ofNullable(queryBuilder != null ? queryBuilder.build()._toQuery() : null);
	}

	private static ObjectBuilder<GeoDistanceQuery> withinQuery(String fieldName, Object... values) {

		Assert.noNullElements(values, "Geo distance filter takes 2 not null elements array as parameter.");
		Assert.isTrue(values.length == 2, "Geo distance filter takes a 2-elements array as parameter.");
		Assert.isTrue(values[0] instanceof GeoPoint || values[0] instanceof String || values[0] instanceof Point,
				"First element of a geo distance filter must be a GeoPoint, a Point or a text");
		Assert.isTrue(values[1] instanceof String || values[1] instanceof Distance,
				"Second element of a geo distance filter must be a text or a Distance");

		String dist = (values[1] instanceof Distance distance) ? extractDistanceString(distance) : (String) values[1];

		return QueryBuilders.geoDistance() //
				.field(fieldName) //
				.distance(dist) //
				.distanceType(GeoDistanceType.Plane) //
				.location(location -> {
					if (values[0] instanceof GeoPoint loc) {
						location.latlon(latlon -> latlon.lat(loc.getLat()).lon(loc.getLon()));
					} else if (values[0] instanceof Point point) {
						GeoPoint loc = GeoPoint.fromPoint(point);
						location.latlon(latlon -> latlon.lat(loc.getLat()).lon(loc.getLon()));
					} else {
						String loc = (String) values[0];
						if (loc.contains(",")) {
							String[] c = loc.split(",");
							location.latlon(latlon -> latlon.lat(Double.parseDouble(c[0])).lon(Double.parseDouble(c[1])));
						} else {
							location.geohash(geohash -> geohash.geohash(loc));
						}
					}
					return location;
				});
	}

	private static ObjectBuilder<GeoBoundingBoxQuery> boundingBoxQuery(String fieldName, Object... values) {

		Assert.noNullElements(values, "Geo boundedBy filter takes a not null element array as parameter.");

		GeoBoundingBoxQuery.Builder queryBuilder = QueryBuilders.geoBoundingBox() //
				.field(fieldName);

		if (values.length == 1) {
			// GeoEnvelop
			oneParameterBBox(queryBuilder, values[0]);
		} else if (values.length == 2) {
			// 2x GeoPoint
			// 2x text
			twoParameterBBox(queryBuilder, values);
		} else {
			throw new IllegalArgumentException(
					"Geo distance filter takes a 1-elements array(GeoBox) or 2-elements array(GeoPoints or Strings(format lat,lon or geohash)).");
		}
		return queryBuilder;
	}

	private static void oneParameterBBox(GeoBoundingBoxQuery.Builder queryBuilder, Object value) {
		Assert.isTrue(value instanceof GeoBox || value instanceof Box,
				"single-element of boundedBy filter must be type of GeoBox or Box");

		GeoBox geoBBox;
		if (value instanceof Box box) {
			geoBBox = GeoBox.fromBox(box);
		} else {
			geoBBox = (GeoBox) value;
		}

		queryBuilder.boundingBox(bb -> bb //
				.tlbr(tlbr -> tlbr //
						.topLeft(glb -> glb //
								.latlon(latlon -> latlon //
										.lat(geoBBox.getTopLeft().getLat()) //
										.lon(geoBBox.getTopLeft().getLon()))) //
						.bottomRight(glb -> glb //
								.latlon(latlon -> latlon //
										.lat(geoBBox.getBottomRight().getLat())//
										.lon(geoBBox.getBottomRight().getLon()// )
										)))));
	}

	private static void twoParameterBBox(GeoBoundingBoxQuery.Builder queryBuilder, Object... values) {

		Assert.isTrue(allElementsAreOfType(values, GeoPoint.class) || allElementsAreOfType(values, String.class),
				" both elements of boundedBy filter must be type of GeoPoint or text(format lat,lon or geohash)");

		if (values[0] instanceof GeoPoint topLeft) {
			GeoPoint bottomRight = (GeoPoint) values[1];
			queryBuilder.boundingBox(bb -> bb //
					.tlbr(tlbr -> tlbr //
							.topLeft(glb -> glb //
									.latlon(latlon -> latlon //
											.lat(topLeft.getLat()) //
											.lon(topLeft.getLon()))) //
							.bottomRight(glb -> glb //
									.latlon(latlon -> latlon //
											.lat(bottomRight.getLat()) //
											.lon(bottomRight.getLon()))) //
					) //
			);
		} else {
			String topLeft = (String) values[0];
			String bottomRight = (String) values[1];
			boolean isGeoHash = !topLeft.contains(",");
			queryBuilder.boundingBox(bb -> bb //
					.tlbr(tlbr -> tlbr //
							.topLeft(glb -> {
								if (isGeoHash) {
									// although the builder in 8.13.2 supports geohash, the server throws an error, so we convert to a
									// lat,lon string here
									glb.text(Geohash.toLatLon(topLeft));
									// glb.geohash(gh -> gh.geohash(topLeft));
								} else {
									glb.text(topLeft);
								}
								return glb;
							}) //
							.bottomRight(glb -> {
								if (isGeoHash) {
									glb.text(Geohash.toLatLon(bottomRight));
									// glb.geohash(gh -> gh.geohash(bottomRight));
								} else {
									glb.text(bottomRight);
								}
								return glb;
							}) //
					));
		}
	}

	private static boolean allElementsAreOfType(Object[] array, Class<?> clazz) {
		for (Object o : array) {
			if (!clazz.isInstance(o)) {
				return false;
			}
		}
		return true;
	}

	private static ObjectBuilder<? extends QueryVariant> geoJsonQuery(String fieldName, GeoJson<?> geoJson,
			String relation) {
		return buildGeoShapeQuery(fieldName, geoJson, relation);
	}

	private static ObjectBuilder<GeoShapeQuery> buildGeoShapeQuery(String fieldName, GeoJson<?> geoJson,
			String relation) {
		return QueryBuilders.geoShape().field(fieldName) //
				.shape(gsf -> gsf //
						.shape(JsonData.of(GeoConverters.GeoJsonToMapConverter.INSTANCE.convert(geoJson))) //
						.relation(toRelation(relation))); //
	}

	private static GeoShapeRelation toRelation(String relation) {

		for (GeoShapeRelation geoShapeRelation : GeoShapeRelation.values()) {

			if (geoShapeRelation.name().equalsIgnoreCase(relation)) {
				return geoShapeRelation;
			}
		}
		throw new IllegalArgumentException("Unknown geo_shape relation: " + relation);
	}

	/**
	 * extract the distance string from a {@link org.springframework.data.geo.Distance} object.
	 *
	 * @param distance distance object to extract string from
	 */
	private static String extractDistanceString(Distance distance) {

		StringBuilder sb = new StringBuilder();
		sb.append((int) distance.getValue());
		switch ((Metrics) distance.getMetric()) {
			case KILOMETERS -> sb.append("km");
			case MILES -> sb.append("mi");
		}

		return sb.toString();
	}

}
