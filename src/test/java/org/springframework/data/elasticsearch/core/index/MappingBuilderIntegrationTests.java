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

package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.Object;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.Integer;
import java.math.BigDecimal;
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
import org.elasticsearch.search.suggest.completion.context.ContextMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
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
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
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
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class MappingBuilderIntegrationTests extends MappingContextBaseTests {

	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@AfterEach
	@BeforeEach
	public void deleteIndices() {
		indexOperations = operations.indexOps(SimpleRecursiveEntity.class);
		indexOperations.delete();
		operations.indexOps(StockPrice.class).delete();
		operations.indexOps(SampleInheritedEntity.class).delete();
		operations.indexOps(User.class).delete();
		operations.indexOps(Group.class).delete();
		operations.indexOps(Book.class).delete();
		operations.indexOps(NormalizerEntity.class).delete();
		operations.indexOps(CopyToEntity.class).delete();
	}

	@Test
	public void shouldNotFailOnCircularReference() {

		operations.indexOps(SimpleRecursiveEntity.class).create();
		indexOperations.putMapping(SimpleRecursiveEntity.class);
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
		operations.index(buildIndex(StockPrice.builder() //
				.id(id) //
				.symbol(symbol) //
				.price(BigDecimal.valueOf(price)) //
				.build()), index);
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
		IndexCoordinates index = IndexCoordinates.of("test-index-sample-inherited-mapping-builder");
		IndexOperations indexOps = operations.indexOps(index);

		// when
		indexOps.create();
		indexOps.putMapping(SampleInheritedEntity.class);
		Date createdDate = new Date();
		String message = "msg";
		String id = "abc";
		operations.index(new SampleInheritedEntityBuilder(id).createdDate(createdDate).message(message).buildIndex(),
				index);
		operations.indexOps(SampleInheritedEntity.class).refresh();

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		SearchHits<SampleInheritedEntity> result = operations.search(searchQuery, SampleInheritedEntity.class, index);

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

	/**
	 * @author Xiao Yu
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "ignore-above-index")
	static class IgnoreAboveEntity {

		@Id private String id;

		@Field(type = FieldType.Keyword, ignoreAbove = 10) private String message;
	}

	/**
	 * @author Peter-Josef Meisch
	 */
	static class FieldNameEntity {

		@Document(indexName = "fieldname-index")
		static class IdEntity {
			@Nullable @Id @Field("id-property") private String id;
		}

		@Document(indexName = "fieldname-index")
		static class TextEntity {

			@Nullable @Id @Field("id-property") private String id;

			@Field(name = "text-property", type = FieldType.Text) //
			@Nullable private String textProperty;
		}

		@Document(indexName = "fieldname-index")
		static class MappingEntity {

			@Nullable @Id @Field("id-property") private String id;

			@Field("mapping-property") @Mapping(mappingPath = "/mappings/test-field-analyzed-mappings.json") //
			@Nullable private byte[] mappingProperty;
		}

		@Document(indexName = "fieldname-index")
		static class GeoPointEntity {

			@Nullable @Id @Field("id-property") private String id;

			@Nullable @Field("geopoint-property") private GeoPoint geoPoint;
		}

		@Document(indexName = "fieldname-index")
		static class CircularEntity {

			@Nullable @Id @Field("id-property") private String id;

			@Nullable @Field(name = "circular-property", type = FieldType.Object, ignoreFields = { "circular-property" }) //
			private CircularEntity circularProperty;
		}

		@Document(indexName = "fieldname-index")
		static class CompletionEntity {

			@Nullable @Id @Field("id-property") private String id;

			@Nullable @Field("completion-property") @CompletionField(maxInputLength = 100) //
			private Completion suggest;
		}

		@Document(indexName = "fieldname-index")
		static class MultiFieldEntity {

			@Nullable @Id @Field("id-property") private String id;

			@Nullable //
			@MultiField(mainField = @Field(name = "main-field", type = FieldType.Text, analyzer = "whitespace"),
					otherFields = {
							@InnerField(suffix = "suff-ix", type = FieldType.Text, analyzer = "stop", searchAnalyzer = "standard") }) //
			private String description;
		}
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
	@Document(indexName = "test-index-book-mapping-builder", replicas = 0, refreshInterval = "-1")
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
	@Data
	@Document(indexName = "test-index-simple-recursive-mapping-builder", replicas = 0, refreshInterval = "-1")
	static class SimpleRecursiveEntity {

		@Nullable @Id private String id;
		@Nullable @Field(type = FieldType.Object,
				ignoreFields = { "circularObject" }) private SimpleRecursiveEntity circularObject;
	}

	/**
	 * @author Sascha Woo
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-copy-to-mapping-builder", replicas = 0, refreshInterval = "-1")
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
	@Document(indexName = "test-index-normalizer-mapping-builder", replicas = 0, refreshInterval = "-1")
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

	/**
	 * @author Kevin Leturc
	 */
	@Document(indexName = "test-index-sample-inherited-mapping-builder", replicas = 0, refreshInterval = "-1")
	static class SampleInheritedEntity extends AbstractInheritedEntity {

		@Nullable @Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		@Nullable
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

	/**
	 * @author Artur Konczak
	 * @author Mohsin Husen
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-stock-mapping-builder", replicas = 0, refreshInterval = "-1")
	static class StockPrice {

		@Id private String id;

		private String symbol;

		@Field(type = FieldType.Double) private BigDecimal price;
	}

	/**
	 * @author Kevin Letur
	 */
	static class AbstractInheritedEntity {

		@Nullable @Id private String id;

		@Nullable @Field(type = FieldType.Date, format = DateFormat.date_time, index = false) private Date createdDate;

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

	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-geo-mapping-builder", replicas = 0, refreshInterval = "-1")
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

		// geo shape, until e have the classes for this, us a strng
		@GeoShapeField private String shape1;
		@GeoShapeField(coerce = true, ignoreMalformed = true, ignoreZValue = false,
				orientation = GeoShapeField.Orientation.clockwise) private String shape2;
	}

	/**
	 * Created by akonczak on 21/08/2016.
	 */
	@Document(indexName = "test-index-user-mapping-builder")
	static class User {
		@Nullable @Id private String id;

		@Field(type = FieldType.Nested, ignoreFields = { "users" }) private Set<Group> groups = new HashSet<>();
	}

	/**
	 * Created by akonczak on 21/08/2016.
	 */
	@Document(indexName = "test-index-group-mapping-builder")
	static class Group {

		@Nullable @Id String id;

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

	@Getter
	@Setter
	@Document(indexName = "completion")
	static class CompletionDocument {
		@Id private String id;

		@CompletionField(contexts = { @CompletionContext(name = "location", type = ContextMapping.Type.GEO,
				path = "proppath") }) private Completion suggest;
	}

	@Data
	@Document(indexName = "test-index-entity-with-seq-no-primary-term-mapping-builder")
	static class EntityWithSeqNoPrimaryTerm {

		@Field(type = Object) private SeqNoPrimaryTerm seqNoPrimaryTerm;
	}

	@Data
	static class RankFeatureEntity {
		@Id private String id;

		@Field(type = FieldType.Rank_Feature) private Integer pageRank;

		@Field(type = FieldType.Rank_Feature, positiveScoreImpact = false) private Integer urlLength;

		@Field(type = FieldType.Rank_Features) private Map<String, Integer> topics;
	}

	@Data
	@Document(indexName = "termvectors-test")
	static class TermVectorFieldEntity {
		@Id private String id;
		@Field(type = FieldType.Text, termVector = TermVector.no) private String no;
		@Field(type = FieldType.Text, termVector = TermVector.yes) private String yes;
		@Field(type = FieldType.Text, termVector = TermVector.with_positions) private String with_positions;
		@Field(type = FieldType.Text, termVector = TermVector.with_offsets) private String with_offsets;
		@Field(type = FieldType.Text, termVector = TermVector.with_positions_offsets) private String with_positions_offsets;
		@Field(type = FieldType.Text,
				termVector = TermVector.with_positions_payloads) private String with_positions_payloads;
		@Field(type = FieldType.Text,
				termVector = TermVector.with_positions_offsets_payloads) private String with_positions_offsets_payloads;
	}

	@Data
	@Document(indexName = "wildcard-test")
	static class WildcardEntity {
		@Nullable @Field(type = Wildcard) private String wildcardWithoutParams;
		@Nullable @Field(type = Wildcard, nullValue = "WILD", ignoreAbove = 42) private String wildcardWithParams;
	}
}
