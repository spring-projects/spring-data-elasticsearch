package org.springframework.data.elasticsearch.core.query;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface Query {

    int DEFAULT_PAGE_SIZE = 10;


    /**
     * restrict result to entries on given page. Corresponds to the 'start' and 'rows' parameter in solr
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

}
