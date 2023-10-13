package org.springframework.data.elasticsearch.core

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates

/**
 * Extension functions for [DocumentOperations] methods that take a Class parameter leveraging reified type parameters.
 * @author Peter-Josef Meisch
 * @since 5.2
 */

inline fun <reified T : Any> ElasticsearchOperations.indexOps(): IndexOperations =
    indexOps(T::class.java)

inline fun <reified T : Any> ElasticsearchOperations.getIndexCoordinatesFor(): IndexCoordinates =
    getIndexCoordinatesFor(T::class.java)
