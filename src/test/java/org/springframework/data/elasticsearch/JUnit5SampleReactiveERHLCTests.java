/*
 * Copyright 2020-2023 the original author or authors.
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
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * class demonstrating the setup of a JUnit 5 test in Spring Data Elasticsearch that uses the reactive rest client. The
 * ContextConfiguration must include the {@link ElasticsearchRestTemplateConfiguration} class.
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveElasticsearchRestTemplateConfiguration.class })
@DisplayName("a sample JUnit 5 test with reactive rest client")
public class JUnit5SampleReactiveERHLCTests {

	@Autowired private ReactiveElasticsearchOperations elasticsearchOperations;

	@Test
	@DisplayName("should have a ReactiveElasticsearchOperations")
	void shouldHaveARestTemplate() {
		assertThat(elasticsearchOperations).isNotNull().isInstanceOf(ReactiveElasticsearchOperations.class);
	}
}
