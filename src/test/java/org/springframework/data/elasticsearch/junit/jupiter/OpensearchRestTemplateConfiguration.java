/*
 * Copyright 2019-2022 the original author or authors.
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

import static org.springframework.util.StringUtils.*;

import java.time.Duration;

import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.orhlc.ClientConfiguration;
import org.springframework.data.elasticsearch.client.orhlc.AbstractOpensearchConfiguration;
import org.springframework.data.elasticsearch.client.orhlc.OpensearchRestTemplate;
import org.springframework.data.elasticsearch.client.orhlc.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

/**
 * Configuration for Spring Data Elasticsearch using {@link ElasticsearchRestTemplate}.
 *
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 * @since 5.0
 */
@Configuration
public class OpensearchRestTemplateConfiguration extends AbstractOpensearchConfiguration {

	@Autowired private ClusterConnectionInfo clusterConnectionInfo;

	@Override
	@Bean
	public RestHighLevelClient opensearchClient() {

		String elasticsearchHostPort = clusterConnectionInfo.getHost() + ':' + clusterConnectionInfo.getHttpPort();

		ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder()
				.connectedTo(elasticsearchHostPort);

		String proxy = System.getenv("DATAOS_OPENSEARCH_PROXY");

		if (proxy != null) {
			configurationBuilder = configurationBuilder.withProxy(proxy);
		}

		if (clusterConnectionInfo.isUseSsl()) {
			configurationBuilder = ((ClientConfiguration.MaybeSecureClientConfigurationBuilder) configurationBuilder)
					.usingSsl();
		}

		String user = System.getenv("DATAOS_OPENSEARCH_USER");
		String password = System.getenv("DATAOS_OPENSEARCH_PASSWORD");

		if (hasText(user) && hasText(password)) {
			configurationBuilder.withBasicAuth(user, password);
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

		OpensearchRestTemplate template = new OpensearchRestTemplate(elasticsearchClient, elasticsearchConverter) {
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
		template.setRefreshPolicy(refreshPolicy());

		return template;
	}

	@Override
	protected RefreshPolicy refreshPolicy() {
		return RefreshPolicy.IMMEDIATE;
	}
}
