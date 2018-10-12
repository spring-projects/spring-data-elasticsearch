/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Elasticsearch specific repository implementation. Likely to be used as target within
 * {@link ElasticsearchRepositoryFactory}
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ryan Henszey
 */
public class SimpleElasticsearchRepository<T> extends AbstractElasticsearchRepository<T, String> {

	public SimpleElasticsearchRepository() {
		super();
	}

	public SimpleElasticsearchRepository(ElasticsearchEntityInformation<T, String> metadata,
										 ElasticsearchOperations elasticsearchOperations) {
		super(metadata, elasticsearchOperations);
	}

	public SimpleElasticsearchRepository(ElasticsearchOperations elasticsearchOperations) {
		super(elasticsearchOperations);
	}

	@Override
	protected String stringIdRepresentation(String id) {
		return id;
	}
}
