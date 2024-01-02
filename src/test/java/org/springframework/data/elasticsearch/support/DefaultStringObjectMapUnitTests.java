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
package org.springframework.data.elasticsearch.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 */
class DefaultStringObjectMapUnitTests {

	private final SOM stringObjectMap = new SOM();

	@BeforeEach
	void setUp() {
		String json = """
				{
				  "index": {
				    "some": {
				      "deeply": {
				        "nested": {
				          "answer": 42
				        }
				      }
				    }
				  }
				}
				""";
		stringObjectMap.fromJson(json);
	}

	@Test
	@DisplayName("should parse key path")
	void shouldParseKeyPath() {
		assertThat(stringObjectMap.path("index.some.deeply.nested.answer")).isEqualTo(42);
	}

	@Test
	@DisplayName("should return null on non existing path")
	void shouldReturnNullOnNonExistingPath() {
		assertThat(stringObjectMap.path("index.some.deeply.nested.question")).isNull();
	}

	@Test
	@DisplayName("should return map object on partial path")
	void shouldReturnMapObjectOnPartialPath() {
		Object object = stringObjectMap.path("index.some.deeply.nested");
		assertThat(object).isNotNull().isInstanceOf(Map.class);
		// noinspection unchecked
		Map<String, Object> map = (Map<String, Object>) object;
		assertThat(map.get("answer")).isEqualTo(42);
	}

	static class SOM extends DefaultStringObjectMap<SOM> {}
}
