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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.json.JsonpMapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.client.ResponseException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.VersionConflictException;

/**
 * Simple {@link PersistenceExceptionTranslator} for Elasticsearch. Convert the given runtime exception to an
 * appropriate exception from the {@code org.springframework.dao} hierarchy. Return {@literal null} if no translation is
 * appropriate: any other exception may have resulted from user code, and should not be translated.
 *
 * @author Peter-Josef Meisch
 * @author Junghoon Ban
 * @since 4.4
 */
public class ElasticsearchExceptionTranslator implements PersistenceExceptionTranslator {

	private final JsonpMapper jsonpMapper;

	public ElasticsearchExceptionTranslator(JsonpMapper jsonpMapper) {
		this.jsonpMapper = jsonpMapper;
	}

	/**
	 * translates an Exception if possible. Exceptions that are no {@link RuntimeException}s are wrapped in a
	 * RuntimeException
	 *
	 * @param throwable the Exception to map
	 * @return the potentially translated RuntimeException.
	 */
	public RuntimeException translateException(Throwable throwable) {

		RuntimeException runtimeException = throwable instanceof RuntimeException ex ? ex
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = translateExceptionIfPossible(runtimeException);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

		checkForConflictException(ex);

		if (ex instanceof ElasticsearchException elasticsearchException) {

			ErrorResponse response = elasticsearchException.response();
			var errorType = response.error().type();
			var errorReason = response.error().reason() != null ? response.error().reason() : "undefined reason";

			if (response.status() == 404) {

				if ("index_not_found_exception".equals(errorType)) {
					// noinspection RegExpRedundantEscape
					Pattern pattern = Pattern.compile(".*no such index \\[(.*)\\]");
					String index = "";
					Matcher matcher = pattern.matcher(errorReason);
					if (matcher.matches()) {
						index = matcher.group(1);
					}
					return new NoSuchIndexException(index);
				}

				return new ResourceNotFoundException(errorReason);
			}

			if (response.status() == 409) {

			}
			String body = JsonUtils.toJson(response, jsonpMapper);

			if (errorType != null && errorType.contains("validation_exception")) {
				return new DataIntegrityViolationException(errorReason);
			}

			return new UncategorizedElasticsearchException(ex.getMessage(), response.status(), body, ex);
		}

		Throwable cause = ex.getCause();
		if (cause instanceof IOException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}

		return null;
	}

	private void checkForConflictException(Throwable exception) {
		Integer status = null;
		String message = null;

		if (exception instanceof ResponseException responseException) {
			status = responseException.getResponse().getStatusLine().getStatusCode();
			message = responseException.getMessage();
		} else if (exception.getCause() != null) {
			checkForConflictException(exception.getCause());
		}

		if (status != null && message != null) {
			if (status == 409 && message.contains("type\":\"version_conflict_engine_exception"))
				if (message.contains("version conflict, required seqNo")) {
					throw new OptimisticLockingFailureException("Cannot index a document due to seq_no+primary_term conflict",
							exception);
				} else if (message.contains("version conflict, current version [")) {
					throw new VersionConflictException("Version conflict", exception);
				}
		}
	}
}
