/*
 * Copyright 2020-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.elasticsearch.index.seqno.SequenceNumbers;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Puchkovskiy
 */
class SeqNoPrimaryTermTests {
	@Test
	void shouldConstructInstanceWithAssignedSeqNoAndPrimaryTerm() {
		SeqNoPrimaryTerm instance = new SeqNoPrimaryTerm(1, 2);

		assertThat(instance.getSequenceNumber()).isEqualTo(1);
		assertThat(instance.getPrimaryTerm()).isEqualTo(2);
	}

	@Test
	void shouldThrowAnExceptionWhenTryingToConstructWithUnassignedSeqNo() {
		assertThatThrownBy(() -> new SeqNoPrimaryTerm(SequenceNumbers.UNASSIGNED_SEQ_NO, 2))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldThrowAnExceptionWhenTryingToConstructWithSeqNoForNoOpsPerformed() {
		assertThatThrownBy(() -> new SeqNoPrimaryTerm(SequenceNumbers.NO_OPS_PERFORMED, 2))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldThrowAnExceptionWhenTryingToConstructWithUnassignedPrimaryTerm() {
		assertThatThrownBy(() -> new SeqNoPrimaryTerm(1, SequenceNumbers.UNASSIGNED_PRIMARY_TERM))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
