/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.query

import org.springframework.context.annotation.*
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration
import org.springframework.data.elasticsearch.repository.CoroutineElasticsearchRepository
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories
import org.springframework.data.elasticsearch.utils.IndexNameProvider
import org.springframework.test.context.ContextConfiguration

/**
 * @author Peter-Josef Meisch
 * @since 5.2
 */
@ContextConfiguration(classes = [CoroutineRepositoryELCIntegrationTests.Config::class])
class CoroutineRepositoryELCIntegrationTests : CoroutineRepositoryIntegrationTests() {

    @Configuration
    @Import(ReactiveElasticsearchTemplateConfiguration::class)
    @EnableReactiveElasticsearchRepositories(
        considerNestedRepositories = true,
        includeFilters = [ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [CoroutineElasticsearchRepository::class]
        )]
    )
    open class Config {
        @Bean
        open fun indexNameProvider(): IndexNameProvider {
            return IndexNameProvider("coroutine-repository")
        }
    }
}
