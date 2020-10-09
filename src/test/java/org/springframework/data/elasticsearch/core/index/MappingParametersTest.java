package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.Object;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
public class MappingParametersTest extends MappingContextBaseTests {

	private ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.get().getMappingContext()
			.getRequiredPersistentEntity(AnnotatedClass.class);

	@Test // DATAES-621
	public void shouldCreateParametersForFieldAnnotation() {
		Annotation annotation = entity.getRequiredPersistentProperty("field").findAnnotation(Field.class);

		MappingParameters mappingParameters = MappingParameters.from(annotation);

		assertThat(mappingParameters).isNotNull();
	}

	@Test // DATAES-621
	public void shouldCreateParametersForInnerFieldAnnotation() {

		MultiField multiField = entity.getRequiredPersistentProperty("mainField").findAnnotation(MultiField.class);
		InnerField innerField = multiField.otherFields()[0];
		MappingParameters mappingParameters = MappingParameters.from(innerField);

		assertThat(mappingParameters).isNotNull();
	}

	@Test // DATAES-621
	public void shouldNotCreateParametersForUnknownAnnotation() {
		Annotation annotation = entity.getRequiredPersistentProperty("score").findAnnotation(Score.class);

		assertThatThrownBy(() -> MappingParameters.from(annotation)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAES-621
	public void shouldNotAllowDocValueFalseOnFieldTypeText() {
		Annotation annotation = entity.getRequiredPersistentProperty("docValuesText").findAnnotation(Field.class);

		assertThatThrownBy(() -> MappingParameters.from(annotation)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAES-621
	public void shouldNotAllowDocValuesFalseOnFieldTypeNested() {
		Annotation annotation = entity.getRequiredPersistentProperty("docValuesNested").findAnnotation(Field.class);

		assertThatThrownBy(() -> MappingParameters.from(annotation)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAES-943
	@DisplayName("should allow enabled false only on object fields")
	void shouldAllowEnabledFalseOnlyOnObjectFields() {
		ElasticsearchPersistentEntity<?> failEntity = elasticsearchConverter.get().getMappingContext()
				.getRequiredPersistentEntity(InvalidEnabledFieldClass.class);
		Annotation annotation = failEntity.getRequiredPersistentProperty("disabledObject").findAnnotation(Field.class);

		assertThatThrownBy(() -> MappingParameters.from(annotation)).isInstanceOf(IllegalArgumentException.class);
	}

	static class AnnotatedClass {
		@Nullable @Field private String field;
		@Nullable @MultiField(mainField = @Field,
				otherFields = { @InnerField(suffix = "test", type = FieldType.Text) }) private String mainField;
		@Score private float score;
		@Nullable @Field(type = FieldType.Text, docValues = false) private String docValuesText;
		@Nullable @Field(type = FieldType.Nested, docValues = false) private String docValuesNested;
		@Nullable @Field(type = Object, enabled = true) private String enabledObject;
		@Nullable @Field(type = Object, enabled = false) private String disabledObject;
	}

	static class InvalidEnabledFieldClass {
		@Nullable @Field(type = FieldType.Text, enabled = false) private String disabledObject;
	}
}
