package org.springframework.data.elasticsearch.core

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.BulkOptions
import org.springframework.data.elasticsearch.core.query.ByQueryResponse
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.data.elasticsearch.core.query.UpdateQuery

/**
 * Extension functions for [DocumentOperations] methods that take a Class parameter leveraging reified type parameters.
 * @author Peter-Josef Meisch
 * @since 5.2
 */

inline fun <reified T : Any> DocumentOperations.get(id: String): T? =
    get(id, T::class.java)

inline fun <reified T : Any> DocumentOperations.get(id: String, index: IndexCoordinates): T? =
    get(id, T::class.java, index)

inline fun <reified T : Any> DocumentOperations.multiGet(query: Query): List<MultiGetItem<T>> =
    multiGet(query, T::class.java)

inline fun <reified T : Any> DocumentOperations.multiGet(query: Query, index: IndexCoordinates): List<MultiGetItem<T>> =
    multiGet(query, T::class.java, index)

inline fun <reified T : Any> DocumentOperations.exists(id: String): Boolean = exists(id, T::class.java)

inline fun <reified T : Any> DocumentOperations.bulkIndex(queries: List<IndexQuery>): List<IndexedObjectInformation> =
    bulkIndex(queries, T::class.java)

inline fun <reified T : Any> DocumentOperations.bulkIndex(
    queries: List<IndexQuery>,
    bulkOptions: BulkOptions
): List<IndexedObjectInformation> =
    bulkIndex(queries, bulkOptions, T::class.java)

inline fun <reified T : Any> DocumentOperations.bulkUpdate(queries: List<UpdateQuery>) =
    bulkUpdate(queries, T::class.java)

inline fun <reified T : Any> DocumentOperations.delete(id: String): String =
    delete(id, T::class.java)

inline fun <reified T : Any> DocumentOperations.delete(query: Query): ByQueryResponse =
    delete(query, T::class.java)
