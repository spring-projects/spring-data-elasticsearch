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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Peter-Josef Meisch
 */
public class DefaultStringObjectMap<T extends StringObjectMap<T>> implements StringObjectMap<T> {

	static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private final LinkedHashMap<String, Object> delegate;

	public DefaultStringObjectMap() {
		this(new LinkedHashMap<>());
	}

	public DefaultStringObjectMap(Map<String, ?> map) {
		this.delegate = new LinkedHashMap<>(map);
	}

	@Override
	public String toJson() {
		try {
			return OBJECT_MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cannot render document to JSON", e);
		}
	}

	@Override
	public T fromJson(String json) {

		Assert.notNull(json, "JSON must not be null");

		delegate.clear();
		try {
			delegate.putAll(OBJECT_MAPPER.readerFor(Map.class).readValue(json));
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot parse JSON", e);
		}
		return (T) this;
	}

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
	public Object get(Object key) {
		return delegate.get(key);
	}

	@Override
	public Object getOrDefault(Object key, Object defaultValue) {
		return delegate.getOrDefault(key, defaultValue);
	}

	@Override
	public Object put(String key, Object value) {
		return delegate.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
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
	public Collection<Object> values() {
		return delegate.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super Object> action) {
		delegate.forEach(action);
	}

	@Override
	public String toString() {
		return "DefaultStringObjectMap: " + toJson();
	}

	/**
	 * Gets the object at the given key path (dot separated) or null if no object exists with this path.
	 *
	 * @param path the key path, must not be {@literal null}
	 * @return the found object or null
	 */
	@Nullable
	public Object path(String path) {

		Assert.notNull(path, "path must not be null");

		Map<String, Object> current = this;

		String[] segments = path.split("\\.");
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];

			if (current.containsKey(segment)) {
				Object currentObject = current.get(segment);

				if (i == segments.length - 1) {
					return currentObject;
				}

				if (currentObject instanceof Map) {
					current = (Map<String, Object>) currentObject;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		return null;
	}
}
