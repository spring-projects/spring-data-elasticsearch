/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.elasticsearch.junit.jupiter;

import org.elasticsearch.client.Client;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * The information about the ClusterConnection. the {@link #client} field is only set if a local node is started,
 * otherwise it is null. <br/>
 * The {@link #host}, {@link #httpPort} and {@link #useSsl} values specify the values needed to connect to the cluster
 * with a rest client for both a local started cluster and for one defined by the cluster URL when creating the
 * {@link ClusterConnection}. <br/>
 * The object must be created by using a {@link ClusterConnectionInfo.Builder}.
 * 
 * @author Peter-Josef Meisch
 */
public final class ClusterConnectionInfo {
	private final boolean useSsl;
	private final String host;
	private final int httpPort;
	private final Client client;

	public static Builder builder() {
		return new Builder();
	}

	private ClusterConnectionInfo(String host, int httpPort, boolean useSsl, Client client) {
		this.host = host;
		this.httpPort = httpPort;
		this.useSsl = useSsl;
		this.client = client;
	}

	@Override
	public String toString() {
		return "ClusterConnectionInfo{" + "useSsl=" + useSsl + ", host='" + host + '\'' + ", httpPort=" + httpPort
				+ ", client=" + client + '}';
	}

	public String getHost() {
		return host;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public boolean isUseSsl() {
		return useSsl;
	}

	@Nullable
	public Client getClient() {
		return client;
	}

	public static class Builder {
		boolean useSsl = false;
		private String host;
		private int httpPort;
		private Client client = null;

		public Builder withHostAndPort(String host, int httpPort) {
			Assert.hasLength(host, "host must not be empty");
			this.host = host;
			this.httpPort = httpPort;
			return this;
		}

		public Builder useSsl(boolean useSsl) {
			this.useSsl = useSsl;
			return this;
		}

		public Builder withClient(Client client) {
			this.client = client;
			return this;
		}

		public ClusterConnectionInfo build() {
			return new ClusterConnectionInfo(host, httpPort, useSsl, client);
		}
	}
}
