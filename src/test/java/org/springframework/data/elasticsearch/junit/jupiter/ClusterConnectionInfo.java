/*
 * Copyright 2019-2024 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * The information about the ClusterConnection.<br/>
 * The {@link #host}, {@link #httpPort} and {@link #useSsl} values specify the values needed to connect to the cluster
 * with a rest client for both a local started cluster and for one defined by the cluster URL when creating the
 * {@link ClusterConnection}.<br/>
 * The object must be created by using a {@link ClusterConnectionInfo.Builder}.
 *
 * @author Peter-Josef Meisch
 */
public final class ClusterConnectionInfo {
	private final IntegrationtestEnvironment integrationtestEnvironment;
	private final boolean useSsl;
	private final String host;
	private final int httpPort;
	private final String clusterName;
	@Nullable private final ElasticsearchContainer elasticsearchContainer;

	public static Builder builder() {
		return new Builder();
	}

	private ClusterConnectionInfo(IntegrationtestEnvironment integrationtestEnvironment, String host, int httpPort,
			boolean useSsl, @Nullable ElasticsearchContainer elasticsearchContainer) {
		this.integrationtestEnvironment = integrationtestEnvironment;
		this.host = host;
		this.httpPort = httpPort;
		this.useSsl = useSsl;
		this.elasticsearchContainer = elasticsearchContainer;
		this.clusterName = "docker-cluster";
	}

	@Override
	public String toString() {
		return "ClusterConnectionInfo{" + //
				"configuration: " + integrationtestEnvironment + //
				", useSsl=" + useSsl + //
				", host='" + host + '\'' + //
				", httpPort=" + httpPort + //
				'}'; //
	}

	public String getHost() {
		return host;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public String getClusterName() {
		return clusterName;
	}

	public boolean isUseSsl() {
		return useSsl;
	}

	@Nullable
	public ElasticsearchContainer getElasticsearchContainer() {
		return elasticsearchContainer;
	}

	public static class Builder {
		private IntegrationtestEnvironment integrationtestEnvironment;
		private boolean useSsl = false;
		private String host;
		private int httpPort;
		@Nullable private ElasticsearchContainer elasticsearchContainer;

		public Builder withIntegrationtestEnvironment(IntegrationtestEnvironment integrationtestEnvironment) {
			this.integrationtestEnvironment = integrationtestEnvironment;
			return this;
		}

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

		public Builder withElasticsearchContainer(ElasticsearchContainer elasticsearchContainer) {
			this.elasticsearchContainer = elasticsearchContainer;
			return this;
		}

		public ClusterConnectionInfo build() {
			return new ClusterConnectionInfo(integrationtestEnvironment, host, httpPort, useSsl, elasticsearchContainer);
		}
	}
}
