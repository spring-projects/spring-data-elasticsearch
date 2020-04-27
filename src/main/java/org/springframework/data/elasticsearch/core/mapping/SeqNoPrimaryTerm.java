/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import org.springframework.lang.Nullable;

/**
 * <p>A container for seq_no and primary_term values. When an entity class contains a field of this type,
 * it will be automatically filled with SeqNoPrimaryTerm instance on read operations (like get or search),
 * and also, when the SeqNoPrimaryTerm is not {@literal null} and filled with seq_no and primary_term,
 * they will be sent to Elasticsearch when indexing such an entity.
 * </p>
 * <p>This allows to implement optimistic locking pattern for full-update scenario, when an entity is first
 * read from Elasticsearch and then gets reindexed with new _content.
 * Index operations will throw an {@link org.springframework.dao.OptimisticLockingFailureException} if the
 * seq_no + primary_term pair already has different values for the given document. See Elasticsearch documentation
 * for more information: https://www.elastic.co/guide/en/elasticsearch/reference/current/optimistic-concurrency-control.html
 * </p>
 * <p>
 * A property of this type is implicitly @{@link org.springframework.data.annotation.Transient} and never gets included
 * into a mapping at Elasticsearch side.
 * </p>
 *
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public class SeqNoPrimaryTerm {
	@Nullable private Long sequenceNumber;
	@Nullable private Long primaryTerm;

	public SeqNoPrimaryTerm() {
	}

	public SeqNoPrimaryTerm(@Nullable Long sequenceNumber, @Nullable Long primaryTerm) {
		this.sequenceNumber = sequenceNumber;
		this.primaryTerm = primaryTerm;
	}

	@Nullable
	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(@Nullable Long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	@Nullable
	public Long getPrimaryTerm() {
		return primaryTerm;
	}

	public void setPrimaryTerm(@Nullable Long primaryTerm) {
		this.primaryTerm = primaryTerm;
	}

	@Override
	public String toString() {
		return "SeqNoPrimaryTerm{" +
				"sequenceNumber=" + sequenceNumber +
				", primaryTerm=" + primaryTerm +
				'}';
	}
}
