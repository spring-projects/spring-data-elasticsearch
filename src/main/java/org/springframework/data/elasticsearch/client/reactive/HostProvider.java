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
package org.springframework.data.elasticsearch.client.reactive;

import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;

import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Infrastructure helper class aware of hosts within the cluster and the health of those allowing easy selection of
 * active ones.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
public interface HostProvider {

	/**
	 * Create a new {@link HostProvider} best suited for the given {@link WebClientProvider} and number of hosts.
	 *
	 * @param clientProvider must not be {@literal null} .
	 * @param endpoints must not be {@literal null} nor empty.
	 * @return new instance of {@link HostProvider}.
	 */
	static HostProvider provider(WebClientProvider clientProvider, InetSocketAddress... endpoints) {

		Assert.notNull(clientProvider, "WebClientProvider must not be null");
		Assert.notEmpty(endpoints, "Please provide at least one endpoint to connect to.");

		if (endpoints.length == 1) {
			return new SingleNodeHostProvider(clientProvider, endpoints[0]);
		} else {
			return new MultiNodeHostProvider(clientProvider, endpoints);
		}
	}

	/**
	 * Lookup an active host in {@link Verification#LAZY lazy} mode utilizing cached {@link ElasticsearchHost}.
	 *
	 * @return the {@link Mono} emitting the active host or {@link Mono#error(Throwable) an error} if none found.
	 */
	default Mono<InetSocketAddress> lookupActiveHost() {
		return lookupActiveHost(Verification.LAZY);
	}

	/**
	 * Lookup an active host in using the given {@link Verification}.
	 *
	 * @param verification
	 * @return the {@link Mono} emitting the active host or {@link Mono#error(Throwable) an error}
	 *         ({@link NoReachableHostException}) if none found.
	 */
	Mono<InetSocketAddress> lookupActiveHost(Verification verification);

	/**
	 * Get the {@link WebClient} connecting to an active host utilizing cached {@link ElasticsearchHost}.
	 *
	 * @return the {@link Mono} emitting the client for an active host or {@link Mono#error(Throwable) an error} if none
	 *         found.
	 */
	default Mono<WebClient> getActive() {
		return getActive(Verification.LAZY);
	}

	/**
	 * Get the {@link WebClient} connecting to an active host.
	 *
	 * @param verification must not be {@literal null}.
	 * @return the {@link Mono} emitting the client for an active host or {@link Mono#error(Throwable) an error} if none
	 *         found.
	 */
	default Mono<WebClient> getActive(Verification verification) {
		return lookupActiveHost(verification).map(this::createWebClient);
	}

	/**
	 * Creates a {@link WebClient} for {@link InetSocketAddress endpoint}.
	 *
	 * @param endpoint must not be {@literal null}.
	 * @return a {@link WebClient} using the the given endpoint as {@literal base url}.
	 */
	WebClient createWebClient(InetSocketAddress endpoint);

	/**
	 * Obtain information about known cluster nodes.
	 *
	 * @return the {@link Mono} emitting {@link ClusterInformation} when available.
	 */
	Mono<ClusterInformation> clusterInfo();

	/**
	 * {@link Verification} allows to influence the lookup strategy for active hosts.
	 *
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	enum Verification {

		/**
		 * Actively check for cluster node health.
		 */
		ACTIVE,

		/**
		 * Use cached data for cluster node health.
		 */
		LAZY
	}

	/**
	 * Value object accumulating information about an Elasticsearch cluster.
	 *
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	class ClusterInformation {

		private final Set<ElasticsearchHost> nodes;

		public ClusterInformation(Set<ElasticsearchHost> nodes) {
			this.nodes = nodes;
		}

		public Set<ElasticsearchHost> getNodes() {
			return Collections.unmodifiableSet(nodes);
		}
	}
}
