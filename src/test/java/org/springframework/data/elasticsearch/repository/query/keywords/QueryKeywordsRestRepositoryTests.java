/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.query.keywords;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.RestElasticsearchTestConfiguration;
import org.springframework.data.elasticsearch.TestNodeResource;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link QueryKeywordsTests} using a Repository backed by an ElasticsearchRestTemplate.
 *
 * @author Peter-Josef Meisch
 */
@ContextConfiguration(classes = { QueryKeywordsRestRepositoryTests.class, RestElasticsearchTestConfiguration.class })
@Configuration
@EnableElasticsearchRepositories(considerNestedRepositories = true)
public class QueryKeywordsRestRepositoryTests extends QueryKeywordsTests {

	@ClassRule public static TestNodeResource testNodeResource = new TestNodeResource();
}
