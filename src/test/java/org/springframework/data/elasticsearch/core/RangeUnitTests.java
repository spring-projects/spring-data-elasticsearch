/*
 * Copyright 2021-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * @author Sascha Woo
 * @since 4.3
 */
@Deprecated(since = "5.0", forRemoval = true)
public class RangeUnitTests {

	@Test
	public void shouldContainsLocalDate() {

		assertThat(Range.open(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1)).contains(LocalDate.of(2021, 1, 10)))
				.isTrue();
	}

	@Test
	public void shouldEqualToSameRange() {

		Range<LocalDate> range1 = Range.open(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1));
		Range<LocalDate> range2 = Range.open(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1));

		assertThat(range1).isEqualTo(range2);
	}

	@Test
	public void shouldHaveClosedBoundaries() {

		Range<Integer> range = Range.closed(1, 3);

		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
	}

	@Test
	public void shouldHaveJustOneValue() {

		Range<Integer> range = Range.just(2);

		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveLeftOpenBoundary() {

		Range<Integer> range = Range.leftOpen(1, 3);

		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
	}

	@Test
	public void shouldHaveLeftUnboundedAndRightExclusive() {

		Range<Integer> range = Range.leftUnbounded(Range.Bound.exclusive(3));

		assertThat(range.contains(0)).isTrue();
		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveLeftUnboundedAndRightInclusive() {

		Range<Integer> range = Range.leftUnbounded(Range.Bound.inclusive(3));

		assertThat(range.contains(0)).isTrue();
		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
	}

	@Test
	public void shouldHaveOpenBoundaries() {

		Range<Integer> range = Range.open(1, 3);

		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveRightOpenBoundary() {

		Range<Integer> range = Range.rightOpen(1, 3);

		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveRightUnboundedAndLeftExclusive() {

		Range<Integer> range = Range.rightUnbounded(Range.Bound.exclusive(1));

		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
		assertThat(range.contains(4)).isTrue();
	}

	@Test
	public void shouldHaveRightUnboundedAndLeftInclusive() {

		Range<Integer> range = Range.rightUnbounded(Range.Bound.inclusive(1));

		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
		assertThat(range.contains(4)).isTrue();
	}

	@Test
	public void shouldThrowExceptionIfNotComparable() {

		assertThatThrownBy(() -> Range.just(Collections.singletonList("test"))).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("value must implements Comparable!");
	}
}
