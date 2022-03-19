/*
 * Copyright 2021-2022 the original author or authors.
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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.elasticsearch.RestStatusException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;

/**
 * Simple {@link PersistenceExceptionTranslator} for Elasticsearch. Convert the given runtime exception to an
 * appropriate exception from the {@code org.springframework.dao} hierarchy. Return {@literal null} if no translation is
 * appropriate: any other exception may have resulted from user code, and should not be translated.
 *
 * @author Peter-Josef Meisch
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

		RuntimeException runtimeException = throwable instanceof RuntimeException ? (RuntimeException) throwable
				: new RuntimeException(throwable.getMessage(), throwable);
		RuntimeException potentiallyTranslatedException = translateExceptionIfPossible(runtimeException);

		return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

		if (isSeqNoConflict(ex)) {
			return new OptimisticLockingFailureException("Cannot index a document due to seq_no+primary_term conflict", ex);
		}

		// todo #1973 index unavailable?

		if (ex instanceof ElasticsearchException) {
			ElasticsearchException elasticsearchException = (ElasticsearchException) ex;

			ErrorResponse response = elasticsearchException.response();
			String body = JsonUtils.toJson(response, jsonpMapper);

			return new UncategorizedElasticsearchException(ex.getMessage(), response.status(), body, ex);
		}

		Throwable cause = ex.getCause();
		if (cause instanceof IOException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}

		return null;
	}

	private boolean isSeqNoConflict(Exception exception) {
		// todo #1973 check if this works
		Integer status = null;
		String message = null;

		if (exception instanceof RestStatusException) {

			RestStatusException statusException = (RestStatusException) exception;
			status = statusException.getStatus();
			message = statusException.getMessage();
		}

		if (status != null && message != null) {
			return status == 409 && message.contains("type=version_conflict_engine_exception")
					&& message.contains("version conflict, required seqNo");
		}

		return false;
	}
}
