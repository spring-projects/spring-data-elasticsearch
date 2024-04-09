package org.springframework.data.elasticsearch.repository.query

import kotlinx.coroutines.flow.Flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext
import org.springframework.data.elasticsearch.repository.CoroutineElasticsearchRepository
import org.springframework.data.projection.SpelAwareProxyProjectionFactory
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata
import kotlin.coroutines.Continuation

/**
 * a@author Peter-Josef Meisch
 * @since 5.2
 */
class ReactiveElasticsearchQueryMethodCoroutineUnitTests {

    private val projectionFactory = SpelAwareProxyProjectionFactory()

    interface PersonRepository : CoroutineElasticsearchRepository<Person, String> {

        suspend fun findSuspendAllByName(): Flow<Person>

        fun findAllByName(): Flow<Person>

        suspend fun findSuspendByName(): List<Person>
    }

    @Test // #2545
    internal fun `should consider methods returning Flow as collection queries`() {

        val method = PersonRepository::class.java.getMethod("findAllByName")
        val queryMethod = ReactiveElasticsearchQueryMethod(
            method,
            DefaultRepositoryMetadata(PersonRepository::class.java),
            projectionFactory,
            SimpleElasticsearchMappingContext()
        )

        assertThat(queryMethod.isCollectionQuery).isTrue()
    }

    @Test // #2545
    internal fun `should consider suspended methods returning Flow as collection queries`() {

        val method = PersonRepository::class.java.getMethod("findSuspendAllByName", Continuation::class.java)
        val queryMethod = ReactiveElasticsearchQueryMethod(
            method,
            DefaultRepositoryMetadata(PersonRepository::class.java),
            projectionFactory,
            SimpleElasticsearchMappingContext()
        )

        assertThat(queryMethod.isCollectionQuery).isTrue()
    }

    @Test // #2545
    internal fun `should consider suspended methods returning List as collection queries`() {

        val method = PersonRepository::class.java.getMethod("findSuspendByName", Continuation::class.java)
        val queryMethod = ReactiveElasticsearchQueryMethod(
            method,
            DefaultRepositoryMetadata(PersonRepository::class.java),
            projectionFactory,
            SimpleElasticsearchMappingContext()
        )

        assertThat(queryMethod.isCollectionQuery).isTrue()
    }

    data class Person(
        @Id val id: String?,
        @Field val name: String?
    )
}
