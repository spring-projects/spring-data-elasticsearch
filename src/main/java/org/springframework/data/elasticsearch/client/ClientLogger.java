/*
 * Copyright 2018-2019 the original author or authors.
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

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;

/**
 * Logging Utility to log client requests and responses. Logs client requests and responses to Elasticsearch to a
 * dedicated logger: {@code org.springframework.data.elasticsearch.client.WIRE} on {@link org.slf4j.event.Level#TRACE}
 * level.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.2
 */
public abstract class ClientLogger {

	private static final String lineSeparator = System.getProperty("line.separator");
	private static final Logger WIRE_LOGGER = LoggerFactory
			.getLogger("org.springframework.data.elasticsearch.client.WIRE");

	private ClientLogger() {}

	/**
	 * Returns {@literal true} if the logger is enabled.
	 *
	 * @return {@literal true} if the logger is enabled.
	 */
	public static boolean isEnabled() {
		return WIRE_LOGGER.isTraceEnabled();
	}

	/**
	 * Log an outgoing HTTP request.
	 *
	 * @param logId the correlation Id, see {@link #newLogId()}.
	 * @param method HTTP method
	 * @param endpoint URI
	 * @param parameters optional parameters.
	 */
	public static void logRequest(String logId, String method, String endpoint, Object parameters) {

		if (isEnabled()) {

			WIRE_LOGGER.trace("[{}] Sending request {} {} with parameters: {}", logId, method.toUpperCase(), endpoint,
					parameters);
		}
	}

	/**
	 * Log an outgoing HTTP request with a request body.
	 *
	 * @param logId the correlation Id, see {@link #newLogId()}.
	 * @param method HTTP method
	 * @param endpoint URI
	 * @param parameters optional parameters.
	 * @param body body content supplier.
	 */
	public static void logRequest(String logId, String method, String endpoint, Object parameters,
			Supplier<Object> body) {

		if (isEnabled()) {

			WIRE_LOGGER.trace("[{}] Sending request {} {} with parameters: {}{}Request body: {}", logId, method.toUpperCase(),
					endpoint, parameters, lineSeparator, body.get());
		}
	}

	/**
	 * Log a raw HTTP response without logging the body.
	 *
	 * @param logId the correlation Id, see {@link #newLogId()}.
	 * @param statusCode the HTTP status code.
	 */
	public static void logRawResponse(String logId, HttpStatus statusCode) {

		if (isEnabled()) {
			WIRE_LOGGER.trace("[{}] Received raw response: {}", logId, statusCode);
		}
	}

	/**
	 * Log a raw HTTP response along with the body.
	 *
	 * @param logId the correlation Id, see {@link #newLogId()}.
	 * @param statusCode the HTTP status code.
	 * @param body body content.
	 */
	public static void logResponse(String logId, HttpStatus statusCode, String body) {

		if (isEnabled()) {
			WIRE_LOGGER.trace("[{}] Received response: {}{}Response body: {}", logId, statusCode, lineSeparator, body);
		}
	}

	/**
	 * Creates a new, unique correlation Id to improve tracing across log events.
	 *
	 * @return a new, unique correlation Id.
	 */
	public static String newLogId() {

		if (!isEnabled()) {
			return "-";
		}

		return ObjectUtils.getIdentityHexString(new Object());
	}
}
