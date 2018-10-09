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

import org.springframework.data.elasticsearch.client.NoReachableHostException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Infrastructure helper class aware of hosts within the cluster and the health of those allowing easy selection of
 * active ones.
 *
 * @author Christoph Strobl
 * @since 4.0
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
		return getActive(verificationMode, getDefaultHeaders());
	}

	/**
	 * Get the {@link WebClient} with default {@link HttpHeaders} connecting to an active host.
	 *
	 * @param verificationMode must not be {@literal null}.
	 * @param headers must not be {@literal null}.
	 * @return the {@link Mono} emitting the client for an active host or {@link Mono#error(Throwable) an error} if none
	 *         found.
	 */
	default Mono<WebClient> getActive(VerificationMode verificationMode, HttpHeaders headers) {
		return lookupActiveHost(verificationMode).map(host -> createWebClient(host, headers));
	}

	/**
	 * Get the {@link WebClient} with default {@link HttpHeaders} connecting to the given host.
	 *
	 * @param host must not be {@literal null}.
	 * @param headers must not be {@literal null}.
	 * @return
	 */
	default WebClient createWebClient(String host, HttpHeaders headers) {
		return WebClient.builder().baseUrl(host).defaultHeaders(defaultHeaders -> defaultHeaders.putAll(headers)).build();
	}

	/**
	 * Obtain information about known cluster nodes.
	 *
	 * @return the {@link Mono} emitting {@link ClusterInformation} when available.
	 */
	Mono<ClusterInformation> clusterInfo();

	/**
	 * Obtain the {@link HttpHeaders} to be used by default.
	 *
	 * @return never {@literal null}. {@link HttpHeaders#EMPTY} by default.
	 */
	HttpHeaders getDefaultHeaders();

	/**
	 * Create a new instance of {@link HostProvider} applying the given headers by default.
	 *
	 * @param headers must not be {@literal null}.
	 * @return new instance of {@link HostProvider}.
	 */
	HostProvider withDefaultHeaders(HttpHeaders headers);

	/**
	 * Create a new instance of {@link HostProvider} calling the given {@link Consumer} on error.
	 *
	 * @param errorListener must not be {@literal null}.
	 * @return new instance of {@link HostProvider}.
	 */
	HostProvider withErrorListener(Consumer<Throwable> errorListener);

	/**
	 * Create a new {@link HostProvider} best suited for the given number of hosts.
	 *
	 * @param hosts must not be {@literal null} nor empty.
	 * @return new instance of {@link HostProvider}.
	 */
	static HostProvider provider(String... hosts) {

		Assert.notEmpty(hosts, "Please provide at least one host to connect to.");

		if (hosts.length == 1) {
			return new SingleNodeHostProvider(HttpHeaders.EMPTY, (err) -> {}, hosts[0]);
		} else {
			return new MultiNodeHostProvider(HttpHeaders.EMPTY, (err) -> {}, hosts);
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
		FORCE,

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
