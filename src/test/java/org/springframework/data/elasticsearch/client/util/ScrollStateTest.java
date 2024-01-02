/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.util;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 */
class ScrollStateTest {

	@Test // DATAES-817
	void shouldReturnLastSetScrollId() {
		ScrollState scrollState = new ScrollState();

		scrollState.updateScrollId("id-1");
		scrollState.updateScrollId("id-2");

		assertThat(scrollState.getScrollId()).isEqualTo("id-2");
	}

	@Test
	void shouldReturnUniqueListOfUsedScrollIdsInCorrectOrder() {

		ScrollState scrollState = new ScrollState();

		scrollState.updateScrollId("id-1");
		scrollState.updateScrollId("id-2");
		scrollState.updateScrollId("id-1");
		scrollState.updateScrollId("id-3");
		scrollState.updateScrollId("id-2");

		assertThat(scrollState.getScrollIds()).isEqualTo(Arrays.asList("id-1", "id-2", "id-3"));
	}
}
