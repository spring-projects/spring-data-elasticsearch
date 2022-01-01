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
package org.springframework.data.elasticsearch.support;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 */
class VersionInfoTest {

	@Test // #1824
	@DisplayName("should read version properties")
	void shouldReadVersionProperties() throws IOException {

		Properties properties = VersionInfo.versionProperties();

		assertThat(properties).isNotNull();
		assertThat(properties.getProperty("version.spring-data-elasticsearch")).isNotNull();
		assertThat(properties.getProperty("version.elasticsearch-client")).isNotNull();
	}
}
