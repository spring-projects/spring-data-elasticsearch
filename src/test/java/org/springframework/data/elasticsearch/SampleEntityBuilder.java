package org.springframework.data.elasticsearch;

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * User: dead
 * Date: 23/01/14
 * Time: 18:25
 */
public class SampleEntityBuilder {

    private SampleEntity result;

    public SampleEntityBuilder(String id) {
        result = new SampleEntity();
        result.setId(id);
    }

    public SampleEntityBuilder type(String type) {
        result.setType(type);
        return this;
    }

    public SampleEntityBuilder message(String message) {
        result.setMessage(message);
        return this;
    }

    public SampleEntityBuilder rate(int rate) {
        result.setRate(rate);
        return this;
    }

    public SampleEntityBuilder available(boolean available) {
        result.setAvailable(available);
        return this;
    }

    public SampleEntityBuilder highlightedMessage(String highlightedMessage) {
        result.setHighlightedMessage(highlightedMessage);
        return this;
    }

    public SampleEntityBuilder version(Long version) {
        result.setVersion(version);
        return this;
    }

    public SampleEntity build() {
        return result;
    }

    public IndexQuery buildIndex() {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(result.getId());
        indexQuery.setObject(result);
        return indexQuery;
    }
}
