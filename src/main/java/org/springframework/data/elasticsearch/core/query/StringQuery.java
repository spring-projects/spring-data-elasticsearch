package org.springframework.data.elasticsearch.core.query;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class StringQuery extends AbstractQuery{

    private String source;

    public StringQuery(String source) {
        this.source = source;
    }

    public StringQuery(String source, Pageable pageable) {
        this.source = source;
        this.pageable = pageable;
    }

    public StringQuery(String source, Pageable pageable, Sort sort) {
        this.pageable = pageable;
        this.sort = sort;
        this.source = source;
    }


    public String getSource() {
        return source;
    }

}
