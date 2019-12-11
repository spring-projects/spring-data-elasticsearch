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

package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.data.Percentage;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Stuart Stevenson
 * @author Jakub Vavrik
 * @author Mohsin Husen
 * @author Keivn Leturc
 * @author Nordine Bittich
 * @author Don Wellington
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 * @author Xiao Yu
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
public class MappingBuilderTests extends MappingContextBaseTests {

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@BeforeEach
	public void before() {

		indexOperations = operations.getIndexOperations();

		indexOperations.deleteIndex(StockPrice.class);
		indexOperations.deleteIndex(SimpleRecursiveEntity.class);
		indexOperations.deleteIndex(StockPrice.class);
		indexOperations.deleteIndex(SampleInheritedEntity.class);
		indexOperations.deleteIndex(User.class);
		indexOperations.deleteIndex(Group.class);
		indexOperations.deleteIndex(Book.class);
		indexOperations.deleteIndex(NormalizerEntity.class);
		indexOperations.deleteIndex(CopyToEntity.class);
	}

	@Test
	public void shouldNotFailOnCircularReference() {

		indexOperations.createIndex(SimpleRecursiveEntity.class);
		indexOperations.putMapping(SimpleRecursiveEntity.class);
		indexOperations.refresh(SimpleRecursiveEntity.class);
	}

	@Test // DATAES-568
	public void testInfiniteLoopAvoidance() throws IOException, JSONException {

		String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\""
				+ "type\":\"text\",\"index\":false," + "\"analyzer\":\"standard\"}}}}";

		String mapping = getMappingBuilder().buildPropertyMapping(SampleTransientEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseValueFromAnnotationType() throws IOException, JSONException {

		// Given
		String expected = "{\"price\":{\"properties\":{\"price\":{\"type\":\"double\"}}}}";

		// When
		String mapping = getMappingBuilder().buildPropertyMapping(StockPrice.class);

		// Then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-530
	public void shouldAddStockPriceDocumentToIndex() {

		// Given

		// When
		indexOperations.createIndex(StockPrice.class);
		indexOperations.putMapping(StockPrice.class);
		String symbol = "AU";
		double price = 2.34;
		String id = "abc";

		IndexCoordinates index = IndexCoordinates.of("test-index-stock-mapping-builder").withTypes("price");
		operations.index(buildIndex(StockPrice.builder() //
				.id(id) //
				.symbol(symbol) //
				.price(BigDecimal.valueOf(price)) //
				.build()), index);
		indexOperations.refresh(StockPrice.class);

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		SearchHits<StockPrice> result = operations.search(searchQuery, StockPrice.class, index);

		// Then
		assertThat(result).hasSize(1);
		StockPrice entry = result.getSearchHit(0).getContent();
		assertThat(entry.getSymbol()).isEqualTo(symbol);
		assertThat(entry.getPrice()).isCloseTo(BigDecimal.valueOf(price), Percentage.withPercentage(0.01));
	}

	@Test // DATAES-568
	public void shouldCreateMappingForSpecifiedParentType() throws IOException, JSONException {

		String expected = "{\"mapping\":{\"_parent\":{\"type\":\"parentType\"},\"properties\":{}}}";

		String mapping = getMappingBuilder().buildPropertyMapping(MinimalChildEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-76
	public void shouldBuildMappingWithSuperclass() throws IOException, JSONException {

		String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\""
				+ "type\":\"text\",\"index\":false,\"analyzer\":\"standard\"}" + ",\"createdDate\":{"
				+ "\"type\":\"date\",\"index\":false}}}}";

		String mapping = getMappingBuilder().buildPropertyMapping(SampleInheritedEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-76
	public void shouldAddSampleInheritedEntityDocumentToIndex() {

		// given

		// when
		indexOperations.createIndex(SampleInheritedEntity.class);
		indexOperations.putMapping(SampleInheritedEntity.class);
		Date createdDate = new Date();
		String message = "msg";
		String id = "abc";
		IndexCoordinates index = IndexCoordinates.of("test-index-sample-inherited-mapping-builder").withTypes("mapping");
		operations.index(new SampleInheritedEntityBuilder(id).createdDate(createdDate).message(message).buildIndex(),
				index);
		operations.refresh(SampleInheritedEntity.class);

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		SearchHits<SampleInheritedEntity> result = operations.search(searchQuery, SampleInheritedEntity.class, index);

		// then
		assertThat(result).hasSize(1);

		SampleInheritedEntity entry = result.getSearchHit(0).getContent();
		assertThat(entry.getCreatedDate()).isEqualTo(createdDate);
		assertThat(entry.getMessage()).isEqualTo(message);
	}

	@Test // DATAES-568
	public void shouldBuildMappingsForGeoPoint() throws IOException, JSONException {

		// given
		String expected = "{\"geo-test-index\": {\"properties\": {" + "\"pointA\":{\"type\":\"geo_point\"},"
				+ "\"pointB\":{\"type\":\"geo_point\"}," + "\"pointC\":{\"type\":\"geo_point\"},"
				+ "\"pointD\":{\"type\":\"geo_point\"}" + "}}}";

		// when
		String mapping;
		mapping = getMappingBuilder().buildPropertyMapping(GeoEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-260 - StackOverflow when two reverse relationship.
	public void shouldHandleReverseRelationship() {

		// given
		indexOperations.createIndex(User.class);
		indexOperations.putMapping(User.class);
		indexOperations.createIndex(Group.class);
		indexOperations.putMapping(Group.class);

		// when

		// then
	}

	@Test // DATAES-285
	public void shouldMapBooks() {

		// given
		indexOperations.createIndex(Book.class);
		indexOperations.putMapping(Book.class);

		// when

		// then
	}

	@Test // DATAES-420
	public void shouldUseBothAnalyzer() {

		// given
		indexOperations.createIndex(Book.class);
		indexOperations.putMapping(Book.class);

		// when
		Map mapping = operations.getMapping(Book.class);
		Map descriptionMapping = (Map) ((Map) mapping.get("properties")).get("description");
		Map prefixDescription = (Map) ((Map) descriptionMapping.get("fields")).get("prefix");

		// then
		assertThat(prefixDescription).hasSize(3);
		assertThat(prefixDescription.get("type")).isEqualTo("text");
		assertThat(prefixDescription.get("analyzer")).isEqualTo("stop");
		assertThat(prefixDescription.get("search_analyzer")).isEqualTo("standard");
		assertThat(descriptionMapping.get("type")).isEqualTo("text");
		assertThat(descriptionMapping.get("analyzer")).isEqualTo("whitespace");
	}

	@Test // DATAES-492
	public void shouldUseKeywordNormalizer() {

		// given
		operations.createIndex(NormalizerEntity.class);
		operations.putMapping(NormalizerEntity.class);

		// when
		Map mapping = operations.getMapping(NormalizerEntity.class);
		Map properties = (Map) mapping.get("properties");
		Map fieldName = (Map) properties.get("name");
		Map fieldDescriptionLowerCase = (Map) ((Map) ((Map) properties.get("description")).get("fields")).get("lower_case");

		// then
		assertThat(fieldName.get("type")).isEqualTo("keyword");
		assertThat(fieldName.get("normalizer")).isEqualTo("lower_case_normalizer");
		assertThat(fieldDescriptionLowerCase.get("type")).isEqualTo("keyword");
		assertThat(fieldDescriptionLowerCase.get("normalizer")).isEqualTo("lower_case_normalizer");
	}

	@Test // DATAES-503
	public void shouldUseCopyTo() {

		// given
		operations.createIndex(CopyToEntity.class);
		operations.putMapping(CopyToEntity.class);

		// when
		Map mapping = operations.getMapping(CopyToEntity.class);
		Map properties = (Map) mapping.get("properties");
		Map fieldFirstName = (Map) properties.get("firstName");
		Map fieldLastName = (Map) properties.get("lastName");

		// then
		List<String> copyToValue = Collections.singletonList("name");
		assertThat(fieldFirstName.get("copy_to")).isEqualTo(copyToValue);
		assertThat(fieldLastName.get("copy_to")).isEqualTo(copyToValue);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnId() throws IOException, JSONException {

		// given
		String expected = "{\"fieldname-type\":{\"properties\":{" + "\"id-property\":{\"type\":\"keyword\",\"index\":true}"
				+ "}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.IdEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnText() throws IOException, JSONException {

		// given
		String expected = "{\"fieldname-type\":{\"properties\":{" + "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"text-property\":{\"type\":\"text\"}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.TextEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnMapping() throws IOException, JSONException {

		// given
		String expected = "{\"fieldname-type\":{\"properties\":{" + "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"mapping-property\":{\"type\":\"string\",\"analyzer\":\"standard_lowercase_asciifolding\"}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.MappingEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnGeoPoint() throws IOException, JSONException {

		// given
		String expected = "{\"fieldname-type\":{\"properties\":{" + "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"geopoint-property\":{\"type\":\"geo_point\"}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.GeoPointEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnCircularEntity() throws IOException, JSONException {

		// given
		String expected = "{\"fieldname-type\":{\"properties\":{" + "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"circular-property\":{\"type\":\"object\",\"properties\":{\"id-property\":{}}}" + "}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.CircularEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnCompletion() throws IOException, JSONException {

		// given
		String expected = "{\"fieldname-type\":{\"properties\":{" + "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"completion-property\":{\"type\":\"completion\",\"max_input_length\":100,\"preserve_position_increments\":true,\"preserve_separators\":true,\"search_analyzer\":\"simple\",\"analyzer\":\"simple\"},\"completion-property\":{}"
				+ "}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.CompletionEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnMultiField() throws IOException, JSONException {

		// given
		String expected = "{\"fieldname-type\":{\"properties\":{" + "\"id-property\":{\"type\":\"keyword\",\"index\":true},"
				+ "\"multifield-property\":{\"type\":\"text\",\"analyzer\":\"whitespace\",\"fields\":{\"prefix\":{\"type\":\"text\",\"analyzer\":\"stop\",\"search_analyzer\":\"standard\"}}}"
				+ "}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.MultiFieldEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-639
	public void shouldUseIgnoreAbove() throws IOException, JSONException {

		// given
		String expected = "{\"ignore-above-type\":{\"properties\":{\"message\":{\"type\":\"keyword\",\"ignore_above\":10}}}}";

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(IgnoreAboveEntity.class);

		// then
		assertEquals(expected, mapping, false);
	}

	@Test
	public void shouldSetFieldMappingProperties() throws JSONException, IOException {
		String expected = "{\n" + //
				"    \"fmp\": {\n" + //
				"        \"properties\": {\n" + //
				"            \"storeTrue\": {\n" + //
				"                \"store\": true\n" + //
				"            },\n" + //
				"            \"storeFalse\": {},\n" + //
				"            \"indexTrue\": {},\n" + //
				"            \"indexFalse\": {\n" + //
				"                \"index\": false\n" + //
				"            },\n" + //
				"            \"coerceTrue\": {},\n" + //
				"            \"coerceFalse\": {\n" + //
				"                \"coerce\": false\n" + //
				"            },\n" + //
				"            \"fielddataTrue\": {\n" + //
				"                \"fielddata\": true\n" + //
				"            },\n" + //
				"            \"fielddataFalse\": {},\n" + //
				"            \"type\": {\n" + //
				"                \"type\": \"integer\"\n" + //
				"            },\n" + //
				"            \"ignoreAbove\": {\n" + //
				"                \"ignore_above\": 42\n" + //
				"            },\n" + //
				"            \"copyTo\": {\n" + //
				"                \"copy_to\": [\"foo\", \"bar\"]\n" + //
				"            },\n" + //
				"            \"date\": {\n" + //
				"                \"type\": \"date\",\n" + //
				"                \"format\": \"YYYYMMDD\"\n" + //
				"            },\n" + //
				"            \"analyzers\": {\n" + //
				"                \"analyzer\": \"ana\",\n" + //
				"                \"search_analyzer\": \"sana\",\n" + //
				"                \"normalizer\": \"norma\"\n" + //
				"            },\n" + //
				"            \"docValuesTrue\": {\n" + //
				"                \"type\": \"keyword\"\n" + //
				"            },\n" + //
				"            \"docValuesFalse\": {\n" + //
				"                \"type\": \"keyword\",\n" + //
				"                \"doc_values\": false\n" + //
				"            },\n" + //
				"            \"ignoreMalformedFalse\": {},\n" + //
				"            \"ignoreMalformedTrue\": {\n" + //
				"                \"ignore_malformed\": true\n" + //
				"            },\n" + //
				"            \"indexPhrasesTrue\": {\n" + //
				"                \"index_phrases\": true\n" + //
				"            },\n" + //
				"            \"indexPhrasesFalse\": {},\n" + //
				"            \"indexOptionsNone\": {},\n" + //
				"            \"indexOptionsPositions\": {\n" + //
				"                \"index_options\": \"positions\"\n" + //
				"            },\n" + //
				"            \"defaultIndexPrefixes\": {\n" + //
				"                \"index_prefixes\":{}" + //
				"            },\n" + //
				"            \"customIndexPrefixes\": {\n" + //
				"                \"index_prefixes\":{\"min_chars\":1,\"max_chars\":10}" + //
				"            },\n" + //
				"            \"normsTrue\": {},\n" + //
				"            \"normsFalse\": {\n" + //
				"                \"norms\": false\n" + //
				"            },\n" + //
				"            \"nullValueNotSet\": {},\n" + //
				"            \"nullValueSet\": {\n" + //
				"                \"null_value\": \"NULLNULL\"\n" + //
				"            },\n" + //
				"            \"positionIncrementGap\": {\n" + //
				"                \"position_increment_gap\": 42\n" + //
				"            },\n" + //
				"            \"similarityDefault\": {},\n" + //
				"            \"similarityBoolean\": {\n" + //
				"                \"similarity\": \"boolean\"\n" + //
				"            },\n" + //
				"            \"termVectorDefault\": {},\n" + //
				"            \"termVectorWithOffsets\": {\n" + //
				"                \"term_vector\": \"with_offsets\"\n" + //
				"            },\n" + //
				"            \"scaledFloat\": {\n" + //
				"                \"type\": \"scaled_float\",\n" + //
				"                \"scaling_factor\": 100.0\n" + //
				"            }\n" + //
				"        }\n" + //
				"    }\n" + //
				"}\n"; //

		// when
		String mapping = getMappingBuilder().buildPropertyMapping(FieldMappingParameters.class);

		// then
		assertEquals(expected, mapping, true);
	}

	@Test
	void shouldWriteDynamicMappingSettings() throws IOException, JSONException {

		String expected = "{\n" + //
				"  \"dms\": {\n" + //
				"    \"dynamic\": \"false\",\n" + //
				"    \"properties\": {\n" + //
				"      \"author\": {\n" + //
				"        \"dynamic\": \"strict\",\n" + //
				"        \"type\": \"object\",\n" + //
				"        \"properties\": {}\n" + //
				"      }\n" + //
				"    }\n" + //
				"  }\n" + //
				"}\n";

		String mapping = getMappingBuilder().buildPropertyMapping(ConfigureDynamicMappingEntity.class);

		assertEquals(expected, mapping, true);
	}

	/**
	 * @author Xiao Yu
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "ignore-above-index", type = "ignore-above-type")
	static class IgnoreAboveEntity {

		@Id private String id;

		@Field(type = FieldType.Keyword, ignoreAbove = 10) private String message;
	}

	/**
	 * @author Peter-Josef Meisch
	 */
	@SuppressWarnings("unused")
	static class FieldNameEntity {

		@Document(indexName = "fieldname-index", type = "fieldname-type")
		static class IdEntity {
			@Id @Field("id-property") private String id;
		}

		@Document(indexName = "fieldname-index", type = "fieldname-type")
		static class TextEntity {

			@Id @Field("id-property") private String id;

			@Field(name = "text-property", type = FieldType.Text) //
			private String textProperty;
		}

		@Document(indexName = "fieldname-index", type = "fieldname-type")
		static class MappingEntity {

			@Id @Field("id-property") private String id;

			@Field("mapping-property") @Mapping(mappingPath = "/mappings/test-field-analyzed-mappings.json") //
			private byte[] mappingProperty;
		}

		@Document(indexName = "fieldname-index", type = "fieldname-type")
		static class GeoPointEntity {

			@Id @Field("id-property") private String id;

			@Field("geopoint-property") private GeoPoint geoPoint;
		}

		@Document(indexName = "fieldname-index", type = "fieldname-type")
		static class CircularEntity {

			@Id @Field("id-property") private String id;

			@Field(name = "circular-property", type = FieldType.Object, ignoreFields = { "circular-property" }) //
			private CircularEntity circularProperty;
		}

		@Document(indexName = "fieldname-index", type = "fieldname-type")
		static class CompletionEntity {

			@Id @Field("id-property") private String id;

			@Field("completion-property") @CompletionField(maxInputLength = 100) //
			private Completion suggest;
		}

		@Document(indexName = "fieldname-index", type = "fieldname-type")
		static class MultiFieldEntity {

			@Id @Field("id-property") private String id;

			@Field("multifield-property") //
			@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"), otherFields = {
					@InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop", searchAnalyzer = "standard") }) //
			private String description;
		}
	}

	/**
	 * MinimalChildEntity
	 *
	 * @author Peter-Josef Meisch
	 */
	@Document(indexName = "test-index-minimal", type = "mapping")
	static class MinimalChildEntity {

		@Id private String id;

		@Parent(type = "parentType") private String parentId;
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Nordine Bittich
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-book-mapping-builder", type = "book", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class Book {

		@Id private String id;
		private String name;
		@Field(type = FieldType.Object) private Author author;
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;
	}

	/**
	 * @author Stuart Stevenson
	 * @author Mohsin Husen
	 */
	@Document(indexName = "test-index-simple-recursive-mapping-builder", type = "circular-object", shards = 1,
			replicas = 0, refreshInterval = "-1")
	static class SimpleRecursiveEntity {

		@Id private String id;
		@Field(type = FieldType.Object, ignoreFields = { "circularObject" }) private SimpleRecursiveEntity circularObject;
	}

	/**
	 * @author Sascha Woo
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-copy-to-mapping-builder", type = "test", shards = 1, replicas = 0, refreshInterval = "-1")
	static class CopyToEntity {

		@Id private String id;

		@Field(type = FieldType.Keyword, copyTo = "name") private String firstName;

		@Field(type = FieldType.Keyword, copyTo = "name") private String lastName;

		@Field(type = FieldType.Keyword) private String name;
	}

	/**
	 * @author Sascha Woo
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-normalizer-mapping-builder", type = "test", shards = 1, replicas = 0,
			refreshInterval = "-1")
	@Setting(settingPath = "/settings/test-normalizer.json")
	static class NormalizerEntity {

		@Id private String id;

		@Field(type = FieldType.Keyword, normalizer = "lower_case_normalizer") private String name;

		@MultiField(mainField = @Field(type = FieldType.Text), otherFields = { @InnerField(suffix = "lower_case",
				type = FieldType.Keyword, normalizer = "lower_case_normalizer") }) private String description;
	}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 */
	static class Author {

		private String id;
		private String name;

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
	}

	/**
	 * @author Kevin Leturc
	 */
	@Document(indexName = "test-index-sample-inherited-mapping-builder", type = "mapping", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleInheritedEntity extends AbstractInheritedEntity {

		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	/**
	 * @author Kevin Leturc
	 */
	static class SampleInheritedEntityBuilder {

		private SampleInheritedEntity result;

		public SampleInheritedEntityBuilder(String id) {
			result = new SampleInheritedEntity();
			result.setId(id);
		}

		public SampleInheritedEntityBuilder createdDate(Date createdDate) {
			result.setCreatedDate(createdDate);
			return this;
		}

		public SampleInheritedEntityBuilder message(String message) {
			result.setMessage(message);
			return this;
		}

		public SampleInheritedEntity build() {
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
	 * @author Artur Konczak
	 * @author Mohsin Husen
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-stock-mapping-builder", type = "price", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class StockPrice {

		@Id private String id;

		private String symbol;

		@Field(type = FieldType.Double) private BigDecimal price;
	}

	/**
	 * @author Kevin Letur
	 */
	static class AbstractInheritedEntity {

		@Id private String id;

		@Field(type = FieldType.Date, index = false) private Date createdDate;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Date getCreatedDate() {
			return createdDate;
		}

		public void setCreatedDate(Date createdDate) {
			this.createdDate = createdDate;
		}
	}

	/**
	 * @author Jakub Vavrik
	 */
	@Document(indexName = "test-index-recursive-mapping-mapping-builder", type = "mapping", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleTransientEntity {

		@Id private String id;

		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		@Transient private SampleTransientEntity.NestedEntity nested;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		static class NestedEntity {

			@Field private static SampleTransientEntity.NestedEntity someField = new SampleTransientEntity.NestedEntity();
			@Field private Boolean something;

			public SampleTransientEntity.NestedEntity getSomeField() {
				return someField;
			}

			public void setSomeField(SampleTransientEntity.NestedEntity someField) {
				this.someField = someField;
			}

			public Boolean getSomething() {
				return something;
			}

			public void setSomething(Boolean something) {
				this.something = something;
			}
		}
	}

	/**
	 * @author Artur Konczak
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-geo-mapping-builder", type = "geo-test-index", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class GeoEntity {

		@Id private String id;

		// geo shape - Spring Data
		private Box box;
		private Circle circle;
		private Polygon polygon;

		// geo point - Custom implementation + Spring Data
		@GeoPointField private Point pointA;

		private GeoPoint pointB;

		@GeoPointField private String pointC;

		@GeoPointField private double[] pointD;
	}

	/**
	 * Created by akonczak on 21/08/2016.
	 */
	@Document(indexName = "test-index-user-mapping-builder", type = "user")
	static class User {
		@Id private String id;

		@Field(type = FieldType.Nested, ignoreFields = { "users" }) private Set<Group> groups = new HashSet<>();
	}

	/**
	 * Created by akonczak on 21/08/2016.
	 */
	@Document(indexName = "test-index-group-mapping-builder", type = "group")
	static class Group {

		@Id String id;

		@Field(type = FieldType.Nested, ignoreFields = { "groups" }) private Set<User> users = new HashSet<>();
	}

	@Document(indexName = "test-index-field-mapping-parameters", type = "fmp")
	static class FieldMappingParameters {
		@Field private String indexTrue;
		@Field(index = false) private String indexFalse;
		@Field(store = true) private String storeTrue;
		@Field private String storeFalse;
		@Field private String coerceTrue;
		@Field(coerce = false) private String coerceFalse;
		@Field(fielddata = true) private String fielddataTrue;
		@Field private String fielddataFalse;
		@Field(copyTo = { "foo", "bar" }) private String copyTo;
		@Field(ignoreAbove = 42) private String ignoreAbove;
		@Field(type = FieldType.Integer) private String type;
		@Field(type = FieldType.Date, format = DateFormat.custom, pattern = "YYYYMMDD") private String date;
		@Field(analyzer = "ana", searchAnalyzer = "sana", normalizer = "norma") private String analyzers;
		@Field(type = Keyword, docValues = true) private String docValuesTrue;
		@Field(type = Keyword, docValues = false) private String docValuesFalse;
		@Field(ignoreMalformed = true) private String ignoreMalformedTrue;
		@Field(ignoreMalformed = false) private String ignoreMalformedFalse;
		@Field(indexOptions = IndexOptions.none) private String indexOptionsNone;
		@Field(indexOptions = IndexOptions.positions) private String indexOptionsPositions;
		@Field(indexPhrases = true) private String indexPhrasesTrue;
		@Field(indexPhrases = false) private String indexPhrasesFalse;
		@Field(indexPrefixes = @IndexPrefixes) private String defaultIndexPrefixes;
		@Field(indexPrefixes = @IndexPrefixes(minChars = 1, maxChars = 10)) private String customIndexPrefixes;
		@Field private String normsTrue;
		@Field(norms = false) private String normsFalse;
		@Field private String nullValueNotSet;
		@Field(nullValue = "NULLNULL") private String nullValueSet;
		@Field(positionIncrementGap = 42) private String positionIncrementGap;
		@Field private String similarityDefault;
		@Field(similarity = Similarity.Boolean) private String similarityBoolean;
		@Field private String termVectorDefault;
		@Field(termVector = TermVector.with_offsets) private String termVectorWithOffsets;
		@Field(type = FieldType.Scaled_Float, scalingFactor = 100.0) Double scaledFloat;
	}

	@Document(indexName = "test-index-configure-dynamic-mapping", type = "dms")
	@DynamicMapping(DynamicMappingValue.False)
	static class ConfigureDynamicMappingEntity {

		@DynamicMapping(DynamicMappingValue.Strict) @Field(type = FieldType.Object) private Author author;

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}
}
