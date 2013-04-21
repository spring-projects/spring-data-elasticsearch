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

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.mlt.MoreLikeThisRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.elasticsearch.action.search.SearchType.DFS_QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.client.Requests.indicesExistsRequest;
import static org.elasticsearch.client.Requests.refreshRequest;
import static org.elasticsearch.index.VersionType.EXTERNAL;
import static org.springframework.data.elasticsearch.core.MappingBuilder.buildMapping;

/**
 * ElasticsearchTemplate
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */

public class ElasticsearchTemplate implements ElasticsearchOperations {

    private Client client;
    private ElasticsearchConverter elasticsearchConverter;
    private ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ElasticsearchTemplate(Client client) {
        this(client, null);
    }

    public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter) {
        this.client = client;
        this.elasticsearchConverter = (elasticsearchConverter == null)? new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()) : elasticsearchConverter ;
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        return createIndexIfNotCreated(persistentEntity.getIndexName());
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        PutMappingRequestBuilder requestBuilder = client.admin().indices().preparePutMapping(persistentEntity.getIndexName())
                .setType(persistentEntity.getIndexType());

        try {
            XContentBuilder xContentBuilder = buildMapping(clazz, persistentEntity.getIndexType(), persistentEntity.getIdProperty().getFieldName());
            return requestBuilder.setSource(xContentBuilder).execute().actionGet().acknowledged();
        } catch (Exception e) {
            throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName() , e);
        }
    }

    @Override
    public ElasticsearchConverter getElasticsearchConverter() {
        return elasticsearchConverter;
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        GetResponse response = client.prepareGet(persistentEntity.getIndexName(), persistentEntity.getIndexType(), query.getId())
                .execute().actionGet();
        return mapResult(response.getSourceAsString(), clazz);
    }

    @Override
    public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
        Page<T> page =  queryForPage(query,clazz);
        Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found "+  page.getTotalElements() +" results");
        return page.getTotalElements() > 0? page.getContent().get(0) : null;
    }

    @Override
    public <T> T queryForObject(StringQuery query, Class<T> clazz) {
        Page<T> page =  queryForPage(query,clazz);
        Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found "+  page.getTotalElements() +" results");
        return page.getTotalElements() > 0? page.getContent().get(0) : null;
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz) {
        SearchResponse response = doSearch(prepareSearch(query,clazz), query.getQuery(), query.getFilter(), query.getElasticsearchSort());
        return mapResults(response, clazz, query.getPageable());
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, ResultsMapper<T> resultsMapper) {
        SearchResponse response = doSearch(prepareSearch(query), query.getQuery(), query.getFilter(), query.getElasticsearchSort());
        return resultsMapper.mapResults(response);
    }

    @Override
    public <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz){
        return queryForPage(query, clazz).getContent();
    }

    @Override
    public <T> List<T> queryForList(StringQuery query, Class<T> clazz){
        return queryForPage(query, clazz).getContent();
    }


    @Override
    public <T> List<String> queryForIds(SearchQuery query) {
        SearchRequestBuilder request = prepareSearch(query).setQuery(query.getQuery())
                .setNoFields();
        if(query.getFilter() != null){
            request.setFilter(query.getFilter());
        }
        SearchResponse response = request.execute().actionGet();
        return extractIds(response);
    }

    @Override
    public <T> Page<T> queryForPage(CriteriaQuery criteriaQuery, Class<T> clazz) {
        QueryBuilder query = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
        SearchResponse response =  prepareSearch(criteriaQuery,clazz)
                .setQuery(query)
                .execute().actionGet();
        return  mapResults(response, clazz, criteriaQuery.getPageable());
    }

    @Override
    public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
        SearchResponse response =  prepareSearch(query,clazz)
                .setQuery(query.getSource())
                .execute().actionGet();
        return  mapResults(response, clazz, query.getPageable());
    }

    @Override
    public <T> long count(SearchQuery query, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        CountRequestBuilder countRequestBuilder = client.prepareCount(persistentEntity.getIndexName())
                .setTypes(persistentEntity.getIndexType());
        if(query.getQuery() != null){
            countRequestBuilder.setQuery(query.getQuery());
        }
        return countRequestBuilder.execute().actionGet().count();
    }

    @Override
    public String index(IndexQuery query) {
        return  prepareIndex(query)
                .execute()
                .actionGet().getId();
    }

    @Override
    public void bulkIndex(List<IndexQuery> queries) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for(IndexQuery query : queries){
            bulkRequest.add(prepareIndex(query));
        }
        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            Map<String, String> failedDocuments = new HashMap<String, String>();
            for (BulkItemResponse item : bulkResponse.items()) {
                if (item.failed())
                    failedDocuments.put(item.getId(), item.failureMessage());
            }
            throw new ElasticsearchException("Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages [" + failedDocuments+"]", failedDocuments);
        }
    }

    @Override
    public <T> boolean indexExists(Class<T> clazz){
        return indexExists(getPersistentEntityFor(clazz).getIndexName());
    }

    @Override
    public <T> boolean deleteIndex(Class<T> clazz){
        String indexName = getPersistentEntityFor(clazz).getIndexName();
        if(indexExists(indexName)){
            return client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet().acknowledged();
        }
        return false;
    }

    @Override
    public String delete(String indexName, String type, String id) {
        return client.prepareDelete(indexName, type, id)
                .execute().actionGet().getId();
    }

    @Override
    public <T> String delete(Class<T> clazz, String id) {
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        return delete(persistentEntity.getIndexName(), persistentEntity.getIndexType(), id);
    }

    @Override
    public <T> void delete(DeleteQuery deleteQuery, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        client.prepareDeleteByQuery(persistentEntity.getIndexName())
                .setTypes(persistentEntity.getIndexType())
                .setQuery(deleteQuery.getQuery())
                .execute().actionGet();
    }

    @Override
    public String scan(SearchQuery searchQuery, long scrollTimeInMillis, boolean noFields) {
        Assert.notNull(searchQuery.getIndices(), "No index defined for Query");
        Assert.notNull(searchQuery.getTypes(), "No type define for Query");
        Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scan & scroll");

        SearchRequestBuilder requestBuilder = client.prepareSearch(toArray(searchQuery.getIndices()))
                .setSearchType(SCAN)
                .setQuery(searchQuery.getQuery())
                .setTypes(toArray(searchQuery.getTypes()))
                .setScroll(TimeValue.timeValueMillis(scrollTimeInMillis))
                .setFrom(0)
                .setSize(searchQuery.getPageable().getPageSize());

        if(searchQuery.getFilter() != null){
            requestBuilder.setFilter(searchQuery.getFilter());
        }

        if(noFields){
            requestBuilder.setNoFields();
        }
        return requestBuilder.execute().actionGet().getScrollId();
    }

    @Override
    public <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, ResultsMapper<T> resultsMapper) {
        SearchResponse response = client.prepareSearchScroll(scrollId)
                .setScroll(TimeValue.timeValueMillis(scrollTimeInMillis))
                .execute().actionGet();
        return resultsMapper.mapResults(response);
    }

    @Override
    public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {
        int startRecord = 0;
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        String indexName = isNotBlank(query.getIndexName())? query.getIndexName(): persistentEntity.getIndexName();
        String type = isNotBlank(query.getType())? query.getType() : persistentEntity.getIndexType();

        Assert.notNull(indexName,"No 'indexName' defined for MoreLikeThisQuery");
        Assert.notNull(type, "No 'type' defined for MoreLikeThisQuery");
        Assert.notNull(query.getId(), "No document id defined for MoreLikeThisQuery");

        MoreLikeThisRequestBuilder requestBuilder =
                client.prepareMoreLikeThis(indexName,type, query.getId());

        if(query.getPageable() != null){
            startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
            requestBuilder.setSearchSize(query.getPageable().getPageSize());
        }
        requestBuilder.setSearchFrom(startRecord);

        if(isNotEmpty(query.getSearchIndices())){
            requestBuilder.setSearchIndices(toArray(query.getSearchIndices()));
        }
        if(isNotEmpty(query.getSearchTypes())){
            requestBuilder.setSearchTypes(toArray(query.getSearchTypes()));
        }
        if(isNotEmpty(query.getFields())){
            requestBuilder.setField(toArray(query.getFields()));
        }
        if(isNotBlank(query.getRouting())){
            requestBuilder.setRouting(query.getRouting());
        }
        if(query.getPercentTermsToMatch() != null){
            requestBuilder.setPercentTermsToMatch(query.getPercentTermsToMatch());
        }
        if(query.getMinTermFreq() != null){
            requestBuilder.setMinTermFreq(query.getMinTermFreq());
        }
        if(query.getMaxQueryTerms() != null){
            requestBuilder.maxQueryTerms(query.getMaxQueryTerms());
        }
        if(isNotEmpty(query.getStopWords())){
            requestBuilder.setStopWords(toArray(query.getStopWords()));
        }
        if(query.getMinDocFreq() != null){
            requestBuilder.setMinDocFreq(query.getMinDocFreq());
        }
        if(query.getMaxDocFreq() != null){
            requestBuilder.setMaxDocFreq(query.getMaxDocFreq());
        }
        if(query.getMinWordLen() != null){
            requestBuilder.setMinWordLen(query.getMinWordLen());
        }
        if(query.getMaxWordLen() != null){
            requestBuilder.setMaxWordLen(query.getMaxWordLen());
        }
        if(query.getBoostTerms() != null){
            requestBuilder.setBoostTerms(query.getBoostTerms());
        }

        SearchResponse response = requestBuilder.execute().actionGet();
        return mapResults(response, clazz, query.getPageable());
    }

    private SearchResponse doSearch(SearchRequestBuilder searchRequest, QueryBuilder query,  FilterBuilder filter, SortBuilder sortBuilder){
        if(filter != null){
            searchRequest.setFilter(filter);
        }

        if(sortBuilder != null){
            searchRequest.addSort(sortBuilder);
        }

        return searchRequest.setQuery(query).execute().actionGet();
    }

    private boolean createIndexIfNotCreated(String indexName) {
        return  indexExists(indexName) ||  createIndex(indexName);
    }

    private boolean indexExists(String indexName) {
        return client.admin()
                .indices()
                .exists(indicesExistsRequest(indexName)).actionGet().exists();
    }

    private boolean createIndex(String indexName) {
        return client.admin().indices().create(Requests.createIndexRequest(indexName).
                settings(new MapBuilder<String, String>().put("index.refresh_interval", "-1").map())).actionGet().acknowledged();
    }

    private <T> SearchRequestBuilder prepareSearch(Query query, Class<T> clazz){
        if(query.getIndices().isEmpty()){
            query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
        }
        if(query.getTypes().isEmpty()){
            query.addTypes(retrieveTypeFromPersistentEntity(clazz));
        }
        return prepareSearch(query);
    }

    private SearchRequestBuilder prepareSearch(Query query){
        Assert.notNull(query.getIndices(), "No index defined for Query");
        Assert.notNull(query.getTypes(), "No type defined for Query");

        int startRecord = 0;
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(toArray(query.getIndices()))
                .setSearchType(DFS_QUERY_THEN_FETCH)
                .setTypes(toArray(query.getTypes()));

        if(query.getPageable() != null){
            startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
            searchRequestBuilder.setSize(query.getPageable().getPageSize());
        }
        searchRequestBuilder.setFrom(startRecord);


        if(!query.getFields().isEmpty()){
            searchRequestBuilder.addFields(toArray(query.getFields()));
        }

        if(query.getSort() != null){
            for(Sort.Order order : query.getSort()){
                searchRequestBuilder.addSort(order.getProperty(), order.getDirection() == Sort.Direction.DESC? SortOrder.DESC : SortOrder.ASC);
            }
        }
        return searchRequestBuilder;
    }

    private IndexRequestBuilder prepareIndex(IndexQuery query){
        try {
            String indexName = isBlank(query.getIndexName())?
                    retrieveIndexNameFromPersistentEntity(query.getObject().getClass())[0] : query.getIndexName();
            String type = isBlank(query.getType())?
                    retrieveTypeFromPersistentEntity(query.getObject().getClass())[0] : query.getType();

            IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName,type,query.getId())
                    .setSource(objectMapper.writeValueAsString(query.getObject()));

            if(query.getVersion() != null){
                indexRequestBuilder.setVersion(query.getVersion());
                indexRequestBuilder.setVersionType(EXTERNAL);
            }
            return indexRequestBuilder;
        } catch (IOException e) {
            throw new ElasticsearchException("failed to index the document [id: " + query.getId() +"]",e);
        }
    }

    public void refresh(String indexName, boolean waitForOperation) {
        client.admin().indices()
                .refresh(refreshRequest(indexName).waitForOperations(waitForOperation)).actionGet();
    }

    public <T> void refresh(Class<T> clazz, boolean waitForOperation) {
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        client.admin().indices()
                .refresh(refreshRequest(persistentEntity.getIndexName()).waitForOperations(waitForOperation)).actionGet();
    }

    private ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz){
        Assert.isTrue(clazz.isAnnotationPresent(Document.class), "Unable to identify index name. " +
                clazz.getSimpleName() + " is not a Document. Make sure the document class is annotated with @Document(indexName=\"foo\")");
        return elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
    }

    private String[] retrieveIndexNameFromPersistentEntity(Class clazz){
        return new String[]{getPersistentEntityFor(clazz).getIndexName()};
    }

    private String[] retrieveTypeFromPersistentEntity(Class clazz){
        return new String[]{getPersistentEntityFor(clazz).getIndexType()};
    }

    private <T> Page<T> mapResults(SearchResponse response, final Class<T> elementType,final Pageable pageable){
        ResultsMapper<T> resultsMapper =  new ResultsMapper<T>(){
            @Override
            public Page<T> mapResults(SearchResponse response) {
                long totalHits =  response.getHits().totalHits();
                List<T> results = new ArrayList<T>();
                for (SearchHit hit : response.getHits()) {
                    if (hit != null) {
                        results.add(mapResult(hit.sourceAsString(), elementType));
                    }
                }
                return new PageImpl<T>(results, pageable, totalHits);
            }
        };
        return resultsMapper.mapResults(response);
    }

    private List<String> extractIds(SearchResponse response){
        List<String> ids = new ArrayList<String>();
        for (SearchHit hit : response.getHits()) {
            if (hit != null) {
                ids.add(hit.getId());
            }
        }
        return ids;
    }

    private <T> T mapResult(String source, Class<T> clazz){
        if(isBlank(source)){
            return null;
        }
        try {
            return objectMapper.readValue(source, clazz);
        } catch (IOException e) {
            throw new ElasticsearchException("failed to map source [ " + source + "] to class " + clazz.getSimpleName() , e);
        }
    }

    private static String[] toArray(List<String> values){
        String[] valuesAsArray = new String[values.size()];
        return values.toArray(valuesAsArray);

    }

}

