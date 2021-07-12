/*
 * Copyright 2021 the original author or authors.
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
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public class RangeTests {

	@Test
	public void shouldContainsLocalDate() {

		// given
		// when
		// then
		assertThat(Range.open(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1)).contains(LocalDate.of(2021, 1, 10)))
				.isTrue();
	}

	@Test
	public void shouldEqualToSameRange() {

		// given
		Range<LocalDate> range1 = Range.open(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1));
		Range<LocalDate> range2 = Range.open(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1));
		// when
		// then
		assertThat(range1).isEqualTo(range2);
	}

	@Test
	public void shouldHaveClosedBoundaries() {

		// given
		Range<Integer> range = Range.closed(1, 3);
		// when
		// then
		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
	}

	@Test
	public void shouldHaveJustOneValue() {

		// given
		Range<Integer> range = Range.just(2);
		// when
		// then
		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveLeftOpenBoundary() {

		// given
		Range<Integer> range = Range.leftOpen(1, 3);
		// when
		// then
		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
	}

	@Test
	public void shouldHaveLeftUnboundedAndRightExclusive() {

		// given
		Range<Integer> range = Range.leftUnbounded(Range.Bound.exclusive(3));
		// when
		// then
		assertThat(range.contains(0)).isTrue();
		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveLeftUnboundedAndRightInclusive() {

		// given
		Range<Integer> range = Range.leftUnbounded(Range.Bound.inclusive(3));
		// when
		// then
		assertThat(range.contains(0)).isTrue();
		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
	}

	@Test
	public void shouldHaveOpenBoundaries() {

		// given
		Range<Integer> range = Range.open(1, 3);
		// when
		// then
		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveRightOpenBoundary() {

		// given
		Range<Integer> range = Range.rightOpen(1, 3);
		// when
		// then
		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isFalse();
	}

	@Test
	public void shouldHaveRightUnboundedAndLeftExclusive() {

		// given
		Range<Integer> range = Range.rightUnbounded(Range.Bound.exclusive(1));
		// when
		// then
		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
		assertThat(range.contains(4)).isTrue();
	}

	@Test
	public void shouldHaveRightUnboundedAndLeftInclusive() {

		// given
		Range<Integer> range = Range.rightUnbounded(Range.Bound.inclusive(1));
		// when
		// then
		assertThat(range.contains(1)).isTrue();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(3)).isTrue();
		assertThat(range.contains(4)).isTrue();
	}

	@Test
	public void shouldThrowExceptionIfNotComparable() {

		// given
		// when
		Throwable thrown = catchThrowable(() -> {
			Range.just(Arrays.asList("test"));
		});
		// then
		assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("value must implements Comparable!");
	}

}
