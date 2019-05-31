/*
 * (c) Copyright 2019 codecentric AG
 */
package org.springframework.data.elasticsearch.utils;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Utility to initialize indexes.
 *
 * @author Peter-Josef Meisch
 */
public class IndexInitializer {

	private IndexInitializer() {}

	/**
	 * Initialize a fresh index with mappings for {@link Class}. Drops the index if it exists before creation.
	 *
	 * @param operations
	 * @param clazz
	 */
	public static void init(ElasticsearchOperations operations, Class<?> clazz) {

		operations.deleteIndex(clazz);
		operations.createIndex(clazz);
		operations.putMapping(clazz);
		operations.refresh(clazz);
	}
}
