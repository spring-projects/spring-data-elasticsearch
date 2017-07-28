package org.springframework.data.elasticsearch.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author zzt
 */
@Document(indexName = "test-highlight-annotation", type = "test-highlight-annotation", shards = 1, replicas = 0, refreshInterval = "-1")
public class File {

	@Id
	private String id;
	@Field(type = FieldType.text)
	private String title;
	@Field(type = FieldType.text)
	private String content;

	public File() {
	}

	public File(String id, String title, String content) {
		this.id = id;
		this.title = title;
		this.content = content;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}
}
