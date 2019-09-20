/*
 * Copyright 2018-2019 the original author or authors.
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

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.HashMap;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Request;
import org.junit.Test;

/**
 * Unit tests for {@link RequestConverters}.
 *
 * @author Roman Puchkovskiy
 */
public class RequestConvertersTest {
	@Test // DATAES-652
	public void shouldNotAddIfSeqNoAndIfPrimaryTermToResultIfInputDoesNotcontainThemWhenConvertingIndexRequest() {
		IndexRequest request = createMinimalIndexRequest();

		Request result = RequestConverters.index(request);

		assertThat(result.getParameters(), not(hasKey("if_seq_no")));
		assertThat(result.getParameters(), not(hasKey("if_primary_term")));
	}

	private IndexRequest createMinimalIndexRequest() {
		IndexRequest request = new IndexRequest("the-index", "the-type", "id");
		request.source(new HashMap<String, String>() {
			{
				put("test", "test");
			}
		});
		return request;
	}

	@Test // DATAES-652
	public void shouldAddIfSeqNoAndIfPrimaryTermToResultIfInputcontainsThemWhenConvertingIndexRequest() {
		IndexRequest request = createMinimalIndexRequest();
		request.setIfSeqNo(3);
		request.setIfPrimaryTerm(4);

		Request result = RequestConverters.index(request);

		assertThat(result.getParameters(), hasEntry("if_seq_no", "3"));
		assertThat(result.getParameters(), hasEntry("if_primary_term", "4"));
	}
}
