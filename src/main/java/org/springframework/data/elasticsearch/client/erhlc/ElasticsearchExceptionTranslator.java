/*
 * Copyright 2018-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.common.ValidationException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.RestStatusException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Simple {@link PersistenceExceptionTranslator} for Elasticsearch. Convert the given runtime exception to an
 * appropriate exception from the {@code org.springframework.dao} hierarchy. Return {@literal null} if no translation is
 * appropriate: any other exception may have resulted from user code, and should not be translated.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @author Mark Paluch
 * @since 3.2
 * @deprecated since 5.0
 */
@Deprecated
public class ElasticsearchExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

		if (isSeqNoConflict(ex)) {
			return new OptimisticLockingFailureException("Cannot index a document due to seq_no+primary_term conflict", ex);
		}

		if (ex instanceof ElasticsearchException) {

			ElasticsearchException elasticsearchException = (ElasticsearchException) ex;

			if (!indexAvailable(elasticsearchException)) {
				return new NoSuchIndexException(ObjectUtils.nullSafeToString(elasticsearchException.getMetadata("es.index")),
						ex);
			}

			if (elasticsearchException instanceof ElasticsearchStatusException) {
				ElasticsearchStatusException elasticsearchStatusException = (ElasticsearchStatusException) elasticsearchException;
				return new RestStatusException(elasticsearchStatusException.status().getStatus(),
						elasticsearchStatusException.getMessage(), elasticsearchStatusException);
			}

			return new UncategorizedElasticsearchException(ex.getMessage(), ex);
		}

		if (ex instanceof RestStatusException) {
			RestStatusException restStatusException = (RestStatusException) ex;
			Throwable cause = restStatusException.getCause();
			if (cause instanceof ElasticsearchException) {
				ElasticsearchException elasticsearchException = (ElasticsearchException) cause;

				if (!indexAvailable(elasticsearchException)) {
					return new NoSuchIndexException(ObjectUtils.nullSafeToString(elasticsearchException.getMetadata("es.index")),
							ex);
				}
			}
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

	private boolean isSeqNoConflict(Exception exception) {

		Integer status = null;
		String message = null;

		if (exception instanceof ElasticsearchStatusException) {

			ElasticsearchStatusException statusException = (ElasticsearchStatusException) exception;
			status = statusException.status().getStatus();
			message = statusException.getMessage();
		}

		if (exception instanceof RestStatusException) {

			RestStatusException statusException = (RestStatusException) exception;
			status = statusException.getStatus();
			message = statusException.getMessage();
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

	private boolean indexAvailable(ElasticsearchException ex) {

		List<String> metadata = ex.getMetadata("es.index_uuid");
		if (metadata == null) {

			if (ex.getCause() instanceof ElasticsearchException) {
				return indexAvailable((ElasticsearchException) ex.getCause());
			}

			if (ex instanceof ElasticsearchStatusException) {
				return StringUtils.hasText(ObjectUtils.nullSafeToString(ex.getIndex()));
			}
			return true;
		}
		return !CollectionUtils.contains(metadata.iterator(), "_na_");
	}
}
