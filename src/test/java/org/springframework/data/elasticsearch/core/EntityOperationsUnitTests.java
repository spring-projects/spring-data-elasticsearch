/*
 * Copyright2020-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.IndexedIndexName;
import org.springframework.data.elasticsearch.annotations.Routing;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.routing.DefaultRoutingResolver;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
class EntityOperationsUnitTests {

	@Nullable private static SimpleElasticsearchMappingContext mappingContext;
	@Nullable private static EntityOperations entityOperations;
	@Nullable private static ElasticsearchConverter elasticsearchConverter;
	@Nullable private static ConversionService conversionService;

	@BeforeAll
	static void setUpAll() {
		mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(EntityWithRouting.class)));
		mappingContext.afterPropertiesSet();

		entityOperations = new EntityOperations(mappingContext);

		elasticsearchConverter = new MappingElasticsearchConverter(mappingContext,
				new GenericConversionService());
		((MappingElasticsearchConverter) elasticsearchConverter).afterPropertiesSet();

		conversionService = elasticsearchConverter.getConversionService();
	}

	@Test // #1218
	@DisplayName("should return routing from DefaultRoutingAccessor")
	void shouldReturnRoutingFromDefaultRoutingAccessor() {

		EntityWithRouting entity = new EntityWithRouting();
		entity.setId("42");
		entity.setRouting("theRoute");
		EntityOperations.AdaptableEntity<EntityWithRouting> adaptableEntity = entityOperations.forEntity(entity,
				conversionService, new DefaultRoutingResolver(mappingContext));

		String routing = adaptableEntity.getRouting();

		assertThat(routing).isEqualTo("theRoute");
	}

	@Test // #1218
	@DisplayName("should return routing from JoinField when routing value is null")
	void shouldReturnRoutingFromJoinFieldWhenRoutingValueIsNull() {

		EntityWithRoutingAndJoinField entity = new EntityWithRoutingAndJoinField();
		entity.setId("42");
		entity.setJoinField(new JoinField<>("foo", "foo-routing"));

		EntityOperations.AdaptableEntity<EntityWithRoutingAndJoinField> adaptableEntity = entityOperations.forEntity(entity,
				conversionService, new DefaultRoutingResolver(mappingContext));

		String routing = adaptableEntity.getRouting();

		assertThat(routing).isEqualTo("foo-routing");
	}

	@Test // #1218
	@DisplayName("should return routing from routing when JoinField is set")
	void shouldReturnRoutingFromRoutingWhenJoinFieldIsSet() {
		EntityWithRoutingAndJoinField entity = new EntityWithRoutingAndJoinField();
		entity.setId("42");
		entity.setRouting("theRoute");
		entity.setJoinField(new JoinField<>("foo", "foo-routing"));

		EntityOperations.AdaptableEntity<EntityWithRoutingAndJoinField> adaptableEntity = entityOperations.forEntity(entity,
				conversionService, new DefaultRoutingResolver(mappingContext));

		String routing = adaptableEntity.getRouting();

		assertThat(routing).isEqualTo("theRoute");
	}

	@Test // #2756
	@DisplayName("should update indexed information of class entity")
	void shouldUpdateIndexedInformationOfClassEntity() {

		var entity = new EntityFromClass();
		entity.setId(null);
		entity.setText("some text");
		var indexedObjectInformation = new IndexedObjectInformation(
				"id-42",
				"index-42",
				1L,
				2L,
				3L);
		entity = entityOperations.updateIndexedObject(entity,
				indexedObjectInformation,
				elasticsearchConverter,
				new DefaultRoutingResolver(mappingContext));

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(entity.getId()).isEqualTo(indexedObjectInformation.id());
		softly.assertThat(entity.getSeqNoPrimaryTerm().sequenceNumber()).isEqualTo(indexedObjectInformation.seqNo());
		softly.assertThat(entity.getSeqNoPrimaryTerm().primaryTerm()).isEqualTo(indexedObjectInformation.primaryTerm());
		softly.assertThat(entity.getVersion()).isEqualTo(indexedObjectInformation.version());
		softly.assertThat(entity.getIndexName()).isEqualTo(indexedObjectInformation.index());
		softly.assertAll();
	}

	@Test // #2756
	@DisplayName("should update indexed information of record entity")
	void shouldUpdateIndexedInformationOfRecordEntity() {

		var entity = new EntityFromRecord(
				null,
				"someText",
				null,
				null,
				null);

		var indexedObjectInformation = new IndexedObjectInformation(
				"id-42",
				"index-42",
				1L,
				2L,
				3L);
		entity = entityOperations.updateIndexedObject(entity,
				indexedObjectInformation,
				elasticsearchConverter,
				new DefaultRoutingResolver(mappingContext));

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(entity.id()).isEqualTo(indexedObjectInformation.id());
		softly.assertThat(entity.seqNoPrimaryTerm().sequenceNumber()).isEqualTo(indexedObjectInformation.seqNo());
		softly.assertThat(entity.seqNoPrimaryTerm().primaryTerm()).isEqualTo(indexedObjectInformation.primaryTerm());
		softly.assertThat(entity.version()).isEqualTo(indexedObjectInformation.version());
		softly.assertThat(entity.indexName()).isEqualTo(indexedObjectInformation.index());
		softly.assertAll();
	}

	@Document(indexName = "entity-operations-test")
	@Routing("routing")
	static class EntityWithRouting {
		@Nullable
		@Id private String id;
		@Nullable private String routing;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getRouting() {
			return routing;
		}

		public void setRouting(@Nullable String routing) {
			this.routing = routing;
		}
	}

	@Document(indexName = "entity-operations-test")
	@Routing("routing")
	static class EntityWithRoutingAndJoinField {
		@Nullable
		@Id private String id;
		@Nullable private String routing;
		@Nullable private JoinField<String> joinField;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getRouting() {
			return routing;
		}

		public void setRouting(@Nullable String routing) {
			this.routing = routing;
		}

		@Nullable
		public JoinField<String> getJoinField() {
			return joinField;
		}

		public void setJoinField(@Nullable JoinField<String> joinField) {
			this.joinField = joinField;
		}
	}

	@Document(indexName = "entity-operations-test")
	static class EntityFromClass {
		@Id
		@Nullable private String id;
		@Field(type = FieldType.Text)
		@Nullable private String text;
		@Version
		@Nullable private Long version;
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;
		@IndexedIndexName
		@Nullable private String indexName;

		public String getId() {
			return id;
		}

		public void setId(String id) {
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
		public Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable Long version) {
			this.version = version;
		}

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}

		@Nullable
		public String getIndexName() {
			return indexName;
		}

		public void setIndexName(@Nullable String indexName) {
			this.indexName = indexName;
		}
	}

	@Document(indexName = "entity-operations-test")
	record EntityFromRecord(
			@Id @Nullable String id,
			@Field(type = FieldType.Text) @Nullable String text,
			@Version @Nullable Long version,
			@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm,
			@IndexedIndexName @Nullable String indexName) {
	}
}
