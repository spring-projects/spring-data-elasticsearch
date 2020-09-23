/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.junit.jupiter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.data.elasticsearch.config.AbstractReactiveElasticsearchConfiguration;

/**
 * Configuration for Spring Data Elasticsearch Integration Tests using
 * {@link org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations}
 *
 * @author Peter-Josef Meisch
 */
@Configuration
public class ReactiveElasticsearchRestTemplateConfiguration extends AbstractReactiveElasticsearchConfiguration {

	@Autowired private ClusterConnectionInfo clusterConnectionInfo;

	@Override
	public ReactiveElasticsearchClient reactiveElasticsearchClient() {
		String elasticsearchHostPort = clusterConnectionInfo.getHost() + ':' + clusterConnectionInfo.getHttpPort();

		ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder() //
				.connectedTo(elasticsearchHostPort);

		if (clusterConnectionInfo.isUseSsl()) {
			configurationBuilder = ((ClientConfiguration.MaybeSecureClientConfigurationBuilder) configurationBuilder)
					.usingSsl();
		}

		return ReactiveRestClients.create(configurationBuilder //
				.withConnectTimeout(Duration.ofSeconds(20)) //
				.withSocketTimeout(Duration.ofSeconds(20)) //
				.build());
	}
}
