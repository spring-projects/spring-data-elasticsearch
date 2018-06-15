/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.builder.SampleInheritedEntityBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.*;
import org.springframework.data.elasticsearch.entities.GeoEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Stuart Stevenson
 * @author Jakub Vavrik
 * @author Mohsin Husen
 * @author Keivn Leturc
 * @author Nordine Bittich
 * @author Don Wellington
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class MappingBuilderTests {

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	private String xContentBuilderToString(XContentBuilder builder) {
		builder.close();
		ByteArrayOutputStream bos = (ByteArrayOutputStream) builder.getOutputStream();
		return bos.toString();
	}

	@Test
	public void shouldNotFailOnCircularReference() {
		elasticsearchTemplate.deleteIndex(SimpleRecursiveEntity.class);
		elasticsearchTemplate.createIndex(SimpleRecursiveEntity.class);
		elasticsearchTemplate.putMapping(SimpleRecursiveEntity.class);
		elasticsearchTemplate.refresh(SimpleRecursiveEntity.class);
	}

	@Test
	public void testInfiniteLoopAvoidance() throws IOException {
		final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\""
				+ "type\":\"text\",\"index\":false," + "\"analyzer\":\"standard\"}}}}";

		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleTransientEntity.class, "mapping", "id", null);
		assertThat(xContentBuilderToString(xContentBuilder), is(expected));
	}

	@Test
	public void shouldUseValueFromAnnotationType() throws IOException {
		// Given
		final String expected = "{\"mapping\":{\"properties\":{\"price\":{\"store\":false,\"type\":\"double\"}}}}";

		// When
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(StockPrice.class, "mapping", "id", null);

		// Then
		assertThat(xContentBuilderToString(xContentBuilder), is(expected));
	}

	@Test
	public void shouldAddStockPriceDocumentToIndex() throws IOException {
		// Given

		// When
		elasticsearchTemplate.deleteIndex(StockPrice.class);
		elasticsearchTemplate.createIndex(StockPrice.class);
		elasticsearchTemplate.putMapping(StockPrice.class);
		String symbol = "AU";
		double price = 2.34;
		String id = "abc";
		elasticsearchTemplate
				.index(buildIndex(StockPrice.builder().id(id).symbol(symbol).price(new BigDecimal(price)).build()));
		elasticsearchTemplate.refresh(StockPrice.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		List<StockPrice> result = elasticsearchTemplate.queryForList(searchQuery, StockPrice.class);
		// Then
		assertThat(result.size(), is(1));
		StockPrice entry = result.get(0);
		assertThat(entry.getSymbol(), is(symbol));
		assertThat(entry.getPrice(), is(new BigDecimal(price)));
	}

	@Test
	public void shouldCreateMappingForSpecifiedParentType() throws IOException {
		final String expected = "{\"mapping\":{\"_parent\":{\"type\":\"parentType\"},\"properties\":{}}}";
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(MinimalEntity.class, "mapping", "id", "parentType");
		assertThat(xContentBuilderToString(xContentBuilder), is(expected));
	}

	/*
	 * DATAES-76
	 */
	@Test
	public void shouldBuildMappingWithSuperclass() throws IOException {
		final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\""
				+ "type\":\"text\",\"index\":false,\"analyzer\":\"standard\"}" + ",\"createdDate\":{\"store\":false,"
				+ "\"type\":\"date\",\"index\":false}}}}";

		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleInheritedEntity.class, "mapping", "id", null);
		assertThat(xContentBuilderToString(xContentBuilder), is(expected));
	}

	/*
	 * DATAES-76
	 */
	@Test
	public void shouldAddSampleInheritedEntityDocumentToIndex() throws IOException {
		// Given

		// When
		elasticsearchTemplate.deleteIndex(SampleInheritedEntity.class);
		elasticsearchTemplate.createIndex(SampleInheritedEntity.class);
		elasticsearchTemplate.putMapping(SampleInheritedEntity.class);
		Date createdDate = new Date();
		String message = "msg";
		String id = "abc";
		elasticsearchTemplate
				.index(new SampleInheritedEntityBuilder(id).createdDate(createdDate).message(message).buildIndex());
		elasticsearchTemplate.refresh(SampleInheritedEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		List<SampleInheritedEntity> result = elasticsearchTemplate.queryForList(searchQuery, SampleInheritedEntity.class);
		// Then
		assertThat(result.size(), is(1));
		SampleInheritedEntity entry = result.get(0);
		assertThat(entry.getCreatedDate(), is(createdDate));
		assertThat(entry.getMessage(), is(message));
	}

	@Test
	public void shouldBuildMappingsForGeoPoint() throws IOException {
		// given

		// when
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(GeoEntity.class, "mapping", "id", null);

		// then
		final String result = xContentBuilderToString(xContentBuilder);

		assertThat(result, containsString("\"pointA\":{\"type\":\"geo_point\""));
		assertThat(result, containsString("\"pointB\":{\"type\":\"geo_point\""));
		assertThat(result, containsString("\"pointC\":{\"type\":\"geo_point\""));
		assertThat(result, containsString("\"pointD\":{\"type\":\"geo_point\""));
	}

	/**
	 * DATAES-260 - StacOverflow when two reverse relationship.
	 */
	@Test
	public void shouldHandleReverseRelationship() {
		// given
		elasticsearchTemplate.createIndex(User.class);
		elasticsearchTemplate.putMapping(User.class);
		elasticsearchTemplate.createIndex(Group.class);
		elasticsearchTemplate.putMapping(Group.class);
		// when

		// then

	}

	@Test
	public void shouldMapBooks() {
		// given
		elasticsearchTemplate.createIndex(Book.class);
		elasticsearchTemplate.putMapping(Book.class);
		// when
		// then

	}

	@Test // DATAES-420
	public void shouldUseBothAnalyzer() {
		// given
		elasticsearchTemplate.deleteIndex(Book.class);
		elasticsearchTemplate.createIndex(Book.class);
		elasticsearchTemplate.putMapping(Book.class);

		// when
		Map mapping = elasticsearchTemplate.getMapping(Book.class);
		Map descriptionMapping = (Map) ((Map) mapping.get("properties")).get("description");
		Map prefixDescription = (Map) ((Map) descriptionMapping.get("fields")).get("prefix");

		// then
		assertThat(prefixDescription.size(), is(3));
		assertThat(prefixDescription.get("type"), equalTo("text"));
		assertThat(prefixDescription.get("analyzer"), equalTo("stop"));
		assertThat(prefixDescription.get("search_analyzer"), equalTo("standard"));
		assertThat(descriptionMapping.get("type"), equalTo("text"));
		assertThat(descriptionMapping.get("analyzer"), equalTo("whitespace"));
	}
}
