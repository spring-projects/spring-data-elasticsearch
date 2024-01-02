/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author cdalxndr
 * @since 5.0
 */
class RuntimeFieldTest {

	@Test // #2267
	@DisplayName("should return mapping with script")
	void testMapping() {

		RuntimeField runtimeField = new RuntimeField("name", "double", "myscript");
		Map<String, Object> mapping = runtimeField.getMapping();
		assertThat(mapping).containsEntry("type", "double").containsEntry("script", "myscript");
	}

	@Test // #2267
	@DisplayName("should return mapping without script")
	void testMappingNoScript() {

		RuntimeField runtimeField = new RuntimeField("name", "double");
		Map<String, Object> mapping = runtimeField.getMapping();
		assertThat(mapping).containsEntry("type", "double").doesNotContainKey("script");
	}

}
