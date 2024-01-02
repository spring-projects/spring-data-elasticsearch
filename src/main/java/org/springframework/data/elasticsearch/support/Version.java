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
package org.springframework.data.elasticsearch.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * A version defined by 3 parts: major minor and revision number.
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public record Version(int major, int minor, int revision) {

	private static final Pattern PATTERN = Pattern.compile("^(\\d+)(\\.(\\d+))?(\\.(\\d+))?.*$");

	@Override
	public String toString() {
		return major + "." + minor + '.' + revision;
	}

	/**
	 * Creates a version from a String that matches {@link #PATTERN}; major, minor and revision numbers separated by dots with optional trailing characters. A missing revision is treated as 0.
	 *
	 * @param s the String to parse
	 * @return the Version
	 * @throws IllegalArgumentException if the input is null or cannot be parsed.
	 */
	public static Version fromString(String s) {

		Assert.notNull(s, "s must not be null");

		Matcher matcher = PATTERN.matcher(s);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("invalid input pattern");
		}

		int major = Integer.parseInt(matcher.group(1));
		int minor = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
		int revision = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : 0;

		return new Version(major, minor, revision);
	}
}
