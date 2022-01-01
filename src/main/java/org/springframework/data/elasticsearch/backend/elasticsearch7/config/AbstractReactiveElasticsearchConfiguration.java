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
package org.springframework.data.elasticsearch.backend.elasticsearch7.config;

import org.elasticsearch.action.support.IndicesOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.backend.elasticsearch7.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.backend.elasticsearch7.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 4.4
 * @see ElasticsearchConfigurationSupport
 */
public abstract class AbstractReactiveElasticsearchConfiguration extends ElasticsearchConfigurationSupport {

	/**
	 * Return the {@link ReactiveElasticsearchClient} instance used to connect to the cluster. <br />
	 *
	 * @return never {@literal null}.
	 */
	@Bean
	public abstract ReactiveElasticsearchClient reactiveElasticsearchClient();

	/**
	 * Creates {@link ReactiveElasticsearchOperations}.
	 *
	 * @return never {@literal null}.
	 */
	@Bean
	public ReactiveElasticsearchOperations reactiveElasticsearchTemplate(ElasticsearchConverter elasticsearchConverter,
			ReactiveElasticsearchClient reactiveElasticsearchClient) {

		ReactiveElasticsearchTemplate template = new ReactiveElasticsearchTemplate(reactiveElasticsearchClient,
				elasticsearchConverter);
		template.setIndicesOptions(indicesOptions());
		template.setRefreshPolicy(refreshPolicy());

		return template;
	}

	/**
	 * Set up the write {@link RefreshPolicy}. Default is set to null to use the cluster defaults..
	 *
	 * @return {@literal null} to use the server defaults.
	 */
	@Nullable
	protected RefreshPolicy refreshPolicy() {
		return null;
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
