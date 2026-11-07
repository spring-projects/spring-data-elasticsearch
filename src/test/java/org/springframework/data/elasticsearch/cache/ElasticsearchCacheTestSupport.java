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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Anıl Şenocak
 */
abstract class ElasticsearchCacheTestSupport {

	static ElasticsearchConverter newConverter() {

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.afterPropertiesSet();
		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
		converter.afterPropertiesSet();
		return converter;
	}

	static final class InMemoryCacheDocumentStore implements CacheDocumentStore {

		private final Map<String, CacheDocument> documents = new LinkedHashMap<>();

		@Override
		public @Nullable CacheDocument get(String cacheName, String cacheKey) {
			return documents.get(documentKey(cacheName, cacheKey));
		}

		@Override
		public void put(CacheDocument document) {
			documents.put(documentKey(document.getRequiredCacheName(), document.getRequiredCacheKey()), document);
		}

		@Override
		public @Nullable CacheDocument delete(String cacheName, String cacheKey) {
			return documents.remove(documentKey(cacheName, cacheKey));
		}

		@Override
		public List<CacheDocument> findAll(String cacheName) {
			return documents.values().stream()
					.filter(document -> document.getRequiredCacheName().equals(cacheName)).toList();
		}

		private String documentKey(String cacheName, String cacheKey) {
			return cacheName + ':' + cacheKey;
		}
	}

	static final class RecordingApplicationEventPublisher implements ApplicationEventPublisher {

		final List<Object> events = new ArrayList<>();

		@Override
		public void publishEvent(Object event) {
			events.add(event);
		}
	}
}
