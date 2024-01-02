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

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Defines an interface for a Map&lt;String, Object> with additional convenience methods. All iterators must preserve
 * original insertion order.
 * <p>
 * StringObjectMap does not allow {@code null} keys. It allows {@literal null} values.
 * <p>
 * Implementing classes can bei either mutable or immutable. In case a subclass is immutable, its methods may throw
 * {@link UnsupportedOperationException} when calling modifying methods. *
 *
 * @param <M> the type extending this interface
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public interface StringObjectMap<M extends StringObjectMap<M>> extends Map<String, Object> {
	/**
	 * {@link #put(Object, Object)} the {@code key}/{@code value} tuple and return {@code this} object.
	 *
	 * @param key key with which the specified value is to be associated. must not be {@literal null}.
	 * @param value value to be associated with the specified key.
	 * @return {@code this} object.
	 */
	default M append(String key, Object value) {

		Assert.notNull(key, "Key must not be null");

		put(key, value);
		// noinspection unchecked
		return (M) this;
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped, or {@literal null} if this document contains no
	 * mapping for the key. The value is casted within the method which makes it useful for calling code as it does not
	 * require casting on the calling side. If the value type is not assignable to {@code type}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @param type the expected return value type.
	 * @param <T> expected return type.
	 * @return the value to which the specified key is mapped, or {@literal null} if this document contains no mapping for
	 *         the key.
	 * @throws ClassCastException if the value of the given key is not of {@code type T}.
	 */
	@Nullable
	default <T> T get(Object key, Class<T> type) {

		Assert.notNull(key, "Key must not be null");
		Assert.notNull(type, "Type must not be null");

		return type.cast(get(key));
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped, or {@literal null} if this document contains no
	 * mapping for the key. If the value type is not a {@link Boolean}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@literal null} if this document contains no mapping for
	 *         the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Boolean}.
	 */
	@Nullable
	default Boolean getBoolean(String key) {
		return get(key, Boolean.class);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or {@code defaultValue} if this document contains no
	 * mapping for the key. If the value type is not a {@link Boolean}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or {@code defaultValue} if this document contains no mapping
	 *         for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Boolean}.
	 */
	default boolean getBooleanOrDefault(String key, boolean defaultValue) {
		return getBooleanOrDefault(key, () -> defaultValue);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or the value from {@code defaultValue} if this
	 * document contains no mapping for the key. If the value type is not a {@link Boolean}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or the value from {@code defaultValue} if this document
	 *         contains no mapping for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Boolean}.
	 * @see BooleanSupplier
	 */
	default boolean getBooleanOrDefault(String key, BooleanSupplier defaultValue) {

		Boolean value = getBoolean(key);

		return value == null ? defaultValue.getAsBoolean() : value;
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped, or {@literal null} if this document contains no
	 * mapping for the key. If the value type is not a {@link Integer}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@literal null} if this document contains no mapping for
	 *         the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Integer}.
	 */
	@Nullable
	default Integer getInt(String key) {
		return get(key, Integer.class);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or {@code defaultValue} if this document contains no
	 * mapping for the key. If the value type is not a {@link Integer}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or {@code defaultValue} if this document contains no mapping
	 *         for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Integer}.
	 */
	default int getIntOrDefault(String key, int defaultValue) {
		return getIntOrDefault(key, () -> defaultValue);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or the value from {@code defaultValue} if this
	 * document contains no mapping for the key. If the value type is not a {@link Integer}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or the value from {@code defaultValue} if this document
	 *         contains no mapping for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Integer}.
	 * @see IntSupplier
	 */
	default int getIntOrDefault(String key, IntSupplier defaultValue) {

		Integer value = getInt(key);

		return value == null ? defaultValue.getAsInt() : value;
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped, or {@literal null} if this document contains no
	 * mapping for the key. If the value type is not a {@link Long}, then this method throws {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@literal null} if this document contains no mapping for
	 *         the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Long}.
	 */
	@Nullable
	default Long getLong(String key) {
		return get(key, Long.class);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or {@code defaultValue} if this document contains no
	 * mapping for the key. If the value type is not a {@link Long}, then this method throws {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or {@code defaultValue} if this document contains no mapping
	 *         for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Long}.
	 */
	default long getLongOrDefault(String key, long defaultValue) {
		return getLongOrDefault(key, () -> defaultValue);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or the value from {@code defaultValue} if this
	 * document contains no mapping for the key. If the value type is not a {@link Long}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or the value from {@code defaultValue} if this document
	 *         contains no mapping for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link Long}.
	 * @see LongSupplier
	 */
	default long getLongOrDefault(String key, LongSupplier defaultValue) {

		Long value = getLong(key);

		return value == null ? defaultValue.getAsLong() : value;
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped, or {@literal null} if this document contains no
	 * mapping for the key. If the value type is not a {@link String}, then this method throws {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@literal null} if this document contains no mapping for
	 *         the key.
	 * @throws ClassCastException if the value of the given key is not a {@link String}.
	 */
	@Nullable
	default String getString(String key) {
		return get(key, String.class);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or {@code defaultValue} if this document contains no
	 * mapping for the key. If the value type is not a {@link String}, then this method throws {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or {@code defaultValue} if this document contains no mapping
	 *         for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link String}.
	 */
	default String getStringOrDefault(String key, String defaultValue) {
		return getStringOrDefault(key, () -> defaultValue);
	}

	/**
	 * Returns the value to which the specified {@code key} is mapped or the value from {@code defaultValue} if this
	 * document contains no mapping for the key. If the value type is not a {@link String}, then this method throws
	 * {@link ClassCastException}.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped or the value from {@code defaultValue} if this document
	 *         contains no mapping for the key.
	 * @throws ClassCastException if the value of the given key is not a {@link String}.
	 * @see Supplier
	 */
	default String getStringOrDefault(String key, Supplier<String> defaultValue) {

		String value = getString(key);

		return value == null ? defaultValue.get() : value;
	}

	/**
	 * Render this {@link Document} to JSON. Auxiliary values such as Id and version are not considered within the JSON
	 * representation.
	 *
	 * @return a JSON representation of this document.
	 */
	String toJson();

	/**
	 * initializes this object from the given JSON String.
	 *
	 * @param json must not be {@literal null}
	 */
	M fromJson(String json);
}
