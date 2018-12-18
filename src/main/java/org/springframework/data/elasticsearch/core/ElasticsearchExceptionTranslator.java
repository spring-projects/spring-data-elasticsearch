/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.elasticsearch.core;

import java.net.ConnectException;

import org.elasticsearch.ElasticsearchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.util.CollectionUtils;

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
				return new NoSuchIndexException(elasticsearchException.getMetadata("es.index").toString(), ex);
			}
		}

		if (ex.getCause() instanceof ConnectException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}

		return null;
	}

	private boolean indexAvailable(ElasticsearchException ex) {
		return !CollectionUtils.contains(ex.getMetadata("es.index_uuid").iterator(), "_na_");
	}
}
