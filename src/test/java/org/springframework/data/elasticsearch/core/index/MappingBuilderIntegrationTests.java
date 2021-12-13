/*
 * Copyright 2013-2021 the original author or authors.
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
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.Object;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import java.lang.Integer;
import java.lang.Object;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.backend.elasticsearch7.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.backend.elasticsearch7.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.MappingContextBaseTests;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.lang.Nullable;
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
 * @author Roman Puchkovskiy
 * @author Brian Kimmig
 * @author Morgan Lutz
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { MappingBuilderIntegrationTests.Config.class })
public class MappingBuilderIntegrationTests extends MappingContextBaseTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("mapping-builder");
		}
	}

	@Autowired private ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of("*")).delete();
	}

	@Test
	public void shouldNotFailOnCircularReference() {

		IndexOperations indexOperations = operations.indexOps(SimpleRecursiveEntity.class);
		indexOperations.createWithMapping();
		indexOperations.refresh();
	}

	@Test // DATAES-530
	public void shouldAddStockPriceDocumentToIndex() {

		// Given
		IndexOperations indexOps = operations.indexOps(StockPrice.class);

		// When
		indexOps.create();
		indexOps.putMapping(StockPrice.class);
		String symbol = "AU";
		double price = 2.34;
		String id = "abc";

		IndexCoordinates index = IndexCoordinates.of("test-index-stock-mapping-builder");
		StockPrice stockPrice = new StockPrice(); //
		stockPrice.setId(id);
		stockPrice.setSymbol(symbol);
		stockPrice.setPrice(BigDecimal.valueOf(price));
		operations.index(buildIndex(stockPrice), index);
		operations.indexOps(StockPrice.class).refresh();

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		SearchHits<StockPrice> result = operations.search(searchQuery, StockPrice.class, index);

		// Then
		assertThat(result).hasSize(1);
		StockPrice entry = result.getSearchHit(0).getContent();
		assertThat(entry.getSymbol()).isEqualTo(symbol);
		assertThat(entry.getPrice()).isCloseTo(BigDecimal.valueOf(price), Percentage.withPercentage(0.01));
	}

	@Test // DATAES-76
	public void shouldAddSampleInheritedEntityDocumentToIndex() {
		// given
		IndexOperations indexOps = operations.indexOps(SampleInheritedEntity.class);

		// when
		indexOps.create();
		indexOps.putMapping(SampleInheritedEntity.class);
		Date createdDate = new Date();
		String message = "msg";
		String id = "abc";
		operations.index(new SampleInheritedEntityBuilder(id).createdDate(createdDate).message(message).buildIndex(),
				IndexCoordinates.of(indexNameProvider.indexName()));

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		SearchHits<SampleInheritedEntity> result = operations.search(searchQuery, SampleInheritedEntity.class);

		// then
		assertThat(result).hasSize(1);

		SampleInheritedEntity entry = result.getSearchHit(0).getContent();
		assertThat(entry.getCreatedDate()).isEqualTo(createdDate);
		assertThat(entry.getMessage()).isEqualTo(message);
	}

	@Test // DATAES-260 - StackOverflow when two reverse relationship.
	public void shouldHandleReverseRelationship() {

		// given
		IndexOperations indexOpsUser = operations.indexOps(User.class);
		indexOpsUser.create();
		indexOpsUser.putMapping(User.class);
		indexNameProvider.increment();
		IndexOperations indexOpsGroup = operations.indexOps(Group.class);
		indexOpsGroup.create();
		indexOpsGroup.putMapping(Group.class);

		// when

		// then
	}

	@Test // DATAES-420
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void shouldUseBothAnalyzer() {

		// given
		IndexOperations indexOps = this.operations.indexOps(Book.class);
		indexOps.create();
		indexOps.putMapping(Book.class);

		// when
		Map mapping = indexOps.getMapping();
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
	@SuppressWarnings("rawtypes")
	public void shouldUseKeywordNormalizer() {

		// given
		IndexOperations indexOps = operations.indexOps(NormalizerEntity.class);
		indexOps.create();
		indexOps.putMapping();

		// when
		Map mapping = indexOps.getMapping();
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
	@SuppressWarnings("rawtypes")
	public void shouldUseCopyTo() {

		// given
		IndexOperations indexOps = operations.indexOps(CopyToEntity.class);
		indexOps.create();
		indexOps.putMapping(CopyToEntity.class);

		// when
		Map mapping = indexOps.getMapping();
		Map properties = (Map) mapping.get("properties");
		Map fieldFirstName = (Map) properties.get("firstName");
		Map fieldLastName = (Map) properties.get("lastName");

		// then
		List<String> copyToValue = Collections.singletonList("name");
		assertThat(fieldFirstName.get("copy_to")).isEqualTo(copyToValue);
		assertThat(fieldLastName.get("copy_to")).isEqualTo(copyToValue);
	}

	@Test // DATAES-991
	@DisplayName("should write correct TermVector values")
	void shouldWriteCorrectTermVectorValues() {

		IndexOperations indexOps = operations.indexOps(TermVectorFieldEntity.class);
		indexOps.create();
		indexOps.putMapping();
	}

	@Test // DATAES-946
	@DisplayName("should write wildcard field mapping")
	void shouldWriteWildcardFieldMapping() {

		IndexOperations indexOps = operations.indexOps(WildcardEntity.class);
		indexOps.create();
		indexOps.putMapping();
	}

	@Test // #1700
	@DisplayName("should write dense_vector field mapping")
	void shouldWriteDenseVectorFieldMapping() {

		IndexOperations indexOps = operations.indexOps(DenseVectorEntity.class);
		indexOps.create();
		indexOps.putMapping();
	}

	@Test // #1370
	@DisplayName("should write mapping for disabled entity")
	void shouldWriteMappingForDisabledEntity() {

		IndexOperations indexOps = operations.indexOps(DisabledMappingEntity.class);
		indexOps.create();
		indexOps.putMapping();

	}

	@Test // #1370
	@DisplayName("should write mapping for disabled property")
	void shouldWriteMappingForDisabledProperty() {

		IndexOperations indexOps = operations.indexOps(DisabledMappingProperty.class);
		indexOps.create();
		indexOps.putMapping();

	}

	@Test // #1767
	@DisplayName("should write dynamic mapping annotations")
	void shouldWriteDynamicMappingAnnotations() {

		IndexOperations indexOps = operations.indexOps(DynamicMappingAnnotationEntity.class);
		indexOps.create();
		indexOps.putMapping();

	}

	@Test // #1871
	@DisplayName("should write dynamic mapping")
	void shouldWriteDynamicMapping() {

		IndexOperations indexOps = operations.indexOps(DynamicMappingEntity.class);
		indexOps.create();
		indexOps.putMapping();

	}

	@Test // #638
	@DisplayName("should write dynamic detection values")
	void shouldWriteDynamicDetectionValues() {

		IndexOperations indexOps = operations.indexOps(DynamicDetectionMapping.class);
		indexOps.create();
		indexOps.putMapping();

	}

	@Test // #1816
	@DisplayName("should write runtime fields")
	void shouldWriteRuntimeFields() {

		IndexOperations indexOps = operations.indexOps(RuntimeFieldEntity.class);
		indexOps.create();
		indexOps.putMapping();

	}

	@Test // #796
	@DisplayName("should write source excludes")
	void shouldWriteSourceExcludes() {

		IndexOperations indexOps = operations.indexOps(ExcludedFieldEntity.class);
		indexOps.create();
		indexOps.putMapping();

	}

	@Test // #2024
	@DisplayName("should map all field type values")
	void shouldMapAllFieldTypeValues() {
		operations.indexOps(EntityWithAllTypes.class).createWithMapping();
	}

	// region entities
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class IgnoreAboveEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Keyword, ignoreAbove = 10) private String message;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

	static class FieldNameEntity {

		@Document(indexName = "fieldname-index")
		static class IdEntity {
			@Nullable
			@Id
			@Field("id-property") private String id;
		}

		@Document(indexName = "#{@indexNameProvider.indexName()}")
		static class TextEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Field(name = "text-property", type = FieldType.Text) //
			@Nullable private String textProperty;
		}

		@Document(indexName = "#{@indexNameProvider.indexName()}")
		static class MappingEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Field("mapping-property")
			@Mapping(mappingPath = "/mappings/test-field-analyzed-mappings.json") //
			@Nullable private byte[] mappingProperty;
		}

		@Document(indexName = "#{@indexNameProvider.indexName()}")
		static class GeoPointEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable
			@Field("geopoint-property") private GeoPoint geoPoint;
		}

		@Document(indexName = "#{@indexNameProvider.indexName()}")
		static class CircularEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable
			@Field(name = "circular-property", type = FieldType.Object, ignoreFields = { "circular-property" }) //
			private CircularEntity circularProperty;
		}

		@Document(indexName = "#{@indexNameProvider.indexName()}")
		static class CompletionEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable
			@Field("completion-property")
			@CompletionField(maxInputLength = 100) //
			private Completion suggest;
		}

		@Document(indexName = "#{@indexNameProvider.indexName()}")
		static class MultiFieldEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable //
			@MultiField(mainField = @Field(name = "main-field", type = FieldType.Text, analyzer = "whitespace"),
					otherFields = {
							@InnerField(suffix = "suff-ix", type = FieldType.Text, analyzer = "stop", searchAnalyzer = "standard") }) //
			private String description;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Book {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Object) private Author author;
		@Nullable
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@Nullable
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;

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
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(@Nullable Author author) {
			this.author = author;
		}

		@Nullable
		public Map<java.lang.Integer, Collection<String>> getBuckets() {
			return buckets;
		}

		public void setBuckets(@Nullable Map<java.lang.Integer, Collection<String>> buckets) {
			this.buckets = buckets;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SimpleRecursiveEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Object, ignoreFields = { "circularObject" }) private SimpleRecursiveEntity circularObject;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public SimpleRecursiveEntity getCircularObject() {
			return circularObject;
		}

		public void setCircularObject(@Nullable SimpleRecursiveEntity circularObject) {
			this.circularObject = circularObject;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class CopyToEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Keyword, copyTo = "name") private String firstName;
		@Nullable
		@Field(type = FieldType.Keyword, copyTo = "name") private String lastName;
		@Nullable
		@Field(type = FieldType.Keyword) private String name;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Setting(settingPath = "/settings/test-normalizer.json")
	static class NormalizerEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Keyword, normalizer = "lower_case_normalizer") private String name;
		@Nullable
		@MultiField(mainField = @Field(type = FieldType.Text), otherFields = { @InnerField(suffix = "lower_case",
				type = FieldType.Keyword, normalizer = "lower_case_normalizer") }) private String description;

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
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}
	}

	static class Author {

		@Nullable private String id;
		@Nullable private String name;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleInheritedEntity extends AbstractInheritedEntity {

		@Nullable
		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	static class SampleInheritedEntityBuilder {

		private final SampleInheritedEntity result;

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
			indexQuery.setId(Objects.requireNonNull(result.getId()));
			indexQuery.setObject(result);
			return indexQuery;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class StockPrice {
		@Nullable
		@Id private String id;
		@Nullable private String symbol;
		@Nullable
		@Field(type = FieldType.Double) private BigDecimal price;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getSymbol() {
			return symbol;
		}

		public void setSymbol(@Nullable String symbol) {
			this.symbol = symbol;
		}

		@Nullable
		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(@Nullable BigDecimal price) {
			this.price = price;
		}
	}

	static class AbstractInheritedEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Date, format = DateFormat.date_time, index = false) private Date createdDate;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public Date getCreatedDate() {
			return createdDate;
		}

		public void setCreatedDate(Date createdDate) {
			this.createdDate = createdDate;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class GeoEntity {
		@Nullable
		@Id private String id;
		// geo shape - Spring Data
		@Nullable private Box box;
		@Nullable private Circle circle;
		@Nullable private Polygon polygon;
		// geo point - Custom implementation + Spring Data
		@Nullable
		@GeoPointField private Point pointA;
		@Nullable private GeoPoint pointB;
		@Nullable
		@GeoPointField private String pointC;
		@Nullable
		@GeoPointField private double[] pointD;
		// geo shape, until e have the classes for this, us a strng
		@Nullable
		@GeoShapeField private String shape1;
		@Nullable
		@GeoShapeField(coerce = true, ignoreMalformed = true, ignoreZValue = false,
				orientation = GeoShapeField.Orientation.clockwise) private String shape2;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Box getBox() {
			return box;
		}

		public void setBox(@Nullable Box box) {
			this.box = box;
		}

		@Nullable
		public Circle getCircle() {
			return circle;
		}

		public void setCircle(@Nullable Circle circle) {
			this.circle = circle;
		}

		@Nullable
		public Polygon getPolygon() {
			return polygon;
		}

		public void setPolygon(@Nullable Polygon polygon) {
			this.polygon = polygon;
		}

		@Nullable
		public Point getPointA() {
			return pointA;
		}

		public void setPointA(@Nullable Point pointA) {
			this.pointA = pointA;
		}

		@Nullable
		public GeoPoint getPointB() {
			return pointB;
		}

		public void setPointB(@Nullable GeoPoint pointB) {
			this.pointB = pointB;
		}

		@Nullable
		public String getPointC() {
			return pointC;
		}

		public void setPointC(@Nullable String pointC) {
			this.pointC = pointC;
		}

		@Nullable
		public double[] getPointD() {
			return pointD;
		}

		public void setPointD(@Nullable double[] pointD) {
			this.pointD = pointD;
		}

		@Nullable
		public String getShape1() {
			return shape1;
		}

		public void setShape1(@Nullable String shape1) {
			this.shape1 = shape1;
		}

		@Nullable
		public String getShape2() {
			return shape2;
		}

		public void setShape2(@Nullable String shape2) {
			this.shape2 = shape2;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")

	static class User {
		@Nullable
		@Id private String id;

		@Field(type = FieldType.Nested, ignoreFields = { "users" }) private Set<Group> groups = new HashSet<>();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")

	static class Group {

		@Nullable
		@Id String id;

		@Field(type = FieldType.Nested, ignoreFields = { "groups" }) private Set<User> users = new HashSet<>();
	}

	static class ValueObject {
		private String value;

		public ValueObject(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")

	static class CompletionDocument {
		@Nullable
		@Id private String id;
		@Nullable
		@CompletionField(contexts = { @CompletionContext(name = "location", type = CompletionContext.ContextMappingType.GEO,
				path = "proppath") }) private Completion suggest;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Completion getSuggest() {
			return suggest;
		}

		public void setSuggest(@Nullable Completion suggest) {
			this.suggest = suggest;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class EntityWithSeqNoPrimaryTerm {
		@Nullable
		@Field(type = Object) private SeqNoPrimaryTerm seqNoPrimaryTerm;

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}

	static class RankFeatureEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Rank_Feature) private Integer pageRank;
		@Nullable
		@Field(type = FieldType.Rank_Feature, positiveScoreImpact = false) private Integer urlLength;
		@Nullable
		@Field(type = FieldType.Rank_Features) private Map<String, Integer> topics;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public java.lang.Integer getPageRank() {
			return pageRank;
		}

		public void setPageRank(@Nullable java.lang.Integer pageRank) {
			this.pageRank = pageRank;
		}

		@Nullable
		public java.lang.Integer getUrlLength() {
			return urlLength;
		}

		public void setUrlLength(@Nullable java.lang.Integer urlLength) {
			this.urlLength = urlLength;
		}

		@Nullable
		public Map<String, java.lang.Integer> getTopics() {
			return topics;
		}

		public void setTopics(@Nullable Map<String, java.lang.Integer> topics) {
			this.topics = topics;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class TermVectorFieldEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text, termVector = TermVector.no) private String no;
		@Nullable
		@Field(type = FieldType.Text, termVector = TermVector.yes) private String yes;
		@Nullable
		@Field(type = FieldType.Text, termVector = TermVector.with_positions) private String with_positions;
		@Nullable
		@Field(type = FieldType.Text, termVector = TermVector.with_offsets) private String with_offsets;
		@Nullable
		@Field(type = FieldType.Text, termVector = TermVector.with_positions_offsets) private String with_positions_offsets;
		@Nullable
		@Field(type = FieldType.Text,
				termVector = TermVector.with_positions_payloads) private String with_positions_payloads;
		@Nullable
		@Field(type = FieldType.Text,
				termVector = TermVector.with_positions_offsets_payloads) private String with_positions_offsets_payloads;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getNo() {
			return no;
		}

		public void setNo(@Nullable String no) {
			this.no = no;
		}

		@Nullable
		public String getYes() {
			return yes;
		}

		public void setYes(@Nullable String yes) {
			this.yes = yes;
		}

		@Nullable
		public String getWith_positions() {
			return with_positions;
		}

		public void setWith_positions(@Nullable String with_positions) {
			this.with_positions = with_positions;
		}

		@Nullable
		public String getWith_offsets() {
			return with_offsets;
		}

		public void setWith_offsets(@Nullable String with_offsets) {
			this.with_offsets = with_offsets;
		}

		@Nullable
		public String getWith_positions_offsets() {
			return with_positions_offsets;
		}

		public void setWith_positions_offsets(@Nullable String with_positions_offsets) {
			this.with_positions_offsets = with_positions_offsets;
		}

		@Nullable
		public String getWith_positions_payloads() {
			return with_positions_payloads;
		}

		public void setWith_positions_payloads(@Nullable String with_positions_payloads) {
			this.with_positions_payloads = with_positions_payloads;
		}

		@Nullable
		public String getWith_positions_offsets_payloads() {
			return with_positions_offsets_payloads;
		}

		public void setWith_positions_offsets_payloads(@Nullable String with_positions_offsets_payloads) {
			this.with_positions_offsets_payloads = with_positions_offsets_payloads;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class WildcardEntity {
		@Nullable
		@Field(type = Wildcard) private String wildcardWithoutParams;
		@Nullable
		@Field(type = Wildcard, nullValue = "WILD", ignoreAbove = 42) private String wildcardWithParams;

		@Nullable
		public String getWildcardWithoutParams() {
			return wildcardWithoutParams;
		}

		public void setWildcardWithoutParams(@Nullable String wildcardWithoutParams) {
			this.wildcardWithoutParams = wildcardWithoutParams;
		}

		@Nullable
		public String getWildcardWithParams() {
			return wildcardWithParams;
		}

		public void setWildcardWithParams(@Nullable String wildcardWithParams) {
			this.wildcardWithParams = wildcardWithParams;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Mapping(enabled = false)
	static class DisabledMappingEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text) private String text;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}

	@Document(indexName = "disabled-property-mapping")
	static class DisabledMappingProperty {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text) private String text;
		@Nullable
		@Mapping(enabled = false)
		@Field(type = Object) private Object object;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public java.lang.Object getObject() {
			return object;
		}

		public void setObject(@Nullable java.lang.Object object) {
			this.object = object;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class DenseVectorEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Dense_Vector, dims = 3) private float[] dense_vector;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public float[] getDense_vector() {
			return dense_vector;
		}

		public void setDense_vector(@Nullable float[] dense_vector) {
			this.dense_vector = dense_vector;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@DynamicMapping(DynamicMappingValue.False)
	static class DynamicMappingAnnotationEntity {

		@Nullable
		@DynamicMapping(DynamicMappingValue.Strict)
		@Field(type = FieldType.Object) private Author author;
		@Nullable
		@DynamicMapping(DynamicMappingValue.False)
		@Field(type = FieldType.Object) private Map<String, Object> objectMap;
		@Nullable
		@DynamicMapping(DynamicMappingValue.False)
		@Field(type = FieldType.Nested) private List<Map<String, Object>> nestedObjectMap;

		@Nullable
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class DynamicMappingEntity {

		@Nullable
		@Field(type = FieldType.Object) //
		private Map<String, Object> objectInherit;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.FALSE) //
		private Map<String, Object> objectFalse;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.TRUE) //
		private Map<String, Object> objectTrue;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.STRICT) //
		private Map<String, Object> objectStrict;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.RUNTIME) //
		private Map<String, Object> objectRuntime;
		@Nullable
		@Field(type = FieldType.Nested) //
		private List<Map<String, Object>> nestedObjectInherit;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.FALSE) //
		private List<Map<String, Object>> nestedObjectFalse;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.TRUE) //
		private List<Map<String, Object>> nestedObjectTrue;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.STRICT) //
		private List<Map<String, Object>> nestedObjectStrict;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.RUNTIME) //
		private List<Map<String, Object>> nestedObjectRuntime;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Mapping(dateDetection = Mapping.Detection.TRUE, numericDetection = Mapping.Detection.TRUE,
			dynamicDateFormats = { "MM/dd/yyyy" })
	private static class DynamicDetectionMapping {
		@Id
		@Nullable private String id;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Mapping(runtimeFieldsPath = "/mappings/runtime-fields.json")
	private static class RuntimeFieldEntity {
		@Id
		@Nullable private String id;
		@Field(type = Date, format = DateFormat.epoch_millis, name = "@timestamp")
		@Nullable private Instant timestamp;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class ExcludedFieldEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(name = "excluded-date", type = Date, format = DateFormat.date,
				excludeFromSource = true) private LocalDate excludedDate;
		@Nullable
		@Field(type = Nested) private NestedExcludedFieldEntity nestedEntity;
	}

	private static class NestedExcludedFieldEntity {
		@Nullable
		@Field(name = "excluded-text", type = Text, excludeFromSource = true) private String excludedText;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class EntityWithAllTypes {
		@Nullable
		@Field(type = FieldType.Auto) String autoField;
		@Nullable
		@Field(type = FieldType.Text) String textField;
		@Nullable
		@Field(type = FieldType.Keyword) String keywordField;
		@Nullable
		@Field(type = FieldType.Long) String longField;
		@Nullable
		@Field(type = FieldType.Integer) String integerField;
		@Nullable
		@Field(type = FieldType.Short) String shortField;
		@Nullable
		@Field(type = FieldType.Byte) String byteField;
		@Nullable
		@Field(type = FieldType.Double) String doubleField;
		@Nullable
		@Field(type = FieldType.Float) String floatField;
		@Nullable
		@Field(type = FieldType.Half_Float) String halfFloatField;
		@Nullable
		@Field(type = FieldType.Scaled_Float) String scaledFloatField;
		@Nullable
		@Field(type = FieldType.Date) String dateField;
		@Nullable
		@Field(type = FieldType.Date_Nanos) String dateNanosField;
		@Nullable
		@Field(type = FieldType.Boolean) String booleanField;
		@Nullable
		@Field(type = FieldType.Binary) String binaryField;
		@Nullable
		@Field(type = FieldType.Integer_Range) String integerRangeField;
		@Nullable
		@Field(type = FieldType.Float_Range) String floatRangeField;
		@Nullable
		@Field(type = FieldType.Long_Range) String longRangeField;
		@Nullable
		@Field(type = FieldType.Double_Range) String doubleRangeField;
		@Nullable
		@Field(type = FieldType.Date_Range) String dateRangeField;
		@Nullable
		@Field(type = FieldType.Ip_Range) String ipRangeField;
		@Nullable
		@Field(type = FieldType.Object) String objectField;
		@Nullable
		@Field(type = FieldType.Nested) String nestedField;
		@Nullable
		@Field(type = FieldType.Ip) String ipField;
		@Nullable
		@Field(type = FieldType.TokenCount, analyzer = "standard") String tokenCountField;
		@Nullable
		@Field(type = FieldType.Percolator) String percolatorField;
		@Nullable
		@Field(type = FieldType.Flattened) String flattenedField;
		@Nullable
		@Field(type = FieldType.Search_As_You_Type) String searchAsYouTypeField;
		@Nullable
		@Field(type = FieldType.Rank_Feature) String rankFeatureField;
		@Nullable
		@Field(type = FieldType.Rank_Features) String rankFeaturesField;
		@Nullable
		@Field(type = FieldType.Wildcard) String wildcardField;
		@Nullable
		@Field(type = FieldType.Dense_Vector, dims = 1) String denseVectorField;
	}
	// endregion
}
