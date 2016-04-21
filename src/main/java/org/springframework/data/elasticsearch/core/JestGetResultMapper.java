package org.springframework.data.elasticsearch.core;

import io.searchbox.core.DocumentResult;

/**
 * Jest specific get result mapper.
 *
 * @author Julien Roy
 */
public interface JestGetResultMapper {

	<T> T mapResult(DocumentResult response, Class<T> clazz);
}
