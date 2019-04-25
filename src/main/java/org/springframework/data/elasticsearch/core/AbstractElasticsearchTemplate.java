/*
 * Copyright 2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * AbstractElasticsearchTemplate
 *
 * @author Sascha Woo
 */
public abstract class AbstractElasticsearchTemplate implements ElasticsearchOperations, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticsearchTemplate.class);

	protected final EntityOperations operations;
	protected final ElasticsearchConverter elasticsearchConverter;

	public AbstractElasticsearchTemplate(ElasticsearchConverter elasticsearchConverter) {

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null!");
		this.elasticsearchConverter = elasticsearchConverter;
		this.operations = new EntityOperations(elasticsearchConverter.getMappingContext());
	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return elasticsearchConverter;
	}

	@Override
	public ElasticsearchPersistentEntity<?> getPersistentEntityFor(Class<?> type) {

		Assert.notNull(type, "type must not be null!");
		Assert.isTrue(type.isAnnotationPresent(Document.class), "Unable to identify index name. " + type.getSimpleName()
				+ " is not a Document. Make sure the document class is annotated with @Document(indexName=\"foo\")");

		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(type);
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (elasticsearchConverter instanceof ApplicationContextAware) {
			((ApplicationContextAware) elasticsearchConverter).setApplicationContext(context);
		}
	}

	protected String buildMapping(Class<?> clazz) {

		// load mapping specified in Mapping annotation if present
		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();
			if (!StringUtils.isEmpty(mappingPath)) {
				String mappings = ResourceUtil.readFileFromClasspath(mappingPath);
				if (!StringUtils.isEmpty(mappings)) {
					return mappings;
				}
			} else {
				LOGGER.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
			}
		}

		// build mapping from field annotations
		try {
			MappingBuilder mappingBuilder = new MappingBuilder(elasticsearchConverter);
			return mappingBuilder.buildPropertyMapping(clazz);
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}

	protected List<String> extractIds(SearchResponse response) {

		List<String> ids = new ArrayList<>();
		for (SearchHit hit : response.getHits()) {
			if (hit != null)
				ids.add(hit.getId());
		}
		return ids;
	}

}
