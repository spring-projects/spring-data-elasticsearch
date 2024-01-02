/*
 * Copyright 2021-2024 the original author or authors.
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 */
class VersionUnitTest {

	@Test // #1885
	@DisplayName("shouldThrowOnNullInput")
	void shouldThrowOnNullInput() {

		// noinspection ConstantConditions
		assertThatThrownBy(() -> Version.fromString(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // #1885
	@DisplayName("shouldThrowOnInvalidInput")
	void shouldThrowOnInvalidInput() {

		assertThatThrownBy(() -> Version.fromString("no-version")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test // #1885
	@DisplayName("should match major only")
	void shouldMatchMajorOnly() {

		Version version = Version.fromString("12");

		assertThat(version.major()).isEqualTo(12);
		assertThat(version.minor()).isEqualTo(0);
		assertThat(version.revision()).isEqualTo(0);
	}

	@Test // #1885
	@DisplayName("should match major only with details")
	void shouldMatchMajorOnlyWithDetails() {

		Version version = Version.fromString("12-alpha");

		assertThat(version.major()).isEqualTo(12);
		assertThat(version.minor()).isEqualTo(0);
		assertThat(version.revision()).isEqualTo(0);
	}

	@Test // #1885
	@DisplayName("should match major and minor only")
	void shouldMatchMajorAndMinorOnly() {

		Version version = Version.fromString("12.34");

		assertThat(version.major()).isEqualTo(12);
		assertThat(version.minor()).isEqualTo(34);
		assertThat(version.revision()).isEqualTo(0);
	}

	@Test // #1885
	@DisplayName("should match major and minor only with details")
	void shouldMatchMajorAndMinorOnlyWithDetails() {

		Version version = Version.fromString("12.34-alpha");

		assertThat(version.major()).isEqualTo(12);
		assertThat(version.minor()).isEqualTo(34);
		assertThat(version.revision()).isEqualTo(0);
	}

	@Test // #1885
	@DisplayName("should match all")
	void shouldMatchAll() {

		Version version = Version.fromString("12.34.56");

		assertThat(version.major()).isEqualTo(12);
		assertThat(version.minor()).isEqualTo(34);
		assertThat(version.revision()).isEqualTo(56);
	}

	@Test // #1885
	@DisplayName("should match all with details")
	void shouldMatchAllWithDetails() {

		Version version = Version.fromString("12.34.56-alpha");

		assertThat(version.major()).isEqualTo(12);
		assertThat(version.minor()).isEqualTo(34);
		assertThat(version.revision()).isEqualTo(56);
	}
}
