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
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * @author Sascha Woo
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "test-index-normalizer", type = "test", shards = 1, replicas = 0, refreshInterval = "-1")
@Setting(settingPath = "/settings/test-normalizer.json")
public class NormalizerEntity {

	@Id private String id;

	@Field(type = FieldType.Keyword, normalizer = "lower_case_normalizer")
	private String name;

	@MultiField(mainField = @Field(type = FieldType.Text), otherFields = { @InnerField(suffix = "lower_case",
			type = FieldType.Keyword, normalizer = "lower_case_normalizer") })
	private String description;
}
