/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.elasticsearch;

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
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
