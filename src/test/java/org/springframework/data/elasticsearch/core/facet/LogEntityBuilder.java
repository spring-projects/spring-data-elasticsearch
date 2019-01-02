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
package org.springframework.data.elasticsearch.core.facet;

import java.util.Date;

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * Simple type to test facets
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 */

public class LogEntityBuilder {

	private LogEntity result;

	public LogEntityBuilder(String id) {
		result = new LogEntity(id);
	}

	public LogEntityBuilder action(String action) {
		result.setAction(action);
		return this;
	}

	public LogEntityBuilder code(long sequenceCode) {
		result.setSequenceCode(sequenceCode);
		return this;
	}

	public LogEntityBuilder date(Date date) {
		result.setDate(date);
		return this;
	}

	public LogEntityBuilder ip(String ip) {
		result.setIp(ip);
		return this;
	}

	public LogEntity build() {
		return result;
	}

	public IndexQuery buildIndex() {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}
}
