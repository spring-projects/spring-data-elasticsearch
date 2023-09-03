/*
 * Copyright 2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

/**
 * Tests for the factory class to create {@link SearchDocumentResponse} instances.
 *
 * @author Sébastien Comeau
 * @since 5.2
 */
class SearchDocumentResponseBuilderUnitTests {

	private JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();

	@Test // GH-2681
	void shouldGetPhraseSuggestion() throws JSONException {
		// arrange
		final var hitsMetadata = new HitsMetadata.Builder<EntityAsMap>()
				.total(total -> total
					.value(0)
					.relation(TotalHitsRelation.Eq))
				.hits(new ArrayList<>())
				.build();

		final var suggestionTest = new Suggestion.Builder<EntityAsMap>()
			.phrase(phrase -> phrase
				.text("National")
				.offset(0)
				.length(8)
				.options(option -> option
					.text("nations")
					.highlighted("highlighted-nations")
					.score(0.11480146)
					.collateMatch(false))
				.options(option -> option
					.text("national")
					.highlighted("highlighted-national")
					.score( 0.08063514)
					.collateMatch(false)))
			.build();

		final var sortProperties = ImmutableMap.<String, List<Suggestion<EntityAsMap>>>builder()
			.put("suggestionTest", ImmutableList.of(suggestionTest))
			.build();

		// act
		final var actual = SearchDocumentResponseBuilder.from(hitsMetadata, null, null, null, sortProperties, null, jsonpMapper);

		// assert
		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(actual).isNotNull();
		softly.assertThat(actual.getSuggest()).isNotNull();
		softly.assertThat(actual.getSuggest().getSuggestions()).isNotNull().hasSize(1);

		final var actualSuggestion = actual.getSuggest().getSuggestions().get(0);
		softly.assertThat(actualSuggestion.getName()).isEqualTo("suggestionTest");
		softly.assertThat(actualSuggestion.getEntries()).isNotNull().hasSize(1);

		final var actualEntry = actualSuggestion.getEntries().get(0);
		softly.assertThat(actualEntry).isNotNull();
		softly.assertThat(actualEntry.getText()).isEqualTo("National");
		softly.assertThat(actualEntry.getOffset()).isEqualTo(0);
		softly.assertThat(actualEntry.getLength()).isEqualTo(8);
		softly.assertThat(actualEntry.getOptions()).isNotNull().hasSize(2);

		final var actualOption1 = actualEntry.getOptions().get(0);
		softly.assertThat(actualOption1.getText()).isEqualTo("nations");
		softly.assertThat(actualOption1.getHighlighted()).isEqualTo("highlighted-nations");
		softly.assertThat(actualOption1.getScore()).isEqualTo(0.11480146);
		softly.assertThat(actualOption1.getCollateMatch()).isEqualTo(false);

		final var actualOption2 = actualEntry.getOptions().get(1);
		softly.assertThat(actualOption2.getText()).isEqualTo("national");
		softly.assertThat(actualOption2.getHighlighted()).isEqualTo("highlighted-national");
		softly.assertThat(actualOption2.getScore()).isEqualTo(0.08063514);
		softly.assertThat(actualOption2.getCollateMatch()).isEqualTo(false);

		softly.assertAll();
	}
}
