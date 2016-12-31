/*
 * Copyright 2016 the original author or authors.
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

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Aleksandr Olisov
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@Document(indexName = "sample-partial-search-entity",
		type = "sample-partial-search-entity",
		shards = 1,
		replicas = 0,
		refreshInterval = "-1")
public class SampleAllFieldsSearchEntity {

	@Id
	private String id;

	private String fullSearchField1;
	@Field(type = FieldType.String)
	private String fullSearchField2;
	@Field(type = FieldType.String, includeInAll = true)
	private String fullSearchField3;

	@Field(type = FieldType.String, includeInAll = false)
	private String attributeOnlySearchField;
}
