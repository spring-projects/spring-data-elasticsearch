package org.springframework.data.elasticsearch.core

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates

/**
 * Extension functions for [ReactiveElasticsearchOperations] methods that take a Class parameter leveraging reified type parameters.
 * @author Peter-Josef Meisch
 * @since 5.2
 */

inline fun <reified T : Any> ReactiveElasticsearchOperations.indexOps(): ReactiveIndexOperations =
    indexOps(T::class.java)

inline fun <reified T : Any> ReactiveElasticsearchOperations.getIndexCoordinatesFor(): IndexCoordinates =
    getIndexCoordinatesFor(T::class.java)
