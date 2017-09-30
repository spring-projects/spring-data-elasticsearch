package org.springframework.data.elasticsearch.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.TermVector;

/**
 * @author Nikita Klimov
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "test-term-vectors", type = "test-term-vectors")
public class TermVectorsEntity {

	@Id private String id;

	@MultiField(
			mainField = @Field(termVector = TermVector.YES),
			otherFields = {
					@InnerField(suffix = "withTerms",
								type = FieldType.text,
								termVector = TermVector.YES),
					@InnerField(suffix = "withTermsAndItPositions",
								type = FieldType.text,
								termVector = TermVector.WITH_POSITIONS),
					@InnerField(suffix = "withTermsAndItOffsets",
								type = FieldType.text,
								termVector = TermVector.WITH_OFFSETS),
					@InnerField(suffix = "withTermsAndItPositionsAndItOffsets",
								type = FieldType.text,
								termVector = TermVector.WITH_POSITIONS_OFFSETS)
			})
	private String content;

	@Field(termVector = TermVector.YES)
	private String standaloneContent;
}
