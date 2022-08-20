/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import org.junit.jupiter.api.Disabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * This test class is disabled on purpose. PIT will be introduced in Spring Data Elasticsearch 5.0 where the old
 * RestHighLevelClient and the {@link org.springframework.data.elasticsearch.client.erhlc.ElasticsearchRestTemplate} are
 * deprecated. We therefore do not add new features to this implementation anymore. Furthermore we cannot copy the
 * necessary code for the reactive implementation like we did before, as point in time was introduced in Elasticsearch
 * 7.12 after the license change.
 *
 * @author Peter-Josef Meisch
 */
@Disabled
@ContextConfiguration(classes = ReactivePointInTimeERHLCIntegrationTests.Config.class)
public class ReactivePointInTimeERHLCIntegrationTests extends ReactivePointInTimeIntegrationTests {

	@Configuration
	@Import({ ReactiveElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-point-in-time-es7");
		}
	}
}
