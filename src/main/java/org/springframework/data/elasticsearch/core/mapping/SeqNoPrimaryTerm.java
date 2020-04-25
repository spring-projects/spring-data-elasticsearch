package org.springframework.data.elasticsearch.core.mapping;

/**
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public class SeqNoPrimaryTerm {
	private Long sequenceNumber;
	private Long primaryTerm;

	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public Long getPrimaryTerm() {
		return primaryTerm;
	}

	public void setPrimaryTerm(Long primaryTerm) {
		this.primaryTerm = primaryTerm;
	}
}
