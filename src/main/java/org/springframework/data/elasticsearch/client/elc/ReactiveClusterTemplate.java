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

import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.transport.ElasticsearchTransport;
import reactor.core.publisher.Mono;

import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ReactiveClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ReactiveClusterTemplate
		extends ReactiveChildTemplate<ElasticsearchTransport, ReactiveElasticsearchClusterClient>
		implements ReactiveClusterOperations {

	public ReactiveClusterTemplate(ReactiveElasticsearchClusterClient client,
			ElasticsearchConverter elasticsearchConverter) {
		super(client, elasticsearchConverter);
	}

	@Override
	public Mono<ClusterHealth> health() {

		HealthRequest healthRequest = requestConverter.clusterHealthRequest();
		Mono<HealthResponse> healthResponse = Mono.from(execute(client -> client.health(healthRequest)));
		return healthResponse.map(responseConverter::clusterHealth);
	}

}
