/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;


import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.query.*;

import java.util.List;

/**
 * ElasticsearchOperations
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public interface ElasticsearchOperations {


    /**
     * @return Converter in use
     */
    ElasticsearchConverter getElasticsearchConverter();

    /**
     * Create an index for a class
     * @param clazz
     * @param <T>
     */
    <T> boolean createIndex(Class<T> clazz);

    /**
     * Create mapping for a class
     * @param clazz
     * @param <T>
     */
    <T> boolean putMapping(Class<T> clazz);


    /**
     * Execute the query against elasticsearch and return the first returned object
     *
     * @param query
     * @param clazz
     * @return the first matching object
     */
    <T> T queryForObject(GetQuery query, Class<T> clazz);

    /**
     * Execute the query against elasticsearch and return the first returned object
     *
     * @param query
     * @param clazz
     * @return the first matching object
     */
    <T> T queryForObject(CriteriaQuery query, Class<T> clazz);


    /**
     * Execute the query against elasticsearch and return the first returned object
     *
     * @param query
     * @param clazz
     * @return the first matching object
     */
    <T> T queryForObject(StringQuery query, Class<T> clazz);


    /**
     * Execute the query against elasticsearch and return result as {@link Page}
     *
     * @param query
     * @param clazz
     * @return
     */
    <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz);


    /**
     * Execute the query against elasticsearch and return result as {@link Page}
     *
     * @param query
     * @param resultsMapper
     * @return
     */
    <T> Page<T> queryForPage(SearchQuery query, ResultsMapper<T> resultsMapper);


    /**
     * Execute the query against elasticsearch and return result as {@link Page}
     *
     * @param query
     * @param clazz
     * @return
     */
    <T> Page<T> queryForPage(CriteriaQuery query, Class<T> clazz);


    /**
     * Execute the query against elasticsearch and return result as {@link Page}
     *
     * @param query
     * @param clazz
     * @return
     */
    <T> Page<T> queryForPage(StringQuery query, Class<T> clazz);

    /**
     * Execute the criteria query against elasticsearch and return result as {@link List}
     *
     * @param query
     * @param clazz
     * @param <T>
     * @return
     */
    <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz);

    /**
     * Execute the string query against elasticsearch and return result as {@link List}
     *
     * @param query
     * @param clazz
     * @param <T>
     * @return
     */
    <T> List<T> queryForList(StringQuery query, Class<T> clazz);

    /**
     * Execute the query against elasticsearch and return ids
     *
     * @param query
     * @return
     */
    <T> List<String> queryForIds(SearchQuery query);

    /**
     * return number of elements found by for given query
     *
     * @param query
     * @param clazz
     * @return
     */
    <T> long count(SearchQuery query, Class<T> clazz);

    /**
     * Index an object. Will do save or update
     *
     * @param query
     * @return returns the document id
     */
     String index(IndexQuery query);

    /**
     * Bulk index all objects. Will do save or update
     *
     * @param queries
     */
     void bulkIndex(List<IndexQuery> queries);

    /**
     * Delete the one object with provided id
     *
     * @param indexName
     * @param type
     * @param id
     * @return documentId of the document deleted
     */
    String delete(String indexName, String type, String id);

    /**
     * Delete the one object with provided id
     *
     * @param clazz
     * @param id
     * @return documentId of the document deleted
     */
    <T> String delete(Class<T> clazz, String id);

    /**
     * Delete all records matching the query
     * @param clazz
     * @param query
     */
    <T> void delete(DeleteQuery query, Class<T> clazz);

    /**
     * Deletes an index for given entity
     * @param clazz
     * @param <T>
     * @return
     */
    <T> boolean deleteIndex(Class<T> clazz);

    /**
     * check if index is exists
     * @param clazz
     * @param <T>
     * @return
     */
    <T> boolean indexExists(Class<T> clazz);

    /**
     * refresh the index
     * @param indexName
     * @param waitForOperation
     */
    void refresh(String indexName,boolean waitForOperation);

    /**
     * refresh the index
     * @param clazz
     * @param waitForOperation
     */
    <T> void refresh(Class<T> clazz,boolean waitForOperation);

    /**
     * Returns scroll id for scan query
     * @param query
     * @param scrollTimeInMillis
     * @param noFields
     * @return
     */
     String scan(SearchQuery query, long scrollTimeInMillis, boolean noFields);

    /**
     * Scrolls the results for give scroll id
     * @param scrollId
     * @param scrollTimeInMillis
     * @param resultsMapper
     * @param <T>
     * @return
     */
    <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, ResultsMapper<T> resultsMapper);

    /**
     *  more like this query to search for documents that are "like" a specific document.
     * @param query
     * @param clazz
     * @param <T>
     * @return
     */
    <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz);

}
