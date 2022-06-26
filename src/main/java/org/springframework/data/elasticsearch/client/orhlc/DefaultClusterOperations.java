/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.orhlc;

import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.client.RequestOptions;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;

/**
 * Default implementation of {@link ClusterOperations} using the {@link OpensearchRestTemplate}.
 *
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 * @since 5.0
 */
class DefaultClusterOperations implements ClusterOperations {

	private final OpensearchRestTemplate template;

	DefaultClusterOperations(OpensearchRestTemplate template) {
		this.template = template;
	}

	@Override
	public ClusterHealth health() {

		ClusterHealthResponse clusterHealthResponse = template
				.execute(client -> client.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT));
		return ResponseConverter.clusterHealth(clusterHealthResponse);
	}
}
