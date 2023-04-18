/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import reactor.core.publisher.Mono;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ReactiveClusterOperations;

/**
 * Default implementation of {@link ReactiveClusterOperations} using the {@link ReactiveElasticsearchOperations}.
 *
 * @author Peter-Josef Meisch
 * @since 4.2
 * @deprecated since 5.0
 */
@Deprecated
public class DefaultReactiveClusterOperations implements ReactiveClusterOperations {
	private final ReactiveElasticsearchOperations operations;

	public DefaultReactiveClusterOperations(ReactiveElasticsearchOperations operations) {
		this.operations = operations;
	}

	@Override
	public Mono<ClusterHealth> health() {
		return Mono.from(operations.executeWithClusterClient(
				client -> client.health(new ClusterHealthRequest()).map(ResponseConverter::clusterHealth)));
	}
}
