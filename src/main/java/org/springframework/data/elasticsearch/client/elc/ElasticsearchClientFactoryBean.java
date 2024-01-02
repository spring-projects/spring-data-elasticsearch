/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * ElasticsearchClientFactoryBean
 *
 * @author Peter-Josef Meisch
 * @since 5.0
 */
public class ElasticsearchClientFactoryBean
		implements FactoryBean<ElasticsearchClient>, InitializingBean, DisposableBean {

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchClientFactoryBean.class);

	private @Nullable AutoCloseableElasticsearchClient client;
	private String hosts = "http://localhost:9200";
	static final String COMMA = ",";

	@Override
	public void destroy() {
		try {
			LOGGER.info("Closing elasticSearch  client");
			if (client != null) {
				client.close();
			}
		} catch (final Exception e) {
			LOGGER.error("Error closing ElasticSearch client: ", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		buildClient();
	}

	@Override
	public ElasticsearchClient getObject() {

		if (client == null) {
			throw new FactoryBeanNotInitializedException();
		}

		return client;
	}

	@Override
	public Class<?> getObjectType() {
		return ElasticsearchClient.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	protected void buildClient() throws Exception {

		Assert.hasText(hosts, "[Assertion Failed] At least one host must be set.");

		var clientConfiguration = ClientConfiguration.builder().connectedTo(hosts).build();
		client = (AutoCloseableElasticsearchClient) ElasticsearchClients.createImperative(clientConfiguration);
	}

	public void setHosts(String hosts) {
		this.hosts = hosts;
	}

	public String getHosts() {
		return this.hosts;
	}
}
