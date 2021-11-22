/*
 * Copyright 2021-2022 the original author or authors.
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

import java.util.Optional;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Simple value object to work with ranges and boundaries.
 *
 * @author Sascha Woo
 * @since 4.3
 */
public class Range<T> {

	private final static Range<?> UNBOUNDED = Range.of(Bound.unbounded(), Bound.UNBOUNDED);

	/**
	 * The lower bound of the range.
	 */
	private final Bound<T> lowerBound;

	/**
	 * The upper bound of the range.
	 */
	private final Bound<T> upperBound;

	/**
	 * Creates a new {@link Range} with inclusive bounds for both values.
	 *
	 * @param <T>
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return
	 */
	public static <T> Range<T> closed(T from, T to) {
		return new Range<>(Bound.inclusive(from), Bound.inclusive(to));
	}

	/**
	 * Creates a new Range with the given value as sole member.
	 *
	 * @param <T>
	 * @param value must not be {@literal null}.
	 * @return
	 * @see Range#closed(Object, Object) 
	 */
	public static <T> Range<T> just(T value) {
		return Range.closed(value, value);
	}

	/**
	 * Creates a new left-open {@link Range}, i.e. left exclusive, right inclusive.
	 *
	 * @param <T>
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return
	 */
	public static <T> Range<T> leftOpen(T from, T to) {
		return new Range<>(Bound.exclusive(from), Bound.inclusive(to));
	}

	/**
	 * Creates a left-unbounded {@link Range} (the left bound set to {@link Bound#unbounded()}) with the given right
	 * bound.
	 *
	 * @param <T>
	 * @param to the right {@link Bound}, must not be {@literal null}.
	 * @return
	 */
	public static <T> Range<T> leftUnbounded(Bound<T> to) {
		return new Range<>(Bound.unbounded(), to);
	}

	/**
	 * Creates a new {@link Range} with the given lower and upper bound.
	 *
	 * @param lowerBound must not be {@literal null}.
	 * @param upperBound must not be {@literal null}.
	 */
	public static <T> Range<T> of(Bound<T> lowerBound, Bound<T> upperBound) {
		return new Range<>(lowerBound, upperBound);
	}

	/**
	 * Creates a new {@link Range} with exclusive bounds for both values.
	 *
	 * @param <T>
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return
	 */
	public static <T> Range<T> open(T from, T to) {
		return new Range<>(Bound.exclusive(from), Bound.exclusive(to));
	}

	/**
	 * Creates a new right-open {@link Range}, i.e. left inclusive, right exclusive.
	 *
	 * @param <T>
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return
	 */
	public static <T> Range<T> rightOpen(T from, T to) {
		return new Range<>(Bound.inclusive(from), Bound.exclusive(to));
	}

	/**
	 * Creates a right-unbounded {@link Range} (the right bound set to {@link Bound#unbounded()}) with the given left
	 * bound.
	 *
	 * @param <T>
	 * @param from the left {@link Bound}, must not be {@literal null}.
	 * @return
	 */
	public static <T> Range<T> rightUnbounded(Bound<T> from) {
		return new Range<>(from, Bound.unbounded());
	}

	/**
	 * Returns an unbounded {@link Range}.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Range<T> unbounded() {
		return (Range<T>) UNBOUNDED;
	}

	private Range(Bound<T> lowerBound, Bound<T> upperBound) {

		Assert.notNull(lowerBound, "Lower bound must not be null!");
		Assert.notNull(upperBound, "Upper bound must not be null!");

		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	/**
	 * Returns whether the {@link Range} contains the given value.
	 *
	 * @param value must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean contains(T value) {

		Assert.notNull(value, "Reference value must not be null!");
		Assert.isInstanceOf(Comparable.class, value, "value must implements Comparable!");

		boolean greaterThanLowerBound = lowerBound.getValue() //
				.map(it -> lowerBound.isInclusive() ? ((Comparable<? super T>) it).compareTo(value) <= 0
						: ((Comparable<? super T>) it).compareTo(value) < 0) //
				.orElse(true);

		boolean lessThanUpperBound = upperBound.getValue() //
				.map(it -> upperBound.isInclusive() ? ((Comparable<? super T>) it).compareTo(value) >= 0
						: ((Comparable<? super T>) it).compareTo(value) > 0) //
				.orElse(true);

		return greaterThanLowerBound && lessThanUpperBound;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Range)) {
			return false;
		}

		Range<?> range = (Range<?>) o;

		if (!ObjectUtils.nullSafeEquals(lowerBound, range.lowerBound)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(upperBound, range.upperBound);
	}

	public Range.Bound<T> getLowerBound() {
		return this.lowerBound;
	}

	public Range.Bound<T> getUpperBound() {
		return this.upperBound;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(lowerBound);
		result = 31 * result + ObjectUtils.nullSafeHashCode(upperBound);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s-%s", lowerBound.toPrefixString(), upperBound.toSuffixString());
	}

	/**
	 * Value object representing a boundary. A boundary can either be {@link #unbounded() unbounded}, {@link #inclusive(Object)} 
	 * including its value} or {@link #exclusive(Object)}  its value}.
	 */
	public static final class Bound<T> {

		@SuppressWarnings({ "rawtypes", "unchecked" }) //
		private static final Bound<?> UNBOUNDED = new Bound(Optional.empty(), true);

		@SuppressWarnings("OptionalUsedAsFieldOrParameterType") private final Optional<T> value;
		private final boolean inclusive;

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Double> exclusive(double value) {
			return exclusive((Double) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Float> exclusive(float value) {
			return exclusive((Float) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Integer> exclusive(int value) {
			return exclusive((Integer) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Long> exclusive(long value) {
			return exclusive((Long) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static <T> Bound<T> exclusive(T value) {

			Assert.notNull(value, "Value must not be null!");
			Assert.isInstanceOf(Comparable.class, value, "value must implements Comparable!");
			return new Bound<>(Optional.of(value), false);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Double> inclusive(double value) {
			return inclusive((Double) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Float> inclusive(float value) {
			return inclusive((Float) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Integer> inclusive(int value) {
			return inclusive((Integer) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Long> inclusive(long value) {
			return inclusive((Long) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static <T> Bound<T> inclusive(T value) {

			Assert.notNull(value, "Value must not be null!");
			Assert.isInstanceOf(Comparable.class, value, "value must implements Comparable!");
			return new Bound<>(Optional.of(value), true);
		}

		/**
		 * Creates an unbounded {@link Bound}.
		 */
		@SuppressWarnings("unchecked")
		public static <T> Bound<T> unbounded() {
			return (Bound<T>) UNBOUNDED;
		}

		@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
		private Bound(Optional<T> value, boolean inclusive) {
			this.value = value;
			this.inclusive = inclusive;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof Bound)) {
				return false;
			}

			Bound<?> bound = (Bound<?>) o;

			if (inclusive != bound.inclusive)
				return false;

			return ObjectUtils.nullSafeEquals(value, bound.value);
		}

		public Optional<T> getValue() {
			return this.value;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(value);
			result = 31 * result + (inclusive ? 1 : 0);
			return result;
		}

		/**
		 * Returns whether this boundary is bounded.
		 *
		 * @return
		 */
		public boolean isBounded() {
			return value.isPresent();
		}

		public boolean isInclusive() {
			return this.inclusive;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return value.map(Object::toString).orElse("unbounded");
		}

		String toPrefixString() {

			return getValue() //
					.map(Object::toString) //
					.map(it -> isInclusive() ? "[".concat(it) : "(".concat(it)) //
					.orElse("unbounded");
		}

		String toSuffixString() {

			return getValue() //
					.map(Object::toString) //
					.map(it -> isInclusive() ? it.concat("]") : it.concat(")")) //
					.orElse("unbounded");
		}
	}
}
