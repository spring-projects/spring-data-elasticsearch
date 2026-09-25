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
package org.springframework.data.elasticsearch.cache.event;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * Event published after an entry has been removed from an Elasticsearch-backed cache.
 *
 * @author Anıl Şenocak
 * @since 6.2
 */
public final class CacheEvictedEvent implements CacheEvent {

	private final String cacheName;
	private final Object key;
	private final @Nullable Object value;
	private final Instant occurredAt;

	public CacheEvictedEvent(String cacheName, Object key, @Nullable Object value) {
		this(cacheName, key, value, Instant.now());
	}

	public CacheEvictedEvent(String cacheName, Object key, @Nullable Object value, Instant occurredAt) {

		Assert.hasText(cacheName, "Cache name must not be empty");
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(occurredAt, "OccurredAt must not be null");
		this.cacheName = cacheName;
		this.key = key;
		this.value = value;
		this.occurredAt = occurredAt;
	}

	@Override
	public String getCacheName() {
		return cacheName;
	}

	@Override
	public Object getKey() {
		return key;
	}

	@Override
	public @Nullable Object getValue() {
		return value;
	}

	@Override
	public Instant getOccurredAt() {
		return occurredAt;
	}
}
