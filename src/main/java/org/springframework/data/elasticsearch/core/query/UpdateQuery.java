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
package org.springframework.data.elasticsearch.core.query;

import org.elasticsearch.action.index.IndexRequest;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class UpdateQuery {

    private String id;
    private IndexRequest indexRequest;
    private String indexName;
    private String type;
    private Class clazz;
    private boolean doUpsert;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IndexRequest getIndexRequest() {
        return indexRequest;
    }

    public void setIndexRequest(IndexRequest indexRequest) {
        this.indexRequest = indexRequest;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public boolean DoUpsert() {
        return doUpsert;
    }

    public void setDoUpsert(boolean doUpsert) {
        this.doUpsert = doUpsert;
    }
}
