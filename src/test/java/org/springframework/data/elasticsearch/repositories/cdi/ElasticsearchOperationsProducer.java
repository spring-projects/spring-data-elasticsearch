/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories.cdi;

import static org.springframework.util.StringUtils.*;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnection;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnectionInfo;

/**
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
@ApplicationScoped
class ElasticsearchOperationsProducer {

	@Produces
	public ElasticsearchOperations createElasticsearchTemplate(RestHighLevelClient restHighLevelClient) {
		return new ElasticsearchRestTemplate(restHighLevelClient);
	}

	@Produces
	@OtherQualifier
	@PersonDB
	public ElasticsearchOperations createQualifiedElasticsearchTemplate(RestHighLevelClient restHighLevelClient) {
		return new ElasticsearchRestTemplate(restHighLevelClient);
	}

	@PreDestroy
	public void shutdown() {
		// remove everything to avoid conflicts with other tests in case server not shut down properly
	}

	@Produces
	public RestHighLevelClient elasticsearchClient() {
		// we rely on the tests being run with the SpringDataElasticsearchExtension class that sets up a containerized ES.
		ClusterConnectionInfo connectionInfo = ClusterConnection.clusterConnectionInfo();

		ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder() //
				.connectedTo(connectionInfo.getHost() + ':' + connectionInfo.getHttpPort());

		String user = System.getenv("DATAES_ELASTICSEARCH_USER");
		String password = System.getenv("DATAES_ELASTICSEARCH_PASSWORD");

		if (hasText(user) && hasText(password)) {
			configurationBuilder.withBasicAuth(user, password);
		}

		ClientConfiguration clientConfiguration = configurationBuilder //
				.build();

		return RestClients.create(clientConfiguration).rest();
	}
}
