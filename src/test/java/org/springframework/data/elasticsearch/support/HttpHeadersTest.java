/*
 * Copyright 2022-2024 the original author or authors.
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
import static org.springframework.data.elasticsearch.support.HttpHeaders.*;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 * @since 5.0
 */
@SuppressWarnings("UastIncorrectHttpHeaderInspection")
class HttpHeadersTest {

	public static final String X_TEST_HEADER = "X-Test-Header";

	@Test // #2277
	@DisplayName("should find with case insensitive key")
	void shouldFindWithCaseInsensitiveKey() {

		var httpHeaders = new HttpHeaders();
		httpHeaders.set(X_TEST_HEADER, "foo");

		assertThat(httpHeaders.get(X_TEST_HEADER.toLowerCase(Locale.ENGLISH))).containsExactly("foo");
	}

	@Test // #2277
	@DisplayName("should overwrite values with set")
	void shouldOverwriteValuesWithSet() {
		var httpHeaders = new HttpHeaders();
		httpHeaders.add(X_TEST_HEADER, "foo");

		httpHeaders.set(X_TEST_HEADER, "bar");

		assertThat(httpHeaders.get(X_TEST_HEADER)).containsExactly("bar");
	}

	@Test // #2277
	@DisplayName("should set authentication header")
	void shouldSetAuthenticationHeader() {

		var encodedAuth = encodeBasicAuth("foo", "bar");
		var httpHeaders = new HttpHeaders();
		httpHeaders.setBasicAuth("foo", "bar");

		assertThat(httpHeaders.getFirst(AUTHORIZATION)).isEqualTo("Basic " + encodedAuth);
	}

	@Test // #2277
	@DisplayName("should initialize from Spring HttpHeaders")
	void shouldInitializeFromSpringHttpHeaders() {

		// we can use the Spring class in this test as we have spring-webflux as optional dependency and so have spring-web
		// as well

		org.springframework.http.HttpHeaders springHttpHeaders = new org.springframework.http.HttpHeaders();
		springHttpHeaders.addAll(X_TEST_HEADER, List.of("foo", "bar"));
		var headerName = "x-from-spring";
		springHttpHeaders.add(headerName, "true");

		var httpHeaders = new HttpHeaders();
		httpHeaders.addAll(springHttpHeaders);

		assertThat(httpHeaders.get(X_TEST_HEADER)).containsExactly("foo", "bar");
		assertThat(httpHeaders.get(headerName)).containsExactly("true");
	}
}
