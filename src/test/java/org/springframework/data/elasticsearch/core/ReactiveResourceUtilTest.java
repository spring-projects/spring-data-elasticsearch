/*
 * Copyright 2020-2022 the original author or authors.
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
import static org.skyscreamer.jsonassert.JSONAssert.*;

import reactor.test.StepVerifier;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 */
class ReactiveResourceUtilTest {

	@Test
	void shouldReadFromClasspath() {

		String expected = "{\n" + //
				"    \"index\": {\n" + //
				"        \"number_of_shards\": \"1\",\n" + //
				"        \"number_of_replicas\": \"0\",\n" + //
				"        \"analysis\": {\n" + //
				"            \"analyzer\": {\n" + //
				"                \"emailAnalyzer\": {\n" + //
				"                    \"type\": \"custom\",\n" + //
				"                    \"tokenizer\": \"uax_url_email\"\n" + //
				"                }\n" + //
				"            }\n" + //
				"        }\n" + //
				"    }\n" + //
				"}\n"; //

		ReactiveResourceUtil.readFileFromClasspath("/settings/test-settings.json") //
				.as(StepVerifier::create) //
				.consumeNextWith(actual -> {
					try {
						assertEquals(expected, actual, false);
					} catch (JSONException e) {
						fail("", e);
					}
				}) //
				.verifyComplete();

	}

	@Test
	void shouldErrorOnNonExistingResource() {
		ReactiveResourceUtil.readFileFromClasspath("/this/should/really/not/exist") //
				.as(StepVerifier::create) //
				.verifyError();
	}
}
