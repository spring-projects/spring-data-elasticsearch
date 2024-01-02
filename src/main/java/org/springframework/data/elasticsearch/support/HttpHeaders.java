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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/**
 * A simple class implementing HTTP headers as a MultiValueMap. This own implementation is necessary to remove the
 * dependency to the class with the same name from org.springframework:spring-web. Under the hood is uses a
 * {@link LinkedCaseInsensitiveMap}.
 *
 * @author Peter-Josef Meisch
 * @since 5.0
 */
public class HttpHeaders implements MultiValueMap<String, String> {

	public static final String AUTHORIZATION = "Authorization";

	private final MultiValueMap<String, String> delegate;

	public HttpHeaders() {
		this.delegate = new MultiValueMapAdapter<>(new LinkedCaseInsensitiveMap<>(Locale.ENGLISH));
	}

	// region MultiValueMap
	@Override
	@Nullable
	public String getFirst(String key) {
		return delegate.getFirst(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		delegate.add(key, value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		delegate.addAll(key, values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		delegate.addAll(values);
	}

	@Override
	public void set(String key, @Nullable String value) {
		delegate.set(key, value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		delegate.setAll(values);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		return delegate.toSingleValueMap();
	}
	// endregion

	// region Map
	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		return delegate.get(key);
	}

	@Nullable
	@Override
	public List<String> put(String key, List<String> value) {
		return delegate.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> m) {

		Assert.notNull(m, "m must not be null");

		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<String> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return delegate.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return delegate.entrySet();
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	// endregion
	public void setBasicAuth(String username, String password) {

		Assert.notNull(username, "username must not be null");
		Assert.notNull(password, "password must not be null");

		set(AUTHORIZATION, "Basic " + encodeBasicAuth(username, password));
	}

	/**
	 * Encode a username and password to be used in basic authorization. Code copied from the spring-web HttpHeaders
	 * class.
	 *
	 * @param username the username, must not contain a colon
	 * @param password the password
	 * @return the encoded value
	 */
	public static String encodeBasicAuth(String username, String password) {
		return encodeBasicAuth(username, password, null);
	}

	/**
	 * Encode a username and password to be used in basic authorization. Code copied from the spring-web HttpHeaders
	 * class.
	 *
	 * @param username the username, must not contain a colon
	 * @param password the password
	 * @param charset charset for the encoding, if {@literal null} StandardCharsets.ISO_8859_1 is used
	 * @return the encoded value
	 */
	public static String encodeBasicAuth(String username, String password, @Nullable Charset charset) {
		Assert.notNull(username, "Username must not be null");
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		Assert.notNull(password, "Password must not be null");
		if (charset == null) {
			charset = StandardCharsets.ISO_8859_1;
		}

		CharsetEncoder encoder = charset.newEncoder();
		if (encoder.canEncode(username) && encoder.canEncode(password)) {
			String credentialsString = username + ':' + password;
			byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
			return new String(encodedBytes, charset);
		} else {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to " + charset.displayName());
		}
	}
}
