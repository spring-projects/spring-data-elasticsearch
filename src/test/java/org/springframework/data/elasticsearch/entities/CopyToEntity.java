/*
 * Copyright 2018-2019 the original author or authors.
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

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Sascha Woo
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "test-copy-to", type = "test", shards = 1, replicas = 0, refreshInterval = "-1")
public class CopyToEntity {

	@Id private String id;

	@Field(type = FieldType.Keyword, copyTo = "name") private String firstName;

	@Field(type = FieldType.Keyword, copyTo = "name") private String lastName;

	@Field(type = FieldType.Keyword) private String name;
}
