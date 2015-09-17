/*
 * Copyright 2015 the original author or authors.
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

import static org.elasticsearch.node.NodeBuilder.*;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * NodeClientFactoryBean
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */

public class NodeClientFactoryBean implements FactoryBean<NodeClient>, InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(NodeClientFactoryBean.class);
	private boolean local;
	private boolean enableHttp;
	private String clusterName;
	private NodeClient nodeClient;
	private String pathData;

	NodeClientFactoryBean() {
	}

	public NodeClientFactoryBean(boolean local) {
		this.local = local;
	}

	@Override
	public NodeClient getObject() throws Exception {
		return nodeClient;
	}

	@Override
	public Class<? extends Client> getObjectType() {
		return NodeClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder()
				.put("http.enabled", String.valueOf(this.enableHttp))
				.put("path.data", this.pathData);


		nodeClient = (NodeClient) nodeBuilder().settings(settings).clusterName(this.clusterName).local(this.local).node()
				.client();
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public void setEnableHttp(boolean enableHttp) {
		this.enableHttp = enableHttp;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public void setPathData(String pathData) {
		this.pathData = pathData;
	}

	@Override
	public void destroy() throws Exception {
		try {
			logger.info("Closing elasticSearch  client");
			if (nodeClient != null) {
				nodeClient.close();
			}
		} catch (final Exception e) {
			logger.error("Error closing ElasticSearch client: ", e);
		}
	}
}
