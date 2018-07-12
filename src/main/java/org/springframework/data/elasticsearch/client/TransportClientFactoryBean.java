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

import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.join.ParentJoinPlugin;
import org.elasticsearch.percolator.PercolatorPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.mustache.MustachePlugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * TransportClientFactoryBean
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Jakub Vavrik
 * @author Piotr Betkier
 * @author Oliver Gierke
 */
public class TransportClientFactoryBean implements FactoryBean<TransportClient>, InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(TransportClientFactoryBean.class);
	private ClusterNodes clusterNodes = ClusterNodes.of("127.0.0.1:9300");
	private String clusterName = "elasticsearch";
	private Boolean clientTransportSniff = true;
	private Boolean clientIgnoreClusterName = Boolean.FALSE;
	private String clientPingTimeout = "5s";
	private String clientNodesSamplerInterval = "5s";
	private TransportClient client;
	private Properties properties;

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

		client = new SpringDataTransportClient(settings());

		clusterNodes.stream() //
				.peek(it -> logger.info("Adding transport node : " + it.toString())) //
				.forEach(client::addTransportAddress);

		client.connectedNodes();
	}

	private Settings settings() {
		if (properties != null) {
			return Settings.builder().put(properties).build();
		}
		return Settings.builder().put("cluster.name", clusterName).put("client.transport.sniff", clientTransportSniff)
				.put("client.transport.ignore_cluster_name", clientIgnoreClusterName)
				.put("client.transport.ping_timeout", clientPingTimeout)
				.put("client.transport.nodes_sampler_interval", clientNodesSamplerInterval).build();
	}

	public void setClusterNodes(String clusterNodes) {
		this.clusterNodes = ClusterNodes.of(clusterNodes);
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

	/**
	 * Pretty exact copy of {@link PreBuiltTransportClient} except that we're inspecting the classpath for Netty
	 * dependencies to only include the ones available. {@link PreBuiltTransportClient} expects both Netty 3 and Netty 4
	 * to be present.
	 *
	 * @author Oliver Gierke
	 * @see https://github.com/elastic/elasticsearch/issues/31240
	 */
	@SuppressWarnings("unchecked")
	private static class SpringDataTransportClient extends TransportClient {

		/**
		 * Netty wants to do some unwelcome things like use unsafe and replace a private field, or use a poorly considered
		 * buffer recycler. This method disables these things by default, but can be overridden by setting the corresponding
		 * system properties.
		 */
		private static void initializeNetty() {
			/*
			 * We disable three pieces of Netty functionality here:
			 *  - we disable Netty from being unsafe
			 *  - we disable Netty from replacing the selector key set
			 *  - we disable Netty from using the recycler
			 *
			 * While permissions are needed to read and set these, the permissions needed here are innocuous and thus should simply be granted
			 * rather than us handling a security exception here.
			 */
			setSystemPropertyIfUnset("io.netty.noUnsafe", Boolean.toString(true));
			setSystemPropertyIfUnset("io.netty.noKeySetOptimization", Boolean.toString(true));
			setSystemPropertyIfUnset("io.netty.recycler.maxCapacityPerThread", Integer.toString(0));
		}

		@SuppressForbidden(reason = "set system properties to configure Netty")
		private static void setSystemPropertyIfUnset(final String key, final String value) {
			final String currentValue = System.getProperty(key);
			if (currentValue == null) {
				System.setProperty(key, value);
			}
		}

		private static final List<String> OPTIONAL_DEPENDENCIES = Arrays.asList( //
				"org.elasticsearch.transport.Netty3Plugin", //
				"org.elasticsearch.transport.Netty4Plugin");

		private static final Collection<Class<? extends Plugin>> PRE_INSTALLED_PLUGINS;

		static {

			initializeNetty();

			List<Class<? extends Plugin>> plugins = new ArrayList<>();
			boolean found = false;

			for (String dependency : OPTIONAL_DEPENDENCIES) {
				try {
					plugins.add((Class<? extends Plugin>) ClassUtils.forName(dependency,
							SpringDataTransportClient.class.getClassLoader()));
					found = true;
				} catch (ClassNotFoundException | LinkageError e) {}
			}

			Assert.state(found,
					"Neither Netty 3 or Netty 4 plugin found on the classpath. One of them is required to run the transport client!");

			plugins.add(ReindexPlugin.class);
			plugins.add(PercolatorPlugin.class);
			plugins.add(MustachePlugin.class);
			plugins.add(ParentJoinPlugin.class);

			PRE_INSTALLED_PLUGINS = Collections.unmodifiableList(plugins);
		}

		public SpringDataTransportClient(Settings settings) {
			super(settings, PRE_INSTALLED_PLUGINS);
		}

		@Override
		public void close() {
			super.close();
			if (NetworkModule.TRANSPORT_TYPE_SETTING.exists(settings) == false
					|| NetworkModule.TRANSPORT_TYPE_SETTING.get(settings).equals(Netty4Plugin.NETTY_TRANSPORT_NAME)) {
				try {
					GlobalEventExecutor.INSTANCE.awaitInactivity(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				try {
					ThreadDeathWatcher.awaitInactivity(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
