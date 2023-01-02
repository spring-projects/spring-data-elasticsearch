/*
 * Copyright 2019-2023 the original author or authors.
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

import static org.elasticsearch.index.query.QueryBuilders.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.erhlc.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@ContextConfiguration(classes = { LogEntityERHLCIntegrationTests.Config.class })
public class LogEntityERHLCIntegrationTests extends LogEntityIntegrationTests {
	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("logentity-es7");
		}
	}

	@Override
	Query termQueryForIp(String ip) {
		return new NativeSearchQueryBuilder().withQuery(termQuery("ip", ip)).build();
	}

	@Override
	Query rangeQueryForIp(String from, String to) {
		return new NativeSearchQueryBuilder().withQuery(rangeQuery("ip").from("10.10.10.1").to("10.10.10.3")).build();

	}
}
