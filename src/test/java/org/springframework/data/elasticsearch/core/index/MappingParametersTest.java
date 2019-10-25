package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;

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
		Annotation annotation = entity.getRequiredPersistentProperty("innerField").findAnnotation(InnerField.class);

		MappingParameters mappingParameters = MappingParameters.from(annotation);

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

	static class AnnotatedClass {
		@Field private String field;
		@InnerField(suffix = "test", type = FieldType.Text) private String innerField;
		@Score private float score;
		@Field(type = FieldType.Text, docValues = false) private String docValuesText;
		@Field(type = FieldType.Nested, docValues = false) private String docValuesNested;
	}
}
