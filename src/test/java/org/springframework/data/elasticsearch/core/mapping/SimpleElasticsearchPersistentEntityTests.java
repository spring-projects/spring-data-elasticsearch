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
package org.springframework.data.elasticsearch.core.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.MappingContextBaseTests;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
public class SimpleElasticsearchPersistentEntityTests extends MappingContextBaseTests {

	@Nested
	@DisplayName("properties setup")
	class PropertiesTests {

		@Test
		public void shouldThrowExceptionGivenVersionPropertyIsNotLong() {

			TypeInformation<EntityWithWrongVersionType> typeInformation = ClassTypeInformation
					.from(EntityWithWrongVersionType.class);
			SimpleElasticsearchPersistentEntity<EntityWithWrongVersionType> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);

			assertThatThrownBy(() -> createProperty(entity, "version")).isInstanceOf(MappingException.class);
		}

		@Test
		public void shouldThrowExceptionGivenMultipleVersionPropertiesArePresent() {

			TypeInformation<EntityWithMultipleVersionField> typeInformation = ClassTypeInformation
					.from(EntityWithMultipleVersionField.class);
			SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);
			SimpleElasticsearchPersistentProperty persistentProperty1 = createProperty(entity, "version1");
			SimpleElasticsearchPersistentProperty persistentProperty2 = createProperty(entity, "version2");
			entity.addPersistentProperty(persistentProperty1);

			assertThatThrownBy(() -> entity.addPersistentProperty(persistentProperty2)).isInstanceOf(MappingException.class);
		}

		@Test
		void shouldFindPropertiesByMappedName() {

			SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
			SimpleElasticsearchPersistentEntity<?> persistentEntity = context
					.getRequiredPersistentEntity(FieldNameEntity.class);

			ElasticsearchPersistentProperty persistentProperty = persistentEntity
					.getPersistentPropertyWithFieldName("renamed-field");

			assertThat(persistentProperty).isNotNull();
			assertThat(persistentProperty.getName()).isEqualTo("renamedField");
			assertThat(persistentProperty.getFieldName()).isEqualTo("renamed-field");
		}

		@Test
		// DATAES-799
		void shouldReportThatThereIsNoSeqNoPrimaryTermPropertyWhenThereIsNoSuchProperty() {
			TypeInformation<EntityWithoutSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
					.from(EntityWithoutSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithoutSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);

			assertThat(entity.hasSeqNoPrimaryTermProperty()).isFalse();
		}

		@Test
		// DATAES-799
		void shouldReportThatThereIsSeqNoPrimaryTermPropertyWhenThereIsSuchProperty() {
			TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
					.from(EntityWithSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);

			entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));

			assertThat(entity.hasSeqNoPrimaryTermProperty()).isTrue();
		}

		@Test
		// DATAES-799
		void shouldReturnSeqNoPrimaryTermPropertyWhenThereIsSuchProperty() {

			TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
					.from(EntityWithSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);
			entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));
			EntityWithSeqNoPrimaryTerm instance = new EntityWithSeqNoPrimaryTerm();
			SeqNoPrimaryTerm seqNoPrimaryTerm = new SeqNoPrimaryTerm(1, 2);

			ElasticsearchPersistentProperty property = entity.getSeqNoPrimaryTermProperty();
			assertThat(property).isNotNull();

			entity.getPropertyAccessor(instance).setProperty(property, seqNoPrimaryTerm);

			assertThat(instance.seqNoPrimaryTerm).isSameAs(seqNoPrimaryTerm);
		}

		@Test
		// DATAES-799
		void shouldNotAllowMoreThanOneSeqNoPrimaryTermProperties() {
			TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
					.from(EntityWithSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);
			entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));

			assertThatThrownBy(() -> entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm2")))
					.isInstanceOf(MappingException.class);
		}

		@Test // #1680
		@DisplayName("should allow fields with id property names")
		void shouldAllowFieldsWithIdPropertyNames() {
			elasticsearchConverter.get().getMappingContext().getRequiredPersistentEntity(EntityWithIdNameFields.class);
		}

	}

	@Nested
	@DisplayName("index settings")
	class SettingsTests {

		@Test // #1719
		@DisplayName("should error if index sorting parameters do not have the same number of arguments")
		void shouldErrorIfIndexSortingParametersDoNotHaveTheSameNumberOfArguments() {

			assertThatThrownBy(() -> {
				elasticsearchConverter.get().getMappingContext()
						.getRequiredPersistentEntity(SettingsInvalidSortParameterSizes.class).getDefaultSettings();
			}).isInstanceOf(IllegalArgumentException.class);
		}

		@Test // #1719
		@DisplayName("should write sort parameters to Settings object")
		void shouldWriteSortParametersToSettingsObject() throws JSONException {

			String expected = "{\n" + //
					"  \"index.sort.field\": [\"second_field\", \"first_field\"],\n" + //
					"  \"index.sort.mode\": [\"max\", \"min\"],\n" + //
					"  \"index.sort.order\": [\"desc\",\"asc\"],\n" + //
					"  \"index.sort.missing\": [\"_last\",\"_first\"]\n" + //
					"}\n"; //

			ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.get().getMappingContext()
					.getRequiredPersistentEntity(SettingsValidSortParameterSizes.class);

			String json = entity.getDefaultSettings().toJson();
			assertEquals(expected, json, false);
		}
	}

	// region helper functions
	private static SimpleElasticsearchPersistentProperty createProperty(SimpleElasticsearchPersistentEntity<?> entity,
			String fieldName) {

		TypeInformation<?> type = entity.getTypeInformation();
		java.lang.reflect.Field field = ReflectionUtils.findField(entity.getType(), fieldName);
		assertThat(field).isNotNull();
		Property property = Property.of(type, field);
		return new SimpleElasticsearchPersistentProperty(property, entity, SimpleTypeHolder.DEFAULT, null);

	}
	// endregion

	// region entities
	private static class EntityWithWrongVersionType {

		@Nullable @Version private String version;

		@Nullable
		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}

	@SuppressWarnings("unused")
	private static class EntityWithMultipleVersionField {

		@Nullable @Version private Long version1;
		@Nullable @Version private Long version2;

		@Nullable
		public Long getVersion1() {
			return version1;
		}

		public void setVersion1(Long version1) {
			this.version1 = version1;
		}

		@Nullable
		public Long getVersion2() {
			return version2;
		}

		public void setVersion2(Long version2) {
			this.version2 = version2;
		}
	}

	@SuppressWarnings("unused")
	private static class FieldNameEntity {
		@Nullable @Id private String id;
		@Nullable @Field(name = "renamed-field") private String renamedField;
	}

	private static class EntityWithoutSeqNoPrimaryTerm {}

	@SuppressWarnings("unused")
	private static class EntityWithSeqNoPrimaryTerm {
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm2;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "fieldnames")
	private static class EntityWithIdNameFields {
		@Nullable @Id private String theRealId;
		@Nullable @Field(type = FieldType.Text, name = "document") private String document;
		@Nullable @Field(name = "id") private String renamedId;
	}

	@Document(indexName = "dontcare")
	@Setting(sortFields = { "first-field", "second-field" }, sortModes = { Setting.SortMode.max },
			sortOrders = { Setting.SortOrder.asc },
			sortMissingValues = { Setting.SortMissing._last, Setting.SortMissing._last, Setting.SortMissing._first })
	private static class SettingsInvalidSortParameterSizes {
		@Nullable @Id private String id;
		@Nullable @Field(name = "first-field", type = FieldType.Keyword) private String firstField;
		@Nullable @Field(name = "second-field", type = FieldType.Keyword) private String secondField;
	}

@Document(indexName = "dontcare")
// property names here, not field names
@Setting(sortFields = { "secondField", "firstField" }, sortModes = { Setting.SortMode.max, Setting.SortMode.min },
		sortOrders = { Setting.SortOrder.desc, Setting.SortOrder.asc },
		sortMissingValues = { Setting.SortMissing._last, Setting.SortMissing._first })
private static class SettingsValidSortParameterSizes {
	@Nullable @Id private String id;
	@Nullable @Field(name = "first_field", type = FieldType.Keyword) private String firstField;
	@Nullable @Field(name = "second_field", type = FieldType.Keyword) private String secondField;
}

	// endregion
}
