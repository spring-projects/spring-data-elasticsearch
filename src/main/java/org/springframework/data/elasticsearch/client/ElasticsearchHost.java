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

import java.net.InetSocketAddress;
import java.time.Instant;

import org.springframework.util.Assert;

/**
 * Value Object containing information about Elasticsearch cluster nodes.
 *
 * @author Christoph Strobl
 * @since 3.2
 */
public class ElasticsearchHost {

	/**
	 * Default HTTP port for Elasticsearch servers.
	 */
	public static final int DEFAULT_PORT = 9200;

	private final InetSocketAddress endpoint;
	private final State state;
	private final Instant timestamp;

	public ElasticsearchHost(InetSocketAddress endpoint, State state) {

		Assert.notNull(endpoint, "Host must not be null");
		Assert.notNull(state, "State must not be null");

		this.endpoint = endpoint;
		this.state = state;
		this.timestamp = Instant.now();
	}

	/**
	 * @param host must not be {@literal null}.
	 * @return new instance of {@link ElasticsearchHost}.
	 */
	public static ElasticsearchHost online(InetSocketAddress host) {
		return new ElasticsearchHost(host, State.ONLINE);
	}

	/**
	 * @param host must not be {@literal null}.
	 * @return new instance of {@link ElasticsearchHost}.
	 */
	public static ElasticsearchHost offline(InetSocketAddress host) {
		return new ElasticsearchHost(host, State.OFFLINE);
	}

	/**
	 * Parse a {@literal hostAndPort} string into a {@link InetSocketAddress}.
	 *
	 * @param hostAndPort the string containing host and port or IP address and port in the format {@code host:port}.
	 * @return the parsed {@link InetSocketAddress}.
	 */
	public static InetSocketAddress parse(String hostAndPort) {
		return InetSocketAddressParser.parse(hostAndPort, DEFAULT_PORT);
	}

	/**
	 * @return {@literal true} if the last known {@link State} was {@link State#ONLINE}
	 */
	public boolean isOnline() {
		return State.ONLINE.equals(state);
	}

	/**
	 * @return never {@literal null}.
	 */
	public InetSocketAddress getEndpoint() {
		return endpoint;
	}

	/**
	 * @return the last known {@link State}.
	 */
	public State getState() {
		return state;
	}

	/**
	 * @return the {@link Instant} the information was captured.
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "ElasticsearchHost(" + endpoint + ", " + state.name() + ")";
	}

	public enum State {
		ONLINE, OFFLINE, UNKNOWN
	}
}
