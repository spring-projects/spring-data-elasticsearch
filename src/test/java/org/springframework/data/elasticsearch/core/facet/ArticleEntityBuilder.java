package org.springframework.data.elasticsearch.core.facet;

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * Simple type to test facets
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
