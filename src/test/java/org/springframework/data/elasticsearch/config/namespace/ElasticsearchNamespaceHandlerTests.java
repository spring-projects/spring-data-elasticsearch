/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.config.namespace;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.RestClientFactoryBean;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Don Wellington
 * @author Peter-Josef Meisch
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("namespace.xml")
public class ElasticsearchNamespaceHandlerTests {

	@Autowired private ApplicationContext context;

	@Test
	public void shouldCreateTransportClient() {

		assertThat(context.getBean(TransportClientFactoryBean.class)).isNotNull();
		assertThat(context.getBean(TransportClientFactoryBean.class)).isInstanceOf(TransportClientFactoryBean.class);
	}

	@Test
	public void shouldCreateRepository() {

		assertThat(context.getBean(TransportClientFactoryBean.class)).isNotNull();
		assertThat(context.getBean(CreateIndexFalseRepository.class)).isInstanceOf(CreateIndexFalseRepository.class);
	}

	@Test
	public void shouldCreateRestClient() {

		assertThat(context.getBean(RestClientFactoryBean.class)).isNotNull();
		assertThat(context.getBean(RestClientFactoryBean.class)).isInstanceOf(RestClientFactoryBean.class);
	}

	@Document(indexName = "test-index-config-namespace", type = "test-type", createIndex = false)
	static class CreateIndexFalseEntity {

		@Id private String id;
	}

	interface CreateIndexFalseRepository extends ElasticsearchRepository<CreateIndexFalseEntity, String> {}
}
