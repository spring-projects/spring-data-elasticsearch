package org.springframework.data.elasticsearch.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.elasticsearch.repository.config.ElasticsearchRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryBeanDefinitionParser;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;


public class ElasticsearchNamespaceHandler extends NamespaceHandlerSupport{

    @Override
    public void init() {
        RepositoryConfigurationExtension extension = new ElasticsearchRepositoryConfigExtension();
		RepositoryBeanDefinitionParser parser = new RepositoryBeanDefinitionParser(extension);

		registerBeanDefinitionParser("repositories", parser);
		registerBeanDefinitionParser("node-client", new NodeClientBeanDefinitionParser());
		registerBeanDefinitionParser("transport-client", new TransportClientBeanDefinitionParser());
    }
}
