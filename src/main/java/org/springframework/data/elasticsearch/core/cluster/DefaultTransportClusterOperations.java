/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.elasticsearch.core.cluster;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResponseConverter;

/**
 * Default implementation of {@link ClusterOperations} using the
 * {@link org.elasticsearch.client.transport.TransportClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public class DefaultTransportClusterOperations implements ClusterOperations {

	private final ElasticsearchTemplate template;

	public DefaultTransportClusterOperations(ElasticsearchTemplate template) {
		this.template = template;
	}

	@Override
	public ClusterHealth health() {

		ClusterHealthResponse clusterHealthResponse = template.getClient().admin().cluster()
				.health(new ClusterHealthRequest()).actionGet();
		return ResponseConverter.clusterHealth(clusterHealthResponse);
	}
}
