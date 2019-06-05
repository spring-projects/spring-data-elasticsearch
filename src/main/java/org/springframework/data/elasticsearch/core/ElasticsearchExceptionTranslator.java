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

package org.springframework.data.elasticsearch.core;

import java.net.ConnectException;
import java.util.List;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @since 3.2
 */
public class ElasticsearchExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

		if (ex instanceof ElasticsearchException) {

			ElasticsearchException elasticsearchException = (ElasticsearchException) ex;

			if (!indexAvailable(elasticsearchException)) {
				return new NoSuchIndexException(ObjectUtils.nullSafeToString(elasticsearchException.getMetadata("es.index")),
						ex);
			}
		}

		if (ex.getCause() instanceof ConnectException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}

		return null;
	}

	private boolean indexAvailable(ElasticsearchException ex) {

		List<String> metadata = ex.getMetadata("es.index_uuid");
		if (metadata == null) {
			if (ex instanceof ElasticsearchStatusException) {
				return StringUtils.hasText(ObjectUtils.nullSafeToString(((ElasticsearchStatusException) ex).getIndex()));
			}
		}
		return !CollectionUtils.contains(metadata.iterator(), "_na_");
	}
}
