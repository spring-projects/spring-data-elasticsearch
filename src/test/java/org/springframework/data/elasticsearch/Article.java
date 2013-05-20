package org.springframework.data.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple type to test facets
 */
@Document(indexName = "articles", type = "article", shards = 1, replicas = 0, refreshInterval = "-1")
public class Article {

    @Id
    private String id;

    private String title;

    @Field(type = "string", facetable = true)
    private List<String> authors = new ArrayList<String>();

    @Field(type = "integer", facetable = true)
    private List<Integer> publishedYears = new ArrayList<Integer>();

    public Article() {

    }

    public Article(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public List<Integer> getPublishedYears() {
        return publishedYears;
    }

    public void setPublishedYears(List<Integer> publishedYears) {
        this.publishedYears = publishedYears;
    }
}
