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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anıl Şenocak
 * @since 6.2
 */
@SpringIntegrationTest
@ContextConfiguration(classes = ElasticsearchCacheELCIntegrationTests.Config.class)
class ElasticsearchCacheELCIntegrationTests {

	@Autowired private ElasticsearchOperations operations;

	private String indexName;
	private ElasticsearchCacheManager cacheManager;

	@BeforeEach
	void setUp() {
		indexName = "spring-cache-it-" + UUID.randomUUID();
		cacheManager = newCacheManager(ElasticsearchCacheConfiguration.defaultCacheConfig());
	}

	@AfterEach
	void tearDown() {
		cacheManager.close();
		var indexOperations = operations.indexOps(IndexCoordinates.of(indexName));
		if (indexOperations.exists()) {
			indexOperations.delete();
		}
	}

	@Test
	void persistsEntriesAndReloadsThemWithANewCacheManager() {

		CachedUser expected = new CachedUser("42", "Ada");
		cacheManager.getCache("users").put(expected.id(), expected);

		assertThat(cacheManager.getCache("users").get(expected.id(), CachedUser.class)).isEqualTo(expected);
		try (ElasticsearchCacheManager reloadedCacheManager =
				newCacheManager(ElasticsearchCacheConfiguration.defaultCacheConfig())) {
			assertThat(reloadedCacheManager.getCache("users").get(expected.id(), CachedUser.class)).isEqualTo(expected);
		}
	}

	@Test
	void updatesExistingEntriesInElasticsearch() {

		CachedUser original = new CachedUser("42", "Ada");
		CachedUser updated = new CachedUser("42", "Grace");
		ElasticsearchCache cache = cacheManager.getCache("users");
		cache.put(original.id(), original);
		cache.put(updated.id(), updated);

		try (ElasticsearchCacheManager reloadedCacheManager =
				newCacheManager(ElasticsearchCacheConfiguration.defaultCacheConfig())) {
			assertThat(reloadedCacheManager.getCache("users").get(updated.id(), CachedUser.class)).isEqualTo(updated);
		}
	}

	@Test
	void evictsEntriesFromElasticsearch() {

		CachedUser expected = new CachedUser("42", "Ada");
		ElasticsearchCache cache = cacheManager.getCache("users");
		cache.put(expected.id(), expected);

		assertThat(cache.evictIfPresent(expected.id())).isTrue();
		try (ElasticsearchCacheManager reloadedCacheManager =
				newCacheManager(ElasticsearchCacheConfiguration.defaultCacheConfig())) {
			assertThat(reloadedCacheManager.getCache("users").get(expected.id())).isNull();
		}
	}

	@Test
	void expiresEntriesAndRemovesThemFromElasticsearch() throws InterruptedException {

		ElasticsearchCacheManager expiringCacheManager = newCacheManager(
				ElasticsearchCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMillis(10)));
		try {
			CachedUser expected = new CachedUser("42", "Ada");
			expiringCacheManager.getCache("users").put(expected.id(), expected);
			Thread.sleep(25);

			assertThat(expiringCacheManager.getCache("users").get(expected.id())).isNull();
			assertThat(expiringCacheManager.evictExpired()).isOne();
			try (ElasticsearchCacheManager reloadedCacheManager =
					newCacheManager(ElasticsearchCacheConfiguration.defaultCacheConfig())) {
				assertThat(reloadedCacheManager.getCache("users").get(expected.id())).isNull();
			}
		}
		finally {
			expiringCacheManager.close();
		}
	}

	private ElasticsearchCacheManager newCacheManager(ElasticsearchCacheConfiguration configuration) {
		return ElasticsearchCacheManager.builder(operations).indexName(indexName).cacheDefaults(configuration).build();
	}

	@Configuration
	@Import(ElasticsearchTemplateConfiguration.class)
	static class Config {}

	private record CachedUser(String id, String username) {}
}
