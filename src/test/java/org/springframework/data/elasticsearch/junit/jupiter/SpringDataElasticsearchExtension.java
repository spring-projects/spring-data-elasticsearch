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

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * This extension class check in the {@link #beforeAll(ExtensionContext)} call if there is already an Elasticsearch
 * cluster connection defined in the root store. If no, the connection to the cluster is defined according to the
 * configuration, starting a local node if necessary. The connection is stored and will be closed when the store is
 * shutdown at the end of all tests.
 * <p/>
 * A ParameterResolver is implemented which enables resolving the ClusterConnectionInfo to test methods, and if a Spring
 * context is used, the ClusterConnectionInfo can be autowired.
 *
 * @author Peter-Josef Meisch
 */
public class SpringDataElasticsearchExtension
		implements BeforeAllCallback, ParameterResolver, ContextCustomizerFactory {

	private static final Log LOGGER = LogFactory.getLog(SpringDataElasticsearchExtension.class);

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
			.create(SpringDataElasticsearchExtension.class.getName());
	private static final String STORE_KEY_CLUSTER_CONNECTION = ClusterConnection.class.getSimpleName();
	private static final String STORE_KEY_CLUSTER_CONNECTION_INFO = ClusterConnectionInfo.class.getSimpleName();

	private static final Lock initLock = new ReentrantLock();

	@Override
	public void beforeAll(ExtensionContext extensionContext) {
		initLock.lock();
		try {
			ExtensionContext.Store store = getStore(extensionContext);
			ClusterConnection clusterConnection = store.getOrComputeIfAbsent(STORE_KEY_CLUSTER_CONNECTION, key -> {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("creating ClusterConnection");
				}
				return createClusterConnection();
			}, ClusterConnection.class);
			store.getOrComputeIfAbsent(STORE_KEY_CLUSTER_CONNECTION_INFO,
					key -> clusterConnection.getClusterConnectionInfo());
		} finally {
			initLock.unlock();
		}
	}

	private ExtensionContext.Store getStore(ExtensionContext extensionContext) {
		return extensionContext.getRoot().getStore(NAMESPACE);
	}

	private ClusterConnection createClusterConnection() {
		return new ClusterConnection();
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> parameterType = parameterContext.getParameter().getType();
		return parameterType.isAssignableFrom(ClusterConnectionInfo.class);
	}

	/*
	 * (non javadoc)
	 * no need to check the parameterContext and extensionContext here, this was done before in supportsParameter.
	 */
	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return getStore(extensionContext).get(STORE_KEY_CLUSTER_CONNECTION_INFO, ClusterConnectionInfo.class);
	}

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return this::customizeContext;
	}

	private void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {

		ClusterConnectionInfo clusterConnectionInfo = ClusterConnection.clusterConnectionInfo();

		if (clusterConnectionInfo != null) {
			context.getBeanFactory().registerResolvableDependency(ClusterConnectionInfo.class, clusterConnectionInfo);
		}
	}

}
