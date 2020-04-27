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