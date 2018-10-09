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

package org.springframework.data.elasticsearch.client;

import java.time.Instant;

/**
 * Value Object containing information about Elasticsearch cluster nodes.
 *
 * @author Christoph Strobl
 * @since 4.0
 */
public class ElasticsearchHost {

	private final String host;
	private final State state;
	private final Instant timestamp;

	public ElasticsearchHost(String host, State state) {

		this.host = host;
		this.state = state;
		this.timestamp = Instant.now();
	}

	/**
	 * @param host must not be {@literal null}.
	 * @return new instance of {@link ElasticsearchHost}.
	 */
	public static ElasticsearchHost online(String host) {
		return new ElasticsearchHost(host, State.ONLINE);
	}

	/**
	 * @param host must not be {@literal null}.
	 * @return new instance of {@link ElasticsearchHost}.
	 */
	public static ElasticsearchHost offline(String host) {
		return new ElasticsearchHost(host, State.OFFLINE);
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
	public String getHost() {
		return host;
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
		return "ElasticsearchHost(" + host + ", " + state.name() + ")";
	}

	public enum State {
		ONLINE, OFFLINE, UNKNOWN
	}
}
