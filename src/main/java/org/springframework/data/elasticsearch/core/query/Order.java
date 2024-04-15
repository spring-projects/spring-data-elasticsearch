/*
 * Copyright 2021-2024 the original author or authors.
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

import java.util.function.Function;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Extends the {@link Sort.Order} with properties that can be set on Elasticsearch order options.
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class Order extends Sort.Order {

	public static final Sort.NullHandling DEFAULT_NULL_HANDLING = Sort.NullHandling.NATIVE;

	@Nullable protected final Mode mode;
	@Nullable protected final String unmappedType;

	@Nullable protected final String missing;

	@Nullable protected final Nested nested;

	public Order(Sort.Direction direction, String property) {
		this(direction, property, (Mode) null, null);
	}

	public Order(Sort.Direction direction, String property, @Nullable Mode mode) {
		this(direction, property, DEFAULT_NULL_HANDLING, mode, null);
	}

	public Order(Sort.Direction direction, String property, @Nullable String unmappedType) {
		this(direction, property, DEFAULT_NULL_HANDLING, null, unmappedType);
	}

	public Order(Sort.Direction direction, String property, @Nullable Mode mode, @Nullable String unmappedType) {
		this(direction, property, DEFAULT_NULL_HANDLING, mode, unmappedType);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint) {
		this(direction, property, nullHandlingHint, null, null);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint, @Nullable Mode mode) {
		this(direction, property, nullHandlingHint, mode, null);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint,
			@Nullable String unmappedType) {
		this(direction, property, nullHandlingHint, null, unmappedType);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint, @Nullable Mode mode,
			@Nullable String unmappedType) {
		this(direction, property, nullHandlingHint, mode, unmappedType, null);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint, @Nullable Mode mode,
			@Nullable String unmappedType, @Nullable String missing) {
		this(direction, property, nullHandlingHint, mode, unmappedType, missing, null);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint, @Nullable Mode mode,
			@Nullable String unmappedType, @Nullable String missing, @Nullable Nested nested) {
		super(direction, property, nullHandlingHint);
		this.mode = mode;
		this.unmappedType = unmappedType;
		this.missing = missing;
		this.nested = nested;
	}

	@Nullable
	public String getUnmappedType() {
		return unmappedType;
	}

	@Override
	public Sort.Order with(Sort.Direction direction) {
		return new Order(direction, getProperty(), getNullHandling(), mode, unmappedType, missing, nested);
	}

	@Override
	public Sort.Order withProperty(String property) {
		return new Order(getDirection(), property, getNullHandling(), mode, unmappedType, missing, nested);
	}

	@Override
	public Sort.Order with(Sort.NullHandling nullHandling) {
		return new Order(getDirection(), getProperty(), nullHandling, getMode(), unmappedType, missing, nested);
	}

	public Order withUnmappedType(@Nullable String unmappedType) {
		return new Order(getDirection(), getProperty(), getNullHandling(), getMode(), unmappedType, missing, nested);
	}

	public Order with(@Nullable Mode mode) {
		return new Order(getDirection(), getProperty(), getNullHandling(), mode, unmappedType, missing, nested);
	}

	public Order withMissing(@Nullable String missing) {
		return new Order(getDirection(), getProperty(), getNullHandling(), mode, unmappedType, missing, nested);
	}

	public Order withNested(@Nullable Nested nested) {
		return new Order(getDirection(), getProperty(), getNullHandling(), mode, unmappedType, missing, nested);
	}

	@Nullable
	public Mode getMode() {
		return mode;
	}

	@Nullable
	public String getMissing() {
		return missing;
	}

	@Nullable
	public Nested getNested() {
		return nested;
	}

	public enum Mode {
		min, max, median, avg
	}

	public static class Nested {
		private final String path;
		@Nullable private final Query filter;
		@Nullable private Integer maxChildren = null;
		@Nullable private final Nested nested;

		public static Nested of(String path, Function<Nested.Builder, Nested.Builder> builderFunction) {

			Assert.notNull(path, "path must not be null");
			Assert.notNull(builderFunction, "builderFunction must not be null");

			return builderFunction.apply(builder(path)).build();
		}

		public Nested(String path, @Nullable Query filter, @Nullable Integer maxChildren, @Nullable Nested nested) {

			Assert.notNull(path, "path must not be null");

			this.path = path;
			this.filter = filter;
			this.maxChildren = maxChildren;
			this.nested = nested;
		}

		public String getPath() {
			return path;
		}

		@Nullable
		public Query getFilter() {
			return filter;
		}

		@Nullable
		public Integer getMaxChildren() {
			return maxChildren;
		}

		@Nullable
		public Nested getNested() {
			return nested;
		}

		public static Builder builder(String path) {
			return new Builder(path);
		}

		public static class Builder {
			private final String path;
			@Nullable private Query filter = null;
			@Nullable private Integer maxChildren = null;
			@Nullable private Nested nested = null;

			public Builder(String path) {

				Assert.notNull(path, "path must not be null");

				this.path = path;
			}

			/**
			 * Sets the filter query for a nested sort.<br/>
			 * Note: This cannot be a {@link CriteriaQuery}, as that would be sent as a nested query within the filter, use a
			 * {@link org.springframework.data.elasticsearch.client.elc.NativeQuery} or {@link StringQuery} instead.
			 *
			 * @param filter the filter to set
			 * @return this builder
			 * @throws IllegalArgumentException when a {@link CriteriaQuery} is passed.
			 */
			public Builder withFilter(@Nullable Query filter) {

				if (filter instanceof CriteriaQuery) {
					throw new IllegalArgumentException("Cannot use a CriteriaQuery in a nested sort filter.");
				}
				this.filter = filter;
				return this;
			}

			public Builder withMaxChildren(@Nullable Integer maxChildren) {
				this.maxChildren = maxChildren;
				return this;
			}

			public Builder withNested(@Nullable Nested nested) {
				this.nested = nested;
				return this;
			}

			public Nested build() {
				return new Nested(path, filter, maxChildren, nested);
			}
		}
	}
}
