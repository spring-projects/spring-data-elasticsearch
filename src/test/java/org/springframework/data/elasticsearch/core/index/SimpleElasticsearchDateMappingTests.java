/*
 * Copyright 2013-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.Data;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Jakub Vavrik
 * @author Mohsin Husen
 * @author Don Wellington
 * @author Peter-Josef Meisch
 */
public class SimpleElasticsearchDateMappingTests extends MappingContextBaseTests {

	private static final String EXPECTED_MAPPING = "{\"properties\":{\"message\":{\"store\":true,"
			+ "\"type\":\"text\",\"index\":false,\"analyzer\":\"standard\"},\"customFormatDate\":{\"type\":\"date\",\"format\":\"dd.MM.uuuu hh:mm\"},"
			+ "\"basicFormatDate\":{\""
			+ "type\":\"date\",\"format\":\"basic_date\"}}}";

	@Test // DATAES-568, DATAES-828
	public void testCorrectDateMappings() {

		String mapping = getMappingBuilder().buildPropertyMapping(SampleDateMappingEntity.class);

		assertThat(mapping).isEqualTo(EXPECTED_MAPPING);
	}

	/**
	 * @author Jakub Vavrik
	 */
	@Data
	@Document(indexName = "test-index-date-mapping-core", replicas = 0,
			refreshInterval = "-1")
	static class SampleDateMappingEntity {

		@Id private String id;

		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		@Field(type = Date, format = DateFormat.custom,
				pattern = "dd.MM.uuuu hh:mm") private LocalDateTime customFormatDate;

		@Field(type = FieldType.Date, format = DateFormat.basic_date) private LocalDateTime basicFormatDate;
	}
}
