/*
 * Copyright 2019-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.core.document;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchShardTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.erhlc.DocumentAdapters;

/**
 * Unit tests for {@link DocumentAdapters}.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @author Matt Gilene
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
	public void shouldAdaptSearchResponse() {

		Map<String, DocumentField> fields = Collections.singletonMap("field",
				new DocumentField("field", Collections.singletonList("value")));

		SearchShardTarget shard = new SearchShardTarget("node", new ShardId("index", "uuid", 42), null);
		SearchHit searchHit = new SearchHit(123, "my-id", new Text("type"), null, fields);
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

		SearchShardTarget shard = new SearchShardTarget("node", new ShardId("index", "uuid", 42), null);
		SearchHit searchHit = new SearchHit(123, "my-id", new Text("type"), null, null);
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

	@Test // #725
	@DisplayName("should adapt returned explanations")
	void shouldAdaptReturnedExplanations() {

		SearchHit searchHit = new SearchHit(42);
		searchHit.explanation(org.apache.lucene.search.Explanation.match( //
				3.14, //
				"explanation 3.14", //
				Collections.singletonList(org.apache.lucene.search.Explanation.noMatch( //
						"explanation noMatch", //
						Collections.emptyList()))));

		SearchDocument searchDocument = DocumentAdapters.from(searchHit);

		Explanation explanation = searchDocument.getExplanation();
		assertThat(explanation).isNotNull();
		assertThat(explanation.isMatch()).isTrue();
		assertThat(explanation.getValue()).isEqualTo(3.14);
		assertThat(explanation.getDescription()).isEqualTo("explanation 3.14");
		List<Explanation> details = explanation.getDetails();
		assertThat(details).containsExactly(new Explanation(false, 0.0, "explanation noMatch", Collections.emptyList()));
	}

	@Test // DATAES-979
	@DisplayName("should adapt returned matched queries")
	void shouldAdaptReturnedMatchedQueries() {
		SearchHit searchHit = new SearchHit(42);
		searchHit.matchedQueries(new String[] { "query1", "query2" });

		SearchDocument searchDocument = DocumentAdapters.from(searchHit);

		List<String> matchedQueries = searchDocument.getMatchedQueries();
		assertThat(matchedQueries).isNotNull();
		assertThat(matchedQueries).hasSize(2);
		assertThat(matchedQueries).isEqualTo(Arrays.asList("query1", "query2"));
	}
}
