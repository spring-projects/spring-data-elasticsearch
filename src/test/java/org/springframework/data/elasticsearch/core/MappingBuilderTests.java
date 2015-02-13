/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.builder.SampleInheritedEntityBuilder;
import org.springframework.data.elasticsearch.builder.StockPriceBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Stuart Stevenson
 * @author Jakub Vavrik
 * @author Mohsin Husen
 * @author Keivn Leturc
 * @author Matthias Melitzer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class MappingBuilderTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Test
	public void shouldNotFailOnCircularReference() {
		elasticsearchTemplate.createIndex(SimpleRecursiveEntity.class);
		elasticsearchTemplate.putMapping(SimpleRecursiveEntity.class);
	}

	@Test
	public void testInfiniteLoopAvoidance() throws IOException {
		final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\"" +
				"type\":\"string\",\"index\":\"not_analyzed\",\"search_analyzer\":\"standard\"," +
				"\"index_analyzer\":\"standard\"}}}}";

		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleTransientEntity.class, "mapping", "id",
				null, "", false);
		assertThat(xContentBuilder.string(), is(expected));
	}

	@Test
	public void shouldUseValueFromAnnotationType() throws IOException {
		//Given
		final String expected = "{\"mapping\":{\"properties\":{\"price\":{\"store\":false,\"type\":\"double\"}}}}";

		//When
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(StockPrice.class, "mapping", "id", null, "",
				false);

		//Then
		assertThat(xContentBuilder.string(), is(expected));
	}

	@Test
	public void shouldAddStockPriceDocumentToIndex() throws IOException {
		//Given

		//When
		elasticsearchTemplate.deleteIndex(StockPrice.class);
		elasticsearchTemplate.createIndex(StockPrice.class);
		elasticsearchTemplate.putMapping(StockPrice.class);
		String symbol = "AU";
		double price = 2.34;
		String id = "abc";
		elasticsearchTemplate.index(new StockPriceBuilder(id).symbol(symbol).price(price).buildIndex());
		elasticsearchTemplate.refresh(StockPrice.class, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		List<StockPrice> result = elasticsearchTemplate.queryForList(searchQuery, StockPrice.class);
		//Then
		assertThat(result.size(), is(1));
		StockPrice entry = result.get(0);
		assertThat(entry.getSymbol(), is(symbol));
		assertThat(entry.getPrice(), is(new BigDecimal(price)));
	}

	@Test
	public void shouldCreateMappingForSpecifiedParentType() throws IOException {
		final String expected = "{\"mapping\":{\"_parent\":{\"type\":\"parentType\"},\"properties\":{}}}";
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(MinimalEntity.class, "mapping", "id",
				"parentType", "", false);
		assertThat(xContentBuilder.string(), is(expected));
	}
	
	@Test
	public void shouldCreateMappingForSpecifiedRouting() throws IOException {
		final String expected = "{\"mapping\":{\"_routing\":{\"required\":true},\"properties\":{}}}";
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(MinimalEntity.class, "mapping", "id",
				"", "", true);
		assertThat(xContentBuilder.string(), is(expected));
	}
	
	@Test
	public void shouldCreateMappingForSpecifiedRoutingWithPath() throws IOException {
		final String expected = "{\"mapping\":{\"_routing\":{\"path\":\"somePath.toField\",\"required\":false},\"properties\":{}}}";
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(MinimalEntity.class, "mapping", "id",
				"", "somePath.toField", false);
		assertThat(xContentBuilder.string(), is(expected));
	}

	/*
	 * DATAES-76
	 */
	@Test
	public void shouldBuildMappingWithSuperclass() throws IOException {
		final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\"" +
				"type\":\"string\",\"index\":\"not_analyzed\",\"search_analyzer\":\"standard\"," +
				"\"index_analyzer\":\"standard\"},\"createdDate\":{\"store\":false," +
				"\"type\":\"date\",\"index\":\"not_analyzed\"}}}}";

		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleInheritedEntity.class, "mapping", "id",
				null, "", false);
		assertThat(xContentBuilder.string(), is(expected));
	}

	/*
	 * DATAES-76
	 */
	@Test
	public void shouldAddSampleInheritedEntityDocumentToIndex() throws IOException {
		//Given

		//When
		elasticsearchTemplate.deleteIndex(SampleInheritedEntity.class);
		elasticsearchTemplate.createIndex(SampleInheritedEntity.class);
		elasticsearchTemplate.putMapping(SampleInheritedEntity.class);
		Date createdDate = new Date();
		String message = "msg";
		String id = "abc";
		elasticsearchTemplate.index(new SampleInheritedEntityBuilder(id).createdDate(createdDate).message(message).buildIndex());
		elasticsearchTemplate.refresh(SampleInheritedEntity.class, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		List<SampleInheritedEntity> result = elasticsearchTemplate.queryForList(searchQuery, SampleInheritedEntity.class);
		//Then
		assertThat(result.size(), is(1));
		SampleInheritedEntity entry = result.get(0);
		assertThat(entry.getCreatedDate(), is(createdDate));
		assertThat(entry.getMessage(), is(message));
	}
}
