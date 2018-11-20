/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.client.reactive;

import reactor.core.publisher.Mono;

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
 * @since 4.0 TODO: Encapsulate host (scheme, host, port) within a value object.
 */
public interface HostProvider {

	/**
	 * Lookup an active host in {@link VerificationMode#LAZY lazy} mode utilizing cached {@link ElasticsearchHost}.
	 *
	 * @return the {@link Mono} emitting the active host or {@link Mono#error(Throwable) an error} if none found.
	 */
	default Mono<String> lookupActiveHost() {
		return lookupActiveHost(VerificationMode.LAZY);
	}

	/**
	 * Lookup an active host in using the given {@link VerificationMode}.
	 *
	 * @param verificationMode
	 * @return the {@link Mono} emitting the active host or {@link Mono#error(Throwable) an error}
	 *         ({@link NoReachableHostException}) if none found.
	 */
	Mono<String> lookupActiveHost(VerificationMode verificationMode);

	/**
	 * Get the {@link WebClient} connecting to an active host utilizing cached {@link ElasticsearchHost}.
	 *
	 * @return the {@link Mono} emitting the client for an active host or {@link Mono#error(Throwable) an error} if none
	 *         found.
	 */
	default Mono<WebClient> getActive() {
		return getActive(VerificationMode.LAZY);
	}

	/**
	 * Get the {@link WebClient} connecting to an active host.
	 *
	 * @param verificationMode must not be {@literal null}.
	 * @return the {@link Mono} emitting the client for an active host or {@link Mono#error(Throwable) an error} if none
	 *         found.
	 */
	default Mono<WebClient> getActive(VerificationMode verificationMode) {
		return lookupActiveHost(verificationMode).map(host -> createWebClient(host));
	}

	/**
	 * Creates a {@link WebClient} for {@code baseUrl}.
	 *
	 * @param baseUrl
	 * @return
	 */
	WebClient createWebClient(String baseUrl);

	/**
	 * Obtain information about known cluster nodes.
	 *
	 * @return the {@link Mono} emitting {@link ClusterInformation} when available.
	 */
	Mono<ClusterInformation> clusterInfo();

	/**
	 * Create a new {@link HostProvider} best suited for the given {@link WebClientProvider} and number of hosts.
	 *
	 * @param clientProvider must not be {@literal null} .
	 * @param hosts must not be {@literal null} nor empty.
	 * @return new instance of {@link HostProvider}.
	 */
	static HostProvider provider(WebClientProvider clientProvider, String... hosts) {

		Assert.notNull(clientProvider, "WebClientProvider must not be null");
		Assert.notEmpty(hosts, "Please provide at least one host to connect to.");

		if (hosts.length == 1) {
			return new SingleNodeHostProvider(clientProvider, hosts[0]);
		} else {
			return new MultiNodeHostProvider(clientProvider, hosts);
		}
	}

	/**
	 * @author Christoph Strobl
	 * @since 4.0
	 */
	enum VerificationMode {

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
	 * Value object accumulating information about cluster an Elasticsearch cluster.
	 *
	 * @author Christoph Strobll
	 * @since 4.0.
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
