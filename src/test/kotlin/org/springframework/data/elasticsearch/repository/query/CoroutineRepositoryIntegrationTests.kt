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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest
import org.springframework.data.elasticsearch.repository.CoroutineElasticsearchRepository
import org.springframework.data.elasticsearch.utils.IndexNameProvider

/**
 * Integrationtests for the different methods a [org.springframework.data.elasticsearch.repository.CoroutineElasticsearchRepository] can have.
 * @author Peter-Josef Meisch
 * @since 5.2
 */
@SpringIntegrationTest
abstract class CoroutineRepositoryIntegrationTests {

    @Autowired
    lateinit var indexNameProvider: IndexNameProvider

    @Autowired
    lateinit var operations: ReactiveElasticsearchOperations

    @Autowired
    lateinit var repository: CoroutineEntityRepository

    val entities = listOf(
        Entity("1", "test"),
        Entity("2", "test"),
    )

    @BeforeEach
    fun setUp() = runTest {
        repository.saveAll(entities).last()
    }

    @Test
    @Order(Int.MAX_VALUE)
    fun cleanup() {
        operations.indexOps(IndexCoordinates.of(indexNameProvider.prefix + "*")).delete().block()
    }

    @Test
    fun `should instantiate repository`() = runTest {
        assertThat(repository).isNotNull()
    }

    @Test
    fun `should run with method returning a list of entities`() = runTest {

        val result = repository.searchByText("test")

        assertThat(result).containsExactlyInAnyOrderElementsOf(entities)
    }

    @Test
    fun `should run with method returning a flow of entities`() = runTest {

        val result = repository.findByText("test").toList(mutableListOf())

        assertThat(result).containsExactlyInAnyOrderElementsOf(entities)
    }

    @Test
    fun `should run with method returning a flow of SearchHit`() = runTest {

        val result = repository.queryByText("test").toList(mutableListOf())

        assertThat(result.map { it.content }).containsExactlyInAnyOrderElementsOf(entities)
    }

    @Document(indexName = "#{@indexNameProvider.indexName()}")
    data class Entity(
        @Id val id: String?,
        @Field(type = FieldType.Text) val text: String?,
    )

    interface CoroutineEntityRepository : CoroutineElasticsearchRepository<Entity, String> {

        suspend fun searchByText(text: String): List<Entity>
        suspend fun findByText(text: String): Flow<Entity>
        suspend fun queryByText(text: String): Flow<SearchHit<Entity>>
    }
}
