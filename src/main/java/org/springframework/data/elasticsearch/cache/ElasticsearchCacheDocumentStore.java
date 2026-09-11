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
import org.springframework.dao.DataAccessException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Persists cache documents using the store-independent {@link ElasticsearchOperations} API.
 *
 * @author Anıl Şenocak
 * @since 6.2
 */
final class ElasticsearchCacheDocumentStore implements CacheDocumentStore {

	private final ElasticsearchOperations operations;
	private final IndexCoordinates index;
	private final ReentrantLock indexCreationLock = new ReentrantLock();

	ElasticsearchCacheDocumentStore(ElasticsearchOperations operations, String indexName) {

		Assert.notNull(operations, "ElasticsearchOperations must not be null");
		Assert.hasText(indexName, "Index name must not be empty");
		this.operations = operations.withRefreshPolicy(RefreshPolicy.IMMEDIATE);
		this.index = IndexCoordinates.of(indexName);
	}

	@Override
	public @Nullable CacheDocument get(String cacheName, String cacheKey) {

		if (!indexExists()) {
			return null;
		}
		return operations.get(documentId(cacheName, cacheKey), CacheDocument.class, index);
	}

	@Override
	public void put(CacheDocument document) {
		ensureIndexExists();
		operations.save(document, index);
	}

	@Override
	public @Nullable CacheDocument delete(String cacheName, String cacheKey) {

		CacheDocument existing = get(cacheName, cacheKey);
		if (existing != null) {
			operations.delete(documentId(cacheName, cacheKey), index);
		}
		return existing;
	}

	@Override
	public List<CacheDocument> findAll(String cacheName) {

		if (!indexExists()) {
			return List.of();
		}
		CriteriaQuery query = new CriteriaQuery(Criteria.where("cacheName").is(cacheName));
		List<CacheDocument> documents = new ArrayList<>();
		try (SearchHitsIterator<CacheDocument> iterator = operations.searchForStream(query, CacheDocument.class, index)) {
			while (iterator.hasNext()) {
				SearchHit<CacheDocument> hit = iterator.next();
				documents.add(hit.getContent());
			}
		}
		return documents;
	}

	private void ensureIndexExists() {

		if (indexExists()) {
			return;
		}
		indexCreationLock.lock();
		try {
			if (indexExists()) {
				return;
			}
			IndexOperations indexOperations = operations.indexOps(index);
			try {
				indexOperations.create(indexOperations.createSettings(CacheDocument.class),
						indexOperations.createMapping(CacheDocument.class));
			}
			catch (DataAccessException exception) {
				if (!indexExists()) {
					throw exception;
				}
			}
		}
		finally {
			indexCreationLock.unlock();
		}
	}

	private boolean indexExists() {
		return operations.indexOps(index).exists();
	}

	static String documentId(String cacheName, String cacheKey) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(cacheKey.getBytes(StandardCharsets.UTF_8));
			return cacheName + ':' + HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 digest is unavailable", exception);
		}
	}
}
