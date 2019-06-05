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
package org.springframework.data.elasticsearch.core.query;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class UpdateQueryBuilder {

	private String id;
	private UpdateRequest updateRequest;
	private IndexRequest indexRequest;
	private String indexName;
	private String type;
	private Class clazz;
	private boolean doUpsert;

	public UpdateQueryBuilder withId(String id) {
		this.id = id;
		return this;
	}

	public UpdateQueryBuilder withUpdateRequest(UpdateRequest updateRequest) {
		this.updateRequest = updateRequest;
		return this;
	}

	public UpdateQueryBuilder withIndexRequest(IndexRequest indexRequest) {
		this.indexRequest = indexRequest;
		return this;
	}

	public UpdateQueryBuilder withIndexName(String indexName) {
		this.indexName = indexName;
		return this;
	}

	public UpdateQueryBuilder withType(String type) {
		this.type = type;
		return this;
	}

	public UpdateQueryBuilder withClass(Class clazz) {
		this.clazz = clazz;
		return this;
	}

	public UpdateQueryBuilder withDoUpsert(boolean doUpsert) {
		this.doUpsert = doUpsert;
		return this;
	}

	public UpdateQuery build() {
		UpdateQuery updateQuery = new UpdateQuery();
		updateQuery.setId(id);
		updateQuery.setIndexName(indexName);
		updateQuery.setType(type);
		updateQuery.setClazz(clazz);
		if (this.indexRequest != null) {
			if (this.updateRequest == null) {
				updateRequest = new UpdateRequest();
			}
			updateRequest.doc(indexRequest);
		}
		updateQuery.setUpdateRequest(updateRequest);
		updateQuery.setDoUpsert(doUpsert);
		return updateQuery;
	}
}
