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
package org.springframework.data.elasticsearch.core.bulk;

/**
 * The type of an operation that was added to a {@link BulkIngester}.
 *
 * @author Ralph Ursprung
 * @since 6.2
 */
public enum BulkOperationType {
	/**
	 * an {@link org.springframework.data.elasticsearch.core.query.IndexQuery} with op type
	 * {@link org.springframework.data.elasticsearch.core.query.IndexQuery.OpType#INDEX} or without an op type.
	 */
	INDEX,
	/**
	 * an {@link org.springframework.data.elasticsearch.core.query.IndexQuery} with op type
	 * {@link org.springframework.data.elasticsearch.core.query.IndexQuery.OpType#CREATE}.
	 */
	CREATE,
	/**
	 * an {@link org.springframework.data.elasticsearch.core.query.UpdateQuery}.
	 */
	UPDATE,
	/**
	 * a delete by id.
	 */
	DELETE
}
