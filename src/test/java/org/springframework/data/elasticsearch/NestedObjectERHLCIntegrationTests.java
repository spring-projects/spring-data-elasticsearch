/*
 * Copyright 2022-2023 the original author or authors.
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

import static org.elasticsearch.index.query.QueryBuilders.*;

import org.apache.lucene.search.join.ScoreMode;
import org.jetbrains.annotations.NotNull;
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
@ContextConfiguration(classes = { NestedObjectERHLCIntegrationTests.Config.class })
public class NestedObjectERHLCIntegrationTests extends NestedObjectIntegrationTests {
	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("nestedobject-es7");
		}

	}

	@NotNull
	protected Query getNestedQuery1() {
		return new NativeSearchQueryBuilder().withQuery( //
				nestedQuery("car", //
						boolQuery() //
								.must(termQuery("car.name", "saturn")) //
								.must(termQuery("car.model", "imprezza")), //
						ScoreMode.None)) //
				.build();
	}

	@NotNull
	protected Query getNestedQuery2() {
		return new NativeSearchQueryBuilder().withQuery( //
				boolQuery() //
						.must(nestedQuery("girlFriends", //
								termQuery("girlFriends.type", "temp"), //
								ScoreMode.None)) //
						.must(nestedQuery("girlFriends.cars", //
								termQuery("girlFriends.cars.name", "Ford".toLowerCase()), //
								ScoreMode.None))) //
				.build();
	}

	@NotNull
	protected Query getNestedQuery3() {
		return new NativeSearchQueryBuilder().withQuery( //
				nestedQuery("books", //
						boolQuery() //
								.must(termQuery("books.name", "java")), //
						ScoreMode.None))//
				.build();
	}

	@NotNull
	protected Query getNestedQuery4() {
		return new NativeSearchQueryBuilder().withQuery( //
				nestedQuery("buckets", //
						termQuery("buckets.1", "test3"), //
						ScoreMode.None)) //
				.build();
	}

}
