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

package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.MappingContextBaseTests;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

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
 * @author Haibo Liu
 */
@SpringIntegrationTest
public abstract class MappingBuilderIntegrationTests extends MappingContextBaseTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired protected IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test
	public void shouldNotFailOnCircularReference() {

		IndexOperations indexOperations = operations.indexOps(SimpleRecursiveEntity.class);
		indexOperations.createWithMapping();
	}

	@Test // DATAES-420
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void shouldUseBothAnalyzer() {

		// given
		IndexOperations indexOps = this.operations.indexOps(Book.class);
		indexOps.createWithMapping();

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
		indexOps.createWithMapping();

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
		indexOps.createWithMapping();

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
		indexOps.createWithMapping();
	}

	@Test // DATAES-946
	@DisplayName("should write wildcard field mapping")
	void shouldWriteWildcardFieldMapping() {

		IndexOperations indexOps = operations.indexOps(WildcardEntity.class);
		indexOps.createWithMapping();
	}

	@Test // #1700
	@DisplayName("should write dense_vector field mapping")
	void shouldWriteDenseVectorFieldMapping() {

		IndexOperations indexOps = operations.indexOps(DenseVectorEntity.class);
		indexOps.createWithMapping();
	}

	@Test // #1370
	@DisplayName("should write mapping for disabled entity")
	void shouldWriteMappingForDisabledEntity() {

		IndexOperations indexOps = operations.indexOps(DisabledMappingEntity.class);
		indexOps.createWithMapping();
	}

	@Test // #1370
	@DisplayName("should write mapping for disabled property")
	void shouldWriteMappingForDisabledProperty() {

		IndexOperations indexOps = operations.indexOps(DisabledMappingProperty.class);
		indexOps.createWithMapping();
	}

	@Test // #1767
	@DisplayName("should write dynamic mapping annotations on create")
	void shouldWriteDynamicMappingAnnotationsOnCreate() {

		IndexOperations indexOps = operations.indexOps(DynamicMappingAnnotationEntity.class);
		indexOps.createWithMapping();

		var mapping = indexOps.getMapping();
		var dynamic = mapping.get("dynamic");
		if (dynamic instanceof String s) {
			assertThat(dynamic).isEqualTo("false");
		} else {
			assertThat(mapping.get("dynamic")).isEqualTo(false);
		}
	}

	@Test // #2478
	@DisplayName("should write dynamic mapping annotations on put")
	void shouldWriteDynamicMappingAnnotationsOnPut() {

		IndexOperations indexOps = operations.indexOps(DynamicMappingAnnotationEntity.class);
		indexOps.create();

		indexOps.putMapping();

		var mapping = indexOps.getMapping();
		var dynamic = mapping.get("dynamic");
		if (dynamic instanceof String s) {
			assertThat(dynamic).isEqualTo("false");
		} else {
			assertThat(mapping.get("dynamic")).isEqualTo(false);
		}
	}

	@Test // #1871
	@DisplayName("should write dynamic mapping")
	void shouldWriteDynamicMapping() {

		IndexOperations indexOps = operations.indexOps(DynamicMappingEntity.class);
		indexOps.createWithMapping();
	}

	@Test // #638
	@DisplayName("should write dynamic detection values")
	void shouldWriteDynamicDetectionValues() {

		IndexOperations indexOps = operations.indexOps(DynamicDetectionMapping.class);
		indexOps.createWithMapping();
	}

	@Test // #1816
	@DisplayName("should write runtime fields")
	void shouldWriteRuntimeFields() {

		IndexOperations indexOps = operations.indexOps(RuntimeFieldEntity.class);
		indexOps.createWithMapping();
	}

	@Test // #796
	@DisplayName("should write source excludes")
	void shouldWriteSourceExcludes() {

		IndexOperations indexOps = operations.indexOps(ExcludedFieldEntity.class);
		indexOps.createWithMapping();
	}

	@Test // #2502
	@DisplayName(" should write mapping with field name with dots")
	void shouldWriteMappingWithFieldNameWithDots() {

		IndexOperations indexOps = operations.indexOps(FieldNameDotsEntity.class);
		indexOps.createWithMapping();
	}

	@Test // #2659
	@DisplayName("should write correct mapping for dense vector property")
	void shouldWriteCorrectMappingForDenseVectorProperty() {
		operations.indexOps(SimilarityEntity.class).createWithMapping();
	}

	@Test // #2845
	@DisplayName("should write mapping with field aliases")
	void shouldWriteMappingWithFieldAliases() {
		operations.indexOps(FieldAliasEntity.class).createWithMapping();
	}

	// region Entities
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
	static class User {
		@Nullable
		@Id private String id;

		@Field(type = FieldType.Nested, ignoreFields = { "users" }) private final Set<Group> groups = new HashSet<>();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Group {

		@Nullable
		@Id String id;

		@Field(type = FieldType.Nested, ignoreFields = { "groups" }) private final Set<User> users = new HashSet<>();
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

	@Document(indexName = "#{@indexNameProvider.indexName()}")
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

	@Document(indexName = "#{@indexNameProvider.indexName()}", dynamic = Dynamic.FALSE)
	static class DynamicMappingAnnotationEntity {

		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.STRICT) private Author author;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.FALSE) private Map<String, Object> objectMap;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.FALSE) private List<Map<String, Object>> nestedObjectMap;

		@Nullable
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}", dynamic = Dynamic.FALSE)
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

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class FieldNameDotsEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(name = "dotted.field", type = Text) private String dottedField;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SimilarityEntity {
		@Nullable
		@Id private String id;

		@Field(type = FieldType.Dense_Vector, dims = 42, knnSimilarity = KnnSimilarity.COSINE) private double[] denseVector;
	}

	@Mapping(aliases = {
			@MappingAlias(name = "someAlly", path = "someText"),
			@MappingAlias(name = "otherAlly", path = "otherText")
	})
	@Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class FieldAliasEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(type = Text) private String someText;
		@Nullable
		@Field(type = Text) private String otherText;
	}

	// endregion
}
