package org.springframework.data.elasticsearch.core.partition;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.partition.keys.LongPartition;
import org.springframework.data.elasticsearch.core.partition.keys.StringPartition;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.entities.partition.LongPartitionedEntity;
import org.springframework.data.elasticsearch.entities.partition.StringPartitionedEntity;
import org.springframework.data.elasticsearch.repositories.partition.StringPartitionedEntityRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by franck.lefebure on 28/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/partitioned-repository-test.xml")
public class TemplateTests {

    @Autowired
    private ElasticsearchPartitionsCache cache;
    @Autowired
    private ElasticsearchOperations template;
    @Autowired
    private StringPartitionedEntityRepository stringPartitionedEntityRepository;
    String id;

    @Before
    public void init() {
        List<String> indices = cache.listIndicesForPrefix("index_");
        for (String indice : indices) {
            template.deleteIndex(indice);
        }
        StringPartitionedEntity e = new StringPartitionedEntity();
        e.setCustomerId("johndoe");
        e.setLabel("test");
        stringPartitionedEntityRepository.save(e);
        this.id = e.getId();

    }

    @Test
    public void testQueryForObject() {
        GetQuery q = new GetQuery();
        q.setId(id);
        StringPartitionedEntity e = template.queryForObject(q, StringPartitionedEntity.class);
        Assert.notNull(e);
    }

    @Test
    public void testAddAlias() {
        AliasQuery q = new AliasQuery();
        q.setIndexName("index_johndoe");
        q.setAliasName("index_all");
        template.addAlias(q);
        Set<String> aliases = template.queryForAlias("index_johndoe");
        Assert.isTrue(aliases.contains("index_all"));
        template.removeAlias(q);
        aliases = template.queryForAlias("index_johndoe");
        Assert.isTrue(!aliases.contains("index_all"));
    }

    @Test
    public void testResultsExtractor() {

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withSearchType(COUNT)
                .addAggregation(terms("labels").field("label"))
                .build();
        // when
        Aggregations aggregations = template.query(searchQuery, new ResultsExtractor<Aggregations>() {
            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        },StringPartitionedEntity.class);
        // then
        assertThat(aggregations, is(notNullValue()));
        assertEquals(1, ((StringTerms)aggregations.asMap().get("labels")).getBuckets().get(0).getDocCount());
        assertEquals("test", ((StringTerms)aggregations.asMap().get("labels")).getBuckets().get(0).getKey());

        searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withSearchType(COUNT)
                .withPartition(new StringPartition("customerId", new String[]{"johndoe"}))
                .addAggregation(terms("labels").field("label"))
                .build();

        aggregations = template.query(searchQuery, new ResultsExtractor<Aggregations>() {
            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        },StringPartitionedEntity.class);
        // then
        assertThat(aggregations, is(notNullValue()));
        assertEquals(1, ((StringTerms)aggregations.asMap().get("labels")).getBuckets().get(0).getDocCount());
        assertEquals("test", ((StringTerms)aggregations.asMap().get("labels")).getBuckets().get(0).getKey());

        searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withSearchType(COUNT)
                .withPartition(new StringPartition("customerId", new String[]{"nobody"}))
                .addAggregation(terms("labels").field("label"))
                .build();

        aggregations = template.query(searchQuery, new ResultsExtractor<Aggregations>() {
            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        },StringPartitionedEntity.class);

        assertThat(aggregations, is(notNullValue()));
        assertEquals(0, ((StringTerms)aggregations.asMap().get("labels")).getBuckets().size());
    }
    @Test
    public void testBulkIndex() {
        LongPartitionedEntity l1 = new LongPartitionedEntity();
        l1.setLabel("test1");
        l1.setId("1500");
        LongPartitionedEntity l2 = new LongPartitionedEntity();
        l2.setLabel("test2");
        l2.setId("3500");
        List<IndexQuery> qs = new ArrayList<IndexQuery>();
        IndexQuery q1 = new IndexQuery();
        q1.setObject(l1);
        q1.setId("1500");
        qs.add(q1);
        IndexQuery q2 = new IndexQuery();
        q2.setObject(l2);
        q2.setId("3500");
        qs.add(q2);
        template.bulkIndex(qs);
        template.refresh("index_1000", true);
        template.refresh("index_3000", true);
        SearchQuery query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).build();
        List<LongPartitionedEntity> list = template.queryForList(query, LongPartitionedEntity.class);
        assertEquals(2, list.size());

        query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).withPartition(new LongPartition("id", 1000, 2000)).build();
        list = template.queryForList(query, LongPartitionedEntity.class);
        assertEquals(1, list.size());

        query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).withPartition(new LongPartition("id", 5000, 6000)).build();
        list = template.queryForList(query, LongPartitionedEntity.class);
        assertEquals(0, list.size());

        IndexRequest indexRequest = new IndexRequest();
        indexRequest.source("label", "label updated");
        UpdateQuery updateQuery = new UpdateQueryBuilder().withId("1000_1500")
                .withClass(LongPartitionedEntity.class).withIndexRequest(indexRequest).build();
        List<UpdateQuery> uqs = new ArrayList<UpdateQuery>();
        uqs.add(updateQuery);
        template.bulkUpdate(uqs);
        template.refresh("index_1000", true);

        query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).withPartition(new LongPartition("id", 1000, 2000)).build();
        list = template.queryForList(query, LongPartitionedEntity.class);
        assertEquals("label updated", list.get(0).getLabel());

    }

}
