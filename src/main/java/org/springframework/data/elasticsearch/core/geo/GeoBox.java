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
package org.springframework.data.elasticsearch.core.geo;

import org.springframework.data.geo.Box;

/**
 * Geo bbox used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Franck Marchand
 */
public class GeoBox {

	private GeoPoint topLeft;
	private GeoPoint bottomRight;

	public GeoBox(GeoPoint topLeft, GeoPoint bottomRight) {
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	public GeoPoint getTopLeft() {
		return topLeft;
	}

	public GeoPoint getBottomRight() {
		return bottomRight;
	}

	/**
	 * return a {@link org.springframework.data.elasticsearch.core.geo.GeoBox}
	 * from a {@link org.springframework.data.geo.Box}.
	 *
	 * @param box {@link org.springframework.data.geo.Box} to use
	 * @return a {@link org.springframework.data.elasticsearch.core.geo.GeoBox}
	 */
	public static GeoBox fromBox(Box box) {
		GeoPoint topLeft = GeoPoint.fromPoint(box.getFirst());
		GeoPoint bottomRight = GeoPoint.fromPoint(box.getSecond());

		return new GeoBox(topLeft, bottomRight);
	}
}
