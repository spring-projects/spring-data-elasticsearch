/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.builder;

import java.util.Date;

import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.entities.SampleInheritedEntity;

/**
 * @author Kevin Leturc
 */
public class SampleInheritedEntityBuilder {

	private SampleInheritedEntity result;

	public SampleInheritedEntityBuilder(String id) {
		result = new SampleInheritedEntity();
		result.setId(id);
	}

	public SampleInheritedEntityBuilder createdDate(Date createdDate) {
		result.setCreatedDate(createdDate);
		return this;
	}

	public SampleInheritedEntityBuilder message(String message) {
		result.setMessage(message);
		return this;
	}

	public SampleInheritedEntity build() {
		return result;
	}

	public IndexQuery buildIndex() {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}
}
