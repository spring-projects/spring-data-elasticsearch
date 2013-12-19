package org.springframework.data.elasticsearch;

import org.springframework.data.elasticsearch.core.facet.ArticleEntity;
import org.springframework.data.elasticsearch.core.geo.AuthorMarkerEntity;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import java.math.BigDecimal;

/**
 * User: dead
 * Date: 09/12/13
 * Time: 17:48
 */
public class StockPriceBuilder {

    private StockPrice result;

    public StockPriceBuilder(String id) {
        result = new StockPrice(id);
    }

    public StockPriceBuilder symbol(String symbol) {
        result.setSymbol(symbol);
        return this;
    }

    public StockPriceBuilder price(BigDecimal price) {
        result.setPrice(price);
        return this;
    }

    public StockPriceBuilder price(double price) {
        result.setPrice(new BigDecimal(price));
        return this;
    }

    public StockPrice build() {
        return result;
    }

    public IndexQuery buildIndex() {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(result.getId());
        indexQuery.setObject(result);
        return indexQuery;
    }
}
