/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

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
@Document(indexName = "test-index-book", type = "book", shards = 1, replicas = 0, refreshInterval = "-1")
public class Book {

	@Id
	private String id;
	private String name;
	@Field(type = FieldType.Object)
	private Author author;
	@Field(type = FieldType.Nested)
	private Map<Integer, Collection<String>> buckets = new HashMap<>();
	@MultiField(
		mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
		otherFields = {
				@InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop", searchAnalyzer = "standard")
		}
	)
	private String description;
}
