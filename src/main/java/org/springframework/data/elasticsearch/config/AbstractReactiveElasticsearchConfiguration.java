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

import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 3.2
 * @see ElasticsearchConfigurationSupport
 */
@Configuration
public abstract class AbstractReactiveElasticsearchConfiguration extends ElasticsearchConfigurationSupport {

	/**
	 * Return the {@link ReactiveElasticsearchClient} instance used to connect to the cluster. <br />
	 * Annotate with {@link Bean} in case you want to expose a {@link ReactiveElasticsearchClient} instance to the
	 * {@link org.springframework.context.ApplicationContext}.
	 *
	 * @return never {@literal null}.
	 */
	public abstract ReactiveElasticsearchClient reactiveElasticsearchClient();

	/**
	 * Creates {@link ReactiveElasticsearchOperations}.
	 *
	 * @return never {@literal null}.
	 */
	@Bean
	public ReactiveElasticsearchOperations reactiveElasticsearchTemplate() {

		ReactiveElasticsearchTemplate template = new ReactiveElasticsearchTemplate(reactiveElasticsearchClient(),
				elasticsearchConverter(), resultsMapper());
		template.setIndicesOptions(indicesOptions());
		template.setRefreshPolicy(refreshPolicy());

		return template;
	}

	/**
	 * Set up the write {@link RefreshPolicy}. Default is set to {@link RefreshPolicy#IMMEDIATE}.
	 *
	 * @return {@literal null} to use the server defaults.
	 */
	@Nullable
	protected RefreshPolicy refreshPolicy() {
		return RefreshPolicy.IMMEDIATE;
	}

	/**
	 * Set up the read {@link IndicesOptions}. Default is set to {@link IndicesOptions#strictExpandOpenAndForbidClosed()}.
	 *
	 * @return {@literal null} to use the server defaults.
	 */
	@Nullable
	protected IndicesOptions indicesOptions() {
		return IndicesOptions.strictExpandOpenAndForbidClosed();
	}

}
