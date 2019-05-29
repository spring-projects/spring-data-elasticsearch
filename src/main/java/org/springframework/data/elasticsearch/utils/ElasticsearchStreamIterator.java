/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.utils;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ScrolledPage;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.util.CloseableIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ElasticsearchStreamIterator
 *
 * @author Simon Schneider
 * */
public class ElasticsearchStreamIterator<T> implements CloseableIterator<T> {

    private ElasticsearchOperations elasticsearchTemplate;
    private final ScrolledPage<T> page;
    private final long scrollTimeInMillis;
    private final Class<T> clazz;
    private final SearchResultMapper mapper;
    /** As we couldn't retrieve single result with scroll, store current hits. */
    private volatile Iterator<T> currentHits;

    /** The scroll id. */
    private volatile String scrollId;

    /** If stream is finished (ie: cluster returns no results. */
    private volatile boolean finished;

    public ElasticsearchStreamIterator(ElasticsearchTemplate elasticsearchTemplate, ScrolledPage<T> page, long scrollTimeInMillis, Class<T> clazz, SearchResultMapper mapper) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.page = page;
        this.scrollTimeInMillis = scrollTimeInMillis;
        this.clazz = clazz;
        this.mapper = mapper;
        currentHits = page.iterator();
        scrollId = page.getScrollId();
        finished = !currentHits.hasNext();
    }

    @Override
    public void close() {
        try {
            // Clear scroll on cluster only in case of error (cause elasticsearch auto clear scroll when it's done)
            if (!finished && scrollId != null && currentHits != null && currentHits.hasNext()) {
                elasticsearchTemplate.clearScroll(scrollId);
            }
        } finally {
            currentHits = null;
            scrollId = null;
        }
    }

    @Override
    public boolean hasNext() {
        // Test if stream is finished
        if (finished) {
            return false;
        }
        // Test if it remains hits
        if (currentHits == null || !currentHits.hasNext()) {
            // Do a new request
            final ScrolledPage<T> scroll = (ScrolledPage<T>) elasticsearchTemplate.continueScroll(scrollId, scrollTimeInMillis, clazz, mapper);
            // Save hits and scroll id
            currentHits = scroll.iterator();
            finished = !currentHits.hasNext();
            scrollId = scroll.getScrollId();
        }
        return currentHits.hasNext();
    }

    @Override
    public T next() {
        if (hasNext()) {
            return currentHits.next();
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
