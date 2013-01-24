package org.springframework.data.elasticsearch.core;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.elasticsearch.action.search.SearchType.DFS_QUERY_THEN_FETCH;
import static org.elasticsearch.client.Requests.indicesExistsRequest;
import static org.elasticsearch.client.Requests.refreshRequest;


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
        SearchRequestBuilder searchRequestBuilder = prepareSearch(query,clazz);
        if(query.getElasticsearchFilter() != null){
            searchRequestBuilder.setFilter(query.getElasticsearchFilter());
        }
        SearchResponse response = searchRequestBuilder.setQuery(query.getElasticsearchQuery()).execute().actionGet();
        return  mapResults(response, clazz, query.getPageable());
    }

    @Override
    public <T> Page<T> queryForPage(CriteriaQuery query, Class<T> clazz) {
        QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(query.getCriteria());
        SearchResponse response =  prepareSearch(query,clazz)
                .setQuery(elasticsearchQuery)
                .execute().actionGet();
        return  mapResults(response, clazz, query.getPageable());
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
        if(query.getElasticsearchQuery() != null){
            countRequestBuilder.setQuery(query.getElasticsearchQuery());
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
    public <T> void delete(DeleteQuery query, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        client.prepareDeleteByQuery(persistentEntity.getIndexName())
                .setTypes(persistentEntity.getIndexType())
                .setQuery(query.getElasticsearchQuery())
                .execute().actionGet();
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
        int startRecord=0;
        if(query.getPageable() != null){
            startRecord = ((query.getPageable().getPageNumber() - 1) * query.getPageable().getPageSize());
        }
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(persistentEntity.getIndexName())
                .setSearchType(DFS_QUERY_THEN_FETCH)
                .setTypes(persistentEntity.getIndexType())
                .setFrom(startRecord < 0 ? 0 : startRecord)
                .setSize(query.getPageable() != null ? query.getPageable().getPageSize() : 10);

        if(query.getSort() != null){
            for(Sort.Order order : query.getSort()){
                searchRequestBuilder.addSort(order.getProperty(), order.getDirection() == Sort.Direction.DESC? SortOrder.DESC : SortOrder.ASC);
            }
        }
        return searchRequestBuilder;
    }

    private IndexRequestBuilder prepareIndex(IndexQuery query){
        try {
            ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(query.getObject().getClass());
            return client.prepareIndex(persistentEntity.getIndexName(), persistentEntity.getIndexType(), query.getId())
                    .setSource(objectMapper.writeValueAsString(query.getObject()));
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
        return elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
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
}
