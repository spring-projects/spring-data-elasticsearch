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

import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.EnabledIfOpensearch;
import org.springframework.data.elasticsearch.client.orhlc.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 * @since 5.0
 */
@EnabledIfOpensearch
@ContextConfiguration(classes = { SearchAsYouTypeORHLCIntegrationTests.Config.class })
public class SearchAsYouTypeORHLCIntegrationTests extends SearchAsYouTypeIntegrationTests {

	@Configuration
	@Import({ OpensearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("search-as-you-type-os");
		}
	}

	@Override
	protected Query buildMultiMatchQuery(String text) {
		return new NativeSearchQuery(QueryBuilders.multiMatchQuery(text, //
				"suggest", "suggest._2gram", "suggest._3gram", "suggest._4gram").type(MultiMatchQueryBuilder.Type.BOOL_PREFIX));
	}

}
