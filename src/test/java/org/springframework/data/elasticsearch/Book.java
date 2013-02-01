package org.springframework.data.elasticsearch;

import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "book",type = "book")
public class Book {
        private String id;
        private String name;
        private Author author;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
