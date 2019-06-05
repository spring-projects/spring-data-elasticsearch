/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static org.springframework.data.elasticsearch.core.query.Criteria.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.index.query.*;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
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
 *
 */
class CriteriaFilterProcessor {


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
