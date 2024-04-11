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
 * https://github.com/elastic/elasticsearch/blob/7.10/libs/geo/src/main/java/org/elasticsearch/geometry/Geometry.java
 * <br/>
 * <br/>
 * Base class for all Geometry objects supported by elasticsearch
 */
public interface Geometry {

	ShapeType type();

	<T, E extends Exception> T visit(GeometryVisitor<T, E> visitor) throws E;

	boolean isEmpty();

	default boolean hasZ() {
		return false;
	}

	default boolean hasAlt() {
		return hasZ();
	}
}
