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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.Utils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * This class manages the connection to an Elasticsearch Cluster, starting a local one if necessary. The information
 * about the ClusterConnection is stored botha s a varaible in the instance for direct aaces from JUnit 5 and in a
 * static ThreadLocal<ClusterConnectionInfo> acessible with the {@link ClusterConnection#clusterConnectionInfo()} method
 * to be integrated in the Spring setup
 *
 * @author Peter-Josef Meisch
 */
class ClusterConnection implements ExtensionContext.Store.CloseableResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClusterConnection.class);

	private static final ThreadLocal<ClusterConnectionInfo> clusterConnectionInfoThreadLocal = new ThreadLocal<>();

	private Node node;
	private final ClusterConnectionInfo clusterConnectionInfo;

	/**
	 * creates the ClusterConnection, starting a local node if necessary.
	 *
	 * @param clusterUrl if null or empty a local cluster is tarted
	 */
	public ClusterConnection(@Nullable String clusterUrl) {
		clusterConnectionInfo = StringUtils.isEmpty(clusterUrl) ? startLocalNode() : parseUrl(clusterUrl);

		if (clusterConnectionInfo != null) {
			LOGGER.debug(clusterConnectionInfo.toString());
			clusterConnectionInfoThreadLocal.set(clusterConnectionInfo);
		} else {
			LOGGER.error("could not create ClusterConnectionInfo");
		}
	}

	/**
	 * @return the {@link ClusterConnectionInfo} from the ThreadLocal storage.
	 */
	@Nullable
	public static ClusterConnectionInfo clusterConnectionInfo() {
		return clusterConnectionInfoThreadLocal.get();
	}

	public ClusterConnectionInfo getClusterConnectionInfo() {
		return clusterConnectionInfo;
	}

	/**
	 * @param clusterUrl the URL to parse
	 * @return the connection information
	 */
	private ClusterConnectionInfo parseUrl(String clusterUrl) {
		try {
			URL url = new URL(clusterUrl);

			if (!url.getProtocol().startsWith("http") || url.getPort() <= 0) {
				throw new ClusterConnectionException("invalid url " + clusterUrl);
			}

			return ClusterConnectionInfo.builder() //
					.withHostAndPort(url.getHost(), url.getPort()) //
					.useSsl(url.getProtocol().equals("https")) //
					.build();
		} catch (MalformedURLException e) {
			throw new ClusterConnectionException(e);
		}

	}

	private @Nullable ClusterConnectionInfo startLocalNode() {
		LOGGER.debug("starting local node");

		try {
			node = Utils.getNode();
			node.start();
			return ClusterConnectionInfo.builder() //
					.withHostAndPort("localhost", 9200) //
					.withClient(node.client()) //
					.build();
		} catch (NodeValidationException e) {
			LOGGER.error("could not start local node", e);
		}

		return null;
	}

	@Override
	public void close() throws Exception {

		if (node != null) {
			LOGGER.debug("closing node");
			try {
				node.close();
			} catch (IOException ignored) {}
		}
		LOGGER.debug("closed");
	}
}
