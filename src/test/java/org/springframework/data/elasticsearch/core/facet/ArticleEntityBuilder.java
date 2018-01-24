/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * Simple type to test facets
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 */
public class ArticleEntityBuilder {

	private ArticleEntity result;

	public ArticleEntityBuilder(String id) {
		result = new ArticleEntity(id);
	}

	public ArticleEntityBuilder title(String title) {
		result.setTitle(title);
		return this;
	}

	public ArticleEntityBuilder subject(String subject) {
		result.setSubject(subject);
		return this;
	}

	public ArticleEntityBuilder addAuthor(String author) {
		result.getAuthors().add(author);
		return this;
	}

	public ArticleEntityBuilder addPublishedYear(Integer year) {
		result.getPublishedYears().add(year);
		return this;
	}

	public ArticleEntityBuilder score(int score) {
		result.setScore(score);
		return this;
	}

	public ArticleEntity build() {
		return result;
	}

	public IndexQuery buildIndex() {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}
}
