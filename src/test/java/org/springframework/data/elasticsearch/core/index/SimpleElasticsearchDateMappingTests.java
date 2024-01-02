/*
 * Copyright 2013-2024 the original author or authors.
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
 */
package org.springframework.data.elasticsearch.core.index;

import static org.skyscreamer.jsonassert.JSONAssert.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.time.LocalDateTime;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.MappingContextBaseTests;
import org.springframework.lang.Nullable;

/**
 * @author Jakub Vavrik
 * @author Mohsin Husen
 * @author Don Wellington
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 */
public class SimpleElasticsearchDateMappingTests extends MappingContextBaseTests {

	private static final String EXPECTED_MAPPING = "{\"properties\":{\"message\":{\"store\":true,"
			+ "\"type\":\"text\",\"index\":false,\"analyzer\":\"standard\"},\"customFormatDate\":{\"type\":\"date\",\"format\":\"dd.MM.uuuu hh:mm\"},"
			+ "\"basicFormatDate\":{\"" + "type\":\"date\",\"format\":\"basic_date\"}}}";

	@Test // DATAES-568, DATAES-828
	public void testCorrectDateMappings() throws JSONException {

		String mapping = getMappingBuilder().buildPropertyMapping(SampleDateMappingEntity.class);

		assertEquals(EXPECTED_MAPPING, mapping, false);
	}

	@Document(indexName = "test-index-date-mapping-core")
	static class SampleDateMappingEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;
		@Nullable
		@Field(type = Date, format = {}, pattern = "dd.MM.uuuu hh:mm") private LocalDateTime customFormatDate;
		@Nullable
		@Field(type = FieldType.Date, format = DateFormat.basic_date) private LocalDateTime basicFormatDate;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		@Nullable
		public LocalDateTime getCustomFormatDate() {
			return customFormatDate;
		}

		public void setCustomFormatDate(@Nullable LocalDateTime customFormatDate) {
			this.customFormatDate = customFormatDate;
		}

		@Nullable
		public LocalDateTime getBasicFormatDate() {
			return basicFormatDate;
		}

		public void setBasicFormatDate(@Nullable LocalDateTime basicFormatDate) {
			this.basicFormatDate = basicFormatDate;
		}
	}
}
