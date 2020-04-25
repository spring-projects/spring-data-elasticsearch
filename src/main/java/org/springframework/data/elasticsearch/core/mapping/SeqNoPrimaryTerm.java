package org.springframework.data.elasticsearch.core.mapping;

import org.springframework.lang.Nullable;

/**
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public class SeqNoPrimaryTerm {
	@Nullable private Long sequenceNumber;
	@Nullable private Long primaryTerm;

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
}
