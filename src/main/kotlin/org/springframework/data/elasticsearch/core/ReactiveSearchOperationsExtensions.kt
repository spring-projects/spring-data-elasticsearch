@file:Suppress("unused")

package org.springframework.data.elasticsearch.core

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.data.elasticsearch.core.suggest.response.Suggest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Extension functions for [SearchOperations] methods that take a Class parameter leveraging reified type parameters.
 * @author Peter-Josef Meisch
 * @since 5.2
 */

inline fun <reified T : Any> ReactiveSearchOperations.count(): Mono<Long> = count(T::class.java)
inline fun <reified T : Any> ReactiveSearchOperations.count(query: Query): Mono<Long> = count(query, T::class.java)
inline fun <reified T : Any> ReactiveSearchOperations.count(query: Query, index: IndexCoordinates): Mono<Long> =
    count(query, T::class.java, index)

inline fun <reified T : Any> ReactiveSearchOperations.search(query: Query): Flux<SearchHit<T>> =
    search(query, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.search(
    query: Query,
    index: IndexCoordinates
): Flux<SearchHit<T>> = search(query, T::class.java, index)

inline fun <reified T : Any> ReactiveSearchOperations.searchForPage(query: Query): Mono<SearchPage<T>> =
    searchForPage(query, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.searchForPage(
    query: Query,
    index: IndexCoordinates
): Mono<SearchPage<T>> = searchForPage(query, T::class.java, index)

inline fun <reified T : Any> ReactiveSearchOperations.searchForHits(query: Query): Mono<ReactiveSearchHits<T>> =
    searchForHits(query, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.aggregate(query: Query): Flux<out AggregationContainer<*>> =
    aggregate(query, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.aggregate(
    query: Query,
    index: IndexCoordinates
): Flux<out AggregationContainer<*>> = aggregate(query, T::class.java, index)

inline fun <reified T : Any> ReactiveSearchOperations.suggest(query: Query): Mono<Suggest> =
    suggest(query, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.suggest(query: Query, index: IndexCoordinates): Mono<Suggest> =
    suggest(query, T::class.java, index)
