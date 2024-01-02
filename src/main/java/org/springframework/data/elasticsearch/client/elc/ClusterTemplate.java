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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.transport.ElasticsearchTransport;

import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

/**
 * Implementation of the {@link ClusterOperations} interface using en {@link ElasticsearchClusterClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ClusterTemplate extends ChildTemplate<ElasticsearchTransport, ElasticsearchClusterClient>
		implements ClusterOperations {

	public ClusterTemplate(ElasticsearchClusterClient client, ElasticsearchConverter elasticsearchConverter) {
		super(client, elasticsearchConverter);
	}

	@Override
	public ClusterHealth health() {

		HealthRequest healthRequest = requestConverter.clusterHealthRequest();
		HealthResponse healthResponse = execute(client -> client.health(healthRequest));
		return responseConverter.clusterHealth(healthResponse);
	}

}
