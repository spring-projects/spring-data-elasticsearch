/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.convert;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

/**
 * MappingElasticsearchConverter
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 */
public class MappingElasticsearchConverter implements ElasticsearchConverter, ApplicationContextAware {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;

	@SuppressWarnings("unused")
	private ApplicationContext applicationContext;

	public MappingElasticsearchConverter(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		
		Assert.notNull(mappingContext, "MappingContext must not be null!");
		
		this.mappingContext = mappingContext;
		this.conversionService = new DefaultConversionService();
	}

	@Override
	public MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		if (mappingContext instanceof ApplicationContextAware) {
			((ApplicationContextAware) mappingContext).setApplicationContext(applicationContext);
		}
	}
}
