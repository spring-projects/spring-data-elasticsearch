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
package org.springframework.data.elasticsearch.junit.jupiter;

import java.time.Duration;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

/**
 * Configuration for Spring Data Elasticsearch using
 * {@link org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate}.
 *
 * @author Peter-Josef Meisch
 */
@Configuration
public class ElasticsearchRestTemplateConfiguration extends AbstractElasticsearchConfiguration {

	@Autowired private ClusterConnectionInfo clusterConnectionInfo;

	@Override
	@Bean
	public RestHighLevelClient elasticsearchClient() {

		String elasticsearchHostPort = clusterConnectionInfo.getHost() + ':' + clusterConnectionInfo.getHttpPort();

		ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder()
				.connectedTo(elasticsearchHostPort);

		String proxy = System.getenv("DATAES_ELASTICSEARCH_PROXY");

		if (proxy != null) {
			configurationBuilder = configurationBuilder.withProxy(proxy);
		}

		if (clusterConnectionInfo.isUseSsl()) {
			configurationBuilder = ((ClientConfiguration.MaybeSecureClientConfigurationBuilder) configurationBuilder)
					.usingSsl();
		}

		return RestClients.create(configurationBuilder //
				.withConnectTimeout(Duration.ofSeconds(20)) //
				.withSocketTimeout(Duration.ofSeconds(20)) //
				.build()) //
				.rest();
	}

	@Override
	public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
			RestHighLevelClient elasticsearchClient) {
		return new ElasticsearchRestTemplate(elasticsearchClient, elasticsearchConverter) {
			@Override
			public <T> T execute(ClientCallback<T> callback) {
				try {
					return super.execute(callback);
				} catch (DataAccessResourceFailureException e) {
					try {
						Thread.sleep(1_000);
					} catch (InterruptedException ignored) {}
					return super.execute(callback);
				}
			}
		};
	}
}
