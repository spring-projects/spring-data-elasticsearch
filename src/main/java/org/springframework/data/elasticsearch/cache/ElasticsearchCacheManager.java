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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CacheManager} that stores cache entries in Elasticsearch.
 *
 * @author Anıl Şenocak
 * @since 6.2
 */
public final class ElasticsearchCacheManager implements CacheManager, AutoCloseable {

	/** The default Elasticsearch index used to persist cache entries. */
	public static final String DEFAULT_INDEX_NAME = "spring_cache_entries";

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchCacheManager.class);

	private final CacheDocumentStore store;
	private final ElasticsearchConverter converter;
	private final ElasticsearchCacheConfiguration defaultCacheConfiguration;
	private final Map<String, ElasticsearchCacheConfiguration> initialCacheConfigurations;
	private final @Nullable ApplicationEventPublisher eventPublisher;
	private final ConcurrentMap<String, ElasticsearchCache> caches = new ConcurrentHashMap<>();
	private final @Nullable ScheduledExecutorService evictionExecutor;

	/**
	 * Create a cache manager that uses {@value #DEFAULT_INDEX_NAME} and the default cache configuration.
	 *
	 * @param operations the operations used to access Elasticsearch.
	 */
	public ElasticsearchCacheManager(ElasticsearchOperations operations) {
		this(operations, DEFAULT_INDEX_NAME, ElasticsearchCacheConfiguration.defaultCacheConfig(), Map.of(), null, null);
	}

	/**
	 * Create a cache manager that uses {@value #DEFAULT_INDEX_NAME}.
	 *
	 * @param operations the operations used to access Elasticsearch.
	 * @param defaultCacheConfiguration the configuration applied to caches without a specific configuration.
	 */
	public ElasticsearchCacheManager(ElasticsearchOperations operations,
			ElasticsearchCacheConfiguration defaultCacheConfiguration) {
		this(operations, DEFAULT_INDEX_NAME, defaultCacheConfiguration, Map.of(), null, null);
	}

	private ElasticsearchCacheManager(ElasticsearchOperations operations, String indexName,
			ElasticsearchCacheConfiguration defaultCacheConfiguration,
			Map<String, ElasticsearchCacheConfiguration> initialCacheConfigurations,
			@Nullable ApplicationEventPublisher eventPublisher, @Nullable Duration evictionInterval) {

		Assert.notNull(operations, "ElasticsearchOperations must not be null");
		Assert.hasText(indexName, "Index name must not be empty");
		Assert.notNull(defaultCacheConfiguration, "Default cache configuration must not be null");
		Assert.notNull(initialCacheConfigurations, "Initial cache configurations must not be null");
		this.store = new ElasticsearchCacheDocumentStore(operations, indexName);
		this.converter = operations.getElasticsearchConverter();
		this.defaultCacheConfiguration = defaultCacheConfiguration;
		this.initialCacheConfigurations = Map.copyOf(initialCacheConfigurations);
		this.eventPublisher = eventPublisher;
		this.evictionExecutor = scheduleEviction(evictionInterval);
	}

	ElasticsearchCacheManager(CacheDocumentStore store, ElasticsearchConverter converter,
			ElasticsearchCacheConfiguration defaultCacheConfiguration,
			Map<String, ElasticsearchCacheConfiguration> initialCacheConfigurations,
			@Nullable ApplicationEventPublisher eventPublisher, @Nullable Duration evictionInterval) {

		Assert.notNull(store, "CacheDocumentStore must not be null");
		Assert.notNull(converter, "ElasticsearchConverter must not be null");
		Assert.notNull(defaultCacheConfiguration, "Default cache configuration must not be null");
		Assert.notNull(initialCacheConfigurations, "Initial cache configurations must not be null");
		this.store = store;
		this.converter = converter;
		this.defaultCacheConfiguration = defaultCacheConfiguration;
		this.initialCacheConfigurations = Map.copyOf(initialCacheConfigurations);
		this.eventPublisher = eventPublisher;
		this.evictionExecutor = scheduleEviction(evictionInterval);
	}

	/**
	 * Create a builder for an {@link ElasticsearchCacheManager}.
	 *
	 * @param operations the operations used to access Elasticsearch.
	 * @return a new builder.
	 */
	public static Builder builder(ElasticsearchOperations operations) {
		return new Builder(operations);
	}

	@Override
	public ElasticsearchCache getCache(String name) {

		Assert.hasText(name, "Cache name must not be empty");
		return caches.computeIfAbsent(name, this::createCache);
	}

	@Override
	public Collection<String> getCacheNames() {

		LinkedHashSet<String> names = new LinkedHashSet<>(initialCacheConfigurations.keySet());
		names.addAll(caches.keySet());
		return List.copyOf(names);
	}

	/**
	 * Remove every entry from all cache names known to this manager.
	 */
	public void clearAll() {
		getCacheNames().forEach(cacheName -> getCache(cacheName).clear());
	}

	/**
	 * Remove expired entries from all cache names known to this manager.
	 *
	 * @return the number of removed entries.
	 */
	public int evictExpired() {
		return getCacheNames().stream().mapToInt(cacheName -> getCache(cacheName).evictExpired()).sum();
	}

	/**
	 * Return whether this manager is configured to remove expired entries periodically.
	 */
	public boolean isEvictionScheduled() {
		return evictionExecutor != null;
	}

	@Override
	public void close() {

		if (evictionExecutor != null) {
			evictionExecutor.shutdownNow();
		}
	}

	private ElasticsearchCache createCache(String name) {

		ElasticsearchCacheConfiguration configuration = initialCacheConfigurations.getOrDefault(name,
				defaultCacheConfiguration);
		return new ElasticsearchCache(name, store, converter, configuration, eventPublisher);
	}

	private @Nullable ScheduledExecutorService scheduleEviction(@Nullable Duration interval) {

		if (interval == null || interval.isZero()) {
			return null;
		}
		Assert.isTrue(!interval.isNegative(), "Eviction interval must not be negative");

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "elasticsearch-cache-expirer");
			thread.setDaemon(true);
			return thread;
		});
		long delay = Math.max(interval.toMillis(), 1L);
		executor.scheduleWithFixedDelay(this::evictExpiredSafely, delay, delay, TimeUnit.MILLISECONDS);
		return executor;
	}

	private void evictExpiredSafely() {

		try {
			evictExpired();
		}
		catch (RuntimeException exception) {
			LOGGER.warn("Cannot evict expired Elasticsearch cache entries", exception);
		}
	}

	/**
	 * Builder for {@link ElasticsearchCacheManager} instances.
	 *
	 * @author Anıl Şenocak
	 * @since 6.2
	 */
	public static final class Builder {

		private final ElasticsearchOperations operations;
		private String indexName = DEFAULT_INDEX_NAME;
		private ElasticsearchCacheConfiguration defaultCacheConfiguration = ElasticsearchCacheConfiguration.defaultCacheConfig();
		private final Map<String, ElasticsearchCacheConfiguration> initialCacheConfigurations = new LinkedHashMap<>();
		private @Nullable ApplicationEventPublisher eventPublisher;
		private @Nullable Duration evictionInterval;

		private Builder(ElasticsearchOperations operations) {
			Assert.notNull(operations, "ElasticsearchOperations must not be null");
			this.operations = operations;
		}

		/**
		 * Set the Elasticsearch index used to persist cache entries.
		 */
		public Builder indexName(String indexName) {

			Assert.hasText(indexName, "Index name must not be empty");
			this.indexName = indexName;
			return this;
		}

		/**
		 * Set the configuration applied to caches without a specific configuration.
		 */
		public Builder cacheDefaults(ElasticsearchCacheConfiguration defaultCacheConfiguration) {

			Assert.notNull(defaultCacheConfiguration, "Default cache configuration must not be null");
			this.defaultCacheConfiguration = defaultCacheConfiguration;
			return this;
		}

		/**
		 * Configure a named cache before it is first requested.
		 */
		public Builder withCacheConfiguration(String cacheName, ElasticsearchCacheConfiguration cacheConfiguration) {

			Assert.hasText(cacheName, "Cache name must not be empty");
			Assert.notNull(cacheConfiguration, "Cache configuration must not be null");
			initialCacheConfigurations.put(cacheName, cacheConfiguration);
			return this;
		}

		/**
		 * Configure named caches before they are first requested.
		 */
		public Builder withInitialCacheConfigurations(
				Map<String, ElasticsearchCacheConfiguration> initialCacheConfigurations) {

			Assert.notNull(initialCacheConfigurations, "Initial cache configurations must not be null");
			initialCacheConfigurations.forEach(this::withCacheConfiguration);
			return this;
		}

		/**
		 * Set the publisher that receives cache insertion and eviction events.
		 */
		public Builder applicationEventPublisher(ApplicationEventPublisher eventPublisher) {

			Assert.notNull(eventPublisher, "ApplicationEventPublisher must not be null");
			this.eventPublisher = eventPublisher;
			return this;
		}

		/**
		 * Remove expired entries at the given interval. A zero duration disables scheduled eviction.
		 */
		public Builder evictionInterval(Duration evictionInterval) {

			Assert.notNull(evictionInterval, "Eviction interval must not be null");
			Assert.isTrue(!evictionInterval.isNegative(), "Eviction interval must not be negative");
			this.evictionInterval = evictionInterval;
			return this;
		}

		/**
		 * Build the cache manager.
		 */
		public ElasticsearchCacheManager build() {
			return new ElasticsearchCacheManager(operations, indexName, defaultCacheConfiguration,
					initialCacheConfigurations, eventPublisher, evictionInterval);
		}
	}
}
