/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Request;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RequestConverters}.
 *
 * @author Roman Puchkovskiy
 * @author Mark Paluch
 */
public class RequestConvertersTest {

	@Test // DATAES-652
	public void shouldNotAddIfSeqNoAndIfPrimaryTermToResultIfInputDoesNotcontainThemWhenConvertingIndexRequest() {
		IndexRequest request = createMinimalIndexRequest();

		Request result = RequestConverters.index(request);

		assertThat(result.getParameters()).doesNotContainKeys("if_seq_no", "if_primary_term");
	}

	private IndexRequest createMinimalIndexRequest() {

		IndexRequest request = new IndexRequest("the-index", "the-type", "id");
		request.source(Collections.singletonMap("test", "test"));
		return request;
	}

	@Test // DATAES-652
	public void shouldAddIfSeqNoAndIfPrimaryTermToResultIfInputcontainsThemWhenConvertingIndexRequest() {

		IndexRequest request = createMinimalIndexRequest();
		request.setIfSeqNo(3);
		request.setIfPrimaryTerm(4);

		Request result = RequestConverters.index(request);

		assertThat(result.getParameters()).containsEntry("if_seq_no", "3").containsEntry("if_primary_term", "4");
	}

	@Test // DATAES-652
	public void shouldNotAddIfSeqNoAndIfPrimaryTermToResultIfInputDoesNotcontainThemWhenConvertingDeleteRequest() {

		DeleteRequest request = createMinimalDeleteRequest();

		Request result = RequestConverters.delete(request);

		assertThat(result.getParameters()).doesNotContainKeys("if_seq_no", "if_primary_term");
	}

	private DeleteRequest createMinimalDeleteRequest() {
		return new DeleteRequest("the-index", "the-type", "id");
	}

	@Test // DATAES-652
	public void shouldAddIfSeqNoAndIfPrimaryTermToResultIfInputcontainsThemWhenConvertingDeleteRequest() {

		DeleteRequest request = createMinimalDeleteRequest();
		request.setIfSeqNo(3);
		request.setIfPrimaryTerm(4);

		Request result = RequestConverters.delete(request);

		assertThat(result.getParameters()).containsEntry("if_seq_no", "3");
		assertThat(result.getParameters()).containsEntry("if_primary_term", "4");
	}
}
