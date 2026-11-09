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

import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.cache.event.CacheEvictedEvent;
import org.springframework.data.elasticsearch.cache.event.CacheInsertedEvent;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Spring {@link Cache} backed by Elasticsearch.
 *
 * @author Anıl Şenocak
 * @since 6.2
 */
public final class ElasticsearchCache implements Cache {

	private static final String SCALAR_VALUE_FIELD = "value";

	private final String name;
	private final CacheDocumentStore store;
	private final ElasticsearchConverter converter;
	private final ElasticsearchCacheConfiguration configuration;
	private final @Nullable ApplicationEventPublisher eventPublisher;
	private final ReentrantLock operationLock = new ReentrantLock();

	ElasticsearchCache(String name, CacheDocumentStore store, ElasticsearchConverter converter,
			ElasticsearchCacheConfiguration configuration, @Nullable ApplicationEventPublisher eventPublisher) {

		Assert.hasText(name, "Cache name must not be empty");
		Assert.notNull(store, "CacheDocumentStore must not be null");
		Assert.notNull(converter, "ElasticsearchConverter must not be null");
		Assert.notNull(configuration, "ElasticsearchCacheConfiguration must not be null");
		this.name = name;
		this.store = store;
		this.converter = converter;
		this.configuration = configuration;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getNativeCache() {
		return store;
	}

	@Override
	public @Nullable ValueWrapper get(Object key) {

		CacheDocument document = getDocument(key);
		return document != null ? new SimpleValueWrapper(readValue(document)) : null;
	}

	@Override
	public <T> @Nullable T get(Object key, @Nullable Class<T> type) {

		CacheDocument document = getDocument(key);
		if (document == null) {
			return null;
		}
		Object value = readValue(document);
		if (value == null) {
			return null;
		}
		if (type != null && !type.isInstance(value)) {
			throw new IllegalStateException("Cached value is not of required type [%s]: %s".formatted(type.getName(), value));
		}
		@SuppressWarnings("unchecked")
		T result = (T) value;
		return result;
	}

	@Override
	public <T> @Nullable T get(Object key, Callable<T> valueLoader) {

		Assert.notNull(valueLoader, "ValueLoader must not be null");
		CacheDocument document = getDocument(key);
		if (document != null) {
			@SuppressWarnings("unchecked")
			T cached = (T) readValue(document);
			return cached;
		}
		try {
			T value = valueLoader.call();
			put(key, value);
			return value;
		}
		catch (Exception exception) {
			throw new ValueRetrievalException(key, valueLoader, exception);
		}
	}

	@Override
	public void put(Object key, @Nullable Object value) {

		Assert.notNull(key, "Key must not be null");
		if (value == null && !configuration.getAllowCacheNullValues()) {
			throw new IllegalArgumentException("Cache '%s' does not allow null values".formatted(name));
		}
		CacheInsertedEvent event;
		operationLock.lock();
		try {
			String cacheKey = configuration.getKey(key);
			CacheDocument previous = store.get(name, cacheKey);
			Object previousValue = previous != null && !isExpired(previous, nowMillis()) ? readValue(previous) : null;
			store.put(createDocument(cacheKey, value));
			event = new CacheInsertedEvent(name, key, value, previousValue);
		}
		finally {
			operationLock.unlock();
		}
		publish(event);
	}

	@Override
	public @Nullable ValueWrapper putIfAbsent(Object key, @Nullable Object value) {

		operationLock.lock();
		try {
			CacheDocument existing = getDocument(key);
			if (existing != null) {
				return new SimpleValueWrapper(readValue(existing));
			}
			put(key, value);
			return null;
		}
		finally {
			operationLock.unlock();
		}
	}

	@Override
	public void evict(Object key) {
		evictIfPresent(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {

		Assert.notNull(key, "Key must not be null");
		CacheDocument removed;
		operationLock.lock();
		try {
			removed = store.delete(name, configuration.getKey(key));
		}
		finally {
			operationLock.unlock();
		}
		if (removed == null) {
			return false;
		}
		publish(new CacheEvictedEvent(name, key, readValue(removed)));
		return true;
	}

	@Override
	public void clear() {
		clearAndCount();
	}

	@Override
	public boolean invalidate() {
		return clearAndCount() > 0;
	}

	/**
	 * Remove expired entries and publish a {@link CacheEvictedEvent} for each removed entry.
	 *
	 * @return the number of removed entries.
	 */
	public int evictExpired() {

		List<CacheDocument> removed = new ArrayList<>();
		operationLock.lock();
		try {
			long nowMillis = nowMillis();
			for (CacheDocument document : store.findAll(name)) {
				if (isExpired(document, nowMillis)) {
					remove(document, removed);
				}
			}
		}
		finally {
			operationLock.unlock();
		}
		publishEvictions(removed);
		return removed.size();
	}

	private int clearAndCount() {

		List<CacheDocument> removed = new ArrayList<>();
		operationLock.lock();
		try {
			for (CacheDocument document : store.findAll(name)) {
				remove(document, removed);
			}
		}
		finally {
			operationLock.unlock();
		}
		publishEvictions(removed);
		return removed.size();
	}

	private void remove(CacheDocument document, List<CacheDocument> removedDocuments) {

		CacheDocument removed = store.delete(name, document.getRequiredCacheKey());
		if (removed != null) {
			removedDocuments.add(removed);
		}
	}

	private void publishEvictions(List<CacheDocument> removedDocuments) {
		removedDocuments.forEach(document ->
				publish(new CacheEvictedEvent(name, document.getRequiredCacheKey(), readValue(document))));
	}

	private @Nullable CacheDocument getDocument(Object key) {

		Assert.notNull(key, "Key must not be null");
		operationLock.lock();
		try {
			CacheDocument document = store.get(name, configuration.getKey(key));
			return document == null || isExpired(document, nowMillis()) ? null : document;
		}
		finally {
			operationLock.unlock();
		}
	}

	private CacheDocument createDocument(String cacheKey, @Nullable Object value) {

		Long expiresAt = expiresAt(nowMillis());
		String documentId = ElasticsearchCacheDocumentStore.documentId(name, cacheKey);
		if (value == null) {
			return new CacheDocument(documentId, name, cacheKey, null, "{}", false, true, expiresAt);
		}

		Class<?> valueType = value.getClass();
		ConversionService conversionService = converter.getConversionService();
		if (conversionService.canConvert(valueType, String.class)
				&& conversionService.canConvert(String.class, valueType)) {
			Document scalar = Document.create();
			scalar.put(SCALAR_VALUE_FIELD, conversionService.convert(value, String.class));
			return new CacheDocument(documentId, name, cacheKey, valueType.getName(), scalar.toJson(), true, false,
					expiresAt);
		}

		return new CacheDocument(documentId, name, cacheKey, valueType.getName(), converter.mapObject(value).toJson(), false,
				false, expiresAt);
	}

	private @Nullable Object readValue(CacheDocument document) {

		if (document.isNullValue()) {
			return null;
		}
		String valueTypeName = document.getValueType();
		if (valueTypeName == null) {
			throw new IllegalStateException("Cache document value type must not be null");
		}
		Class<?> valueType = ClassUtils.resolveClassName(valueTypeName, ClassUtils.getDefaultClassLoader());
		Document valueDocument = Document.parse(document.getRequiredValueJson());
		if (!document.isScalarValue()) {
			return converter.read(valueType, valueDocument);
		}

		Object scalar = valueDocument.get(SCALAR_VALUE_FIELD);
		if (scalar == null || valueType.isInstance(scalar)) {
			return scalar;
		}
		return converter.getConversionService().convert(scalar, valueType);
	}

	private @Nullable Long expiresAt(long nowMillis) {

		Duration ttl = configuration.getEntryTtl();
		return ttl.isZero() ? null : nowMillis + Math.max(ttl.toMillis(), 1);
	}

	private boolean isExpired(CacheDocument document, long nowMillis) {
		Long expiresAt = document.getExpiresAt();
		return expiresAt != null && expiresAt <= nowMillis;
	}

	private long nowMillis() {
		return System.currentTimeMillis();
	}

	private void publish(Object event) {
		if (eventPublisher != null) {
			eventPublisher.publishEvent(event);
		}
	}
}
