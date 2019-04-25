/*
 * Copyright 2013-2019 the original author or authors.
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

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.builder.SampleInheritedEntityBuilder;
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
 * @author Nordine Bittich
 * @author Don Wellington
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class MappingBuilderTests extends MappingContextBaseTests {

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Test
	public void shouldNotFailOnCircularReference() {
		elasticsearchTemplate.deleteIndex(SimpleRecursiveEntity.class);
		elasticsearchTemplate.createIndex(SimpleRecursiveEntity.class);
		elasticsearchTemplate.putMapping(SimpleRecursiveEntity.class);
		elasticsearchTemplate.refresh(SimpleRecursiveEntity.class);
	}

	@Test
	public void testInfiniteLoopAvoidance() throws IOException, JSONException {
		final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\""
				+ "type\":\"text\",\"index\":false," + "\"analyzer\":\"standard\"}}}}";

		String mapping = getMappingBuilder().buildMapping(SampleTransientEntity.class);

		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test
	public void shouldUseValueFromAnnotationType() throws IOException, JSONException {
		// Given
		final String expected = "{\"price\":{\"properties\":{\"price\":{\"store\":false,\"type\":\"double\"}}}}";

		// When
		String mapping = getMappingBuilder().buildMapping(StockPrice.class);

		// Then
		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test // DATAES-530
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
				.index(buildIndex(StockPrice.builder().id(id).symbol(symbol).price(BigDecimal.valueOf(price)).build()));
		elasticsearchTemplate.refresh(StockPrice.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		List<StockPrice> result = elasticsearchTemplate.queryForList(searchQuery, StockPrice.class);
		// Then
		assertThat(result.size(), is(1));
		StockPrice entry = result.get(0);
		assertThat(entry.getSymbol(), is(symbol));
		assertThat(entry.getPrice(), is(BigDecimal.valueOf(price)));
	}

	@Test
	public void shouldCreateMappingForSpecifiedParentType() throws IOException, JSONException {
		final String expected = "{\"mapping\":{\"_parent\":{\"type\":\"parentType\"},\"properties\":{}}}";

		String mapping = getMappingBuilder().buildMapping(MinimalChildEntity.class);

		JSONAssert.assertEquals(expected, mapping, false);
	}

	/*
	 * DATAES-76
	 */
	@Test
	public void shouldBuildMappingWithSuperclass() throws IOException, JSONException {
		final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\""
				+ "type\":\"text\",\"index\":false,\"analyzer\":\"standard\"}" + ",\"createdDate\":{\"store\":false,"
				+ "\"type\":\"date\",\"index\":false}}}}";

		String mapping = getMappingBuilder().buildMapping(SampleInheritedEntity.class);

		JSONAssert.assertEquals(expected, mapping, false);
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
	public void shouldBuildMappingsForGeoPoint() throws IOException, JSONException {
		// given
		String expected = "{\"geo-test-index\": {\"properties\": {" + "\"pointA\":{\"type\":\"geo_point\"},"
				+ "\"pointB\":{\"type\":\"geo_point\"}," + "\"pointC\":{\"type\":\"geo_point\"},"
				+ "\"pointD\":{\"type\":\"geo_point\"}" + "}}}";

		// when
		String mapping;
		mapping = getMappingBuilder().buildMapping(GeoEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
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

	@Test // DATAES-492
	public void shouldUseKeywordNormalizer() throws IOException {

		// given
		elasticsearchTemplate.deleteIndex(NormalizerEntity.class);
		elasticsearchTemplate.createIndex(NormalizerEntity.class);
		elasticsearchTemplate.putMapping(NormalizerEntity.class);

		// when
		Map mapping = elasticsearchTemplate.getMapping(NormalizerEntity.class);
		Map properties = (Map) mapping.get("properties");
		Map fieldName = (Map) properties.get("name");
		Map fieldDescriptionLowerCase = (Map) ((Map) ((Map) properties.get("description")).get("fields")).get("lower_case");

		// then
		assertThat(fieldName.get("type"), equalTo("keyword"));
		assertThat(fieldName.get("normalizer"), equalTo("lower_case_normalizer"));
		assertThat(fieldDescriptionLowerCase.get("type"), equalTo("keyword"));
		assertThat(fieldDescriptionLowerCase.get("normalizer"), equalTo("lower_case_normalizer"));
	}

	@Test // DATAES-503
	public void shouldUseCopyTo() throws IOException {

		// given
		elasticsearchTemplate.deleteIndex(CopyToEntity.class);
		elasticsearchTemplate.createIndex(CopyToEntity.class);
		elasticsearchTemplate.putMapping(CopyToEntity.class);

		// when
		Map mapping = elasticsearchTemplate.getMapping(CopyToEntity.class);
		Map properties = (Map) mapping.get("properties");
		Map fieldFirstName = (Map) properties.get("firstName");
		Map fieldLastName = (Map) properties.get("lastName");

		// then
		List<String> copyToValue = Arrays.asList("name");
		assertThat(fieldFirstName.get("copy_to"), equalTo(copyToValue));
		assertThat(fieldLastName.get("copy_to"), equalTo(copyToValue));
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnId() throws IOException, JSONException {
		// given
		final String expected = "{\"fieldname-type\":{\"properties\":{"
				+ "\"id-property\":{\"type\":\"keyword\",\"index\":true}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildMapping(FieldNameEntity.IdEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnText() throws IOException, JSONException {
		// given
		final String expected = "{\"fieldname-type\":{\"properties\":{"
				+ "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"text-property\":{\"store\":false,\"type\":\"text\"}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildMapping(FieldNameEntity.TextEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnMapping() throws IOException, JSONException {
		// given
		final String expected = "{\"fieldname-type\":{\"properties\":{"
				+ "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"mapping-property\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildMapping(FieldNameEntity.MappingEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnGeoPoint() throws IOException, JSONException {
		// given
		final String expected = "{\"fieldname-type\":{\"properties\":{"
				+ "\"id-property\":{\"type\":\"keyword\",\"index\":true}," + "\"geopoint-property\":{\"type\":\"geo_point\"}"
				+ "}}}";

		// when
		String mapping = getMappingBuilder().buildMapping(FieldNameEntity.GeoPointEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnCircularEntity() throws IOException, JSONException {
		// given
		final String expected = "{\"fieldname-type\":{\"properties\":{"
				+ "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"circular-property\":{\"type\":\"object\",\"properties\":{\"id-property\":{\"store\":false}}}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildMapping(FieldNameEntity.CircularEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnCompletion() throws IOException, JSONException {
		// given
		final String expected = "{\"fieldname-type\":{\"properties\":{"
				+ "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"completion-property\":{\"type\":\"completion\",\"max_input_length\":100,\"preserve_position_increments\":true,\"preserve_separators\":true,\"search_analyzer\":\"simple\",\"analyzer\":\"simple\"},\"completion-property\":{\"store\":false}"
				+ "}}}";

		// when
		String mapping = getMappingBuilder().buildMapping(FieldNameEntity.CompletionEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnMultiField() throws IOException, JSONException {
		// given
		final String expected = "{\"fieldname-type\":{\"properties\":{"
				+ "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"multifield-property\":{\"store\":false,\"type\":\"text\",\"analyzer\":\"whitespace\",\"fields\":{\"prefix\":{\"store\":false,\"type\":\"text\",\"analyzer\":\"stop\",\"search_analyzer\":\"standard\"}}}"
				+ "}}}";

		// when
		String mapping = getMappingBuilder().buildMapping(FieldNameEntity.MultiFieldEntity.class);

		// then
		JSONAssert.assertEquals(expected, mapping, false);
	}
}
