/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.annotations;

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchPersistentEntity;

/**
 * @author Peter-Josef Meisch
 */
public class ComposableAnnotationsUnitTest {

	private static SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
	private static MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
	private static MappingBuilder mappingBuilder = new MappingBuilder(converter);

	@Test // DATAES-362
	@DisplayName("Document annotation should be composable")
	void documentAnnotationShouldBeComposable() {

		SimpleElasticsearchPersistentEntity<?> entity = mappingContext
				.getRequiredPersistentEntity(ComposedAnnotationEntity.class);

		assertThat(entity.getIndexCoordinates()).isEqualTo(IndexCoordinates.of("test-no-create"));
		assertThat(entity.isCreateIndexAndMapping()).isFalse();
		assertThat(entity.getShards()).isEqualTo((short) 42);
	}

	@Test // DATAES-362
	@DisplayName("Field annotation should be composable")
	void fieldAnnotationShouldBeComposable() {
		SimpleElasticsearchPersistentEntity<?> entity = mappingContext
				.getRequiredPersistentEntity(ComposedAnnotationEntity.class);

		ElasticsearchPersistentProperty property = entity.getRequiredPersistentProperty("nullValue");

		assertThat(property.getFieldName()).isEqualTo("null-value");
		assertThat(property.storeNullValue()).isTrue();
	}

	@Test // DATAES-362
	@DisplayName("should use composed Field annotations in MappingBuilder")
	void shouldUseComposedFieldAnnotationsInMappingBuilder() throws JSONException {

		String expected = "{\n" + //
				"  \"properties\":{\n" + //
				"    \"null-value\": {\n" + //
				"      \"null_value\": \"NULL\"\n" + //
				"    },\n" + //
				"    \"theDate\": {\n" + //
				"      \"type\": \"date\",\n" + //
				"      \"format\": \"date\"\n" + //
				"    },\n" + //
				"    \"multiField\": {\n" + //
				"      \"type\": \"text\",\n" + //
				"      \"fields\": {\n" + //
				"        \"keyword\": {\n" + //
				"          \"type\": \"keyword\"\n" + //
				"        }\n" + //
				"      }\n" + //
				"    }\n" + //
				"  }\n" + //
				"}\n"; //

		String mapping = mappingBuilder.buildPropertyMapping(ComposedAnnotationEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@Document(indexName = "", createIndex = false, shards = 42)
	public @interface DocumentNoCreate {

		@AliasFor(value = "indexName", annotation = Document.class)
		String indexName();
	}

	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Field(storeNullValue = true, nullValue = "NULL")
	public @interface NullValueField {
		@AliasFor(value = "name", annotation = Field.class)
		String name();
	}

	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Field(type = FieldType.Date, format = DateFormat.date)
	public @interface LocalDateField {
		@AliasFor(value = "name", annotation = Field.class)
		String name() default "";
	}

	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@MultiField(mainField = @Field(type = FieldType.Text),
			otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) })
	public @interface TextKeywordField {
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@DocumentNoCreate(indexName = "test-no-create")
	static class ComposedAnnotationEntity {
		@Id private String id;
		@NullValueField(name = "null-value") private String nullValue;
		@LocalDateField private LocalDate theDate;
		@TextKeywordField private String multiField;
	}
}
