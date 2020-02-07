/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.util.Collections;

import org.elasticsearch.action.search.SearchRequest;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
class RequestFactoryTest {

	@Nullable private static RequestFactory requestFactory;
	@Nullable private static MappingElasticsearchConverter converter;

	@BeforeAll

	static void setUpAll() {
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(Collections.singleton(Person.class));
		mappingContext.afterPropertiesSet();

		converter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
		converter.afterPropertiesSet();

		requestFactory = new RequestFactory((converter));
	}

	@Test // FPI-734
	void shouldBuildSearchWithGeoSortSort() throws JSONException {
		CriteriaQuery query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
		Sort sort = Sort.by(new GeoDistanceOrder("location", new GeoPoint(49.0, 8.4)));
		query.addSort(sort);

		converter.updateQuery(query, Person.class);

		String expected = '{' + //
				"  \"query\": {" + //
				"    \"bool\": {" + //
				"      \"must\": [" + //
				"        {" + //
				"          \"query_string\": {" + //
				"            \"query\": \"Smith\"," + //
				"            \"fields\": [" + //
				"              \"first-name^1.0\"" + //
				"            ]" + //
				"          }" + //
				"        }" + //
				"      ]" + //
				"    }" + //
				"  }," + //
				"  \"sort\": [" + //
				"    {" + //
				"      \"_geo_distance\": {" + //
				"        \"current-location\": [" + //
				"          {" + //
				"            \"lat\": 49.0," + //
				"            \"lon\": 8.4" + //
				"          }" + //
				"        ]," + //
				"        \"unit\": \"m\"," + //
				"        \"distance_type\": \"arc\"," + //
				"        \"order\": \"asc\"," + //
				"        \"mode\": \"min\"," + //
				"        \"ignore_unmapped\": false" + //
				"      }" + //
				"    }" + //
				"  ]" + //
				'}';

		String searchRequest = requestFactory.searchRequest(query, Person.class, IndexCoordinates.of("persons")).source()
				.toString();

		assertEquals(expected, searchRequest, false);
	}

	@Test // DATAES-449
	void shouldAddRouting() throws JSONException {
		String route = "route66";
		CriteriaQuery query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
		query.setRoute(route);
		converter.updateQuery(query, Person.class);

		SearchRequest searchRequest = requestFactory.searchRequest(query, Person.class, IndexCoordinates.of("persons"));

		assertThat(searchRequest.routing()).isEqualTo(route);
	}

	static class Person {
		@Nullable @Id String id;
		@Nullable @Field(name = "last-name") String lastName;
		@Nullable @Field(name = "current-location") GeoPoint location;
	}
}
