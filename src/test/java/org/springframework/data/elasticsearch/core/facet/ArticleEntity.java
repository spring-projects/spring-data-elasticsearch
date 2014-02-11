package org.springframework.data.elasticsearch.core.facet;

import static org.springframework.data.elasticsearch.annotations.FieldIndex.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.String;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.NestedField;

/**
 * Simple type to test facets
 */
@Document(indexName = "articles", type = "article", shards = 1, replicas = 0, refreshInterval = "-1", indexStoreType = "memory")
public class ArticleEntity {

	@Id
	private String id;

	private String title;

	@MultiField(
			mainField = @Field(type = String, index = analyzed),
			otherFields = {
					@NestedField(dotSuffix = "untouched", type = String, store = true, index = not_analyzed),
					@NestedField(dotSuffix = "sort", type = String, store = true, indexAnalyzer = "keyword")
			}
	)
	private List<String> authors = new ArrayList<String>();

	@Field(type = Integer, store = true)
	private List<Integer> publishedYears = new ArrayList<Integer>();

	private int score;

	private ArticleEntity() {

	}

	public ArticleEntity(String id) {
		this.id = id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getAuthors() {
		return authors;
	}

	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}

	public List<Integer> getPublishedYears() {
		return publishedYears;
	}

	public void setPublishedYears(List<Integer> publishedYears) {
		this.publishedYears = publishedYears;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}
}
