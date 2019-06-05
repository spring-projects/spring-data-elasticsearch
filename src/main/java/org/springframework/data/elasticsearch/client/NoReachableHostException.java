/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.data.elasticsearch.client;

import java.util.Set;

/**
 * {@link RuntimeException} to be emitted / thrown when the cluster is down (aka none of the known nodes is reachable).
 *
 * @author Christoph Strobl
 * @since 3.2
 */
public class NoReachableHostException extends RuntimeException {

	public NoReachableHostException(Set<ElasticsearchHost> hosts) {
		super(createMessage(hosts));
	}

	public NoReachableHostException(Set<ElasticsearchHost> hosts, Throwable cause) {
		super(createMessage(hosts), cause);
	}

	private static String createMessage(Set<ElasticsearchHost> hosts) {

		if (hosts.size() == 1) {
			return String.format("Host '%s' not reachable. Cluster state is offline.", hosts.iterator().next().getEndpoint());
		}

		return String.format("No active host found in cluster. (%s) of (%s) nodes offline.", hosts.size(), hosts.size());
	}
}
