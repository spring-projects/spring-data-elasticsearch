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
package org.springframework.data.elasticsearch.core.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
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
public class SimpleElasticsearchPersistentEntityTests {

	@Test
	public void shouldThrowExceptionGivenVersionPropertyIsNotLong() {
		// given
		TypeInformation typeInformation = ClassTypeInformation.from(EntityWithWrongVersionType.class);
		SimpleElasticsearchPersistentEntity<EntityWithWrongVersionType> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);
		// when
		assertThatThrownBy(() -> {
			SimpleElasticsearchPersistentProperty persistentProperty = createProperty(entity, "version");
		}).isInstanceOf(MappingException.class);
	}

	@Test
	public void shouldThrowExceptionGivenMultipleVersionPropertiesArePresent() {
		// given
		TypeInformation typeInformation = ClassTypeInformation.from(EntityWithMultipleVersionField.class);
		SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);

		SimpleElasticsearchPersistentProperty persistentProperty1 = createProperty(entity, "version1");

		SimpleElasticsearchPersistentProperty persistentProperty2 = createProperty(entity, "version2");

		entity.addPersistentProperty(persistentProperty1);
		// when
		assertThatThrownBy(() -> {
			entity.addPersistentProperty(persistentProperty2);
		}).isInstanceOf(MappingException.class);
	}

	@Test // DATAES-462
	public void rejectsMultipleScoreProperties() {

		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();

		assertThatExceptionOfType(MappingException.class) //
				.isThrownBy(() -> context.getRequiredPersistentEntity(TwoScoreProperties.class)) //
				.withMessageContaining("first") //
				.withMessageContaining("second");
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

	@Test // DATAES-799
	void shouldReportThatThereIsNoSeqNoPrimaryTermPropertyWhenThereIsNoSuchProperty() {
		TypeInformation<EntityWithoutSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
				.from(EntityWithoutSeqNoPrimaryTerm.class);
		SimpleElasticsearchPersistentEntity<EntityWithoutSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);

		assertThat(entity.hasSeqNoPrimaryTermProperty()).isFalse();
	}

	@Test // DATAES-799
	void shouldReportThatThereIsSeqNoPrimaryTermPropertyWhenThereIsSuchProperty() {
		TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
				.from(EntityWithSeqNoPrimaryTerm.class);
		SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);

		entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));

		assertThat(entity.hasSeqNoPrimaryTermProperty()).isTrue();
	}

	@Test // DATAES-799
	void shouldReturnSeqNoPrimaryTermPropertyWhenThereIsSuchProperty() {
		TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
				.from(EntityWithSeqNoPrimaryTerm.class);
		SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);
		entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));
		EntityWithSeqNoPrimaryTerm instance = new EntityWithSeqNoPrimaryTerm();
		SeqNoPrimaryTerm seqNoPrimaryTerm = new SeqNoPrimaryTerm(1, 2);

		ElasticsearchPersistentProperty property = entity.getSeqNoPrimaryTermProperty();
		entity.getPropertyAccessor(instance).setProperty(property, seqNoPrimaryTerm);

		assertThat(instance.seqNoPrimaryTerm).isSameAs(seqNoPrimaryTerm);
	}

	@Test // DATAES-799
	void shouldNotAllowMoreThanOneSeqNoPrimaryTermProperties() {
		TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation
				.from(EntityWithSeqNoPrimaryTerm.class);
		SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);
		entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));

		assertThatThrownBy(() -> entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm2")))
				.isInstanceOf(MappingException.class);
	}

	private static SimpleElasticsearchPersistentProperty createProperty(SimpleElasticsearchPersistentEntity<?> entity,
			String field) {

		TypeInformation<?> type = entity.getTypeInformation();
		Property property = Property.of(type, ReflectionUtils.findField(entity.getType(), field));
		return new SimpleElasticsearchPersistentProperty(property, entity, SimpleTypeHolder.DEFAULT);

	}

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

	// DATAES-462
	static class TwoScoreProperties {

		@Score float first;
		@Score float second;
	}

	private static class FieldNameEntity {
		@Nullable @Id private String id;
		@Nullable @Field(name = "renamed-field") private String renamedField;
	}

	private static class EntityWithoutSeqNoPrimaryTerm {}

	private static class EntityWithSeqNoPrimaryTerm {
		private SeqNoPrimaryTerm seqNoPrimaryTerm;
		private SeqNoPrimaryTerm seqNoPrimaryTerm2;
	}
}
