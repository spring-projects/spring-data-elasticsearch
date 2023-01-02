/*
 * Copyright 2020-2023 the original author or authors.
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

import static org.skyscreamer.jsonassert.JSONAssert.*;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.HighlightParameters;
import org.springframework.data.elasticsearch.client.erhlc.HighlightQueryBuilder;
import org.springframework.data.elasticsearch.core.ResourceUtil;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 */
@ExtendWith(MockitoExtension.class)
class HighlightQueryBuilderTests {

	private final SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();

	private final HighlightQueryBuilder highlightQueryBuilder = new HighlightQueryBuilder(context);

	@Test
	void shouldProcessAnnotationWithNoParameters() throws NoSuchMethodException, JSONException {
		Highlight highlight = getAnnotation("annotatedMethod");
		String expected = ResourceUtil.readFileFromClasspath("/highlights/highlights.json");

		HighlightBuilder highlightBuilder = highlightQueryBuilder.getHighlightBuilder(
				org.springframework.data.elasticsearch.core.query.highlight.Highlight.of(highlight), HighlightEntity.class);
		String actualStr = highlightBuilder.toString();
		assertEquals(expected, actualStr, false);

	}

	@Test
	void shouldProcessAnnotationWithParameters() throws NoSuchMethodException, JSONException {
		Highlight highlight = getAnnotation("annotatedMethodWithManyValue");
		String expected = ResourceUtil.readFileFromClasspath("/highlights/highlights-with-parameters.json");

		HighlightBuilder highlightBuilder = highlightQueryBuilder.getHighlightBuilder(
				org.springframework.data.elasticsearch.core.query.highlight.Highlight.of(highlight), HighlightEntity.class);
		String actualStr = highlightBuilder.toString();

		assertEquals(expected, actualStr, true);
	}

	private Highlight getAnnotation(String methodName) throws NoSuchMethodException {
		Highlight highlight = HighlightQueryBuilderTests.class.getDeclaredMethod(methodName).getAnnotation(Highlight.class);

		Assert.notNull(highlight, "no highlight annotation found");

		return highlight;
	}

	/**
	 * The annotation values on this method are just random values. The field has just one common parameters and the field
	 * specific, the bunch of parameters is tested on the top level. tagsSchema cannot be tested together with preTags and
	 * postTags, ist it sets its own values for these.
	 */
	// region test data
	@Highlight(fields = { @HighlightField(name = "someField") })
	private void annotatedMethod() {}

	@Highlight( //
			parameters = @HighlightParameters( //
					boundaryChars = "#+*", //
					boundaryMaxScan = 7, //
					boundaryScanner = "chars", //
					boundaryScannerLocale = "de-DE", //
					encoder = "html", //
					forceSource = true, //
					fragmenter = "span", //
					noMatchSize = 2, //
					numberOfFragments = 3, //
					fragmentSize = 5, //
					order = "score", //
					phraseLimit = 42, //
					preTags = { "<ab>", "<cd>" }, //
					postTags = { "</ab>", "</cd>" }, //
					requireFieldMatch = false, //
					type = "plain" //
			), //
			fields = { //
					@HighlightField( //
							name = "someField", //
							parameters = @HighlightParameters( //
									fragmentOffset = 3, //
									matchedFields = { "someField", "otherField" }, //
									numberOfFragments = 4) //
					//
					) //
			} //
	) //
	private void annotatedMethodWithManyValue() {}

	@Document(indexName = "dont-care")
	private static class HighlightEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(name = "some-field") private String someField;
		@Nullable
		@Field(name = "other-field") private String otherField;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getSomeField() {
			return someField;
		}

		public void setSomeField(String someField) {
			this.someField = someField;
		}
	}
	// endregion
}
