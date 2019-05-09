/*
 * (c) Copyright 2019 codecentric AG
 */
package org.springframework.data.elasticsearch.utils;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * @author Peter-Josef Meisch
 */
final public class IndexInitializer {

	private IndexInitializer() {}

	public static void init(ElasticsearchOperations operations, Class<?> clazz) {
		operations.deleteIndex(clazz);
		operations.createIndex(clazz);
		operations.putMapping(clazz);
		operations.refresh(clazz);
	}
}
