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
package org.springframework.data.elasticsearch.client.orhlc;

import org.opensearch.index.engine.VersionConflictEngineException;
import org.opensearch.common.ValidationException;
import org.opensearch.OpenSearchStatusException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opensearch.client.ResponseException;
import org.opensearch.rest.RestStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;

/**
 * Simple {@link PersistenceExceptionTranslator} for Opensearch. Convert the given runtime exception to an
 * appropriate exception from the {@code org.springframework.dao} hierarchy. Return {@literal null} if no translation is
 * appropriate: any other exception may have resulted from user code, and should not be translated.
 *
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 * @since 5.0
 */
public class OpensearchExceptionTranslator implements PersistenceExceptionTranslator {
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

		if (ex instanceof OpenSearchStatusException) {
		    OpenSearchStatusException statusException = (OpenSearchStatusException) ex;

			if (statusException.status() == RestStatus.NOT_FOUND
					&& statusException.getMessage().contains("index_not_found_exception")) {

				Pattern pattern = Pattern.compile(".*no such index \\[(.*)\\]");
				String index = "";
				Matcher matcher = pattern.matcher(statusException.getMessage());
				if (matcher.matches()) {
					index = matcher.group(1);
				}
				return new NoSuchIndexException(index);
			}
			
			if (statusException.getMessage().contains("validation_exception")) {
				return new DataIntegrityViolationException(statusException.getMessage());
			}

			return new UncategorizedElasticsearchException(ex.getMessage(), statusException.status().getStatus(), null, ex);
		}

		if (ex instanceof ValidationException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		
		Throwable cause = ex.getCause();
		if (cause instanceof IOException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}

		return null;
	}

	private boolean isSeqNoConflict(Throwable exception) {
		Integer status = null;
		String message = null;


		if (exception instanceof ResponseException) {
			ResponseException responseException = (ResponseException) exception;
			status = responseException.getResponse().getStatusLine().getStatusCode();
			message = responseException.getMessage();
		} else if (exception instanceof OpenSearchStatusException) {
			OpenSearchStatusException statusException = (OpenSearchStatusException) exception;
			status = statusException.status().getStatus();
			message = statusException.getMessage();
		} else if (exception.getCause() != null) {
			return isSeqNoConflict(exception.getCause());
		}

		if (status != null && message != null) {
			return status == 409 && message.contains("type=version_conflict_engine_exception")
					&& message.contains("version conflict, required seqNo");
		}
		
		if (exception instanceof VersionConflictEngineException) {

			VersionConflictEngineException versionConflictEngineException = (VersionConflictEngineException) exception;

			return versionConflictEngineException.getMessage() != null
					&& versionConflictEngineException.getMessage().contains("version conflict, required seqNo");
		}

		return false;
	}
}
