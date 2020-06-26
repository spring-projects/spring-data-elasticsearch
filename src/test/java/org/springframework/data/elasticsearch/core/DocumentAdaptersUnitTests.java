/*
 * Copyright 2019-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchShardTarget;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.DocumentAdapters;
import org.springframework.data.elasticsearch.core.document.SearchDocument;

/**
 * Unit tests for {@link DocumentAdapters}.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
public class DocumentAdaptersUnitTests {

	@Test // DATAES-628, DATAES-848
	public void shouldAdaptGetResponse() {

		Map<String, DocumentField> fields = Collections.singletonMap("field",
				new DocumentField("field", Collections.singletonList("value")));

		GetResult getResult = new GetResult("index", "type", "my-id", 1, 2, 42, true, null, fields, null);
		GetResponse response = new GetResponse(getResult);

		Document document = DocumentAdapters.from(response);

		assertThat(document.getIndex()).isEqualTo("index");
		assertThat(document.hasId()).isTrue();
		assertThat(document.getId()).isEqualTo("my-id");
		assertThat(document.hasVersion()).isTrue();
		assertThat(document.getVersion()).isEqualTo(42);
		assertThat(document.get("field")).isEqualTo("value");
		assertThat(document.hasSeqNo()).isTrue();
		assertThat(document.getSeqNo()).isEqualTo(1);
		assertThat(document.hasPrimaryTerm()).isTrue();
		assertThat(document.getPrimaryTerm()).isEqualTo(2);
	}

	@Test // DATAES-628, DATAES-848
	public void shouldAdaptGetResponseSource() {

		BytesArray source = new BytesArray("{\"field\":\"value\"}");

		GetResult getResult = new GetResult("index", "type", "my-id", 1, 2, 42, true, source, Collections.emptyMap(), null);
		GetResponse response = new GetResponse(getResult);

		Document document = DocumentAdapters.from(response);

		assertThat(document.getIndex()).isEqualTo("index");
		assertThat(document.hasId()).isTrue();
		assertThat(document.getId()).isEqualTo("my-id");
		assertThat(document.hasVersion()).isTrue();
		assertThat(document.getVersion()).isEqualTo(42);
		assertThat(document.get("field")).isEqualTo("value");
		assertThat(document.hasSeqNo()).isTrue();
		assertThat(document.getSeqNo()).isEqualTo(1);
		assertThat(document.hasPrimaryTerm()).isTrue();
		assertThat(document.getPrimaryTerm()).isEqualTo(2);
	}

	@Test // DATAES-799, DATAES-848
	public void shouldAdaptGetResult() {

		Map<String, DocumentField> fields = Collections.singletonMap("field",
				new DocumentField("field", Collections.singletonList("value")));

		GetResult getResult = new GetResult("index", "type", "my-id", 1, 2, 42, true, null, fields, null);

		Document document = DocumentAdapters.from(getResult);

		assertThat(document.getIndex()).isEqualTo("index");
		assertThat(document.hasId()).isTrue();
		assertThat(document.getId()).isEqualTo("my-id");
		assertThat(document.hasVersion()).isTrue();
		assertThat(document.getVersion()).isEqualTo(42);
		assertThat(document.get("field")).isEqualTo("value");
		assertThat(document.hasSeqNo()).isTrue();
		assertThat(document.getSeqNo()).isEqualTo(1);
		assertThat(document.hasPrimaryTerm()).isTrue();
		assertThat(document.getPrimaryTerm()).isEqualTo(2);
	}

	@Test // DATAES-799, DATAES-848
	public void shouldAdaptGetResultSource() {

		BytesArray source = new BytesArray("{\"field\":\"value\"}");

		GetResult getResult = new GetResult("index", "type", "my-id", 1, 2, 42, true, source, Collections.emptyMap(), null);

		Document document = DocumentAdapters.from(getResult);

		assertThat(document.getIndex()).isEqualTo("index");
		assertThat(document.hasId()).isTrue();
		assertThat(document.getId()).isEqualTo("my-id");
		assertThat(document.hasVersion()).isTrue();
		assertThat(document.getVersion()).isEqualTo(42);
		assertThat(document.get("field")).isEqualTo("value");
		assertThat(document.hasSeqNo()).isTrue();
		assertThat(document.getSeqNo()).isEqualTo(1);
		assertThat(document.hasPrimaryTerm()).isTrue();
		assertThat(document.getPrimaryTerm()).isEqualTo(2);
	}

	@Test // DATAES-628, DATAES-848
	public void shouldAdaptSearchResponse() throws IOException {

		Map<String, DocumentField> fields = Collections.singletonMap("field",
				new DocumentField("field", Collections.singletonList("value")));

		SearchShardTarget shard = new SearchShardTarget("node", new ShardId("index", "uuid", 42), null, null);
		SearchHit searchHit = new SearchHit(123, "my-id", new Text("type"), fields, null);
		searchHit.shard(shard);
		searchHit.setSeqNo(1);
		searchHit.setPrimaryTerm(2);
		searchHit.score(42);

		SearchDocument document = DocumentAdapters.from(searchHit);

		assertThat(document.getIndex()).isEqualTo("index");
		assertThat(document.hasId()).isTrue();
		assertThat(document.getId()).isEqualTo("my-id");
		assertThat(document.hasVersion()).isFalse();
		assertThat(document.getScore()).isBetween(42f, 42f);
		assertThat(document.get("field")).isEqualTo("value");
		assertThat(document.hasSeqNo()).isTrue();
		assertThat(document.getSeqNo()).isEqualTo(1);
		assertThat(document.hasPrimaryTerm()).isTrue();
		assertThat(document.getPrimaryTerm()).isEqualTo(2);
	}

	@Test // DATAES-628
	public void searchResponseShouldReturnContainsKey() {

		Map<String, DocumentField> fields = new LinkedHashMap<>();

		fields.put("string", new DocumentField("string", Collections.singletonList("value")));
		fields.put("bool", new DocumentField("bool", Arrays.asList(true, true, false)));

		SearchHit searchHit = new SearchHit(123, "my-id", new Text("type"), fields, null);

		SearchDocument document = DocumentAdapters.from(searchHit);

		assertThat(document.containsKey("string")).isTrue();
		assertThat(document.containsKey("not-set")).isFalse();
	}

	@Test // DATAES-628
	public void searchResponseShouldReturnContainsValue() {

		Map<String, DocumentField> fields = new LinkedHashMap<>();

		fields.put("string", new DocumentField("string", Collections.singletonList("value")));
		fields.put("bool", new DocumentField("bool", Arrays.asList(true, true, false)));
		fields.put("null", new DocumentField("null", Collections.emptyList()));

		SearchHit searchHit = new SearchHit(123, "my-id", new Text("type"), fields, null);

		SearchDocument document = DocumentAdapters.from(searchHit);

		assertThat(document.containsValue("value")).isTrue();
		assertThat(document.containsValue(Arrays.asList(true, true, false))).isTrue();
		assertThat(document.containsValue(null)).isTrue();
	}

	@Test // DATAES-628
	public void shouldRenderToJson() {

		Map<String, DocumentField> fields = new LinkedHashMap<>();

		fields.put("string", new DocumentField("string", Collections.singletonList("value")));
		fields.put("bool", new DocumentField("bool", Arrays.asList(true, true, false)));

		SearchHit searchHit = new SearchHit(123, "my-id", new Text("type"), fields, null);

		SearchDocument document = DocumentAdapters.from(searchHit);

		assertThat(document.toJson()).isEqualTo("{\"string\":\"value\",\"bool\":[true,true,false]}");
	}

	@Test // DATAES-628, DATAES-848
	public void shouldAdaptSearchResponseSource() {

		BytesArray source = new BytesArray("{\"field\":\"value\"}");

		SearchShardTarget shard = new SearchShardTarget("node", new ShardId("index", "uuid", 42), null, null);
		SearchHit searchHit = new SearchHit(123, "my-id", new Text("type"), Collections.emptyMap(), null);
		searchHit.shard(shard);
		searchHit.sourceRef(source).score(42);
		searchHit.version(22);
		searchHit.setSeqNo(1);
		searchHit.setPrimaryTerm(2);

		SearchDocument document = DocumentAdapters.from(searchHit);

		assertThat(document.getIndex()).isEqualTo("index");
		assertThat(document.hasId()).isTrue();
		assertThat(document.getId()).isEqualTo("my-id");
		assertThat(document.hasVersion()).isTrue();
		assertThat(document.getVersion()).isEqualTo(22);
		assertThat(document.getScore()).isBetween(42f, 42f);
		assertThat(document.get("field")).isEqualTo("value");
		assertThat(document.hasSeqNo()).isTrue();
		assertThat(document.getSeqNo()).isEqualTo(1);
		assertThat(document.hasPrimaryTerm()).isTrue();
		assertThat(document.getPrimaryTerm()).isEqualTo(2);
	}
}
