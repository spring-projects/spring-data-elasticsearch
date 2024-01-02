/*
 * Copyright 2021-2024 the original author or authors.
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

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.MappingContextBaseTests;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
public class ReactiveMappingBuilderUnitTests extends MappingContextBaseTests {

	ReactiveMappingBuilder getReactiveMappingBuilder() {
		return new ReactiveMappingBuilder(elasticsearchConverter.get());
	}

	@Test // #1822, #1824
	@DisplayName("should write runtime fields")
	void shouldWriteRuntimeFields() throws JSONException {

		ReactiveMappingBuilder mappingBuilder = getReactiveMappingBuilder();

		String expected = """
				{
				  "runtime": {
				    "day_of_week": {
				      "type": "keyword",
				      "script": {
				        "source": "emit(doc['@timestamp'].value.dayOfWeekEnum.getDisplayName(TextStyle.FULL, Locale.ROOT))"
				      }
				    }
				  },
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "@timestamp": {
				      "type": "date",
				      "format": "epoch_millis"
				    }
				  }
				}
				"""; //

		String mapping = Mono.defer(() -> mappingBuilder.buildReactivePropertyMapping(RuntimeFieldEntity.class))
				.subscribeOn(Schedulers.parallel()).block();

		assertEquals(expected, mapping, true);
	}

	// region entities
	@Document(indexName = "runtime-fields")
	@Mapping(runtimeFieldsPath = "/mappings/runtime-fields.json")
	private static class RuntimeFieldEntity {
		@Id
		@Nullable private String id;
		@Field(type = Date, format = DateFormat.epoch_millis, name = "@timestamp")
		@Nullable private Instant timestamp;
	}
	// endregion
}
