/*
 * Copyright 2018-2023 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Logging Utility to log client requests and responses. Logs client requests and responses to Elasticsearch to a
 * dedicated logger: {@code org.springframework.data.elasticsearch.client.WIRE} on trace level.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Oliver Drotbohm
 * @since 3.2
 * @deprecated since 5.0, Elasticsearch's RestClient has a trace level logging available.
 */
@Deprecated
public abstract class ClientLogger {

	private static final Log WIRE_LOGGER = LogFactory.getLog("org.springframework.data.elasticsearch.client.WIRE");

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
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param method HTTP method
	 * @param endpoint URI
	 * @param parameters optional parameters.
	 */
	public static void logRequest(String logId, String method, String endpoint, Object parameters) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Sending request %s %s with parameters: %s", logId, method.toUpperCase(),
					endpoint, parameters));
		}
	}

	/**
	 * Log an outgoing HTTP request.
	 *
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param method HTTP method
	 * @param endpoint URI
	 * @param parameters optional parameters.
	 * @param headers a String containing the headers
	 * @since 4.4
	 */
	public static void logRequest(String logId, String method, String endpoint, Object parameters, String headers) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Sending request%n%s %s%nParameters: %s%nHeaders: %s", logId,
					method.toUpperCase(), endpoint, parameters, headers));
		}
	}

	/**
	 * Log an outgoing HTTP request with a request body.
	 *
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param method HTTP method
	 * @param endpoint URI
	 * @param parameters optional parameters.
	 * @param body body content supplier.
	 */
	public static void logRequest(String logId, String method, String endpoint, Object parameters,
			Supplier<Object> body) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Sending request %s %s with parameters: %s%nRequest body: %s", logId,
					method.toUpperCase(), endpoint, parameters, body.get()));
		}
	}

	/**
	 * Log an outgoing HTTP request with a request body.
	 *
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param method HTTP method
	 * @param endpoint URI
	 * @param parameters optional parameters.
	 * @param headers a String containing the headers
	 * @param body body content supplier.
	 * @since 4.4
	 */
	public static void logRequest(String logId, String method, String endpoint, Object parameters, String headers,
			Supplier<Object> body) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Sending request%n%s %s%nParameters: %s%nHeaders: %s%nRequest body: %s",
					logId, method.toUpperCase(), endpoint, parameters, headers, body.get()));
		}
	}

	/**
	 * Log a raw HTTP response without logging the body.
	 *
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param statusCode the HTTP status code.
	 */
	public static void logRawResponse(String logId, @Nullable Integer statusCode) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Received raw response: %d", logId, statusCode));
		}
	}

	/**
	 * Log a raw HTTP response without logging the body.
	 *
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param statusCode the HTTP status code.
	 * @param headers a String containing the headers
	 */
	public static void logRawResponse(String logId, @Nullable Integer statusCode, String headers) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Received response: %d%n%s", logId, statusCode, headers));
		}
	}

	/**
	 * Log a raw HTTP response along with the body.
	 *
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param statusCode the HTTP status code.
	 * @param body body content.
	 */
	public static void logResponse(String logId, Integer statusCode, String body) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Received response: %d%nResponse body: %s", logId, statusCode, body));
		}
	}

	/**
	 * Log a raw HTTP response along with the body.
	 *
	 * @param logId the correlation id, see {@link #newLogId()}.
	 * @param statusCode the HTTP status code.
	 * @param headers a String containing the headers
	 * @param body body content.
	 * @since 4.4
	 */
	public static void logResponse(String logId, @Nullable Integer statusCode, String headers, String body) {

		if (isEnabled()) {
			WIRE_LOGGER.trace(String.format("[%s] Received response: %d%nHeaders: %s%nResponse body: %s", logId, statusCode,
					headers, body));
		}
	}

	/**
	 * Creates a new, unique correlation id to improve tracing across log events.
	 *
	 * @return a new, unique correlation id.
	 */
	public static String newLogId() {

		if (!isEnabled()) {
			return "-";
		}

		return ObjectUtils.getIdentityHexString(new Object());
	}
}
