/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.geo;

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * @author Franck Marchand
 * @author Mohsin Husen
 */

public class AuthorMarkerEntityBuilder {

	private AuthorMarkerEntity result;

	public AuthorMarkerEntityBuilder(String id) {
		result = new AuthorMarkerEntity(id);
	}

	public AuthorMarkerEntityBuilder name(String name) {
		result.setName(name);
		return this;
	}

	public AuthorMarkerEntityBuilder location(double latitude, double longitude) {
		result.setLocation(new GeoPoint(latitude, longitude));
		return this;
	}

	public AuthorMarkerEntity build() {
		return result;
	}

	public IndexQuery buildIndex() {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}
}
