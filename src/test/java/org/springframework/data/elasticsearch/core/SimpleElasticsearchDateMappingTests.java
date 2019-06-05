/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.Data;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

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

	private static final String EXPECTED_MAPPING = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,"
			+ "\"type\":\"text\",\"index\":false,\"analyzer\":\"standard\"},\"customFormatDate\":{\"store\":false,\"type\":\"date\",\"format\":\"dd.MM.yyyy hh:mm\"},"
			+ "\"defaultFormatDate\":{\"store\":false,\"type\":\"date\"},\"basicFormatDate\":{\"store\":false,\""
			+ "type\":\"date\",\"format\":\"basic_date\"}}}}";

	@Test // DATAES-568
	public void testCorrectDateMappings() throws IOException {

		String mapping = getMappingBuilder().buildPropertyMapping(SampleDateMappingEntity.class);

		assertThat(mapping).isEqualTo(EXPECTED_MAPPING);
	}

	/**
	 * @author Jakub Vavrik
	 */
	@Data
	@Document(indexName = "test-index-date-mapping-core", type = "mapping", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleDateMappingEntity {

		@Id private String id;

		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		@Field(type = Date, format = DateFormat.custom,
				pattern = "dd.MM.yyyy hh:mm") private java.util.Date customFormatDate;

		@Field(type = FieldType.Date) private Date defaultFormatDate;

		@Field(type = FieldType.Date, format = DateFormat.basic_date) private Date basicFormatDate;
	}
}
