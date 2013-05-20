package org.springframework.data.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import java.util.ArrayList;

/**
 * Simple type to test facets
 */
public class ArticleBuilder {

    private Article resutl;

    public ArticleBuilder(String id) {
        resutl = new Article(id);
    }

    public ArticleBuilder title(String title) {
        resutl.setTitle(title);
        return this;
    }

    public ArticleBuilder addAuthor(String author) {
        resutl.getAuthors().add(author);
        return this;
    }

    public ArticleBuilder addPublishedYear(Integer year) {
        resutl.getPublishedYears().add(year);
        return this;
    }

    public Article build() {
        return resutl;
    }

    public IndexQuery buildIndex(){
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(resutl.getId());
        indexQuery.setObject(resutl);
        return indexQuery;
    }

}
