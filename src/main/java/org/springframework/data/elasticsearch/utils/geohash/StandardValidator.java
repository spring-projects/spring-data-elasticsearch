/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.utils.geohash;

/**
 * Code copied from Elasticsearch 7.10, Apache License V2
 * https://github.com/elastic/elasticsearch/blob/7.10/libs/geo/src/main/java/org/elasticsearch/geometry/utils/StandardValidator.java
 * <br/>
 * <br/>
 * Validator that only checks that altitude only shows up if ignoreZValue is set to true.
 */
public class StandardValidator implements GeometryValidator {

	private final boolean ignoreZValue;

	public StandardValidator(boolean ignoreZValue) {
		this.ignoreZValue = ignoreZValue;
	}

	protected void checkZ(double zValue) {
		if (ignoreZValue == false && Double.isNaN(zValue) == false) {
			throw new IllegalArgumentException(
					"found Z value [" + zValue + "] but [ignore_z_value] " + "parameter is [" + ignoreZValue + "]");
		}
	}

	@Override
	public void validate(Geometry geometry) {
		if (ignoreZValue == false) {
			geometry.visit(new GeometryVisitor<Void, RuntimeException>() {

				@Override
				public Void visit(Point point) throws RuntimeException {
					checkZ(point.getZ());
					return null;
				}

				@Override
				public Void visit(Rectangle rectangle) throws RuntimeException {
					checkZ(rectangle.getMinZ());
					checkZ(rectangle.getMaxZ());
					return null;
				}
			});
		}
	}
}
