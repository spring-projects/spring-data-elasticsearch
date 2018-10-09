/*
 * Copyright 2018. the original author or authors.
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

package org.springframework.data.elasticsearch.client;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Infrastructure helper class aware of hosts within the cluster and the health of those allowing easy selection of
 * active ones.
 *
 * @author Christoph Strobl
 * @since 3.2
 */
public interface ClientProvider {

	/**
	 * Lookup an active host in {@link VerificationMode#LAZY lazy} mode utilizing cached {@link HostState}.
	 *
	 * @return the {@link Mono} emitting the active host or {@link Mono#error(Throwable) an error} if none found.
	 */
	default Mono<String> lookupActiveHost() {
		return lookupActiveHost(VerificationMode.LAZY);
	}

	/**
	 * Lookup an active host in using the given {@link VerificationMode}.
	 *
	 * @return the {@link Mono} emitting the active host or {@link Mono#error(Throwable) an error} if none found.
	 */
	Mono<String> lookupActiveHost(VerificationMode verificationMode);

	/**
	 * Get the {@link WebClient} connecting to an active host utilizing cached {@link HostState}.
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
		return getActive(verificationMode, HttpHeaders.EMPTY);
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
	 * Refresh cached {@link HostState} for known hosts.
	 *
	 * @return the {@link Mono} signaling operation completion.
	 */
	default Mono<Void> refresh() {
		return Mono.empty();
	}

	/**
	 * Obtain the current {@link HostState} cache. <br />
	 * <strong>Note</strong> Use {@link #refresh()} to update.
	 *
	 * @return never {@literal null}.
	 */
	Collection<HostState> status();

	/**
	 * Create a new instance of {@link ClientProvider} applying the given headers by default.
	 *
	 * @param headers must not be {@literal null}.
	 * @return new instance of {@link ClientProvider}.
	 */
	ClientProvider withDefaultHeaders(HttpHeaders headers);

	/**
	 * Create a new instance of {@link ClientProvider} calling the given {@link Consumer} on error.
	 *
	 * @param errorListener must not be {@literal null}.
	 * @return new instance of {@link ClientProvider}.
	 */
	ClientProvider withErrorListener(Consumer<Throwable> errorListener);

	/**
	 * Create a new {@link ClientProvider} best suited for the given number of hosts.
	 *
	 * @param hosts must not be {@literal null} nor empty.
	 * @return new instance of {@link ClientProvider}.
	 */
	static ClientProvider provider(String... hosts) {

		Assert.notEmpty(hosts, "Please provide at least one host to connect to.");

		if (hosts.length == 1) {
			return new SingleNodeClientProvider(HttpHeaders.EMPTY, (err) -> {}, hosts[0]);
		} else {
			return new MultiNodeClientProvider(HttpHeaders.EMPTY, (err) -> {}, hosts);
		}
	}

	/**
	 * Create a new {@link ClientProvider} best suited for the {@link ClusterNodes}.
	 *
	 * @param nodes must not be {@literal null}.
	 * @return new instance of {@link ClientProvider}.
	 */
	static ClientProvider provider(ClusterNodes nodes) {

		Assert.notNull(nodes, "Nodes must not be null!");
		return provider(nodes.stream().map(it -> it.toString()).toArray(len -> new String[len]));
	}

	/**
	 * @author Christoph Strobl
	 */
	enum VerificationMode {
		ALWAYS, LAZY
	}

	/**
	 * Value Object containing meta information about cluster nodes.
	 *
	 * @author Christoph Strobl
	 */
	class HostState {

		private final String host;
		private final State state;
		private final Instant timestamp;

		HostState(String host, State state) {

			this.host = host;
			this.state = state;
			this.timestamp = Instant.now();
		}

		boolean isOnline() {
			return online(this);
		}

		public String getHost() {
			return host;
		}

		public State getState() {
			return state;
		}

		public Instant getTimestamp() {
			return timestamp;
		}

		@Override
		public String toString() {
			return "Host(" + host + ", " + state.name() + ")";
		}

		static boolean online(HostState state) {
			return state.state.equals(State.ONLINE);
		}

		public enum State {
			ONLINE, OFFLINE, UNKNOWN
		}
	}
}
