/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.geo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.index.query.FilterBuilders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Artur Konczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateGeoTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	private void loadClassBaseEntities() {
		elasticsearchTemplate.deleteIndex(AuthorMarkerEntity.class);
		elasticsearchTemplate.createIndex(AuthorMarkerEntity.class);
		elasticsearchTemplate.refresh(AuthorMarkerEntity.class, true);
		elasticsearchTemplate.putMapping(AuthorMarkerEntity.class);

		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		indexQueries.add(new AuthorMarkerEntityBuilder("1").name("Franck Marchand").location(45.7806d, 3.0875d).buildIndex());
		indexQueries.add(new AuthorMarkerEntityBuilder("2").name("Mohsin Husen").location(51.5171d, 0.1062d).buildIndex());
		indexQueries.add(new AuthorMarkerEntityBuilder("3").name("Rizwan Idrees").location(51.5171d, 0.1062d).buildIndex());
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(AuthorMarkerEntity.class, true);
	}

	private void loadAnnotationBaseEntities() {
		elasticsearchTemplate.deleteIndex(AuthorMarkerAnnotatedEntity.class);
		elasticsearchTemplate.createIndex(AuthorMarkerAnnotatedEntity.class);
		elasticsearchTemplate.refresh(AuthorMarkerAnnotatedEntity.class, true);
		elasticsearchTemplate.putMapping(AuthorMarkerAnnotatedEntity.class);

		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		double[] latLonArray = {0.100000, 51.000000};
		String lonLatString = "51.000000, 0.100000";
		String geohash = "u1044k2bd6u";
		indexQueries.add(new AuthorMarkerAnnotatedEntityBuilder("2").name("Mohsin Husen").location(geohash.substring(0, 5)).additionalLocation(latLonArray).buildIndex());
		indexQueries.add(new AuthorMarkerAnnotatedEntityBuilder("1").name("Artur Konczak").location(lonLatString).additionalLocation(latLonArray).buildIndex());
		indexQueries.add(new AuthorMarkerAnnotatedEntityBuilder("3").name("Rizwan Idrees").location(geohash).additionalLocation(latLonArray).buildIndex());

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(AuthorMarkerEntity.class, true);
	}

	@Test
	public void shouldPutMappingForGivenEntityWithGeoLocation() throws Exception {
		//given
		Class entity = AuthorMarkerEntity.class;
		elasticsearchTemplate.createIndex(entity);
		//when
		assertThat(elasticsearchTemplate.putMapping(entity), is(true));
	}

	@Test
	public void shouldFindAuthorMarkersInRangeForGivenCriteriaQuery() {
		//given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("location").within(new GeoPoint(45.7806d, 3.0875d), "20km"));
		//when
		List<AuthorMarkerEntity> geoAuthorsForGeoCriteria = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery, AuthorMarkerEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria.size(), is(1));
		assertEquals("Franck Marchand", geoAuthorsForGeoCriteria.get(0).getName());
	}

	@Test
	public void shouldFindSelectedAuthorMarkerInRangeForGivenCriteriaQuery() {
		//given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery2 = new CriteriaQuery(
				new Criteria("name").is("Mohsin Husen").and("location").within(new GeoPoint(51.5171d, 0.1062d), "20km"));
		//when
		List<AuthorMarkerEntity> geoAuthorsForGeoCriteria2 = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery2, AuthorMarkerEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria2.size(), is(1));
		assertEquals("Mohsin Husen", geoAuthorsForGeoCriteria2.get(0).getName());
	}

	@Test
	public void shouldFindStringAnnotatedGeoMarkersInRangeForGivenCriteriaQuery() {
		//given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("location").within(new GeoPoint(51.000000, 0.100000), "1km"));
		//when
		List<AuthorMarkerAnnotatedEntity> geoAuthorsForGeoCriteria = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery, AuthorMarkerAnnotatedEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria.size(), is(3));
	}

	@Test
	public void shouldFindDoubleAnnotatedGeoMarkersInRangeForGivenCriteriaQuery() {
		//given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("additionalLocation").within(new GeoPoint(51.001000, 0.10100), "1km"));
		//when
		List<AuthorMarkerAnnotatedEntity> geoAuthorsForGeoCriteria = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery, AuthorMarkerAnnotatedEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria.size(), is(3));
	}

	@Test
	public void shouldFindAnnotatedGeoMarkersInRangeForGivenCriteriaQuery() {
		//given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("additionalLocation").within("51.001000, 0.10100", "1km"));
		//when
		List<AuthorMarkerAnnotatedEntity> geoAuthorsForGeoCriteria = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery, AuthorMarkerAnnotatedEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria.size(), is(3));
	}

	@Test
	public void shouldFindAnnotatedGeoMarkersInRangeForGivenCriteriaQueryUsingGeohashLocation() {
		//given
		loadAnnotationBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
				new Criteria("additionalLocation").within("u1044", "1km"));
		//when
		List<AuthorMarkerAnnotatedEntity> geoAuthorsForGeoCriteria = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery, AuthorMarkerAnnotatedEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria.size(), is(3));
	}

	@Test
	public void shouldFindAllMarkersForNativeSearchQuery() {
		//Given
		loadAnnotationBaseEntities();
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withFilter(FilterBuilders.geoBoundingBoxFilter("additionalLocation").topLeft(52, -1).bottomRight(50, 1));
		//When
		List<AuthorMarkerAnnotatedEntity> geoAuthorsForGeoCriteria = elasticsearchTemplate.queryForList(queryBuilder.build(), AuthorMarkerAnnotatedEntity.class);
		//Then
		assertThat(geoAuthorsForGeoCriteria.size(), is(3));
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeoBox() {
		//given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(
						new GeoBox(new GeoPoint(53.5171d, 0),
								new GeoPoint(49.5171d, 0.2062d))
				)
		);
		//when
		List<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery3, AuthorMarkerEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria3.size(), is(2));
		assertThat(geoAuthorsForGeoCriteria3, containsInAnyOrder(hasProperty("name", equalTo("Mohsin Husen")), hasProperty("name", equalTo("Rizwan Idrees"))));
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeohash() {
		//given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(GeoHashUtils.encode(53.5171d, 0), GeoHashUtils.encode(49.5171d, 0.2062d)));
		//when
		List<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery3, AuthorMarkerEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria3.size(), is(2));
		assertThat(geoAuthorsForGeoCriteria3, containsInAnyOrder(hasProperty("name", equalTo("Mohsin Husen")), hasProperty("name", equalTo("Rizwan Idrees"))));
	}

	@Test
	public void shouldFindAuthorMarkersInBoxForGivenCriteriaQueryUsingGeoPoints() {
		//given
		loadClassBaseEntities();
		CriteriaQuery geoLocationCriteriaQuery3 = new CriteriaQuery(
				new Criteria("location").boundedBy(
						new GeoPoint(53.5171d, 0),
						new GeoPoint(49.5171d, 0.2062d))
		);
		//when
		List<AuthorMarkerEntity> geoAuthorsForGeoCriteria3 = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery3, AuthorMarkerEntity.class);

		//then
		assertThat(geoAuthorsForGeoCriteria3.size(), is(2));
		assertThat(geoAuthorsForGeoCriteria3, containsInAnyOrder(hasProperty("name", equalTo("Mohsin Husen")), hasProperty("name", equalTo("Rizwan Idrees"))));
	}
}
