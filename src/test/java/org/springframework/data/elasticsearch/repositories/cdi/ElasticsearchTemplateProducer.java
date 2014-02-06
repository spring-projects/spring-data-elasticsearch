/*
 * Copyright 2014 the original author or authors.
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

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.xml.sax.SAXException;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * @author Mohsin Husen
 */
@ApplicationScoped
class ElasticsearchTemplateProducer {

 	@Produces
	public ElasticsearchOperations createElasticsearchTemplate() throws IOException, ParserConfigurationException, SAXException {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder().put("http.enabled","false" );
        NodeClient client = (NodeClient) nodeBuilder().settings(settings).clusterName("testClusterForCDI").local(true).node()
                .client();
		return new ElasticsearchTemplate(client);
	}

	@PreDestroy
	public void shutdown() {
		// remove everything to avoid conflicts with other tests in case server not shut down properly
		deleteAll();
	}

	private void deleteAll() {
		ElasticsearchOperations template;
		try {
			template = createElasticsearchTemplate();
            DeleteQuery deleteQuery = new DeleteQuery();
            deleteQuery.setQuery(QueryBuilders.matchAllQuery());
            deleteQuery.setIndex("test-product-index");
            deleteQuery.setType("test-product-type");
			template.delete(deleteQuery);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

	}

}
