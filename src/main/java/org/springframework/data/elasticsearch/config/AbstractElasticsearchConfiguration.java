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
package org.springframework.data.elasticsearch.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 * @see ElasticsearchConfigurationSupport
 */
public abstract class AbstractElasticsearchConfiguration extends ElasticsearchConfigurationSupport {

	/**
	 * Return the {@link RestHighLevelClient} instance used to connect to the cluster. <br />
	 * Annotate with {@link Bean} in case you want to expose a {@link RestHighLevelClient} instance to the
	 * {@link org.springframework.context.ApplicationContext}.
	 *
	 * @return never {@literal null}.
	 */
	public abstract RestHighLevelClient elasticsearchClient();

	/**
	 * Creates {@link ElasticsearchOperations}.
	 *
	 * @return never {@literal null}.
	 */
	@Bean(name = {"elasticsearchOperations", "elasticsearchTemplate"})
	public ElasticsearchOperations elasticsearchOperations() {
		return new ElasticsearchRestTemplate(elasticsearchClient(), elasticsearchConverter(), resultsMapper());
	}
}
