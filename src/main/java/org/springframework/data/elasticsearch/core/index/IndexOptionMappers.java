/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.data.elasticsearch.core.index;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.annotations.CustomIndexOption;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import tools.jackson.databind.node.DecimalNode;

/**
 * The collection of predefined {@link IndexOptionMapper}s
 * 
 * @author Andriy Redko
 * 
 * @since 6.2
 */
public final class IndexOptionMappers {
	private IndexOptionMappers() {
	}

	/**
	 * Writes the {@link CustomIndexOption} instance into mapping as JSON string (or array of strings) property
	 */
	public static final class StringMapper implements IndexOptionMapper {
	@Override
		public void writeIndexOptionTo(CustomIndexOption indexOption, ObjectNode objectNode) {
			final String[] values = indexOption.values();
			if (values.length == 1) {
				writePropertyAsString(indexOption.name(), indexOption.overrideIfPresent(), values[0], objectNode);
			} else if (values.length > 1) {
				writePropertyAsArray(indexOption.name(), indexOption.overrideIfPresent(), values, objectNode);
			} else {
				removeProperty(indexOption.name(), objectNode);
			}
		}
	}

	/**
	 * Writes the {@link CustomIndexOption} instance into mapping as JSON number (or array of numbers) property
	 */
	public static final class NumberMapper implements IndexOptionMapper {
		@Override
		public void writeIndexOptionTo(CustomIndexOption indexOption, ObjectNode objectNode) {
			final @Nullable Number[] values = toNumbers(indexOption.values());
			if (values.length == 1) {
				writePropertyAsNumber(indexOption.name(), indexOption.overrideIfPresent(), values[0], objectNode);
			} else if (values.length > 1) {
				writePropertyAsArray(indexOption.name(), indexOption.overrideIfPresent(), values, objectNode);
			} else {
				removeProperty(indexOption.name(), objectNode);
			}
		}
	}

	/**
	 * Writes the {@link CustomIndexOption} instance into mapping as JSON boolean (or array of booleans) property
	 */
	public static final class BooleanMapper implements IndexOptionMapper {
		@Override
		public void writeIndexOptionTo(CustomIndexOption indexOption, ObjectNode objectNode) {
			final Boolean[] values = toBoolean(indexOption.values());
			if (values.length == 1) {
				writePropertyAsBoolean(indexOption.name(), indexOption.overrideIfPresent(), values[0], objectNode);
			} else if (values.length > 1) {
				writePropertyAsArray(indexOption.name(), indexOption.overrideIfPresent(), values, objectNode);
			} else {
				removeProperty(indexOption.name(), objectNode);
			}
		}
	}

	/**
	 * Convert the array of strings to array of booleans.
	 * 
	 * @param values array of strings
	 * @return array of booleans
	 */
	private static @Nullable Boolean[] toBoolean(@Nullable String[] values) {
		return Arrays.stream(values).map(Boolean::valueOf).toArray(Boolean[]::new);
	}

	/**
	 * Convert the array of strings to array of numbers, throwing {@link NumberFormatException} if the conversion 
	 * is not possible.
	 * 
	 * @param values array of strings
	 * @return array of numbers
	 * 
	 * @throws NumberFormatException
	 */
	private static @Nullable Number[] toNumbers(@Nullable String[] values) {
		final Number[] numbers = new Number[values.length];
		for (int j = 0; j < values.length; ++j) {
			if (values[j] == null) {
				numbers[j] = null;
			} else if(values[j].isBlank()) {
				throw new NumberFormatException("Unable to convert empty/blank string to number");
			} else {
				final String s = values[j].trim();

				boolean isInteger = true;
				for (int i = 0; i < s.length(); i++) {
					if (i == 0 && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
						if (s.length() == 1) {
							throw new NumberFormatException("Unable to convert string  '" + s + "' to number");
						} else {
							continue;
						}
					}

					if (Character.digit(s.charAt(i), 10) < 0) {
						isInteger = false;
						break;
					}
				}

				if (isInteger) {
					numbers[j] = new BigInteger(s);
				} else {
					numbers[j] = new BigDecimal(s);
				}
			}
		}

		return numbers;
	}

	/**
	 * Writes a property as a JSON boolean value.
	 * 
	 * @param name property name
	 * @param override override if present
	 * @param value property value
	 * @param objectNode JSON object node
	 */
	private static void writePropertyAsBoolean(String name, boolean override, Boolean value, ObjectNode objectNode) {
		final boolean exists = objectNode.has(name);
		final JsonNode node = BooleanNode.valueOf(value);
		if (exists && override) {
			objectNode.replace(name, node);
		} else {
			objectNode.putIfAbsent(name, node);
		}
	}

	/**
	 * Writes a property as a JSON number value.
	 * 
	 * @param name property name
	 * @param override override if present
	 * @param value property value
	 * @param objectNode JSON object node
	 */
	private static void writePropertyAsNumber(String name, boolean override, Number value, ObjectNode objectNode) {
		final boolean exists = objectNode.has(name);
		final JsonNode node = toJsonNode(value);
		if (exists && override) {
			objectNode.replace(name, node);
		} else {
			objectNode.putIfAbsent(name, node);
		}
	}

	/**
	 * Writes a property as a JSON string value.
	 * 
	 * @param name property name
	 * @param override override if present
	 * @param value property value
	 * @param objectNode JSON object node
	 */
	private static void writePropertyAsString(String name, boolean override, String value, ObjectNode objectNode) {
		final boolean exists = objectNode.has(name);
		final StringNode node = StringNode.valueOf(value);
		if (exists && override) {
			objectNode.replace(name, node);
		} else {
			objectNode.putIfAbsent(name, node);
		}
	}

	/**
	 * Removes the property if present.
	 * 
	 * @param name property name
	 * @param objectNode JSON object node
	 */
	private static void removeProperty(String name, ObjectNode objectNode) {
		if (objectNode.has(name)) {
			objectNode.remove(name);
		}
	}

	/**
	 * Writes a property as a JSON array of boolean values.
	 * 
	 * @param name property name
	 * @param override override if present
	 * @param values property values
	 * @param objectNode JSON object node
	 */
	private static void writePropertyAsArray(String name, boolean override, Boolean[] values, ObjectNode objectNode) {
		writePropertyAsArray(
			name, 
			override, 
			objectNode
				.arrayNode()
				.addAll(Arrays.stream(values).map(BooleanNode::valueOf).toList()), 
			objectNode);
	}

	/**
	 * Writes a property as a JSON array of number values.
	 * 
	 * @param name property name
	 * @param override override if present
	 * @param values property values
	 * @param objectNode JSON object node
	 */
	private static void writePropertyAsArray(String name, boolean override, Number[] values, ObjectNode objectNode) {
		writePropertyAsArray(
			name, 
			override, 
			objectNode
				.arrayNode()
				.addAll(Arrays.stream(values).map(IndexOptionMappers::toJsonNode).toList()), 
			objectNode);
	}

	/**
	 * Writes a property as a JSON array of string values.
	 * 
	 * @param name property name
	 * @param override override if present
	 * @param values property values
	 * @param objectNode JSON object node
	 */
	private static void writePropertyAsArray(String name, boolean override, String[] values, ObjectNode objectNode) {
		writePropertyAsArray(
			name, 
			override, 
			objectNode
				.arrayNode()
				.addAll(Arrays.stream(values).map(StringNode::valueOf).toList()), 
			objectNode);
	}

	/**
	 * Writes a property as a JSON array.
	 * 
	 * @param name property name
	 * @param override override if present
	 * @param arrayNode JSON array
	 * @param objectNode JSON object node
	 */
	private static void writePropertyAsArray(String name, boolean override, ArrayNode arrayNode, ObjectNode objectNode) {
		final boolean exists = objectNode.has(name);
		if (exists && override) {
			objectNode.replace(name, arrayNode);
		} else {
			objectNode.putIfAbsent(name, arrayNode);
		}
	}

	/**
	 * Converts a number into appropriate JSON node.
	 * Only {@link DecimalNode} and {@link BigInteger} types are supported.
	 * 
	 * @param value {@link Number} instance to convert
	 * @throws IllegalArgumentException
	 */
	private static JsonNode toJsonNode(Number value) {
		if (value instanceof BigDecimal d) {
			return DecimalNode.valueOf(d);
		} else if (value instanceof BigInteger i) {
			return BigIntegerNode.valueOf(i);
		} else {
			// We only support BigDecimal or BigInteger numeric values, should never happen since the
			// conversion is constrained within this class only.
			throw new IllegalArgumentException("Only BigDecimal or BigInteger numbers are supported");
		}
	}
}
