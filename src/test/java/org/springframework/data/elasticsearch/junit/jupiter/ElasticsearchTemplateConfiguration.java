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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

/**
 * Configuration for Spring Data Elasticsearch using
 * {@link org.springframework.data.elasticsearch.core.ElasticsearchTemplate}.
 *
 * @author Peter-Josef Meisch
 */
@Configuration
public class ElasticsearchTemplateConfiguration extends ElasticsearchConfigurationSupport {

	@Bean
	public Client elasticsearchClient(ClusterConnectionInfo clusterConnectionInfo) throws UnknownHostException {

		Settings settings = Settings.builder().put("cluster.name", clusterConnectionInfo.getClusterName()).build();
		TransportClient client = new PreBuiltTransportClient(settings);
		client.addTransportAddress(new TransportAddress(InetAddress.getByName(clusterConnectionInfo.getHost()),
				clusterConnectionInfo.getTransportPort()));

		return client;
	}

	@Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
	public ElasticsearchTemplate elasticsearchTemplate(Client elasticsearchClient,
			ElasticsearchConverter elasticsearchConverter) {
		return new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
	}
}
