/*
 * Copyright 2013-2020 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.geometry.utils.Geohash;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;

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
@ContextConfiguration(classes = { ElasticsearchTemplateGeoTests.Config.class })
public class ElasticsearchTemplateGeoTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	private final IndexCoordinates locationMarkerIndex = IndexCoordinates.of("test-index-location-marker-core-geo");
	private final IndexCoordinates authorMarkerIndex = IndexCoordinates.of("test-index-author-marker-core-geo");

	@Autowired private ElasticsearchOperations operations;

	@BeforeEach
	public void before() {
		IndexInitializer.init(operations.indexOps(AuthorMarkerEntity.class));
		IndexInitializer.init(operations.indexOps(LocationMarkerEntity.class));
	}

	@AfterEach
	void after() {
		operations.indexOps(AuthorMarkerEntity.class).delete();
		operations.indexOps(LocationMarkerEntity.class).delete();
	}

	private void loadClassBaseEntities() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries
				.add(new AuthorMarkerEntityBuilder("1").name("Franck Marchand").location(45.7806d, 3.0875d).buildIndex());
		indexQueries.add(new AuthorMarkerEntityBuilder("2").name("Mohsin Husen").location(51.5171d, 0.1062d).buildIndex());
		indexQueries.add(new AuthorMarkerEntityBuilder("3").name("Rizwan Idrees").location(51.5171d, 0.1062d).buildIndex());
		operations.bulkIndex(indexQueries, authorMarkerIndex);
		operations.indexOps(AuthorMarkerEntity.class).refresh();
	}

	private void loadAnnotationBaseEntities() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		double[] lonLatArray = { 0.100000, 51.000000 };
		String latLonString = "51.000000, 0.100000";
		String geohash = "u10j46mkfekr";
		Geohash.stringEncode(0.100000, 51.000000);
		LocationMarkerEntity location1 = LocationMarkerEntity.builder() //
				.id("1").name("Artur Konczak") //
				.locationAsString(latLonString) //
				.locationAsArray(lonLatArray) //
				.locationAsGeoHash(geohash).build();
		LocationMarkerEntity location2 = LocationMarkerEntity.builder() //
				.id("2").name("Mohsin Husen") //
				.locationAsString(geohash.substring(0, 8)) //
				.locationAsArray(lonLatArray) //
				.locationAsGeoHash(geohash.substring(0, 8)) //
				.build();
		LocationMarkerEntity location3 = LocationMarkerEntity.builder() //
				.id("3").name("Rizwan Idrees") //
				.locationAsString(geohash) //
				.locationAsArray(lonLatArray) //
				.locationAsGeoHash(geohash) //
				.build();
		indexQueries.add(buildIndex(location1));
		indexQueries.add(buildIndex(location2));
		indexQueries.add(buildIndex(location3));

		operations.bulkIndex(indexQueries, locationMarkerIndex);
		operations.indexOps(LocationMarkerEntity.class).refresh();
	}

	@Test
	public void shouldFindAuthorMarkersInRangeForGivenCriteriaQuery() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("location").within(new GeoPoint(45.7806d, 3.0875d), "20km"));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria = operations.search(geoLocationCriteriaQuery,
				AuthorMarkerEntity.class, authorMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(1);
		assertThat(geoAuthorsForGeoCriteria.getSearchHit(0).getContent().getName()).isEqualTo("Franck Marchand");
	}

	@Test
	public void shouldFindSelectedAuthorMarkerInRangeForGivenCriteriaQuery() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery2 = new CriteriaQuery(
				new Criteria("name").is("Mohsin Husen").and("location").within(new GeoPoint(51.5171d, 0.1062d), "20km"));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria2 = operations.search(geoLocationCriteriaQuery2,
				AuthorMarkerEntity.class, authorMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria2).hasSize(1);
		assertThat(geoAuthorsForGeoCriteria2.getSearchHit(0).getContent().getName()).isEqualTo("Mohsin Husen");
	}

	@Test
	public void shouldFindStringAnnotatedGeoMarkersInRangeForGivenCriteriaQuery() {

		// given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("locationAsString").within(new GeoPoint(51.000000, 0.100000), "1km"));
		// when
		SearchHits<LocationMarkerEntity> geoAuthorsForGeoCriteria = operations.search(geoLocationCriteriaQuery,
				LocationMarkerEntity.class, locationMarkerIndex);

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
				LocationMarkerEntity.class, locationMarkerIndex);

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
				LocationMarkerEntity.class, locationMarkerIndex);

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
				LocationMarkerEntity.class, locationMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(3);
	}

	@Test
	public void shouldFindAllMarkersForNativeSearchQuery() {

		// given
		loadAnnotationBaseEntities();
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery("locationAsArray").setCorners(52, -1, 50, 1));

		// when
		SearchHits<LocationMarkerEntity> geoAuthorsForGeoCriteria = operations.search(queryBuilder.build(),
				LocationMarkerEntity.class, locationMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria).hasSize(3);
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeoBox() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(new GeoBox(new GeoPoint(53.5171d, 0), new GeoPoint(49.5171d, 0.2062d))));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class, authorMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("Mohsin Husen", "Rizwan Idrees");
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeohash() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(Geohash.stringEncode(0, 53.5171d), Geohash.stringEncode(0.2062d, 49.5171d)));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class, authorMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("Mohsin Husen", "Rizwan Idrees");
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeoPoints() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(new GeoPoint(53.5171d, 0), new GeoPoint(49.5171d, 0.2062d)));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class, authorMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("Mohsin Husen", "Rizwan Idrees");
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingPoints() {

		// given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(new Point(0, 53.5171d), new Point(0.2062d, 49.5171d)));

		// when
		SearchHits<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = operations.search(geoLocationCriteriaQuery3,
				AuthorMarkerEntity.class, authorMarkerIndex);

		// then
		assertThat(geoAuthorsForGeoCriteria3).hasSize(2);
		assertThat(geoAuthorsForGeoCriteria3.stream().map(SearchHit::getContent).map(AuthorMarkerEntity::getName))
				.containsExactlyInAnyOrder("Mohsin Husen", "Rizwan Idrees");
	}

	@Test
	public void shouldFindLocationWithGeoHashPrefix() {

		// given
		loadAnnotationBaseEntities();
		NativeSearchQueryBuilder location1 = new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery("locationAsGeoHash").setCorners("u"));
		NativeSearchQueryBuilder location2 = new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery("locationAsGeoHash").setCorners("u1"));
		NativeSearchQueryBuilder location3 = new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery("locationAsGeoHash").setCorners("u10"));
		NativeSearchQueryBuilder location4 = new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery("locationAsGeoHash").setCorners("u10j"));
		NativeSearchQueryBuilder location5 = new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery("locationAsGeoHash").setCorners("u10j4"));
		NativeSearchQueryBuilder location11 = new NativeSearchQueryBuilder()
				.withFilter(QueryBuilders.geoBoundingBoxQuery("locationAsGeoHash").setCorners("u10j46mkfek"));

		// when
		SearchHits<LocationMarkerEntity> result1 = operations.search(location1.build(), LocationMarkerEntity.class,
				locationMarkerIndex);
		SearchHits<LocationMarkerEntity> result2 = operations.search(location2.build(), LocationMarkerEntity.class,
				locationMarkerIndex);
		SearchHits<LocationMarkerEntity> result3 = operations.search(location3.build(), LocationMarkerEntity.class,
				locationMarkerIndex);
		SearchHits<LocationMarkerEntity> result4 = operations.search(location4.build(), LocationMarkerEntity.class,
				locationMarkerIndex);
		SearchHits<LocationMarkerEntity> result5 = operations.search(location5.build(), LocationMarkerEntity.class,
				locationMarkerIndex);
		SearchHits<LocationMarkerEntity> result11 = operations.search(location11.build(), LocationMarkerEntity.class,
				locationMarkerIndex);

		// then
		assertThat(result1).hasSize(3);
		assertThat(result2).hasSize(3);
		assertThat(result3).hasSize(3);
		assertThat(result4).hasSize(3);
		assertThat(result5).hasSize(3);
		assertThat(result11).hasSize(2);
	}

	private IndexQuery buildIndex(LocationMarkerEntity result) {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}

	/**
	 * @author Franck Marchand
	 * @author Mohsin Husen
	 */
	@Data
	@Document(indexName = "test-index-author-marker-core-geo", replicas = 0, refreshInterval = "-1")
	static class AuthorMarkerEntity {

		@Id private String id;
		private String name;

		private GeoPoint location;

		private AuthorMarkerEntity() {}

		public AuthorMarkerEntity(String id) {
			this.id = id;
		}
	}

	/**
	 * @author Franck Marchand
	 * @author Mohsin Husen
	 */

	static class AuthorMarkerEntityBuilder {

		private AuthorMarkerEntity result;

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

	/**
	 * @author Franck Marchand
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-location-marker-core-geo", replicas = 0, refreshInterval = "-1")
	static class LocationMarkerEntity {

		@Id private String id;
		private String name;

		@GeoPointField private String locationAsString;

		@GeoPointField private double[] locationAsArray;

		@GeoPointField private String locationAsGeoHash;

	}
}
