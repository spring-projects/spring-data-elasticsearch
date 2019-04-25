/*
 * Copyright 2019 the original author or authors.
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
 */package org.springframework.data.elasticsearch.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

/**
 * @author Peter-Josef Meisch
 */
public class FieldNameEntity {

	@Document(indexName = "fieldname-index", type = "fieldname-type")
	public static class IdEntity {
		@Id @Field("id-property")
		private String id;
	}

	@Document(indexName = "fieldname-index", type = "fieldname-type")
	public static class TextEntity {

		@Id @Field("id-property")
		private String id;

		@Field(name = "text-property", type = FieldType.Text)
		private String textProperty;
	}

	@Document(indexName = "fieldname-index", type = "fieldname-type")
	public static class MappingEntity {

		@Id @Field("id-property")
		private String id;

		@Field("mapping-property") @Mapping(mappingPath = "/mappings/test-field-analyzed-mappings.json")
		private byte[] mappingProperty;
	}

	@Document(indexName = "fieldname-index", type = "fieldname-type")
	public static class GeoPointEntity {

		@Id @Field("id-property")
		private String id;

		@Field("geopoint-property")
		private GeoPoint geoPoint;
	}

	@Document(indexName = "fieldname-index", type = "fieldname-type")
	public static class CircularEntity {

		@Id @Field("id-property")
		private String id;

		@Field(name = "circular-property", type = FieldType.Object,
				ignoreFields = { "circular-property" })
		private CircularEntity circularProperty;
	}

	@Document(indexName = "fieldname-index", type = "fieldname-type")
	public static class CompletionEntity {

		@Id @Field("id-property")
		private String id;

		@Field("completion-property")
		@CompletionField(maxInputLength = 100)
		private Completion suggest;
	}

	@Document(indexName = "fieldname-index", type = "fieldname-type")
	public static class MultiFieldEntity {

		@Id @Field("id-property")
		private String id;

		@Field("multifield-property")
		@MultiField(
				mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = {
						@InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop", searchAnalyzer = "standard")
				}
		)
		private String description;
	}
}
