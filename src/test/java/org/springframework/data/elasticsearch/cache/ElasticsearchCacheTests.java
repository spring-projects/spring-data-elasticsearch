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

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.elasticsearch.cache.event.CacheEvictedEvent;
import org.springframework.data.elasticsearch.cache.event.CacheInsertedEvent;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Anıl Şenocak
 */
class ElasticsearchCacheTests extends ElasticsearchCacheTestSupport {

	@Test
	void persistsAndReadsValuesWithTheConfiguredConverter() {

		InMemoryCacheDocumentStore store = new InMemoryCacheDocumentStore();
		ElasticsearchCache cache = newCache("users", store);
		CachedUser user = new CachedUser("42", "Ada");

		cache.put("42", user);

		ElasticsearchCache reloadedCache = newCache("users", store);
		assertThat(reloadedCache.get("42", CachedUser.class)).isEqualTo(user);
		assertThat(reloadedCache.get("42").get()).isEqualTo(user);
		assertThatIllegalStateException().isThrownBy(() -> reloadedCache.get("42", String.class));
		assertThat(reloadedCache.getNativeCache()).isSameAs(store);
	}

	@Test
	void storesAndReadsScalarValues() {

		ElasticsearchCache cache = newCache("numbers", new InMemoryCacheDocumentStore());

		cache.put("answer", 42);

		assertThat(cache.get("answer", Integer.class)).isEqualTo(42);
	}

	@Test
	void cachesNullValuesByDefaultAndCanRejectThem() {

		ElasticsearchCache acceptingCache = newCache("accepting", new InMemoryCacheDocumentStore());
		acceptingCache.put("nullable", null);

		assertThat(acceptingCache.get("nullable")).isNotNull();
		assertThat(acceptingCache.get("nullable").get()).isNull();

		ElasticsearchCache rejectingCache = new ElasticsearchCache("rejecting", new InMemoryCacheDocumentStore(),
				newConverter(), ElasticsearchCacheConfiguration.defaultCacheConfig().disableCachingNullValues(), null);
		assertThatIllegalArgumentException().isThrownBy(() -> rejectingCache.put("nullable", null));
	}

	@Test
	void wrapsValueLoaderFailures() {

		ElasticsearchCache cache = newCache("users", new InMemoryCacheDocumentStore());

		assertThatExceptionOfType(Cache.ValueRetrievalException.class)
				.isThrownBy(() -> cache.get("42", () -> {
					throw new IllegalStateException("boom");
				}))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("42");
	}

	@Test
	void evictsExpiredEntriesAndPublishesEntryEvents() throws InterruptedException {

		RecordingApplicationEventPublisher publisher = new RecordingApplicationEventPublisher();
		ElasticsearchCache cache = new ElasticsearchCache("users", new InMemoryCacheDocumentStore(), newConverter(),
				ElasticsearchCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMillis(10)), publisher);
		CachedUser user = new CachedUser("42", "Ada");

		cache.put("42", user);
		cache.put("42", new CachedUser("42", "Grace"));
		Thread.sleep(25);

		assertThat(cache.get("42")).isNull();
		assertThat(cache.evictExpired()).isOne();
		assertThat(cache.evictExpired()).isZero();
		assertThat(publisher.events).hasSize(3);
		CacheInsertedEvent inserted = (CacheInsertedEvent) publisher.events.get(0);
		CacheInsertedEvent updated = (CacheInsertedEvent) publisher.events.get(1);
		CacheEvictedEvent evicted = (CacheEvictedEvent) publisher.events.get(2);
		assertThat(inserted.getValue()).isEqualTo(user);
		assertThat(updated.getPreviousValue()).isEqualTo(user);
		assertThat(evicted.getValue()).isEqualTo(new CachedUser("42", "Grace"));
	}

	@Test
	void convertsKeysBeforePersistingThem() {

		ElasticsearchCacheConfiguration configuration = ElasticsearchCacheConfiguration.defaultCacheConfig()
				.serializeKeysWith(key -> ((CacheKey) key).value());
		ElasticsearchCache cache = new ElasticsearchCache("users", new InMemoryCacheDocumentStore(), newConverter(),
				configuration, null);

		cache.put(new CacheKey("42"), new CachedUser("42", "Ada"));

		assertThat(cache.get(new CacheKey("42"), CachedUser.class)).isEqualTo(new CachedUser("42", "Ada"));
	}

	private ElasticsearchCache newCache(String name, CacheDocumentStore store) {
		return new ElasticsearchCache(name, store, newConverter(), ElasticsearchCacheConfiguration.defaultCacheConfig(), null);
	}

	private record CacheKey(String value) {}

	private record CachedUser(String id, String name) {}
}
