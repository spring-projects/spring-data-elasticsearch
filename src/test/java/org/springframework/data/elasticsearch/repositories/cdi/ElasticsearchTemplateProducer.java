/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.elasticsearch.client.Client;
import org.elasticsearch.node.NodeValidationException;
import org.springframework.data.elasticsearch.Utils;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

/**
 * @author Mohsin Husen
 */
@ApplicationScoped
class ElasticsearchTemplateProducer {

	@Produces
	public Client createNodeClient() throws NodeValidationException {
		return Utils.getNodeClient();
	}

	@Produces
	public ElasticsearchOperations createElasticsearchTemplate(Client client) {
		return new ElasticsearchTemplate(client);
	}

	@Produces
	@OtherQualifier
	@PersonDB
	public ElasticsearchOperations createQualifiedElasticsearchTemplate(Client client) {
		return new ElasticsearchTemplate(client);
	}

	@PreDestroy
	public void shutdown() {
		// remove everything to avoid conflicts with other tests in case server not shut down properly
	}

}
