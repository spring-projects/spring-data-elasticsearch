package org.springframework.data.elasticsearch.core;

import java.util.LinkedList;

import org.springframework.data.elasticsearch.core.jest.MultiDocumentResult;

/**
 * Jest specific multi get result mapper.
 *
 * @author Julien Roy
 */
public interface JestMultiGetResultMapper {

	<T> LinkedList<T> mapResults(MultiDocumentResult response, Class<T> clazz);
}
