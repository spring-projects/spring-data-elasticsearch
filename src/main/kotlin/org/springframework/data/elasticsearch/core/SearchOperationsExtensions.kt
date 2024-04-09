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
@file:Suppress("unused")

package org.springframework.data.elasticsearch.core

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery
import org.springframework.data.elasticsearch.core.query.Query

/**
 * Extension functions for [SearchOperations] methods that take a Class parameter leveraging reified type parameters.
 * @author Peter-Josef Meisch
 * @since 5.2
 */

inline fun <reified T : Any> SearchOperations.count(query: Query): Long = count(query, T::class.java)

inline fun <reified T : Any> SearchOperations.searchOne(query: Query): SearchHit<T>? = searchOne(query, T::class.java)
inline fun <reified T : Any> SearchOperations.searchOne(query: Query, index: IndexCoordinates): SearchHit<T>? =
    searchOne(query, T::class.java, index)

inline fun <reified T : Any> SearchOperations.multiSearch(queries: List<out Query>): List<SearchHits<T>> =
    multiSearch(queries, T::class.java)

inline fun <reified T : Any> SearchOperations.multiSearch(
    queries: List<out Query>,
    index: IndexCoordinates
): List<SearchHits<T>> =
    multiSearch(queries, T::class.java, index)

inline fun <reified T : Any> SearchOperations.search(query: Query): SearchHits<T> =
    search(query, T::class.java)

inline fun <reified T : Any> SearchOperations.search(query: Query, index: IndexCoordinates): SearchHits<T> =
    search(query, T::class.java, index)

inline fun <reified T : Any> SearchOperations.search(query: MoreLikeThisQuery): SearchHits<T> =
    search(query, T::class.java)

inline fun <reified T : Any> SearchOperations.search(query: MoreLikeThisQuery, index: IndexCoordinates): SearchHits<T> =
    search(query, T::class.java, index)

inline fun <reified T : Any> SearchOperations.searchForStream(query: Query): SearchHitsIterator<T> =
    searchForStream(query, T::class.java)

inline fun <reified T : Any> SearchOperations.searchForStream(
    query: Query,
    index: IndexCoordinates
): SearchHitsIterator<T> =
    searchForStream(query, T::class.java, index)
