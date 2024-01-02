/*
 * Copyright 2021-2024 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.RefreshPolicy;

/**
 * Configuration for Spring Data Elasticsearch tests using an {@link ElasticsearchTemplate}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@Configuration
public class ElasticsearchTemplateConfiguration extends ElasticsearchConfiguration {

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	@Autowired private ClusterConnectionInfo clusterConnectionInfo;

	@Override
	public ClientConfiguration clientConfiguration() {
		String elasticsearchHostPort = clusterConnectionInfo.getHost() + ':' + clusterConnectionInfo.getHttpPort();

		ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder()
				.connectedTo(elasticsearchHostPort);

		String proxy = System.getenv("DATAES_ELASTICSEARCH_PROXY");

		if (proxy != null) {
			configurationBuilder = configurationBuilder.withProxy(proxy);
		}

		configurationBuilder = ((ClientConfiguration.MaybeSecureClientConfigurationBuilder) configurationBuilder)
				.usingSsl(clusterConnectionInfo.isUseSsl());

		String user = System.getenv("DATAES_ELASTICSEARCH_USER");
		String password = System.getenv("DATAES_ELASTICSEARCH_PASSWORD");

		if (hasText(user) && hasText(password)) {
			configurationBuilder.withBasicAuth(user, password);
		}

		// noinspection UnnecessaryLocalVariable
		ClientConfiguration clientConfiguration = configurationBuilder //
				.withConnectTimeout(Duration.ofSeconds(20)) //
				.withSocketTimeout(Duration.ofSeconds(20)) //
				.build();

		return clientConfiguration;
	}

	@Override
	protected RefreshPolicy refreshPolicy() {
		return RefreshPolicy.IMMEDIATE;
	}
}
