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
package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
class SettingsUnitTests {

	@Test
	@DisplayName("should merge other Settings on this settings")
	void shouldMergeOtherSettingsOnThisSettings() throws JSONException {

		String thisSettingsJson = "{\n" + //
				"  \"index\": {\n" + //
				"    \"weather\": \"sunny\",\n" + //
				"    \"backup\": {\n" + //
				"      \"interval\": 5,\n" + //
				"      \"target\": {\n" + //
				"        \"type\":\"cloud\",\n" + //
				"        \"provider\": \"prov1\"\n" + //
				"      }\n" + //
				"    },\n" + //
				"    \"music\": \"The Eagles\"\n" + //
				"  }\n" + //
				"}\n"; //
		String otherSettingsJson = "{\n" + //
				"  \"index\": {\n" + //
				"    \"weather\": \"rainy\",\n" + //
				"    \"backup\": {\n" + //
				"      \"interval\": 13,\n" + //
				"      \"target\": {\n" + // " \"type\":\"cloud\",\n" + //
				"        \"provider\": \"prov2\"\n" + //
				"      }\n" + //
				"    },\n" + //
				"    \"drink\": \"wine\"\n" + //
				"  }\n" + //
				"}\n"; //
		String mergedSettingsJson = "{\n" + //
				"  \"index\": {\n" + //
				"    \"weather\": \"rainy\",\n" + //
				"    \"backup\": {\n" + //
				"      \"interval\": 13,\n" + //
				"      \"target\": {\n" + //
				"        \"type\":\"cloud\",\n" + //
				"        \"provider\": \"prov2\"\n" + //
				"      }\n" + //
				"    },\n" + //
				"    \"music\": \"The Eagles\",\n" + //
				"    \"drink\": \"wine\"\n" + //
				"  }\n" + //
				"}\n"; //

		Settings thisSettings = Settings.parse(thisSettingsJson);
		Settings otherSettings = Settings.parse(otherSettingsJson);

		thisSettings.merge(otherSettings);

		assertEquals(mergedSettingsJson, thisSettings.toJson(), true);
	}

	@Test
	@DisplayName("should flatten its content")
	void shouldFlattenItsContent() {

		String settingsJson = "{\n" + //
				"  \"index\": {\n" + //
				"    \"weather\": \"sunny\",\n" + //
				"    \"backup\": {\n" + //
				"      \"interval\": 5,\n" + //
				"      \"target\": {\n" + //
				"        \"type\":\"cloud\",\n" + //
				"        \"provider\": \"prov1\"\n" + //
				"      }\n" + //
				"    },\n" + //
				"    \"music\": \"The Eagles\"\n" + //
				"  }\n" + //
				"}\n"; //

		Settings settings = Settings.parse(settingsJson);

		Settings flattened = settings.flatten();

		String flattenedKey = "index.backup.target.type";
		assertThat(flattened).containsKey(flattenedKey);
		assertThat(flattened.get(flattenedKey)).isEqualTo("cloud");
	}

}
