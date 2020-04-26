package org.springframework.data.elasticsearch.core;

/**
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public class SequenceNumbers {
	public static boolean isAssignedSeqNo(long seqNo) {
		return seqNo >= 0;
	}

	public static boolean isAssignedPrimaryTerm(long primaryTerm) {
		return primaryTerm > 0;
	}

	private SequenceNumbers() {}
}
