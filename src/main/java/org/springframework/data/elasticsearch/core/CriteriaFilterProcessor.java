/*
 * Copyright 2013 the original author or authors.
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

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.springframework.data.elasticsearch.core.query.Criteria.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxFilterBuilder;
import org.elasticsearch.index.query.GeoDistanceFilterBuilder;
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
 */
class CriteriaFilterProcessor {


	FilterBuilder createFilterFromCriteria(Criteria criteria) {
		List<FilterBuilder> fbList = new LinkedList<FilterBuilder>();
		FilterBuilder filter = null;

		ListIterator<Criteria> chainIterator = criteria.getCriteriaChain().listIterator();

		while (chainIterator.hasNext()) {
			FilterBuilder fb = null;
			Criteria chainedCriteria = chainIterator.next();
			if (chainedCriteria.isOr()) {
				fb = orFilter(createFilterFragmentForCriteria(chainedCriteria).toArray(new FilterBuilder[]{}));
				fbList.add(fb);
			} else if (chainedCriteria.isNegating()) {
				List<FilterBuilder> negationFilters = buildNegationFilter(criteria.getField().getName(), criteria.getFilterCriteriaEntries().iterator());

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
				filter = andFilter(fbList.toArray(new FilterBuilder[]{}));
			}
		}

		return filter;
	}


	private List<FilterBuilder> createFilterFragmentForCriteria(Criteria chainedCriteria) {
		Iterator<Criteria.CriteriaEntry> it = chainedCriteria.getFilterCriteriaEntries().iterator();
		List<FilterBuilder> filterList = new LinkedList<FilterBuilder>();

		String fieldName = chainedCriteria.getField().getName();
		Assert.notNull(fieldName, "Unknown field");
		FilterBuilder filter = null;

		while (it.hasNext()) {
			Criteria.CriteriaEntry entry = it.next();
			filter = processCriteriaEntry(entry.getKey(), entry.getValue(), fieldName);
			filterList.add(filter);
		}

		return filterList;
	}


	private FilterBuilder processCriteriaEntry(OperationKey key, Object value, String fieldName) {
		if (value == null) {
			return null;
		}
		FilterBuilder filter = null;

		switch (key) {
			case WITHIN: {
				filter = geoDistanceFilter(fieldName);

				Assert.isTrue(value instanceof Object[], "Value of a geo distance filter should be an array of two values.");
				Object[] valArray = (Object[]) value;
				Assert.noNullElements(valArray, "Geo distance filter takes 2 not null elements array as parameter.");
				Assert.isTrue(valArray.length == 2, "Geo distance filter takes a 2-elements array as parameter.");
				Assert.isTrue(valArray[0] instanceof GeoPoint || valArray[0] instanceof String || valArray[0] instanceof Point, "First element of a geo distance filter must be a GeoPoint, a Point or a String");
				Assert.isTrue(valArray[1] instanceof String || valArray[1] instanceof Distance, "Second element of a geo distance filter must be a String or a Distance");

				StringBuilder dist = new StringBuilder();

				if (valArray[1] instanceof Distance) {
					extractDistanceString((Distance) valArray[1], dist);
				} else {
					dist.append((String) valArray[1]);
				}

				if (valArray[0] instanceof GeoPoint) {
					GeoPoint loc = (GeoPoint) valArray[0];
					((GeoDistanceFilterBuilder) filter).lat(loc.getLat()).lon(loc.getLon()).distance(dist.toString());
				} else if (valArray[0] instanceof Point) {
					GeoPoint loc = GeoPoint.fromPoint((Point) valArray[0]);
					((GeoDistanceFilterBuilder) filter).lat(loc.getLat()).lon(loc.getLon()).distance(dist.toString());
				} else {
					String loc = (String) valArray[0];
					if (loc.contains(",")) {
						String c[] = loc.split(",");
						((GeoDistanceFilterBuilder) filter).lat(Double.parseDouble(c[0])).lon(Double.parseDouble(c[1])).distance(dist.toString());
					} else {
						((GeoDistanceFilterBuilder) filter).geohash(loc).distance(dist.toString());
					}
				}

				break;
			}

			case BBOX: {
				filter = geoBoundingBoxFilter(fieldName);

				Assert.isTrue(value instanceof Object[], "Value of a boundedBy filter should be an array of one or two values.");
				Object[] valArray = (Object[]) value;
				Assert.noNullElements(valArray, "Geo boundedBy filter takes a not null element array as parameter.");

				if (valArray.length == 1) {
					//GeoEnvelop
					oneParameterBBox((GeoBoundingBoxFilterBuilder) filter, valArray[0]);
				} else if (valArray.length == 2) {
					//2x GeoPoint
					//2x String
					twoParameterBBox((GeoBoundingBoxFilterBuilder) filter, valArray);
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

	private void oneParameterBBox(GeoBoundingBoxFilterBuilder filter, Object value) {
		Assert.isTrue(value instanceof GeoBox || value instanceof Box, "single-element of boundedBy filter must be type of GeoBox or Box");

		GeoBox geoBBox;
		if (value instanceof Box) {
			Box sdbox = (Box) value;
			geoBBox = GeoBox.fromBox(sdbox);
		} else {
			geoBBox = (GeoBox) value;
		}

		filter.topLeft(geoBBox.getTopLeft().getLat(), geoBBox.getTopLeft().getLon());
		filter.bottomRight(geoBBox.getBottomRight().getLat(), geoBBox.getBottomRight().getLon());
	}

	private static boolean isType(Object[] array, Class clazz) {
		for (Object o : array) {
			if (!clazz.isInstance(o)) {
				return false;
			}
		}
		return true;
	}

	private void twoParameterBBox(GeoBoundingBoxFilterBuilder filter, Object[] values) {
		Assert.isTrue(isType(values, GeoPoint.class) || isType(values, String.class), " both elements of boundedBy filter must be type of GeoPoint or String(format lat,lon or geohash)");
		if (values[0] instanceof GeoPoint) {
			GeoPoint topLeft = (GeoPoint) values[0];
			GeoPoint bottomRight = (GeoPoint) values[1];
			filter.topLeft(topLeft.getLat(), topLeft.getLon());
			filter.bottomRight(bottomRight.getLat(), bottomRight.getLon());
		} else {
			String topLeft = (String) values[0];
			String bottomRight = (String) values[1];
			filter.topLeft(topLeft);
			filter.bottomRight(bottomRight);
		}
	}

	private List<FilterBuilder> buildNegationFilter(String fieldName, Iterator<Criteria.CriteriaEntry> it) {
		List<FilterBuilder> notFilterList = new LinkedList<FilterBuilder>();

		while (it.hasNext()) {
			Criteria.CriteriaEntry criteriaEntry = it.next();
			FilterBuilder notFilter = notFilter(processCriteriaEntry(criteriaEntry.getKey(), criteriaEntry.getValue(), fieldName));
			notFilterList.add(notFilter);
		}

		return notFilterList;
	}
}
