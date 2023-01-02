/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQuery;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;

import com.google.common.collect.Lists;

/**
 * @author owen.qq
 * @since 4.4
 */
public class NativeSearchQueryBuilderTests {

	@Test // #2105
	void shouldContainEffectiveSearchAfterValue() {
		Long lastSortValue = 1L;
		List<Object> searchAfter = Lists.newArrayList(lastSortValue);

		NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
		nativeSearchQueryBuilder.withSearchAfter(searchAfter);
		NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

		assertThat(nativeSearchQuery.getSearchAfter()).isNotNull();
	}

	@Test // #2105
	void shouldIgnoreNullableSearchAfterValue() {
		List<Object> emptySearchValueByFirstSearch = null;
		NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
		nativeSearchQueryBuilder.withSearchAfter(emptySearchValueByFirstSearch);
		NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

		assertThat(nativeSearchQuery.getSearchAfter()).isNull();
	}

	@Test // #2105
	void shouldIgnoreEmptySearchAfterValue() {
		List<Object> emptySearchValueByFirstSearch = Lists.newArrayList();
		NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
		nativeSearchQueryBuilder.withSearchAfter(emptySearchValueByFirstSearch);
		NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

		assertThat(nativeSearchQuery.getSearchAfter()).isNull();
	}
}
