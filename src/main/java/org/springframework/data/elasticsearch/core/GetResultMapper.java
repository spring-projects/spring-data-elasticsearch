package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author Artur Konczak
 */
public interface GetResultMapper {

    <T> T mapResult(GetResponse response, Class<T> clazz);

}
