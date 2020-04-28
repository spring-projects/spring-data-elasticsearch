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

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.elasticsearch.index.seqno.SequenceNumbers;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Puchkovskiy
 */
class SeqNoPrimaryTermTests {
	@Test
	void ofAssignedShouldReturnSomeSeqNoPrimaryTermWhenBothSeqNoAndPrimaryTermAreAssigned() {
		Optional<SeqNoPrimaryTerm> maybeSeqNoPrimaryTerm = SeqNoPrimaryTerm.ofAssigned(1, 2);

		assertThat(maybeSeqNoPrimaryTerm).contains(SeqNoPrimaryTerm.of(1, 2));
	}

	@Test
	void ofAssignedShouldReturnEmptyWhenSeqNoIsUnassigned() {
		Optional<SeqNoPrimaryTerm> maybeSeqNoPrimaryTerm = SeqNoPrimaryTerm.ofAssigned(SequenceNumbers.UNASSIGNED_SEQ_NO, 2);

		assertThat(maybeSeqNoPrimaryTerm).isEmpty();
	}

	@Test
	void ofAssignedShouldReturnEmptyWhenSeqNoIsNoOpsPerformed() {
		Optional<SeqNoPrimaryTerm> maybeSeqNoPrimaryTerm = SeqNoPrimaryTerm.ofAssigned(
				SequenceNumbers.NO_OPS_PERFORMED, 2);

		assertThat(maybeSeqNoPrimaryTerm).isEmpty();
	}

	@Test
	void ofAssignedShouldReturnEmptyWhenPrimaryTermIsUnassigned() {
		Optional<SeqNoPrimaryTerm> maybeSeqNoPrimaryTerm = SeqNoPrimaryTerm.ofAssigned(1,
				SequenceNumbers.UNASSIGNED_PRIMARY_TERM);

		assertThat(maybeSeqNoPrimaryTerm).isEmpty();
	}
}