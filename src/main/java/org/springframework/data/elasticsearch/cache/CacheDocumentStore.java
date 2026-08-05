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

import java.util.List;

/**
 * Persistence operations for cache documents.
 *
 * @author Anıl Şenocak
 * @since 6.2
 */
interface CacheDocumentStore {

	@Nullable CacheDocument get(String cacheName, String cacheKey);

	void put(CacheDocument document);

	@Nullable CacheDocument delete(String cacheName, String cacheKey);

	List<CacheDocument> findAll(String cacheName);
}
