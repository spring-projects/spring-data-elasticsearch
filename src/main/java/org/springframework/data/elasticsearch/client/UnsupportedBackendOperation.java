/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client;

/**
 * Exception to be thrown by a backend implementation on operations that are not supported for that backend.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class UnsupportedBackendOperation extends RuntimeException {
	public UnsupportedBackendOperation() {}

	public UnsupportedBackendOperation(String message) {
		super(message);
	}

	public UnsupportedBackendOperation(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedBackendOperation(Throwable cause) {
		super(cause);
	}

	public UnsupportedBackendOperation(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
