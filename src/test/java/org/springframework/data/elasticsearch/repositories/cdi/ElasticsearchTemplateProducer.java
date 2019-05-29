/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories.cdi;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/**
 * @author Mohsin Husen
 * @author Simon Schneider
 */
@ApplicationScoped
class ElasticsearchTemplateProducer {

	@Produces
	public RestHighLevelClient createRestHighLevelClient() {
		return new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200)));
	}


	@Produces
	public ElasticsearchOperations createElasticsearchTemplate(RestHighLevelClient client) {
		return new ElasticsearchRestTemplate(client);
	}

	@Produces
	@OtherQualifier
	@PersonDB
	public ElasticsearchOperations createQualifiedElasticsearchTemplate(RestHighLevelClient client) {
		return new ElasticsearchRestTemplate(client);
	}

	@PreDestroy
	public void shutdown() {
		// remove everything to avoid conflicts with other tests in case server not shut down properly
	}

}
