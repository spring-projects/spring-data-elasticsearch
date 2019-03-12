/*
 * Copyright 2019 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Ivan Greene
 */
@Document(indexName = "field-names-test", type = "field_named")
public class FieldNamedEntity {

	@Id
	private String id;

	@JsonProperty("snake_name")
	@Field(index = false, type = FieldType.Text)
	private String camelName;

	@JsonProperty
	@Field(index = false, type = FieldType.Text)
	private String noJsonName;

	@Field(index = false, type = FieldType.Text)
	private String noAnnotation;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCamelName() {
		return camelName;
	}

	public void setCamelName(String camelName) {
		this.camelName = camelName;
	}

	public String getNoJsonName() {
		return noJsonName;
	}

	public void setNoJsonName(String noJsonName) {
		this.noJsonName = noJsonName;
	}

	public String getNoAnnotation() {
		return noAnnotation;
	}

	public void setNoAnnotation(String noAnnotation) {
		this.noAnnotation = noAnnotation;
	}
}
