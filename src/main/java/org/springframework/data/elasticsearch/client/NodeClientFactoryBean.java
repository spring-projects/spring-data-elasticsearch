/*
 * Copyright 2015-2020 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.LogConfigurator;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * NodeClientFactoryBean
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ilkang Na
 * @author Peter-Josef Meisch
 */
public class NodeClientFactoryBean implements FactoryBean<Client>, InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(NodeClientFactoryBean.class);
	private boolean local;
	private boolean enableHttp;
	private @Nullable String clusterName;
	private @Nullable Node node;
	private @Nullable NodeClient nodeClient;
	private @Nullable String pathData;
	private @Nullable String pathHome;
	private @Nullable String pathConfiguration;

	public static class TestNode extends Node {

		private static final String DEFAULT_NODE_NAME = "spring-data-elasticsearch-nodeclientfactorybean-test";

		public TestNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {

			super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, Collections.emptyMap(), null,
					() -> DEFAULT_NODE_NAME), classpathPlugins, false);
		}

		protected void registerDerivedNodeNameWithLogger(String nodeName) {
			try {
				LogConfigurator.setNodeName(nodeName);
			} catch (Exception e) {
				// nagh - just forget about it
			}
		}
	}

	NodeClientFactoryBean() {}

	public NodeClientFactoryBean(boolean local) {
		this.local = local;
	}

	@Override
	public NodeClient getObject() {

		if (nodeClient == null) {
			throw new FactoryBeanNotInitializedException();
		}

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

		Settings settings = Settings.builder() //
				.put(loadConfig()) //
				.put("transport.type", "netty4") //
				.put("http.type", "netty4") //
				.put("path.home", this.pathHome) //
				.put("path.data", this.pathData) //
				.put("cluster.name", this.clusterName) //
				.put("node.max_local_storage_nodes", 100) //
				.build();
		node = new TestNode(settings, Collections.singletonList(Netty4Plugin.class));
		nodeClient = (NodeClient) node.start().client();
	}

	private Settings loadConfig() throws IOException {
		if (!StringUtils.isEmpty(pathConfiguration)) {
			InputStream stream = getClass().getClassLoader().getResourceAsStream(pathConfiguration);
			if (stream != null) {
				return Settings.builder().loadFromStream(pathConfiguration,
						getClass().getClassLoader().getResourceAsStream(pathConfiguration), false).build();
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
	public void destroy() {
		try {
			// NodeClient.close() is a noop, no need to call it here
			nodeClient = null;
			logger.info("Closing elasticSearch node");
			if (node != null) {
				node.close();
				node = null;
			}
		} catch (final Exception e) {
			logger.error("Error closing ElasticSearch client: ", e);
		}
	}
}
