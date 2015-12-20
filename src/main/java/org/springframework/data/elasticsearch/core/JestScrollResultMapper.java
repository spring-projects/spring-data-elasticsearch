package org.springframework.data.elasticsearch.core;

import java.util.LinkedList;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.jest.SearchScrollResult;

/**
 * Jest specific scroll result mapper.
 *
 * @author Julien Roy
 */
public interface JestScrollResultMapper {

	<T> Page<T> mapResults(SearchScrollResult response, Class<T> clazz);
}
