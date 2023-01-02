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
package org.springframework.data.elasticsearch.core.query;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * Extends the {@link Sort.Order} with properties that can be set on Elasticsearch order options.
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class Order extends Sort.Order {

	public static final Mode DEFAULT_MODE = Mode.min;
	public static final Sort.NullHandling DEFAULT_NULL_HANDLING = Sort.NullHandling.NATIVE;

	protected final Mode mode;
	@Nullable protected final String unmappedType;

	public Order(Sort.Direction direction, String property) {
		this(direction, property, DEFAULT_MODE, null);
	}

	public Order(Sort.Direction direction, String property, Mode mode) {
		this(direction, property, DEFAULT_NULL_HANDLING, mode, null);
	}

	public Order(Sort.Direction direction, String property, @Nullable String unmappedType) {
		this(direction, property, DEFAULT_NULL_HANDLING, DEFAULT_MODE, unmappedType);
	}

	public Order(Sort.Direction direction, String property, Mode mode, @Nullable String unmappedType) {
		this(direction, property, DEFAULT_NULL_HANDLING, mode, unmappedType);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint) {
		this(direction, property, nullHandlingHint, DEFAULT_MODE, null);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint, Mode mode) {
		this(direction, property, nullHandlingHint, mode, null);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint,
			@Nullable String unmappedType) {
		this(direction, property, nullHandlingHint, DEFAULT_MODE, unmappedType);
	}

	public Order(Sort.Direction direction, String property, Sort.NullHandling nullHandlingHint, Mode mode,
			@Nullable String unmappedType) {
		super(direction, property, nullHandlingHint);
		this.mode = mode;
		this.unmappedType = unmappedType;
	}

	@Nullable
	public String getUnmappedType() {
		return unmappedType;
	}

	@Override
	public Sort.Order with(Sort.Direction direction) {
		return new Order(direction, getProperty(), getNullHandling(), mode, unmappedType);
	}

	@Override
	public Sort.Order withProperty(String property) {
		return new Order(getDirection(), property, getNullHandling(), mode, unmappedType);
	}

	@Override
	public Sort.Order with(Sort.NullHandling nullHandling) {
		return new Order(getDirection(), getProperty(), nullHandling, getMode(), unmappedType);
	}

	public Order withUnmappedType(@Nullable String unmappedType) {
		return new Order(getDirection(), getProperty(), getNullHandling(), getMode(), unmappedType);
	}

	public Order with(Mode mode) {
		return new Order(getDirection(), getProperty(), getNullHandling(), mode, unmappedType);
	}

	public Mode getMode() {
		return mode;
	}

	public enum Mode {
		min, max, median, avg
	}

}
