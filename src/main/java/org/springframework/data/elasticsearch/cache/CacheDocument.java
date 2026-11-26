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
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Internal representation of a persisted cache entry.
 *
 * @author Anıl Şenocak
 * @since 6.2
 */
@Document(indexName = ElasticsearchCacheManager.DEFAULT_INDEX_NAME, createIndex = false,
		writeTypeHint = WriteTypeHint.FALSE, storeIdInSource = false)
class CacheDocument {

	@Id private @Nullable String id;
	@Field(type = FieldType.Keyword) private @Nullable String cacheName;
	@Field(type = FieldType.Keyword) private @Nullable String cacheKey;
	@Field(type = FieldType.Keyword, index = false) private @Nullable String valueType;
	@Field(type = FieldType.Text, index = false) private @Nullable String valueJson;
	@Field(type = FieldType.Boolean) private boolean scalarValue;
	@Field(type = FieldType.Boolean) private boolean nullValue;
	@Field(type = FieldType.Long) private @Nullable Long expiresAt;

	CacheDocument() {}

	CacheDocument(String id, String cacheName, String cacheKey, @Nullable String valueType, String valueJson,
			boolean scalarValue, boolean nullValue, @Nullable Long expiresAt) {
		this.id = id;
		this.cacheName = cacheName;
		this.cacheKey = cacheKey;
		this.valueType = valueType;
		this.valueJson = valueJson;
		this.scalarValue = scalarValue;
		this.nullValue = nullValue;
		this.expiresAt = expiresAt;
	}

	String getRequiredId() {
		return required(id, "id");
	}

	String getRequiredCacheName() {
		return required(cacheName, "cacheName");
	}

	String getRequiredCacheKey() {
		return required(cacheKey, "cacheKey");
	}

	@Nullable String getValueType() {
		return valueType;
	}

	String getRequiredValueJson() {
		return required(valueJson, "valueJson");
	}

	boolean isScalarValue() {
		return scalarValue;
	}

	boolean isNullValue() {
		return nullValue;
	}

	@Nullable Long getExpiresAt() {
		return expiresAt;
	}

	private static String required(@Nullable String value, String property) {
		if (value == null) {
			throw new IllegalStateException("Cache document property '%s' must not be null".formatted(property));
		}
		return value;
	}
}
