package org.springframework.data.elasticsearch.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Puchkovskiy
 */
class SequenceNumbersTests {
	@Test // DATAES-799
	void isAssignedSeqNoShouldReturnTrueIffSeqNoIsNonNegative() {
		assertFalse(SequenceNumbers.isAssignedSeqNo(org.elasticsearch.index.seqno.SequenceNumbers.UNASSIGNED_SEQ_NO));
		assertFalse(SequenceNumbers.isAssignedSeqNo(org.elasticsearch.index.seqno.SequenceNumbers.NO_OPS_PERFORMED));
		assertTrue(SequenceNumbers.isAssignedSeqNo(0));
		assertTrue(SequenceNumbers.isAssignedSeqNo(1));
	}

	@Test // DATAES-799
	void isAssignedPrimaryTermShouldReturnTrueIffPrimaryTermIsPositive() {
		assertFalse(SequenceNumbers.isAssignedPrimaryTerm(org.elasticsearch.index.seqno.SequenceNumbers.UNASSIGNED_PRIMARY_TERM));
		assertTrue(SequenceNumbers.isAssignedPrimaryTerm(1));
	}
}