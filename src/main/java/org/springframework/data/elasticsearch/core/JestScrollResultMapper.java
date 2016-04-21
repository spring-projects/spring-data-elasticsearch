package org.springframework.data.elasticsearch.core;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.jest.ScrollSearchResult;

/**
 * Jest specific scroll result mapper.
 *
 * @author Julien Roy
 */
public interface JestScrollResultMapper {

	<T> Page<T> mapResults(ScrollSearchResult response, Class<T> clazz);
}
