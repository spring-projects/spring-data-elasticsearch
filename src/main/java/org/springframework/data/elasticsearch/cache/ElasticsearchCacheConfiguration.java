/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.elasticsearch.cache;

import org.springframework.util.Assert;

import java.time.Duration;
import java.util.function.Function;

/**
 * Configuration used by an {@link ElasticsearchCache}.
 *
 * @author Anıl Şenocak
 * @since 6.2
 */
public final class ElasticsearchCacheConfiguration {

	private final Duration entryTtl;
	private final Function<Object, String> keyConverter;
	private final boolean cacheNullValues;

	private ElasticsearchCacheConfiguration(Duration entryTtl, Function<Object, String> keyConverter,
			boolean cacheNullValues) {
		this.entryTtl = entryTtl;
		this.keyConverter = keyConverter;
		this.cacheNullValues = cacheNullValues;
	}

	/**
	 * Create a default configuration without expiration and using {@link Object#toString()} for cache keys.
	 */
	public static ElasticsearchCacheConfiguration defaultCacheConfig() {
		return new ElasticsearchCacheConfiguration(Duration.ZERO, Object::toString, true);
	}

	/**
	 * Return a copy configured with the given time-to-live. {@link Duration#ZERO} disables expiration.
	 */
	public ElasticsearchCacheConfiguration entryTtl(Duration entryTtl) {

		Assert.notNull(entryTtl, "Entry TTL must not be null");
		Assert.isTrue(!entryTtl.isNegative(), "Entry TTL must not be negative");
		return new ElasticsearchCacheConfiguration(entryTtl, keyConverter, cacheNullValues);
	}

	/**
	 * Return a copy using the given converter to create the persisted cache key.
	 */
	public ElasticsearchCacheConfiguration serializeKeysWith(Function<Object, String> keyConverter) {

		Assert.notNull(keyConverter, "Key converter must not be null");
		return new ElasticsearchCacheConfiguration(entryTtl, keyConverter, cacheNullValues);
	}

	/**
	 * Return a copy that rejects {@literal null} cache values.
	 */
	public ElasticsearchCacheConfiguration disableCachingNullValues() {
		return new ElasticsearchCacheConfiguration(entryTtl, keyConverter, false);
	}

	Duration getEntryTtl() {
		return entryTtl;
	}

	boolean getAllowCacheNullValues() {
		return cacheNullValues;
	}

	String getKey(Object key) {

		String converted = keyConverter.apply(key);
		Assert.notNull(converted, "Key converter must not return null");
		return converted;
	}
}
