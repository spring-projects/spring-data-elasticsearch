/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.CoordinatesBuilder;
import org.elasticsearch.common.geo.builders.GeometryCollectionBuilder;
import org.elasticsearch.common.geo.builders.LineStringBuilder;
import org.elasticsearch.common.geo.builders.MultiLineStringBuilder;
import org.elasticsearch.common.geo.builders.MultiPointBuilder;
import org.elasticsearch.common.geo.builders.MultiPolygonBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilders;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.geo.GeoShape;
import org.springframework.data.elasticsearch.core.geo.GeoShapeGeometryCollection;
import org.springframework.data.elasticsearch.core.geo.GeoShapeLinestring;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiLinestring;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiPoint;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoShapePoint;
import org.springframework.data.elasticsearch.core.geo.GeoShapePolygon;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.Criteria.OperationKey;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * CriteriaFilterProcessor
 *
 * @author Franck Marchand
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Lukas Vorisek
 *
 */
class CriteriaFilterProcessor {
	Logger log = LoggerFactory.getLogger(CriteriaFilterProcessor.class);


	QueryBuilder createFilterFromCriteria(Criteria criteria) {
		List<QueryBuilder> fbList = new LinkedList<>();
		QueryBuilder filter = null;

		ListIterator<Criteria> chainIterator = criteria.getCriteriaChain().listIterator();

		while (chainIterator.hasNext()) {
			QueryBuilder fb = null;
			Criteria chainedCriteria = chainIterator.next();
			if (chainedCriteria.isOr()) {
				fb = QueryBuilders.boolQuery();
				for(QueryBuilder f: createFilterFragmentForCriteria(chainedCriteria)){
					((BoolQueryBuilder)fb).should(f);
				}
				fbList.add(fb);
			} else if (chainedCriteria.isNegating()) {
				List<QueryBuilder> negationFilters = buildNegationFilter(criteria.getField().getName(), criteria.getFilterCriteriaEntries().iterator());

				if (!negationFilters.isEmpty()) {
					fbList.addAll(negationFilters);
				}
			} else {
				fbList.addAll(createFilterFragmentForCriteria(chainedCriteria));
			}
		}

		if (!fbList.isEmpty()) {
			if (fbList.size() == 1) {
				filter = fbList.get(0);
			} else {
				filter = QueryBuilders.boolQuery();
				for(QueryBuilder f: fbList) {
					((BoolQueryBuilder)filter).must(f);
				}
			}
		}
		return filter;
	}


	private List<QueryBuilder> createFilterFragmentForCriteria(Criteria chainedCriteria) {
		Iterator<Criteria.CriteriaEntry> it = chainedCriteria.getFilterCriteriaEntries().iterator();
		List<QueryBuilder> filterList = new LinkedList<>();

		String fieldName = chainedCriteria.getField().getName();
		Assert.notNull(fieldName, "Unknown field");
		QueryBuilder filter = null;

		while (it.hasNext()) {
			Criteria.CriteriaEntry entry = it.next();
			filter = processCriteriaEntry(entry.getKey(), entry.getValue(), fieldName);
			filterList.add(filter);
		}

		return filterList;
	}


	private QueryBuilder processCriteriaEntry(OperationKey key, Object value, String fieldName) {
		if (value == null) {
			return null;
		}
		QueryBuilder filter = null;

		switch (key) {
			case WITHIN: {
				GeoDistanceQueryBuilder geoDistanceQueryBuilder = QueryBuilders.geoDistanceQuery(fieldName);

				Assert.isTrue(value instanceof Object[], "Value of a geo distance filter should be an array of two values.");
				Object[] valArray = (Object[]) value;
				Assert.noNullElements(valArray, "Geo distance filter takes 2 not null elements array as parameter.");
				Assert.isTrue(valArray.length == 2, "Geo distance filter takes a 2-elements array as parameter.");
				Assert.isTrue(valArray[0] instanceof GeoPoint || valArray[0] instanceof String || valArray[0] instanceof Point, "First element of a geo distance filter must be a GeoPoint, a Point or a text");
				Assert.isTrue(valArray[1] instanceof String || valArray[1] instanceof Distance, "Second element of a geo distance filter must be a text or a Distance");

				StringBuilder dist = new StringBuilder();

				if (valArray[1] instanceof Distance) {
					extractDistanceString((Distance) valArray[1], dist);
				} else {
					dist.append((String) valArray[1]);
				}

				if (valArray[0] instanceof GeoPoint) {
					GeoPoint loc = (GeoPoint) valArray[0];
					geoDistanceQueryBuilder.point(loc.getLat(),loc.getLon()).distance(dist.toString()).geoDistance(GeoDistance.PLANE);
				} else if (valArray[0] instanceof Point) {
					GeoPoint loc = GeoPoint.fromPoint((Point) valArray[0]);
					geoDistanceQueryBuilder.point(loc.getLat(), loc.getLon()).distance(dist.toString()).geoDistance(GeoDistance.PLANE);
				} else {
					String loc = (String) valArray[0];
					if (loc.contains(",")) {
						String c[] = loc.split(",");
						geoDistanceQueryBuilder.point(Double.parseDouble(c[0]), Double.parseDouble(c[1])).distance(dist.toString()).geoDistance(GeoDistance.PLANE);
					} else {
						geoDistanceQueryBuilder.geohash(loc).distance(dist.toString()).geoDistance(GeoDistance.PLANE);
					}
				}
				filter = geoDistanceQueryBuilder;

				break;
			}

			case WITHIN_POLYGON: {
				Assert.isInstanceOf(GeoShape.class, value, "Value of a geo_shape filter should be GeoShape.");

				ShapeBuilder shapeBuilder = null;
				if(value instanceof GeoShapePolygon) {
					shapeBuilder = polygonBuilder((GeoShapePolygon) value);
				} else if (value instanceof GeoShapeMultiPolygon) {
					shapeBuilder = polygonBuilder((GeoShapeMultiPolygon) value);
				} else {
					Assert.isTrue(false, "Value of a geo_shape filter must be GeoShapePolygon or GeoShapeMultiPolygon!");
				}

				Assert.notNull(shapeBuilder, "No one suitable shapeBuilder!");

				GeoShapeQueryBuilder geoShapeQueryBuilder;
				try {
					geoShapeQueryBuilder = QueryBuilders.geoWithinQuery(fieldName, shapeBuilder);
					filter = geoShapeQueryBuilder;
				} catch (IOException e) {
					log.error("Cannot construct GeoShapeQueryBuilder", e);
					filter = null;
				}

				break;
			}

			case INTERSECT: {
				Assert.isInstanceOf(GeoShape.class, value, "Value of a geo_shape filter should be GeoShape.");

				ShapeBuilder shapeBuilder = shapeBuilder((GeoShape<?>) value);
				Assert.notNull(shapeBuilder, "Unrecognized type of shape. Can't create shape builder.");

				try {
					GeoShapeQueryBuilder geoShapeQueryBuilder = QueryBuilders.geoIntersectionQuery(fieldName, shapeBuilder);
					filter = geoShapeQueryBuilder;
				} catch (IOException e) {
					log.error("Cannot construct GeoShapeQueryBuilder", e);
					filter = null;
				}

				break;
			}

			case DISJOINT: {
				Assert.isInstanceOf(GeoShape.class, value, "Value of a geo_shape filter should be GeoShape.");

				ShapeBuilder shapeBuilder = shapeBuilder((GeoShape<?>) value);
				Assert.notNull(shapeBuilder, "Unrecognized type of shape. Can't create shape builder.");

				try {
					GeoShapeQueryBuilder geoShapeQueryBuilder = QueryBuilders.geoDisjointQuery(fieldName, shapeBuilder);
					filter = geoShapeQueryBuilder;
				} catch (IOException e) {
					log.error("Cannot construct GeoShapeQueryBuilder", e);
					filter = null;
				}

				break;
			}

			case CONTAINS: {
				Assert.isInstanceOf(GeoShape.class, value, "Value of a geo_shape filter should be GeoShape.");

				ShapeBuilder shapeBuilder = shapeBuilder((GeoShape<?>) value);
				Assert.notNull(shapeBuilder, "Unrecognized type of shape. Can't create shape builder.");

				try {
					GeoShapeQueryBuilder geoShapeQueryBuilder = QueryBuilders.geoShapeQuery(fieldName, shapeBuilder);
					geoShapeQueryBuilder.relation(ShapeRelation.CONTAINS);
					filter = geoShapeQueryBuilder;
				} catch (IOException e) {
					log.error("Cannot construct GeoShapeQueryBuilder", e);
					filter = null;
				}

				break;
			}

			case BBOX: {
				filter = QueryBuilders.geoBoundingBoxQuery(fieldName);

				Assert.isTrue(value instanceof Object[], "Value of a boundedBy filter should be an array of one or two values.");
				Object[] valArray = (Object[]) value;
				Assert.noNullElements(valArray, "Geo boundedBy filter takes a not null element array as parameter.");

				if (valArray.length == 1) {
					//GeoEnvelop
					oneParameterBBox((GeoBoundingBoxQueryBuilder) filter, valArray[0]);
				} else if (valArray.length == 2) {
					//2x GeoPoint
					//2x text
					twoParameterBBox((GeoBoundingBoxQueryBuilder) filter, valArray);
				} else {
					//error
					Assert.isTrue(false, "Geo distance filter takes a 1-elements array(GeoBox) or 2-elements array(GeoPoints or Strings(format lat,lon or geohash)).");
				}
				break;
			}
		}

		return filter;
	}

	/**
	 * Create ShapeBuilder (Elasticsearch) based on data from GeoShape.
	 * @param value
	 * @return the Elasticsearch polygon builder or null if doesn't know that GeoShape.
	 */
	private ShapeBuilder shapeBuilder(GeoShape<?> value) {
		ShapeBuilder shapeBuilder = null;

		switch(value.getType()) {
		case POLYGON:
			shapeBuilder = polygonBuilder((GeoShapePolygon) value);
			break;
		case MULTIPOLYGON:
			shapeBuilder = polygonBuilder((GeoShapeMultiPolygon) value);
			break;
		case POINT:
			GeoShapePoint pointValue = (GeoShapePoint) value;
			shapeBuilder = ShapeBuilders.newPoint(new Coordinate(pointValue.getX(), pointValue.getY()));
			break;
		case LINESTRING:
			shapeBuilder = linestringBuilder((GeoShapeLinestring) value);
			break;
		case MULTILINESTRING:
			shapeBuilder = multiLinestringBuilder((GeoShapeMultiLinestring) value);
			break;
		case GEOMETRY_COLLECTION:
			shapeBuilder = geometryCollectionBuilder((GeoShapeGeometryCollection) value);
			break;
		case MULTIPOINT:
			shapeBuilder = multiPointBuilder((GeoShapeMultiPoint) value);
			break;
		default:
			break;
		}

		return shapeBuilder;
	}

	/**
	 * Create ShapeBuilder (Elasticsearch) based on data from GeoShapeGeometryCollection.
	 * @param value
	 * @return the Elasticsearch polygon builder
	 */
	private ShapeBuilder geometryCollectionBuilder(GeoShapeGeometryCollection value) {
		GeometryCollectionBuilder builder = new GeometryCollectionBuilder();

		for(GeoShape<?> shape : value.getCoordinates()) {
			builder.shape(shapeBuilder(shape));
		}

		return builder;
	}

	/**
	 * Create MultiPointBuilder (Elasticsearch) based on data from GeoShapeMultiPoint.
	 * @param value
	 * @return the Elasticsearch polygon builder
	 */
	private MultiPointBuilder multiPointBuilder(GeoShapeMultiPoint value) {
		CoordinatesBuilder builder = new CoordinatesBuilder();
		for(org.springframework.data.elasticsearch.core.geo.GeoShape.Coordinate coordiante : value.getCoordinates()) {
			builder.coordinate(coordiante.getX(), coordiante.getY());
		}

		return ShapeBuilders.newMultiPoint(builder.build());
	}

	/**
	 * Create MultiLineStringBuilder (Elasticsearch) based on data from GeoShapeMultiLinestring.
	 * @param value
	 * @return the Elasticsearch polygon builder
	 */
	private MultiLineStringBuilder multiLinestringBuilder(GeoShapeMultiLinestring value) {
		org.elasticsearch.common.geo.builders.MultiLineStringBuilder builder = ShapeBuilders.newMultiLinestring();

		for(GeoShapeLinestring linestring : value.getCoordinates()) {
			builder.linestring(linestringBuilder(linestring));
		}

		return builder;
	}


	/**
	 * Create LineStringBuilder (Elasticsearch) based on data from GeoShapeLinestring.
	 * @param value
	 * @return the Elasticsearch polygon builder
	 */
	private LineStringBuilder linestringBuilder(GeoShapeLinestring value) {
		CoordinatesBuilder coordinatesBuilder = new CoordinatesBuilder();

		for(org.springframework.data.elasticsearch.core.geo.GeoShape.Coordinate coordiante : value.getCoordinates()) {
			coordinatesBuilder.coordinate(coordiante.getX(), coordiante.getY());
		}

		return ShapeBuilders.newLineString(coordinatesBuilder);
	}

	/**
	 * Create PolygonBuilder (Elasticsearch) based on data from GeoShapePolygon.
	 * @param value
	 * @return the Elasticsearch polygon builder
	 */
	private PolygonBuilder polygonBuilder(GeoShapePolygon polygon) {
		CoordinatesBuilder coordinatesBuilder = new CoordinatesBuilder();
		Iterator<GeoShapeLinestring> iterator = polygon.getCoordinates().iterator();

		for(org.springframework.data.elasticsearch.core.geo.GeoShape.Coordinate coordinate : iterator.next().getCoordinates()) {
			coordinatesBuilder.coordinate(coordinate.getX(), coordinate.getY());
		}
		PolygonBuilder shapeBuilder = ShapeBuilders.newPolygon(coordinatesBuilder);
		while(iterator.hasNext()) {
			GeoShapeLinestring linestring = iterator.next();
			CoordinatesBuilder holeCoordinates = new CoordinatesBuilder();
			for(org.springframework.data.elasticsearch.core.geo.GeoShape.Coordinate coordinate : linestring.getCoordinates()) {
				holeCoordinates.coordinate(coordinate.getX(), coordinate.getY());
			}
			LineStringBuilder hole = new LineStringBuilder(holeCoordinates);
			shapeBuilder.hole(hole);
		}

		return shapeBuilder;
	}

	/**
	 * Create MultiPolygonBuilder (Elasticsearch) based on data from GeoShapeMultiPolygon.
	 * @param value
	 * @return the Elasticsearch polygon builder
	 */
	private MultiPolygonBuilder polygonBuilder(GeoShapeMultiPolygon value) {
		GeoShapeMultiPolygon polygon = value;

		MultiPolygonBuilder polygonBuilder = ShapeBuilders.newMultiPolygon();
		for(GeoShapePolygon pol : polygon.getCoordinates()) {
			polygonBuilder.polygon(polygonBuilder(pol));
		}

		return polygonBuilder;
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
			case KILOMETERS:
				sb.append("km");
				break;
			case MILES:
				sb.append("mi");
				break;
		}
	}

	private void oneParameterBBox(GeoBoundingBoxQueryBuilder filter, Object value) {
		Assert.isTrue(value instanceof GeoBox || value instanceof Box, "single-element of boundedBy filter must be type of GeoBox or Box");

		GeoBox geoBBox;
		if (value instanceof Box) {
			Box sdbox = (Box) value;
			geoBBox = GeoBox.fromBox(sdbox);
		} else {
			geoBBox = (GeoBox) value;
		}

		filter.setCorners(geoBBox.getTopLeft().getLat(), geoBBox.getTopLeft().getLon(), geoBBox.getBottomRight().getLat(), geoBBox.getBottomRight().getLon());
	}

	private static boolean isType(Object[] array, Class clazz) {
		for (Object o : array) {
			if (!clazz.isInstance(o)) {
				return false;
			}
		}
		return true;
	}

	private void twoParameterBBox(GeoBoundingBoxQueryBuilder filter, Object[] values) {
		Assert.isTrue(isType(values, GeoPoint.class) || isType(values, String.class), " both elements of boundedBy filter must be type of GeoPoint or text(format lat,lon or geohash)");
		if (values[0] instanceof GeoPoint) {
			GeoPoint topLeft = (GeoPoint) values[0];
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
			QueryBuilder notFilter = QueryBuilders.boolQuery().mustNot(processCriteriaEntry(criteriaEntry.getKey(), criteriaEntry.getValue(), fieldName));
			notFilterList.add(notFilter);
		}

		return notFilterList;
	}
}
