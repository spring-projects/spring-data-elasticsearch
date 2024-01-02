/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to provide sequential IDs. Uses an integer, 2^31 -1 values should be enough for the test runs.
 *
 * @author Peter-Josef Meisch
 */
public final class IdGenerator {

	private static final AtomicInteger NEXT = new AtomicInteger();

	private IdGenerator() {}

	public static int nextIdAsInt() {
		return NEXT.incrementAndGet();
	}

	public static double nextIdAsDouble() {
		return NEXT.incrementAndGet();
	}

	public static String nextIdAsString() {
		return "" + nextIdAsInt();
	}
}
