package org.springframework.data.elasticsearch.core.query;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface Query {

    int DEFAULT_PAGE_SIZE = 10;


    /**
     * restrict result to entries on given page. Corresponds to the 'start' and 'rows' parameter in elasticsearch
     *
     * @param pageable
     * @return
     */
    <T extends Query> T setPageable(Pageable pageable);



    /**
     * Get filter queries if defined
     *
     * @return
     */
    //List<FilterQuery> getFilterQueries();

    /**
     * Get page settings if defined
     *
     * @return
     */
    Pageable getPageable();



    /**
     * Add {@link org.springframework.data.domain.Sort} to query
     *
     * @param sort
     * @return
     */
    <T extends Query> T addSort(Sort sort);

    /**
     * @return null if not set
     */
    Sort getSort();


    /**
     * Get Indices to be searched
     * @return
     */
    List<String> getIndices();


    /**
     * Add Indices to be added as part of search request
     * @param indices
     */
    void addIndices(String...indices);

    /**
     * Add types to be searched
     * @param types
     */
    void addTypes(String...types);

    /**
     * Get types to be searched
     * @return
     */
    List<String> getTypes();

    /**
     * Add fields to be added as part of search request
     * @param fields
     */
    void addFields(String...fields);


    /**
     * Get fields to be returned as part of search request
     * @return
     */
    List<String> getFields();
}
