/*
 * Copyright 2019-2023 the original author or authors.
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

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.erhlc.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.erhlc.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.client.erhlc.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.support.HttpHeaders;

/**
 * Configuration for Spring Data Elasticsearch using {@link ElasticsearchRestTemplate}.
 *
 * @author Peter-Josef Meisch
 * @deprecated since 5.0
 */
@Deprecated
@Configuration
public class ElasticsearchRestTemplateConfiguration extends AbstractElasticsearchConfiguration {

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
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

		String user = System.getenv("DATAES_ELASTICSEARCH_USER");
		String password = System.getenv("DATAES_ELASTICSEARCH_PASSWORD");

		if (hasText(user) && hasText(password)) {
			configurationBuilder.withBasicAuth(user, password);
		}

		HttpHeaders defaultHeaders = new HttpHeaders();
		defaultHeaders.add("Accept", "application/vnd.elasticsearch+json;compatible-with=7");
		defaultHeaders.add("Content-Type", "application/vnd.elasticsearch+json;compatible-with=7");

		// noinspection resource
		return RestClients.create(configurationBuilder //
				.withDefaultHeaders(defaultHeaders) //
				.withConnectTimeout(Duration.ofSeconds(20)) //
				.withSocketTimeout(Duration.ofSeconds(20)) //
				.build()) //
				.rest();
	}

	@Override
	public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
			RestHighLevelClient elasticsearchClient) {

		ElasticsearchRestTemplate template = new ElasticsearchRestTemplate(elasticsearchClient, elasticsearchConverter) {
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
