/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.elasticsearch;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.backend.elasticsearch7.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * class demonstrating the setup of a JUnit 5 test in Spring Data Elasticsearch that uses the transport client. The
 * ContextConfiguration must include the {@link ElasticsearchTemplateConfiguration} class
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
@DisplayName("a sample JUnit 5 test with transport client")
public class JUnit5SampleTransportClientBasedTests {

	@Autowired private ElasticsearchOperations elasticsearchOperations;

	@Test
	@DisplayName("should have a ElasticsearchTemplate")
	void shouldHaveATemplate() {
		assertThat(elasticsearchOperations).isNotNull().isInstanceOf(ElasticsearchTemplate.class);
	}
}
