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
package org.springframework.data.elasticsearch.core.aggregation;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

/**
 * @author Remco Zigterman
 */
public class AggregatedPageImplTest {

	@Test
	public void constructFacetedPageWithPageable() {
		Page<String> page = new AggregatedPageImpl<>(Arrays.asList("Test", "Test 2"), PageRequest.of(0, 2), 10);

		assertEquals(10, page.getTotalElements());
		assertEquals(2, page.getNumberOfElements());
		assertEquals(2, page.getSize());
		assertEquals(5, page.getTotalPages());
	}
}
