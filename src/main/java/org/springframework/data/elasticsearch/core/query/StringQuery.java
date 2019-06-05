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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * StringQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class StringQuery extends AbstractQuery {

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
