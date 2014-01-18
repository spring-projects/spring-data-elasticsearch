package org.springframework.data.elasticsearch.core;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.SampleTransientEntity;
import org.springframework.data.elasticsearch.SimpleRecursiveEntity;
import org.springframework.data.elasticsearch.StockPrice;
import org.springframework.data.elasticsearch.StockPriceBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Stuart Stevenson
 * @author Jakub Vavrik
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class MappingBuilderTests {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void shouldNotFailOnCircularReference() {
        elasticsearchTemplate.createIndex(SimpleRecursiveEntity.class);
        elasticsearchTemplate.putMapping(SimpleRecursiveEntity.class);
    }

    @Test
    public void testInfiniteLoopAvoidance() throws IOException {
        final String expected = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true,\"" +
                "type\":\"string\",\"index\":\"not_analyzed\",\"search_analyzer\":\"standard\"," +
                "\"index_analyzer\":\"standard\"}}}}";

        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleTransientEntity.class, "mapping", "id");
        assertThat(xContentBuilder.string(), is(expected));
    }

    @Test
    public void shouldUseValueFromAnnotationType() throws IOException {
        //Given
        final String expected = "{\"mapping\":{\"properties\":{\"price\":{\"store\":false,\"type\":\"double\"}}}}";

        //When
        XContentBuilder xContentBuilder = MappingBuilder.buildMapping(StockPrice.class, "mapping", "id");

        //Then
        assertThat(xContentBuilder.string(), is(expected));
    }

    @Test
    public void shouldAddStockPriceDocumentToIndex() throws IOException {
        //Given

        //When
        elasticsearchTemplate.deleteIndex(StockPrice.class);
        elasticsearchTemplate.createIndex(StockPrice.class);
        elasticsearchTemplate.putMapping(StockPrice.class);
        String symbol = "AU";
        double price = 2.34;
        String id = "abc";
        elasticsearchTemplate.index(new StockPriceBuilder(id).symbol(symbol).price(price).buildIndex());
        elasticsearchTemplate.refresh(StockPrice.class, true);

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
        List<StockPrice> result = elasticsearchTemplate.queryForList(searchQuery, StockPrice.class);
        //Then
        assertThat(result.size(), is(1));
        StockPrice entry = result.get(0);
        assertThat(entry.getSymbol(), is(symbol));
        assertThat(entry.getPrice(), is(new BigDecimal(price)));
    }

}
