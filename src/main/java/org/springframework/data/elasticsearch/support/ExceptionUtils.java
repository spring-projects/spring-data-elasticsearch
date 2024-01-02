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

import java.net.ConnectException;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public final class ExceptionUtils {
	private ExceptionUtils() {}

	/**
	 * checks if the given throwable is a {@link ConnectException} or has one in its cause chain
	 *
	 * @param throwable the throwable to check
	 * @return true if throwable is caused by a {@link ConnectException}
	 */
	public static boolean isCausedByConnectionException(Throwable throwable) {

		Throwable t = throwable;
		do {

			if (t instanceof ConnectException) {
				return true;
			}

			t = t.getCause();
		} while (t != null);

		return false;
	}

}
