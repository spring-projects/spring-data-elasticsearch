package org.springframework.data.elasticsearch.core;

/**
 * Jest specific result mapper.
 *
 * @author Julien Roy
 */
public interface JestResultsMapper extends JestSearchResultMapper, JestGetResultMapper, JestMultiGetResultMapper, JestScrollResultMapper {

	EntityMapper getEntityMapper();
}
