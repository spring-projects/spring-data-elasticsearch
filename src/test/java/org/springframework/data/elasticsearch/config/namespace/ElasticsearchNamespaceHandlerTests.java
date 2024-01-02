/*
 * Copyright 2013-2024 the original author or authors.
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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClientFactoryBean;
import org.springframework.data.elasticsearch.junit.jupiter.Tags;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Don Wellington
 * @author Peter-Josef Meisch
 */

@ExtendWith(SpringExtension.class)
@ContextConfiguration("namespace.xml")
@Tag(Tags.INTEGRATION_TEST)
public class ElasticsearchNamespaceHandlerTests {

	@Autowired private ApplicationContext context;

	@Test
	public void shouldCreateRepository() {
		assertThat(context.getBean(CreateIndexFalseRepository.class)).isInstanceOf(CreateIndexFalseRepository.class);
	}

	@Test
	public void shouldCreateElasticsearchClient() {

		assertThat(context.getBean(ElasticsearchClientFactoryBean.class)).isNotNull();
		assertThat(context.getBean(ElasticsearchClientFactoryBean.class))
				.isInstanceOf(ElasticsearchClientFactoryBean.class);
	}

	@Document(indexName = "test-index-config-namespace", createIndex = false)
	static class CreateIndexFalseEntity {

		@Id private String id;
	}

	interface CreateIndexFalseRepository extends ElasticsearchRepository<CreateIndexFalseEntity, String> {}
}
