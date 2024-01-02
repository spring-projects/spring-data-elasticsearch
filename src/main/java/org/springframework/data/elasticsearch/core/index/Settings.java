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
package org.springframework.data.elasticsearch.core.index;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.elasticsearch.support.DefaultStringObjectMap;
import org.springframework.util.Assert;

/**
 * class defining the settings for an index.
 *
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public class Settings extends DefaultStringObjectMap<Settings> {

	public Settings() {}

	public Settings(Map<String, Object> map) {
		super(map);
	}

	/**
	 * Creates a {@link Settings} object from the given JSON String
	 *
	 * @param json must not be {@literal null}
	 * @return Settings object
	 */
	public static Settings parse(String json) {
		return new Settings().fromJson(json);
	}

	@Override
	public String toString() {
		return "Settings: " + toJson();
	}

	@Override
	public Object get(Object key) {
		return containsKey(key) ? super.get(key) : path(key.toString());
	}

	/**
	 * Merges some other settings onto this one. Other has higher priority on same keys.
	 *
	 * @param other the other settings. Must not be {@literal null}
	 * @since 4.4
	 */
	public void merge(Settings other) {

		Assert.notNull(other, "other must not be null");

		deepMerge(this, other);
	}

	/*
	 * taken from https://stackoverflow.com/a/29698326/4393565
	 */
	@SuppressWarnings("unchecked")
	private static Map<?, ?> deepMerge(Map<String, Object> original, Map<String, Object> newMap) {
		for (Object key : newMap.keySet()) {
			if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
				Map<String, Object> originalChild = (Map<String, Object>) original.get(key);
				Map<String, Object> newChild = (Map<String, Object>) newMap.get(key);
				original.put(key.toString(), deepMerge(originalChild, newChild));
			} else if (newMap.get(key) instanceof List && original.get(key) instanceof List) {
				List<Object> originalChild = (List<Object>) original.get(key);
				List<Object> newChild = (List<Object>) newMap.get(key);
				for (Object each : newChild) {
					if (!originalChild.contains(each)) {
						originalChild.add(each);
					}
				}
			} else {
				original.put(key.toString(), newMap.get(key));
			}
		}
		return original;
	}

	/**
	 * flattens the nested structure (JSON fields index/foo/bar/: value) into a flat structure (index.foo.bar: value)
	 *
	 * @return Settings with the flattened elements.
	 */
	public Settings flatten() {
		return new Settings( //
				entrySet().stream() //
						.flatMap(Settings::doFlatten) //
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue))); //
	}

	/**
	 * flattens a Map<String, Object> to a stream of Map.Entry objects where the keys are the dot separated concatenated
	 * keys of sub map entries
	 */
	static private Stream<Map.Entry<String, Object>> doFlatten(Map.Entry<String, Object> entry) {

		if (entry.getValue()instanceof Map<?, ?> nested) {

			// noinspection unchecked
			return nested.entrySet().stream() //
					.map(e -> new AbstractMap.SimpleEntry<>(entry.getKey() + "." + e.getKey(), e.getValue()))
					.flatMap(e2 -> doFlatten((Entry<String, Object>) e2));
		} else {
			return Stream.of(entry);
		}
	}
}
