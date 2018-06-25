/*
 * Copyright 2015-2017 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import static java.util.Arrays.*;

/**
 * NodeClientFactoryBean
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ilkang Na
 */

public class NodeClientFactoryBean implements FactoryBean<Client>, InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(NodeClientFactoryBean.class);
	private boolean local;
	private boolean enableHttp;
	private String clusterName;
	private NodeClient nodeClient;
	private String pathData;
	private String pathHome;
	private String pathConfiguration;

	public static class TestNode extends Node {
		public TestNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
			super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
		}
	}

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

		nodeClient = (NodeClient) new TestNode(
				Settings.builder().put(loadConfig())
						.put("transport.type", "netty4")
						.put("http.type", "netty4")
						.put("path.home", this.pathHome)
						.put("path.data", this.pathData)
						.put("cluster.name", this.clusterName)
						.put("node.max_local_storage_nodes", 100)
						.build(), asList(Netty4Plugin.class)).start().client();
	}

	private Settings loadConfig() throws IOException {
		if (!StringUtils.isEmpty(pathConfiguration)) {
			InputStream stream = getClass().getClassLoader().getResourceAsStream(pathConfiguration);
			if (stream != null) {
				return Settings.builder().loadFromStream(pathConfiguration, getClass().getClassLoader().getResourceAsStream(pathConfiguration), false).build();
			}
			logger.error(String.format("Unable to read node configuration from file [%s]", pathConfiguration));
		}
		return Settings.builder().build();
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

	public void setPathHome(String pathHome) {
		this.pathHome = pathHome;
	}

	public void setPathConfiguration(String configuration) {
		this.pathConfiguration = configuration;
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
