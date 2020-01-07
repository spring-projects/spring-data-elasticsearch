/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.elasticsearch;

import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ElasticsearchEntityMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;

/**
 * configuration class for the classic ElasticsearchTemplate. Needs a {@link TestNodeResource} bean that should be set up in
 * the test as ClassRule and exported as bean.
 *
 * @author Peter-Josef Meisch
 */
@Configuration
public class ElasticsearchTestConfiguration extends ElasticsearchConfigurationSupport {

	@Autowired private TestNodeResource testNodeResource;

	@Bean
	public Client elasticsearchClient() {
		return testNodeResource.client();
	}

	@Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
	public ElasticsearchTemplate elasticsearchTemplate(Client elasticsearchClient, EntityMapper entityMapper) {
		return new ElasticsearchTemplate(elasticsearchClient, entityMapper);
	}

	/*
	 * need the ElasticsearchMapper, because some tests rely on @Field(name) being handled correctly
	 */
	@Bean
	@Override
	public EntityMapper entityMapper() {
		ElasticsearchEntityMapper entityMapper = new ElasticsearchEntityMapper(elasticsearchMappingContext(),
				new DefaultConversionService());
		entityMapper.setConversions(elasticsearchCustomConversions());

		return entityMapper;
	}
}
