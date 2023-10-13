package org.springframework.data.elasticsearch.core

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Extension functions for [ReactiveDocumentOperations] methods that take a Class parameter leveraging reified type parameters.
 * @author Peter-Josef Meisch
 * @since 5.2
 */

inline fun <reified T : Any> ReactiveDocumentOperations.multiGet(query: Query): Flux<MultiGetItem<T>> = multiGet(query, T::class.java)
inline fun <reified T : Any> ReactiveDocumentOperations.multiGet(query: Query, index: IndexCoordinates): Flux<MultiGetItem<T>> = multiGet(query, T::class.java, index)

inline fun <reified T : Any> ReactiveDocumentOperations.get(id: String): Mono<T> = get(id, T::class.java)
inline fun <reified T : Any> ReactiveDocumentOperations.get(id: String, index: IndexCoordinates): Mono<T> = get(id, T::class.java, index)
inline fun <reified T : Any> ReactiveDocumentOperations.exists(id: String): Mono<Boolean> = exists(id, T::class.java)
