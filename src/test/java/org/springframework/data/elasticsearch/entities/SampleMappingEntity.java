/*
 * Copyright 2013 the original author or authors.
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

import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
@Document(indexName = "test-index-sample-mapping", type = "mapping", shards = 1, replicas = 0, refreshInterval = "-1")
public class SampleMappingEntity {

	@Id
	private String id;

	@Field(type = Text, index = false, store = true, analyzer = "standard")
	private String message;

	private NestedEntity nested;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	static class NestedEntity {

		@Field(type = Text)
		private String someField;

		public String getSomeField() {
			return someField;
		}

		public void setSomeField(String someField) {
			this.someField = someField;
		}
	}
}
