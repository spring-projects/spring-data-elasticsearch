/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.delete.DeleteMappingRequest;
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
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.facet.FacetMapper;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;
import org.springframework.data.elasticsearch.core.facet.FacetResult;
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
        this.elasticsearchConverter = (elasticsearchConverter == null) ? new MappingElasticsearchConverter(
                new SimpleElasticsearchMappingContext()) : elasticsearchConverter;
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz) {
        return createIndexIfNotCreated(clazz);
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        PutMappingRequestBuilder requestBuilder = client.admin().indices()
                .preparePutMapping(persistentEntity.getIndexName()).setType(persistentEntity.getIndexType());

        try {
            XContentBuilder xContentBuilder = buildMapping(clazz, persistentEntity.getIndexType(), persistentEntity
                    .getIdProperty().getFieldName());
            return requestBuilder.setSource(xContentBuilder).execute().actionGet().isAcknowledged();
        } catch (Exception e) {
            throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public ElasticsearchConverter getElasticsearchConverter() {
        return elasticsearchConverter;
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        GetResponse response = client
                .prepareGet(persistentEntity.getIndexName(), persistentEntity.getIndexType(), query.getId()).execute()
                .actionGet();
        return mapResult(response.getSourceAsString(), clazz);
    }

    @Override
    public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
        Page<T> page = queryForPage(query, clazz);
        Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
        return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
    }

    @Override
    public <T> T queryForObject(StringQuery query, Class<T> clazz) {
        Page<T> page = queryForPage(query, clazz);
        Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
        return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
    }

    @Override
    public <T> FacetedPage<T> queryForPage(SearchQuery query, Class<T> clazz) {
        SearchResponse response = doSearch(prepareSearch(query, clazz), query);
        return mapResults(response, clazz, query.getPageable());
    }

    @Override
    public <T> FacetedPage<T> queryForPage(SearchQuery query, ResultsMapper<T> resultsMapper) {
        SearchResponse response = doSearch(prepareSearch(query), query);
        return resultsMapper.mapResults(response);
    }

    @Override
    public <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz) {
        return queryForPage(query, clazz).getContent();
    }

    @Override
    public <T> List<T> queryForList(StringQuery query, Class<T> clazz) {
        return queryForPage(query, clazz).getContent();
    }

    @Override
    public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
        return queryForPage(query, clazz).getContent();
    }

    @Override
    public <T> List<String> queryForIds(SearchQuery query) {
        SearchRequestBuilder request = prepareSearch(query).setQuery(query.getQuery()).setNoFields();
        if (query.getFilter() != null) {
            request.setFilter(query.getFilter());
        }
        SearchResponse response = request.execute().actionGet();
        return extractIds(response);
    }

    @Override
    public <T> Page<T> queryForPage(CriteriaQuery criteriaQuery, Class<T> clazz) {
        QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
        FilterBuilder elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());
        SearchRequestBuilder searchRequestBuilder = prepareSearch(criteriaQuery, clazz);

        if (elasticsearchQuery != null) {
            searchRequestBuilder.setQuery(elasticsearchQuery);
        } else {
            searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
        }

        if (elasticsearchFilter != null)
            searchRequestBuilder.setFilter(elasticsearchFilter);

        SearchResponse response = searchRequestBuilder
                .execute().actionGet();
        return mapResults(response, clazz, criteriaQuery.getPageable());
    }

    @Override
    public <T> FacetedPage<T> queryForPage(StringQuery query, Class<T> clazz) {
        SearchResponse response = prepareSearch(query, clazz).setQuery(query.getSource()).execute().actionGet();
        return mapResults(response, clazz, query.getPageable());
    }

    @Override
    public <T> long count(SearchQuery query, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        CountRequestBuilder countRequestBuilder = client.prepareCount(persistentEntity.getIndexName()).setTypes(
                persistentEntity.getIndexType());
        if (query.getQuery() != null) {
            countRequestBuilder.setQuery(query.getQuery());
        }
        return countRequestBuilder.execute().actionGet().getCount();
    }

    @Override
    public String index(IndexQuery query) {
        return prepareIndex(query).execute().actionGet().getId();
    }

    @Override
    public UpdateResponse update(UpdateQuery query) {
        String indexName = isNotBlank(query.getIndexName()) ? query.getIndexName() : getPersistentEntityFor(query.getClazz()).getIndexName();
        String type = isNotBlank(query.getType()) ? query.getType() : getPersistentEntityFor(query.getClazz()).getIndexType();
        Assert.notNull(indexName, "No index defined for Query");
        Assert.notNull(type, "No type define for Query");
        Assert.notNull(query.getId(), "No Id define for Query");
        Assert.notNull(query.getIndexRequest(), "No IndexRequest define for Query");
        UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(indexName, type, query.getId());
        if(query.DoUpsert()){
            updateRequestBuilder.setDocAsUpsert(true)
                    .setUpsert(query.getIndexRequest()).setDoc(query.getIndexRequest());
        } else {
            updateRequestBuilder.setDoc(query.getIndexRequest());
        }
        return updateRequestBuilder.execute().actionGet();
    }


    @Override
    public void bulkIndex(List<IndexQuery> queries) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (IndexQuery query : queries) {
            bulkRequest.add(prepareIndex(query));
        }
        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            Map<String, String> failedDocuments = new HashMap<String, String>();
            for (BulkItemResponse item : bulkResponse.getItems()) {
                if (item.isFailed())
                    failedDocuments.put(item.getId(), item.getFailureMessage());
            }
            throw new ElasticsearchException(
                    "Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
                            + failedDocuments + "]", failedDocuments);
        }
    }

    @Override
    public <T> boolean indexExists(Class<T> clazz) {
        return indexExists(getPersistentEntityFor(clazz).getIndexName());
    }

    @Override
    public boolean typeExists(String index, String type) {
        return client.admin().cluster().prepareState().execute().actionGet()
                .getState().metaData().index(index).mappings().containsKey(type);
    }

    @Override
    public <T> boolean deleteIndex(Class<T> clazz) {
        String indexName = getPersistentEntityFor(clazz).getIndexName();
        if (indexExists(indexName)) {
            return client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet().isAcknowledged();
        }
        return false;
    }

    @Override
    public void deleteType(String index, String type){
        Map mappings = client.admin().cluster().prepareState().execute().actionGet()
                .getState().metaData().index(index).mappings();
        if (mappings.containsKey(type)) {
            client.admin().indices().deleteMapping(new DeleteMappingRequest(index).type(type)).actionGet();
        }
    }

    @Override
    public String delete(String indexName, String type, String id) {
        return client.prepareDelete(indexName, type, id).execute().actionGet().getId();
    }

    @Override
    public <T> String delete(Class<T> clazz, String id) {
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        return delete(persistentEntity.getIndexName(), persistentEntity.getIndexType(), id);
    }

    @Override
    public <T> void delete(DeleteQuery deleteQuery, Class<T> clazz) {
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        client.prepareDeleteByQuery(persistentEntity.getIndexName()).setTypes(persistentEntity.getIndexType())
                .setQuery(deleteQuery.getQuery()).execute().actionGet();
    }

    @Override
    public void delete(DeleteQuery deleteQuery) {
        Assert.notNull(deleteQuery.getIndex(), "No index defined for Query");
        Assert.notNull(deleteQuery.getType(), "No type define for Query");
        client.prepareDeleteByQuery(deleteQuery.getIndex()).setTypes(deleteQuery.getType())
                .setQuery(deleteQuery.getQuery()).execute().actionGet();
    }

    @Override
    public String scan(SearchQuery searchQuery, long scrollTimeInMillis, boolean noFields) {
        Assert.notNull(searchQuery.getIndices(), "No index defined for Query");
        Assert.notNull(searchQuery.getTypes(), "No type define for Query");
        Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scan & scroll");

        SearchRequestBuilder requestBuilder = client.prepareSearch(toArray(searchQuery.getIndices())).setSearchType(SCAN)
                .setQuery(searchQuery.getQuery()).setTypes(toArray(searchQuery.getTypes()))
                .setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).setFrom(0)
                .setSize(searchQuery.getPageable().getPageSize());

        if (searchQuery.getFilter() != null) {
            requestBuilder.setFilter(searchQuery.getFilter());
        }

        if (noFields) {
            requestBuilder.setNoFields();
        }
        return requestBuilder.execute().actionGet().getScrollId();
    }

    @Override
    public <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, ResultsMapper<T> resultsMapper) {
        SearchResponse response = client.prepareSearchScroll(scrollId)
                .setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute().actionGet();
        return resultsMapper.mapResults(response);
    }

    @Override
    public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {
        int startRecord = 0;
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        String indexName = isNotBlank(query.getIndexName()) ? query.getIndexName() : persistentEntity.getIndexName();
        String type = isNotBlank(query.getType()) ? query.getType() : persistentEntity.getIndexType();

        Assert.notNull(indexName, "No 'indexName' defined for MoreLikeThisQuery");
        Assert.notNull(type, "No 'type' defined for MoreLikeThisQuery");
        Assert.notNull(query.getId(), "No document id defined for MoreLikeThisQuery");

        MoreLikeThisRequestBuilder requestBuilder = client.prepareMoreLikeThis(indexName, type, query.getId());

        if (query.getPageable() != null) {
            startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
            requestBuilder.setSearchSize(query.getPageable().getPageSize());
        }
        requestBuilder.setSearchFrom(startRecord);

        if (isNotEmpty(query.getSearchIndices())) {
            requestBuilder.setSearchIndices(toArray(query.getSearchIndices()));
        }
        if (isNotEmpty(query.getSearchTypes())) {
            requestBuilder.setSearchTypes(toArray(query.getSearchTypes()));
        }
        if (isNotEmpty(query.getFields())) {
            requestBuilder.setField(toArray(query.getFields()));
        }
        if (isNotBlank(query.getRouting())) {
            requestBuilder.setRouting(query.getRouting());
        }
        if (query.getPercentTermsToMatch() != null) {
            requestBuilder.setPercentTermsToMatch(query.getPercentTermsToMatch());
        }
        if (query.getMinTermFreq() != null) {
            requestBuilder.setMinTermFreq(query.getMinTermFreq());
        }
        if (query.getMaxQueryTerms() != null) {
            requestBuilder.maxQueryTerms(query.getMaxQueryTerms());
        }
        if (isNotEmpty(query.getStopWords())) {
            requestBuilder.setStopWords(toArray(query.getStopWords()));
        }
        if (query.getMinDocFreq() != null) {
            requestBuilder.setMinDocFreq(query.getMinDocFreq());
        }
        if (query.getMaxDocFreq() != null) {
            requestBuilder.setMaxDocFreq(query.getMaxDocFreq());
        }
        if (query.getMinWordLen() != null) {
            requestBuilder.setMinWordLen(query.getMinWordLen());
        }
        if (query.getMaxWordLen() != null) {
            requestBuilder.setMaxWordLen(query.getMaxWordLen());
        }
        if (query.getBoostTerms() != null) {
            requestBuilder.setBoostTerms(query.getBoostTerms());
        }

        SearchResponse response = requestBuilder.execute().actionGet();
        return mapResults(response, clazz, query.getPageable());
    }

    private SearchResponse doSearch(SearchRequestBuilder searchRequest, SearchQuery searchQuery) {
        if (searchQuery.getFilter() != null) {
            searchRequest.setFilter(searchQuery.getFilter());
        }

        if (searchQuery.getElasticsearchSort() != null) {
            searchRequest.addSort(searchQuery.getElasticsearchSort());
        }

        if (CollectionUtils.isNotEmpty(searchQuery.getFacets())) {
            for (FacetRequest facetRequest : searchQuery.getFacets()) {
                FacetBuilder facet = facetRequest.getFacet();
                if (facetRequest.applyQueryFilter() && searchQuery.getFilter() != null) {
                    facet.facetFilter(searchQuery.getFilter());
                }
                searchRequest.addFacet(facet);
            }
        }

        if(searchQuery.getHighlightFields() != null) {
            for(HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()){
                searchRequest.addHighlightedField(highlightField);
            }
        }

        return searchRequest.setQuery(searchQuery.getQuery()).execute().actionGet();
    }

    private <T> boolean createIndexIfNotCreated(Class<T> clazz) {
        return indexExists(getPersistentEntityFor(clazz).getIndexName()) || createIndexWithSettings(clazz);
    }

    private boolean indexExists(String indexName) {
        return client.admin().indices().exists(indicesExistsRequest(indexName)).actionGet().isExists();
    }

    private <T> boolean createIndexWithSettings(Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        return client.admin().indices()
                .create(Requests.createIndexRequest(persistentEntity.getIndexName()).settings(getSettings(persistentEntity)))
                .actionGet().isAcknowledged();
    }

    private <T> Map getSettings(ElasticsearchPersistentEntity<T> persistentEntity) {
        return new MapBuilder<String, String>().put("index.number_of_shards", String.valueOf(persistentEntity.getShards()))
                .put("index.number_of_replicas", String.valueOf(persistentEntity.getReplicas()))
                .put("index.refresh_interval", persistentEntity.getRefreshInterval())
                .put("index.store.type", persistentEntity.getIndexStoreType()).map();
    }

    private <T> SearchRequestBuilder prepareSearch(Query query, Class<T> clazz) {
        if (query.getIndices().isEmpty()) {
            query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
        }
        if (query.getTypes().isEmpty()) {
            query.addTypes(retrieveTypeFromPersistentEntity(clazz));
        }
        return prepareSearch(query);
    }

    private SearchRequestBuilder prepareSearch(Query query) {
        Assert.notNull(query.getIndices(), "No index defined for Query");
        Assert.notNull(query.getTypes(), "No type defined for Query");

        int startRecord = 0;
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(toArray(query.getIndices()))
                .setSearchType(DFS_QUERY_THEN_FETCH).setTypes(toArray(query.getTypes()));

        if (query.getPageable() != null) {
            startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
            searchRequestBuilder.setSize(query.getPageable().getPageSize());
        }
        searchRequestBuilder.setFrom(startRecord);

        if (!query.getFields().isEmpty()) {
            searchRequestBuilder.addFields(toArray(query.getFields()));
        }

        if (query.getSort() != null) {
            for (Sort.Order order : query.getSort()) {
                searchRequestBuilder.addSort(order.getProperty(), order.getDirection() == Sort.Direction.DESC ? SortOrder.DESC
                        : SortOrder.ASC);
            }
        }
        return searchRequestBuilder;
    }

    private IndexRequestBuilder prepareIndex(IndexQuery query) {
        try {
            String indexName = isBlank(query.getIndexName()) ? retrieveIndexNameFromPersistentEntity(query.getObject()
                    .getClass())[0] : query.getIndexName();
            String type = isBlank(query.getType()) ? retrieveTypeFromPersistentEntity(query.getObject().getClass())[0]
                    : query.getType();

            IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName, type, query.getId()).setSource(
                    objectMapper.writeValueAsString(query.getObject()));

            if (query.getVersion() != null) {
                indexRequestBuilder.setVersion(query.getVersion());
                indexRequestBuilder.setVersionType(EXTERNAL);
            }
            return indexRequestBuilder;
        } catch (IOException e) {
            throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
        }
    }

    public void refresh(String indexName, boolean waitForOperation) {
        client.admin().indices().refresh(refreshRequest(indexName).force(waitForOperation)).actionGet();
    }

    public <T> void refresh(Class<T> clazz, boolean waitForOperation) {
        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
        client.admin().indices()
                .refresh(refreshRequest(persistentEntity.getIndexName()).force(waitForOperation)).actionGet();
    }

    private ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz) {
        Assert.isTrue(clazz.isAnnotationPresent(Document.class), "Unable to identify index name. " + clazz.getSimpleName()
                + " is not a Document. Make sure the document class is annotated with @Document(indexName=\"foo\")");
        return elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
    }

    private String[] retrieveIndexNameFromPersistentEntity(Class clazz) {
        return new String[]{getPersistentEntityFor(clazz).getIndexName()};
    }

    private String[] retrieveTypeFromPersistentEntity(Class clazz) {
        return new String[]{getPersistentEntityFor(clazz).getIndexType()};
    }

    private <T> FacetedPage<T> mapResults(SearchResponse response, final Class<T> elementType, final Pageable pageable) {
        ResultsMapper<T> resultsMapper = new ResultsMapper<T>() {
            @Override
            public FacetedPage<T> mapResults(SearchResponse response) {
                long totalHits = response.getHits().totalHits();
                List<T> results = new ArrayList<T>();
                for (SearchHit hit : response.getHits()) {
                    if (hit != null) {
                        results.add(mapResult(hit.sourceAsString(), elementType));
                    }
                }
                List<FacetResult> facets = new ArrayList<FacetResult>();
                if (response.getFacets() != null) {
                    for (Facet facet : response.getFacets()) {
                        FacetResult facetResult = FacetMapper.parse(facet);
                        if (facetResult != null) {
                            facets.add(facetResult);
                        }
                    }
                }

                return new FacetedPageImpl<T>(results, pageable, totalHits, facets);
            }
        };
        return resultsMapper.mapResults(response);
    }

    private List<String> extractIds(SearchResponse response) {
        List<String> ids = new ArrayList<String>();
        for (SearchHit hit : response.getHits()) {
            if (hit != null) {
                ids.add(hit.getId());
            }
        }
        return ids;
    }

    private <T> T mapResult(String source, Class<T> clazz) {
        if (isBlank(source)) {
            return null;
        }
        try {
            return objectMapper.readValue(source, clazz);
        } catch (IOException e) {
            throw new ElasticsearchException("failed to map source [ " + source + "] to class " + clazz.getSimpleName(), e);
        }
    }

    private static String[] toArray(List<String> values) {
        String[] valuesAsArray = new String[values.size()];
        return values.toArray(valuesAsArray);

    }

}
