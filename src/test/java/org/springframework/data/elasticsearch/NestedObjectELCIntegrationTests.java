/*
 * Copyright 2022-2024 the original author or authors.
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

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;
import static org.springframework.data.elasticsearch.client.elc.Queries.*;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 */
@ContextConfiguration(classes = { NestedObjectELCIntegrationTests.Config.class })
public class NestedObjectELCIntegrationTests extends NestedObjectIntegrationTests {
	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("nestedobject");
		}

	}

	@Override
	protected @NotNull Query getNestedQuery1() {
		return NativeQuery.builder().withQuery( //
				nested(n -> n //
						.path("car") //
						.query(bool(b -> b //
								.must(termQueryAsQuery("car.name", "saturn")) //
								.must(termQueryAsQuery("car.model", "imprezza")) //
						)) //
						.scoreMode(ChildScoreMode.None) //
				) //
		)//
				.build();
	}

	@Override
	protected @NotNull Query getNestedQuery2() {
		return NativeQuery.builder().withQuery( //
				bool(b -> b //
						.must(nested(n -> n //
								.path("girlFriends") //
								.query(termQueryAsQuery("girlFriends.type", "temp")) //
								.scoreMode(ChildScoreMode.None) //
						)) //
						.must(nested(n -> n //
								.path("girlFriends.cars") //
								.query(termQueryAsQuery("girlFriends.cars.name", "Ford".toLowerCase())) //
								.scoreMode(ChildScoreMode.None) //
						)) //
				) //
		)//
				.build();
	}

	@Override
	protected @NotNull Query getNestedQuery3() {
		return NativeQuery.builder().withQuery( //
				nested(n -> n //
						.path("books") //
						.query(bool(b -> b //
								.must(termQueryAsQuery("books.name", "java")) //
						)) //
						.scoreMode(ChildScoreMode.None) //
				) //
		)//
				.build();
	}

	@Override
	protected @NotNull Query getNestedQuery4() {
		return NativeQuery.builder().withQuery( //
				nested(n -> n //
						.path("buckets") //
						.query(termQueryAsQuery("buckets.1", "test3")) //
						.scoreMode(ChildScoreMode.None) //
				) //
		)//
				.build();
	}
}
