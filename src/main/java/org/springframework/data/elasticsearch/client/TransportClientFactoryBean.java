/*
 * Copyright 2013 the original author or authors.
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

import static org.apache.commons.lang.StringUtils.*;

import java.net.InetAddress;
import java.util.Properties;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * TransportClientFactoryBean
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Jakub Vavrik
 * @author Piotr Betkier
 */

public class TransportClientFactoryBean implements FactoryBean<TransportClient>, InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(TransportClientFactoryBean.class);
	private String clusterNodes = "127.0.0.1:9300";
	private String clusterName = "elasticsearch";
	private Boolean clientTransportSniff = true;
	private Boolean clientIgnoreClusterName = Boolean.FALSE;
	private String clientPingTimeout = "5s";
	private String clientNodesSamplerInterval = "5s";
	private TransportClient client;
	private Properties properties;
	static final String COLON = ":";
	static final String COMMA = ",";

	@Override
	public void destroy() throws Exception {
		try {
			logger.info("Closing elasticSearch  client");
			if (client != null) {
				client.close();
			}
		} catch (final Exception e) {
			logger.error("Error closing ElasticSearch client: ", e);
		}
	}

	@Override
	public TransportClient getObject() throws Exception {
		return client;
	}

	@Override
	public Class<TransportClient> getObjectType() {
		return TransportClient.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		buildClient();
	}

	protected void buildClient() throws Exception {
		client = TransportClient.builder().settings(settings()).build();
		Assert.hasText(clusterNodes, "[Assertion failed] clusterNodes settings missing.");
		for (String clusterNode : split(clusterNodes, COMMA)) {
			String hostName = substringBeforeLast(clusterNode, COLON);
			String port = substringAfterLast(clusterNode, COLON);
			Assert.hasText(hostName, "[Assertion failed] missing host name in 'clusterNodes'");
			Assert.hasText(port, "[Assertion failed] missing port in 'clusterNodes'");
			logger.info("adding transport node : " + clusterNode);
			client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostName), Integer.valueOf(port)));
		}
		client.connectedNodes();
	}

	private Settings settings() {
		if (properties != null) {
			return Settings.builder().put(properties).build();
		}
		return Settings.builder()
				.put("cluster.name", clusterName)
				.put("client.transport.sniff", clientTransportSniff)
				.put("client.transport.ignore_cluster_name", clientIgnoreClusterName)
				.put("client.transport.ping_timeout", clientPingTimeout)
				.put("client.transport.nodes_sampler_interval", clientNodesSamplerInterval)
				.build();
	}

	public void setClusterNodes(String clusterNodes) {
		this.clusterNodes = clusterNodes;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public void setClientTransportSniff(Boolean clientTransportSniff) {
		this.clientTransportSniff = clientTransportSniff;
	}

	public String getClientNodesSamplerInterval() {
		return clientNodesSamplerInterval;
	}

	public void setClientNodesSamplerInterval(String clientNodesSamplerInterval) {
		this.clientNodesSamplerInterval = clientNodesSamplerInterval;
	}

	public String getClientPingTimeout() {
		return clientPingTimeout;
	}

	public void setClientPingTimeout(String clientPingTimeout) {
		this.clientPingTimeout = clientPingTimeout;
	}

	public Boolean getClientIgnoreClusterName() {
		return clientIgnoreClusterName;
	}

	public void setClientIgnoreClusterName(Boolean clientIgnoreClusterName) {
		this.clientIgnoreClusterName = clientIgnoreClusterName;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
