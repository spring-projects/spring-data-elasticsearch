/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.elasticsearch.repository.config.ElasticsearchRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryBeanDefinitionParser;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * ElasticsearchNamespaceHandler
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Don Wellington
 */
public class ElasticsearchNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		RepositoryConfigurationExtension extension = new ElasticsearchRepositoryConfigExtension();
		RepositoryBeanDefinitionParser parser = new RepositoryBeanDefinitionParser(extension);

		registerBeanDefinitionParser("repositories", parser);
		registerBeanDefinitionParser("node-client", new NodeClientBeanDefinitionParser());
		registerBeanDefinitionParser("transport-client", new TransportClientBeanDefinitionParser());
		registerBeanDefinitionParser("rest-client", new RestClientBeanDefinitionParser());
	}
}
