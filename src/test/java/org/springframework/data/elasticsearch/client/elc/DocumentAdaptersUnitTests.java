/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.document.SearchDocument;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
// todo #1973 check that all is tested what was in the elasticsearch7 version
class DocumentAdaptersUnitTests {

	private final JsonpMapper jsonpMapper = new JacksonJsonpMapper();

	@Test // #1973
	@DisplayName("should adapt search Hit from fields")
	void shouldAdaptSearchHitFromFields() {

		Hit<EntityAsMap> searchHit = new Hit.Builder<EntityAsMap>() //
				.index("index") //
				.id("my-id") //
				.score(42d) //
				.fields("field1", JsonData.of(Collections.singletonList("listValue"))) //
				.fields("field2", JsonData.of("stringValue")) //
				.seqNo(1l) //
				.primaryTerm(2l) //
				.build(); //

		SearchDocument document = DocumentAdapters.from(searchHit, jsonpMapper);

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(document.getIndex()).isEqualTo("index");
		softly.assertThat(document.hasId()).isTrue();
		softly.assertThat(document.getId()).isEqualTo("my-id");
		softly.assertThat(document.hasVersion()).isFalse();
		softly.assertThat(document.getScore()).isBetween(42f, 42f);
		Object field1 = document.get("field1");
		softly.assertThat(field1).isInstanceOf(List.class);
		// noinspection unchecked
		List<String> fieldList = (List<String>) field1;
		softly.assertThat(fieldList).containsExactly("listValue");
		softly.assertThat(document.get("field2")).isEqualTo("stringValue");
		softly.assertThat(document.hasSeqNo()).isTrue();
		softly.assertThat(document.getSeqNo()).isEqualTo(1);
		softly.assertThat(document.hasPrimaryTerm()).isTrue();
		softly.assertThat(document.getPrimaryTerm()).isEqualTo(2);

		softly.assertAll();
	}

	@Test // #1973
	@DisplayName("should adapt search Hit from source")
	void shouldAdaptSearchHitFromSource() {

		EntityAsMap eam = new EntityAsMap();
		eam.put("field", "value");
		Hit<EntityAsMap> searchHit = new Hit.Builder<EntityAsMap>() //
				.index("index") //
				.id("my-id") //
				.score(42d) //
				.seqNo(1l) //
				.primaryTerm(2l) //
				.source(eam) //
				.build(); //

		SearchDocument document = DocumentAdapters.from(searchHit, jsonpMapper);

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(document.getIndex()).isEqualTo("index");
		softly.assertThat(document.hasId()).isTrue();
		softly.assertThat(document.getId()).isEqualTo("my-id");
		softly.assertThat(document.hasVersion()).isFalse();
		softly.assertThat(document.getScore()).isBetween(42f, 42f);
		softly.assertThat(document.get("field")).isEqualTo("value");
		softly.assertThat(document.hasSeqNo()).isTrue();
		softly.assertThat(document.getSeqNo()).isEqualTo(1);
		softly.assertThat(document.hasPrimaryTerm()).isTrue();
		softly.assertThat(document.getPrimaryTerm()).isEqualTo(2);

		softly.assertAll();
	}
}
