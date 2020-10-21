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
package org.springframework.data.elasticsearch.core.geo;

import static org.assertj.core.api.Assertions.*;

import lombok.Builder;
import lombok.Data;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
@DisplayName("GeoJson integration test with REST client")
class GeoJsonIntegrationTest {

	@Autowired private ElasticsearchOperations operations;

	private IndexOperations indexOps;

	// region data

	private final GeoJsonPolygon geoShape10To20 = GeoJsonPolygon.of( //
			new Point(10, 10), //
			new Point(20, 10), //
			new Point(20, 20), //
			new Point(10, 20), //
			new Point(10, 10));
	private final Area area10To20 = Area.builder().id("area10To20").area(geoShape10To20) /**/.build();

	private final GeoJsonPolygon geoShape5To35 = GeoJsonPolygon.of( //
			new Point(5, 5), //
			new Point(35, 5), //
			new Point(35, 35), //
			new Point(5, 35), //
			new Point(5, 5));
	private final Area area5To35 = Area.builder().id("area5To35").area(geoShape5To35).build();

	private final GeoJsonPolygon geoShape15To25 = GeoJsonPolygon.of( //
			new Point(15, 15), //
			new Point(25, 15), //
			new Point(25, 25), //
			new Point(15, 25), //
			new Point(15, 15));
	private final Area area15To25 = Area.builder().id("area15To25").area(geoShape15To25).build();

	private final GeoJsonPolygon geoShape30To40 = GeoJsonPolygon.of( //
			new Point(30, 30), //
			new Point(40, 30), //
			new Point(40, 40), //
			new Point(30, 40), //
			new Point(30, 30));
	private final Area area30To40 = Area.builder().id("area30To40").area(geoShape30To40).build();

	private final GeoJsonPolygon geoShape32To37 = GeoJsonPolygon.of( //
			new Point(32, 32), //
			new Point(37, 32), //
			new Point(37, 37), //
			new Point(32, 37), //
			new Point(32, 32));
	private final Area area32To37 = Area.builder().id("area32To37").area(geoShape30To40).build();
	// endregion

	// region setup
	@BeforeEach
	void setUp() {
		indexOps = operations.indexOps(GeoJsonEntity.class);
		indexOps.delete();
		indexOps.create();
		indexOps.putMapping();

		IndexOperations indexOpsArea = operations.indexOps(Area.class);
		indexOpsArea.delete();
		indexOpsArea.create();
		indexOpsArea.putMapping();

		operations.save(Arrays.asList(area10To20, area30To40));
		indexOpsArea.refresh();
	}

	@AfterEach
	void tearDown() {
		indexOps.delete();
	}
	// endregion

	// region tests
	@Test // DATAES-930
	@DisplayName("should write and read an entity with GeoJson properties")
	void shouldWriteAndReadAnEntityWithGeoJsonProperties() {

		GeoJsonMultiLineString multiLineString = GeoJsonMultiLineString.of(Arrays.asList( //
				GeoJsonLineString.of(new Point(12, 34), new Point(56, 78)), //
				GeoJsonLineString.of(new Point(90, 12), new Point(34, 56)) //
		));
		GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon
				.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34))
				.withInnerRing(new Point(21, 43), new Point(65, 87), new Point(9, 21), new Point(21, 43));
		GeoJsonMultiPolygon geoJsonMultiPolygon = GeoJsonMultiPolygon
				.of(Arrays.asList(GeoJsonPolygon.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)),
						GeoJsonPolygon.of(new Point(21, 43), new Point(65, 87), new Point(9, 21), new Point(21, 43))));
		GeoJsonGeometryCollection geoJsonGeometryCollection = GeoJsonGeometryCollection
				.of(Arrays.asList(GeoJsonPoint.of(12, 34), GeoJsonPolygon
						.of(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)))));

		GeoJsonEntity entity = GeoJsonEntity.builder() //
				.id("42") //
				.point1(GeoJsonPoint.of(12, 34)) //
				.point2(GeoJsonPoint.of(56, 78)) //
				.multiPoint1(GeoJsonMultiPoint.of(new Point(12, 34), new Point(56, 78), new Point(90, 12))) //
				.multiPoint2(GeoJsonMultiPoint.of(new Point(90, 12), new Point(56, 78), new Point(12, 34))) //
				.lineString1(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12))) //
				.lineString2(GeoJsonLineString.of(new Point(90, 12), new Point(56, 78), new Point(12, 34))) //
				.multiLineString1(multiLineString) //
				.multiLineString2(multiLineString) //
				.polygon1(geoJsonPolygon) //
				.polygon2(geoJsonPolygon) //
				.multiPolygon1(geoJsonMultiPolygon) //
				.multiPolygon2(geoJsonMultiPolygon) //
				.geometryCollection1(geoJsonGeometryCollection) //
				.geometryCollection2(geoJsonGeometryCollection) //
				.build(); //

		operations.save(entity);
		indexOps.refresh();

		GeoJsonEntity result = operations.get("42", GeoJsonEntity.class);

		assertThat(result).isEqualTo(entity);
	}

	@Test // DATAES-931
	@DisplayName("should find intersecting objects with Criteria query")
	void shouldFindIntersectingObjectsWithCriteriaQuery() {

		CriteriaQuery query = new CriteriaQuery(new Criteria("area").intersects(geoShape15To25));

		SearchHits<Area> searchHits = operations.search(query, Area.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1L);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo("area10To20");
	}

	@Test // DATAES-931
	@DisplayName("should find disjoint objects with Criteria query")
	void shouldFindDisjointObjectsWithCriteriaQuery() {

		CriteriaQuery query = new CriteriaQuery(new Criteria("area").isDisjoint(geoShape15To25));

		SearchHits<Area> searchHits = operations.search(query, Area.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1L);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo("area30To40");
	}

	@Test // DATAES-931
	@DisplayName("should find within objects with Criteria query")
	void shouldFindWithinObjectsWithCriteriaQuery() {

		CriteriaQuery query = new CriteriaQuery(new Criteria("area").within(geoShape5To35));

		SearchHits<Area> searchHits = operations.search(query, Area.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1L);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo("area10To20");
	}

	@Test // DATAES-931
	@DisplayName("should find contains objects with Criteria query")
	void shouldFindContainsObjectsWithCriteriaQuery() {

		CriteriaQuery query = new CriteriaQuery(new Criteria("area").contains(geoShape32To37));

		SearchHits<Area> searchHits = operations.search(query, Area.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1L);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo("area30To40");
	}
	// endregion

	// region test classes
	@Data
	@Builder
	@Document(indexName = "areas")
	static class Area {
		@Id private String id;
		@Field(name = "the_area") private GeoJsonPolygon area;
	}
	// endregion

}
