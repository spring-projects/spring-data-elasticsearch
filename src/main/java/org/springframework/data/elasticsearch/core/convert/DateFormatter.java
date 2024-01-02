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
package org.springframework.data.elasticsearch.core.convert;

import java.time.temporal.TemporalAccessor;

/**
 * Interface to convert from and to {@link TemporalAccessor}s.
 * 
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public interface DateFormatter {
	/**
	 * Formats a {@link TemporalAccessor} into a String.
	 * 
	 * @param accessor must not be {@literal null}
	 * @return the formatted String
	 */
	String format(TemporalAccessor accessor);

	/**
	 * Parses a String into a {@link TemporalAccessor}.
	 * 
	 * @param input the String to parse, must not be {@literal null}
	 * @param type the class of T
	 * @param <T> the {@link TemporalAccessor} implementation
	 * @return the parsed instance
	 */
	<T extends TemporalAccessor> T parse(String input, Class<T> type);
}
