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
import org.springframework.data.elasticsearch.cache.event.CacheEvictedEvent;
import org.springframework.data.elasticsearch.cache.event.CacheInsertedEvent;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anıl Şenocak
 */
class ElasticsearchCacheManagerTests extends ElasticsearchCacheTestSupport {

	@Test
	void reusesCachesAndIncludesConfiguredCacheNames() {

		ElasticsearchCacheManager manager = newManager(Map.of("expiring",
				ElasticsearchCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1))));

		ElasticsearchCache users = manager.getCache("users");

		assertThat(manager.getCache("users")).isSameAs(users);
		assertThat(manager.getCacheNames()).containsExactlyInAnyOrder("users", "expiring");
	}

	@Test
	void clearsAllKnownCachesAndPublishesEvents() {

		RecordingApplicationEventPublisher publisher = new RecordingApplicationEventPublisher();
		ElasticsearchCacheManager manager = new ElasticsearchCacheManager(new InMemoryCacheDocumentStore(), newConverter(),
				ElasticsearchCacheConfiguration.defaultCacheConfig(), Map.of(), publisher, null);
		manager.getCache("users").put("42", new CachedUser("42", "Ada"));
		manager.getCache("products").put("7", new CachedUser("7", "Grace"));

		manager.clearAll();

		assertThat(manager.getCache("users").get("42")).isNull();
		assertThat(manager.getCache("products").get("7")).isNull();
		assertThat(publisher.events).hasSize(4);
		assertThat(publisher.events.subList(0, 2)).allMatch(CacheInsertedEvent.class::isInstance);
		assertThat(publisher.events.subList(2, 4)).allMatch(CacheEvictedEvent.class::isInstance);
	}

	@Test
	void appliesNamedCacheConfiguration() throws InterruptedException {

		ElasticsearchCacheManager manager = newManager(Map.of("short-lived",
				ElasticsearchCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMillis(10))));
		manager.getCache("short-lived").put("42", new CachedUser("42", "Ada"));
		manager.getCache("default").put("7", new CachedUser("7", "Grace"));
		Thread.sleep(25);

		assertThat(manager.evictExpired()).isOne();
		assertThat(manager.getCache("short-lived").get("42")).isNull();
		assertThat(manager.getCache("default").get("7", CachedUser.class)).isEqualTo(new CachedUser("7", "Grace"));
	}

	@Test
	void removesExpiredEntriesAcrossKnownCaches() throws InterruptedException {

		ElasticsearchCacheConfiguration expiring = ElasticsearchCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMillis(10));
		ElasticsearchCacheManager manager = new ElasticsearchCacheManager(new InMemoryCacheDocumentStore(), newConverter(),
				expiring, Map.of(), null, null);
		manager.getCache("users").put("42", new CachedUser("42", "Ada"));
		manager.getCache("products").put("7", new CachedUser("7", "Grace"));
		Thread.sleep(25);

		assertThat(manager.evictExpired()).isEqualTo(2);
		assertThat(manager.getCache("users").get("42")).isNull();
		assertThat(manager.getCache("products").get("7")).isNull();
	}

	@Test
	void periodicallyRemovesExpiredEntries() throws InterruptedException {

		ElasticsearchCacheManager manager = new ElasticsearchCacheManager(new InMemoryCacheDocumentStore(), newConverter(),
				ElasticsearchCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMillis(10)), Map.of(), null,
				Duration.ofMillis(5));
		try {
			manager.getCache("users").put("42", new CachedUser("42", "Ada"));

			assertThat(manager.isEvictionScheduled()).isTrue();
			assertEventually(Duration.ofSeconds(1), () -> manager.getCache("users").get("42") == null);
		}
		finally {
			manager.close();
		}
	}

	private ElasticsearchCacheManager newManager(Map<String, ElasticsearchCacheConfiguration> configurations) {
		return new ElasticsearchCacheManager(new InMemoryCacheDocumentStore(), newConverter(),
				ElasticsearchCacheConfiguration.defaultCacheConfig(), configurations, null, null);
	}

	private void assertEventually(Duration timeout, Condition condition) throws InterruptedException {

		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (condition.evaluate()) {
				return;
			}
			Thread.sleep(5);
		}
		assertThat(condition.evaluate()).isTrue();
	}

	@FunctionalInterface
	private interface Condition {
		boolean evaluate();
	}

	private record CachedUser(String id, String name) {}
}
