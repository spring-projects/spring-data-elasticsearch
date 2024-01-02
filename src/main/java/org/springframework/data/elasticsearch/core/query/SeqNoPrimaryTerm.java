/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

/**
 * <p>
 * A container for seq_no and primary_term values. When an entity class contains a field of this type, it will be
 * automatically filled with SeqNoPrimaryTerm instance on read operations (like get or search), and also, when the
 * SeqNoPrimaryTerm is not {@literal null} and filled with seq_no and primary_term, they will be sent to Elasticsearch
 * when indexing such an entity.
 * </p>
 * <p>
 * This allows to implement optimistic locking pattern for full-update scenario, when an entity is first read from
 * Elasticsearch and then gets reindexed with new _content. Index operations will throw an
 * {@link org.springframework.dao.OptimisticLockingFailureException} if the seq_no + primary_term pair already has
 * different values for the given document. See Elasticsearch documentation for more information:
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/optimistic-concurrency-control.html
 * </p>
 * <p>
 * A property of this type is implicitly @{@link org.springframework.data.annotation.Transient} and never gets included
 * into a mapping at Elasticsearch side.
 * </p>
 * <p>
 * A SeqNoPrimaryTerm instance cannot contain an invalid or unassigned seq_no or primary_term.
 * </p>
 *
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public record SeqNoPrimaryTerm(long sequenceNumber, long primaryTerm) {
	/**
	 * Creates an instance of SeqNoPrimaryTerm with the given seq_no and primary_term. The passed values are validated:
	 * sequenceNumber must be non-negative, primaryTerm must be positive. If validation fails, an IllegalArgumentException
	 * is thrown.
	 *
	 * @param sequenceNumber seq_no, must not be negative
	 * @param primaryTerm primary_term, must be positive
	 * @throws IllegalArgumentException if seq_no or primary_term is not valid
	 */
	public SeqNoPrimaryTerm {
		if (sequenceNumber < 0) {
			throw new IllegalArgumentException("seq_no should not be negative, but it's " + sequenceNumber);
		}
		if (primaryTerm <= 0) {
			throw new IllegalArgumentException("primary_term should be positive, but it's " + primaryTerm);
		}

	}

	@Override
	public String toString() {
		return "SeqNoPrimaryTerm{" + "sequenceNumber=" + sequenceNumber + ", primaryTerm=" + primaryTerm + '}';
	}
}
