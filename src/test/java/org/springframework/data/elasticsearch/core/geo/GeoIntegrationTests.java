/*
 * Copyright 2013-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.elasticsearch.utils.geohash.Geohash;
import org.springframework.data.geo.Point;
import org.springframework.lang.Nullable;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
 * @author Peter-Josef Meisch <br/>
 *         Basic info: latitude - horizontal lines (equator = 0.0, values -90.0 to 90.0) longitude - vertical lines
 *         (Greenwich = 0.0, values -180 to 180) London [lat,lon] = [51.50985,-0.118082] - geohash = gcpvj3448 Bouding
 *         Box for London = (bbox=-0.489,51.28,0.236,51.686) bbox = left,bottom,right,top bbox = min Longitude , min
 *         Latitude , max Longitude , max Latitude
 */
@SpringIntegrationTest
public abstract class GeoIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(AuthorMarkerEntity.class).createWithMapping();
		operations.indexOps(LocationMarkerEntity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of("*" + indexNameProvider.getPrefix() + "*")).delete();
	}

	private void loadClassBaseEntities() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(new AuthorMarkerEntityBuilder("1").name("abc").location(45.7806d, 3.0875d).buildIndex());
		indexQueries.add(new AuthorMarkerEntityBuilder("2").name("def").location(51.5171d, 0.1062d).buildIndex());
		indexQueries.add(new AuthorMarkerEntityBuilder("3").name("ghi").location(51.5171d, 0.1062d).buildIndex());
		indexQueries.add(
				new AuthorMarkerEntityBuilder("4").name("jkl").location(38.77353441278326d, -9.09882204680034d).buildIndex());
		operations.bulkIndex(indexQueries, AuthorMarkerEntity.class);
	}

	private void loadAnnotationBaseEntities() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		double[] lonLatArray = { 0.100000, 51.000000 };
		String latLonString = "51.000000, 0.100000";
		String geohash = "u10j46mkfekr";
		Geohash.stringEncode(0.100000, 51.000000);

		LocationMarkerEntity location1 = new LocationMarkerEntity();
		location1.setId("1");
		location1.setName("location 1");
		location1.setLocationAsString(latLonString);
		location1.setLocationAsArray(lonLatArray);
		location1.setLocationAsGeoHash(geohash);

		LocationMarkerEntity location2 = new LocationMarkerEntity();
		location2.setId("2");
		location2.setName("location 2");
		location2.setLocationAsString(geohash.substring(0, 8));
		location2.setLocationAsArray(lonLatArray);
		location2.setLocationAsGeoHash(geohash.substring(0, 8));

		LocationMarkerEntity location3 = new LocationMarkerEntity();
		location3.setId("3");
		location3.setName("location 3");
		location3.setLocationAsString(geohash);
		location3.setLocationAsArray(lonLatArray);
		location3.setLocationAsGeoHash(geohash);

		LocationMarkerEntity location4 = new LocationMarkerEntity();
		location4.setId("4");
		location4.setName("location 4");
		location4.setLocationAsArray(-9.09882204680034d, 38.77353441278326d);

		indexQueries.add(buildIndex(location1));
		indexQueries.add(buildIndex(location2));
		indexQueries.add(buildIndex(location3));
		indexQueries.add(buildIndex(location4));

		operations.bulkIndex(indexQueries, LocationMarkerEntity.class);
	}

	@Test
	public void shouldFindAuthorMarkersInRangeForGivenCriteriaQuery() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("location").within(new GeoPoint(45.7806d, 3.0875d), "20km"));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria = operations.search(geoLocationCriteriaQuery,
				AuthorMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(1);
		assertThat(geoAuthorsForGeoCriteria.getSearchHit(0).getContent().getName()).isEqualTo("abc");
	}

	@Test
	public void shouldFindSelectedAuthorMarkerInRangeForGivenCriteriaQuery() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery2 = new CriteriaQuery(
				new Criteria("name").is("def").and("location").within(new GeoPoint(51.5171d, 0.1062d), "20km"));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria2 = operations.search(geoLocationCriteriaQuery2,
				AuthorMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria2).hasSize(1);
		assertThat(geoAuthorsForGeoCriteria2.getSearchHit(0).getContent().getName()).isEqualTo("def");
	}

	@Test
	public void shouldFindStringAnnotatedGeoMarkersInRangeForGivenCriteriaQuery() {

		// given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("locationAsString").within(new GeoPoint(51.000000, 0.100000), "1km"));
		// when
		SearchHits<LocationMarkerEntity> geoAuthorsForGeoCriteria = operations.search(geoLocationCriteriaQuery,
				LocationMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(1);
	}

	@Test
	public void shouldFindDoubleAnnotatedGeoMarkersInRangeForGivenCriteriaQuery() {

		// given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("locationAsArray").within(new GeoPoint(51.001000, 0.10100), "1km"));

		// when
		SearchHits<LocationMarkerEntity> geoAuthorsForGeoCriteria = operations.search(geoLocationCriteriaQuery,
				LocationMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(3);
	}

	@Test
	public void shouldFindAnnotatedGeoMarkersInRangeForGivenCriteriaQuery() {

		// given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("locationAsArray").within("51.001000, 0.10100", "1km"));
		// when
		SearchHits<LocationMarkerEntity> geoAuthorsForGeoCriteria = operations.search(geoLocationCriteriaQuery,
				LocationMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(3);
	}

	@Test
	public void shouldFindAnnotatedGeoMarkersInRangeForGivenCriteriaQueryUsingGeohashLocation() {

		// given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(new Criteria("locationAsArray").within("u1044", "3km"));

		// when
		SearchHits<LocationMarkerEntity> geoAuthorsForGeoCriteria = operations.search(geoLocationCriteriaQuery,
				LocationMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(3);
	}

	@Test
	public void shouldFindAllMarkersForNativeSearchQuery() {

		// given
		loadAnnotationBaseEntities();

		// when
		Query query = nativeQueryForBoundingBox("locationAsArray", 52, -1, 50, 1);
		SearchHits<LocationMarkerEntity> geoAuthorsForGeoCriteria = operations.search(query, LocationMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(3);
	}

	protected abstract Query nativeQueryForBoundingBox(String fieldName, double top, double left, double bottom,
			double right);

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeoBox() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(new GeoBox(new GeoPoint(53.5171d, 0), new GeoPoint(49.5171d, 0.2062d))));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("def", "ghi");
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeohash() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(Geohash.stringEncode(0, 53.5171d), Geohash.stringEncode(0.2062d, 49.5171d)));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("def", "ghi");
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeoPoints() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(new GeoPoint(53.5171d, 0), new GeoPoint(49.5171d, 0.2062d)));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("def", "ghi");
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingPoints() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(new Point(0, 53.5171d), new Point(0.2062d, 49.5171d)));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("def", "ghi");
	}

	@Test
	public void shouldFindLocationWithGeoHashPrefix() {

		// given
		loadAnnotationBaseEntities();
		Query location1 = nativeQueryForBoundingBox("locationAsGeoHash", "u");
		Query location2 = nativeQueryForBoundingBox("locationAsGeoHash", "u1");
		Query location3 = nativeQueryForBoundingBox("locationAsGeoHash", "u10");
		Query location4 = nativeQueryForBoundingBox("locationAsGeoHash", "u10j");
		Query location5 = nativeQueryForBoundingBox("locationAsGeoHash", "u10j4");
		Query location11 = nativeQueryForBoundingBox("locationAsGeoHash", "u10j46mkfek");

		// when
		SearchHits<LocationMarkerEntity> result1 = operations.search(location1, LocationMarkerEntity.class);
		SearchHits<LocationMarkerEntity> result2 = operations.search(location2, LocationMarkerEntity.class);
		SearchHits<LocationMarkerEntity> result3 = operations.search(location3, LocationMarkerEntity.class);
		SearchHits<LocationMarkerEntity> result4 = operations.search(location4, LocationMarkerEntity.class);
		SearchHits<LocationMarkerEntity> result5 = operations.search(location5, LocationMarkerEntity.class);
		SearchHits<LocationMarkerEntity> result11 = operations.search(location11, LocationMarkerEntity.class);

		// then
		assertThat(result1).hasSize(3);
		assertThat(result2).hasSize(3);
		assertThat(result3).hasSize(3);
		assertThat(result4).hasSize(3);
		assertThat(result5).hasSize(3);
		assertThat(result11).hasSize(2);
	}

	protected abstract Query nativeQueryForBoundingBox(String fieldName, String geoHash);

	private IndexQuery buildIndex(LocationMarkerEntity result) {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}

	@Document(indexName = "author-#{@indexNameProvider.indexName()}")
	static class AuthorMarkerEntity {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable private GeoPoint location;

		private AuthorMarkerEntity() {}

		public AuthorMarkerEntity(String id) {
			this.id = id;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public GeoPoint getLocation() {
			return location;
		}

		public void setLocation(@Nullable GeoPoint location) {
			this.location = location;
		}
	}

	static class AuthorMarkerEntityBuilder {

		private final AuthorMarkerEntity result;

		public AuthorMarkerEntityBuilder(String id) {
			result = new AuthorMarkerEntity(id);
		}

		public AuthorMarkerEntityBuilder name(String name) {
			result.setName(name);
			return this;
		}

		public AuthorMarkerEntityBuilder location(double latitude, double longitude) {
			result.setLocation(new GeoPoint(latitude, longitude));
			return this;
		}

		public AuthorMarkerEntity build() {
			return result;
		}

		public IndexQuery buildIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(result.getId());
			indexQuery.setObject(result);
			return indexQuery;
		}
	}

	@Document(indexName = "location-#{@indexNameProvider.indexName()}")
	static class LocationMarkerEntity {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@GeoPointField private String locationAsString;
		@Nullable
		@GeoPointField private double[] locationAsArray;
		@Nullable
		@GeoPointField private String locationAsGeoHash;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLocationAsString() {
			return locationAsString;
		}

		public void setLocationAsString(String locationAsString) {
			this.locationAsString = locationAsString;
		}

		public double[] getLocationAsArray() {
			return locationAsArray;
		}

		public void setLocationAsArray(double... locationAsArray) {
			this.locationAsArray = locationAsArray;
		}

		public String getLocationAsGeoHash() {
			return locationAsGeoHash;
		}

		public void setLocationAsGeoHash(String locationAsGeoHash) {
			this.locationAsGeoHash = locationAsGeoHash;
		}
	}
}
