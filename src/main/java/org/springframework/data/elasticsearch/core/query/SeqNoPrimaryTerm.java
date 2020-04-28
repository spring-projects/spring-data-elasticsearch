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
package org.springframework.data.elasticsearch.core.query;

import java.util.Objects;
import java.util.Optional;

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
 * <p>A property of this type is implicitly @{@link org.springframework.data.annotation.Transient} and never gets included
 * into a mapping at Elasticsearch side.
 * </p>
 * <p>Please note that an instance constructed via {@link SeqNoPrimaryTerm#of(long, long)} may contain unassigned values
 * for seq_no and primary_term. Such values will be accepted by Elasticsearch as if they were not provided at all.
 * {@link SeqNoPrimaryTerm#ofAssigned(long, long)} may be used to get either an instance with assigned seq_no and
 * primary_term values, or no instance at all.
 * </p>
 *
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public final class SeqNoPrimaryTerm {
	private final long sequenceNumber;
	private final long primaryTerm;

	/**
	 * Returns an instance of SeqNoPrimaryTerm with the given seq_no and primary_term. No validation is made of whether
	 * the passed values are valid (and assigned) seq_no and primary_term. If you need such a validation to be
	 * performed, please use {@link #ofAssigned(long, long)},
	 *
	 * @param sequenceNumber seq_no
	 * @param primaryTerm    primary_term
	 * @return SeqNoPrimaryTerm instance with the given seq_no and primary_term
	 * @see #ofAssigned(long, long)
	 */
	public static SeqNoPrimaryTerm of(long sequenceNumber, long primaryTerm) {
		return new SeqNoPrimaryTerm(sequenceNumber, primaryTerm);
	}

	/**
	 * Returns either an instance with valid (and assigned) values of seq_no and primary_term, or nothing.
	 * seq_no is valid and assigned when it is non-negative. primary_term is valid and assigned when it is positive.
	 *
	 * @param sequenceNumber seq_no
	 * @param primaryTerm    primary_term
	 * @return either an instance with valid (and assigned) values of seq_no and primary_term, or nothing
	 */
	public static Optional<SeqNoPrimaryTerm> ofAssigned(long sequenceNumber, long primaryTerm) {

		if (isAssignedSeqNo(sequenceNumber) && isAssignedPrimaryTerm(primaryTerm)) {
			return Optional.of(SeqNoPrimaryTerm.of(sequenceNumber, primaryTerm));
		}

		return Optional.empty();
	}

	private static boolean isAssignedSeqNo(long seqNo) {
		return seqNo >= 0;
	}

	private static boolean isAssignedPrimaryTerm(long primaryTerm) {
		return primaryTerm > 0;
	}

	private SeqNoPrimaryTerm(long sequenceNumber, long primaryTerm) {
		this.sequenceNumber = sequenceNumber;
		this.primaryTerm = primaryTerm;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public long getPrimaryTerm() {
		return primaryTerm;
	}

	@Override
	public String toString() {
		return "SeqNoPrimaryTerm{" +
				"sequenceNumber=" + sequenceNumber +
				", primaryTerm=" + primaryTerm +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SeqNoPrimaryTerm that = (SeqNoPrimaryTerm) o;
		return sequenceNumber == that.sequenceNumber &&
				primaryTerm == that.primaryTerm;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sequenceNumber, primaryTerm);
	}
}
