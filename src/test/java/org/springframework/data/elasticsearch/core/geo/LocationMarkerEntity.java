/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.elasticsearch.core.geo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.GeoPointField;

/**
 * @author Franck Marchand
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "test-geo-index", type = "geo-annotation-point-type", indexStoreType = "memory", shards = 1, replicas = 0, refreshInterval = "-1")
public class LocationMarkerEntity {

	@Id
	private String id;
	private String name;

	@GeoPointField
	private String locationAsString;

	@GeoPointField
	private double[] locationAsArray;

	@GeoPointField(geoHashPrefix = true, geoHashPrecision = "100km")
	private String locationWithPrefixAsDistance;

	@GeoPointField(geoHashPrefix = true, geoHashPrecision = "5")
	private String locationWithPrefixAsLengthOfGeoHash;
}
