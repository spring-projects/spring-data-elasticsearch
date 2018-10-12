/*
 * Copyright 2018 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * RestClientFactoryBean
 * 
 * @author Don Wellington
 */
@Slf4j
public class RestClientFactoryBean implements FactoryBean<RestHighLevelClient>, InitializingBean, DisposableBean {

	private RestHighLevelClient client;
	private String hosts = "http://localhost:9200";
	static final String COMMA = ",";

	@Override
	public void destroy() throws Exception {
		try {
			log.info("Closing elasticSearch  client");
			if (client != null) {
				client.close();
			}
		} catch (final Exception e) {
			log.error("Error closing ElasticSearch client: ", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		buildClient();
	}

	@Override
	public RestHighLevelClient getObject() throws Exception {
		return client;
	}

	@Override
	public Class<?> getObjectType() {
		return RestHighLevelClient.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	protected void buildClient() throws Exception {

		Assert.hasText(hosts, "[Assertion Failed] At least one host must be set.");
		ArrayList<HttpHost> httpHosts = new ArrayList<HttpHost>();
		for (String host : hosts.split(COMMA)) {
			URL hostUrl = new URL(host);
			httpHosts.add(new HttpHost(hostUrl.getHost(), hostUrl.getPort(), hostUrl.getProtocol()));
		}
		client = new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()])));
	}

	public void setHosts(String hosts) {
		this.hosts = hosts;
	}

	public String getHosts() {
		return this.hosts;
	}
}
