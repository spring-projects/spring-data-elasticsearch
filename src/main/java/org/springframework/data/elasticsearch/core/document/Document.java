/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.document;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.springframework.data.elasticsearch.core.convert.ConversionException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A representation of a Elasticsearch document as extended {@link Map} with {@link String} keys. All iterators preserve
 * original insertion order.
 * <p>
 * Document does not allow {@code null} keys. It allows {@literal null} values.
 * <p>
 * Implementing classes can bei either mutable or immutable. In case a subclass is immutable, its methods may throw
 * {@link UnsupportedOperationException} when calling modifying methods.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public interface Document extends Map<String, Object> {

	/**
	 * Create a new mutable {@link Document}.
	 *
	 * @return a new {@link Document}.
	 */
	static Document create() {
		return new MapDocument();
	}

	/**
	 * Create a {@link Document} from a {@link Map} containing key-value pairs and sub-documents.
	 *
	 * @param map source map containing key-value pairs and sub-documents. must not be {@literal null}.
	 * @return a new {@link Document}.
	 */
	static Document from(Map<String, ? extends Object> map) {

		Assert.notNull(map, "Map must not be null");

		if (map instanceof LinkedHashMap) {
			return new MapDocument(map);
		}

		return new MapDocument(new LinkedHashMap<>(map));
	}

	/**
	 * Parse JSON to {@link Document}.
	 *
	 * @param json must not be {@literal null}.
	 * @return the parsed {@link Document}.
	 */
	static Document parse(String json) {

		Assert.notNull(json, "JSON must not be null");

		try {
			return new MapDocument(MapDocument.OBJECT_MAPPER.readerFor(Map.class).readValue(json));
		} catch (IOException e) {
			throw new ConversionException("Cannot parse JSON", e);
		}
	}

	/**
	 * {@link #put(Object, Object)} the {@code key}/{@code value} tuple and return {@code this} {@link Document}.
	 *
	 * @param key key with which the specified value is to be associated. must not be {@literal null}.
	 * @param value value to be associated with the specified key.
	 * @return {@code this} {@link Document}.
	 */
	default Document append(String key, Object value) {

		Assert.notNull(key, "Key must not be null");

		put(key, value);
		return this;
	}

	/**
	 * Return {@literal true} if this {@link Document} is associated with an identifier.
	 *
	 * @return {@literal true} if this {@link Document} is associated with an identifier, {@literal false} otherwise.
	 */
	default boolean hasId() {
		return false;
	}

	/**
	 * @return the index if this document was retrieved from an index
	 * @since 4.1
	 */
	@Nullable
	default String getIndex() {
		return null;
	}

	/**
	 * Sets the index name for this document
	 * 
	 * @param index index name
	 *          <p>
	 *          The default implementation throws {@link UnsupportedOperationException}.
	 * @since 4.1
	 */
	default void setIndex(@Nullable String index) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve the identifier associated with this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}. It's recommended to check {@link #hasId()}
	 * prior to calling this method.
	 *
	 * @return the identifier associated with this {@link Document}.
	 * @throws IllegalStateException if the underlying implementation supports Id's but no Id was yet associated with the
	 *           document.
	 */
	default String getId() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set the identifier for this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}.
	 */
	default void setId(String id) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return {@literal true} if this {@link Document} is associated with a version.
	 *
	 * @return {@literal true} if this {@link Document} is associated with a version, {@literal false} otherwise.
	 */
	default boolean hasVersion() {
		return false;
	}

	/**
	 * Retrieve the version associated with this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}. It's recommended to check
	 * {@link #hasVersion()} prior to calling this method.
	 *
	 * @return the version associated with this {@link Document}.
	 * @throws IllegalStateException if the underlying implementation supports Id's but no Id was yet associated with the
	 *           document.
	 */
	default long getVersion() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set the version for this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}.
	 */
	default void setVersion(long version) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return {@literal true} if this {@link Document} is associated with a seq_no.
	 *
	 * @return {@literal true} if this {@link Document} is associated with a seq_no, {@literal false} otherwise.
	 */
	default boolean hasSeqNo() {
		return false;
	}

	/**
	 * Retrieve the seq_no associated with this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}. It's recommended to check
	 * {@link #hasSeqNo()} prior to calling this method.
	 *
	 * @return the seq_no associated with this {@link Document}.
	 * @throws IllegalStateException if the underlying implementation supports seq_no's but no seq_no was yet associated
	 *           with the document.
	 */
	default long getSeqNo() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set the seq_no for this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}.
	 */
	default void setSeqNo(long seqNo) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return {@literal true} if this {@link Document} is associated with a primary_term.
	 *
	 * @return {@literal true} if this {@link Document} is associated with a primary_term, {@literal false} otherwise.
	 */
	default boolean hasPrimaryTerm() {
		return false;
	}

	/**
	 * Retrieve the primary_term associated with this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}. It's recommended to check
	 * {@link #hasPrimaryTerm()} prior to calling this method.
	 *
	 * @return the primary_term associated with this {@link Document}.
	 * @throws IllegalStateException if the underlying implementation supports primary_term's but no primary_term was yet
	 *           associated with the document.
	 */
	default long getPrimaryTerm() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set the primary_term for this {@link Document}.
	 * <p>
	 * The default implementation throws {@link UnsupportedOperationException}.
	 */
	default void setPrimaryTerm(long primaryTerm) {
		throw new UnsupportedOperationException();
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
	 * This method allows the application of a function to {@code this} {@link Document}. The function should expect a
	 * single {@link Document} argument and produce an {@code R} result.
	 * <p>
	 * Any exception thrown by the function will be propagated to the caller.
	 *
	 * @param transformer functional interface to a apply. must not be {@literal null}.
	 * @param <R> class of the result
	 * @return the result of applying the function to this string
	 * @see java.util.function.Function
	 */
	default <R> R transform(Function<? super Document, ? extends R> transformer) {

		Assert.notNull(transformer, "transformer must not be null");

		return transformer.apply(this);
	}

	/**
	 * Render this {@link Document} to JSON. Auxiliary values such as Id and version are not considered within the JSON
	 * representation.
	 *
	 * @return a JSON representation of this document.
	 */
	String toJson();

}
