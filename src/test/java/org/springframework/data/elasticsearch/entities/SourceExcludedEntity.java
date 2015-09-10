package org.springframework.data.elasticsearch.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "test-source-exclusion", type = "mapping", indexStoreType = "memory", shards = 1, replicas = 0, refreshInterval = "-1")
public class SourceExcludedEntity {

	@Id
	private String id;

	@Field(type = FieldType.String, excludeFromSource = true)
	private String indexOnlyField;

	@Field(type = FieldType.String)
	private String simpleField;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIndexOnlyField() {
		return indexOnlyField;
	}

	public void setIndexOnlyField(String indexOnlyField) {
		this.indexOnlyField = indexOnlyField;
	}

	public String getSimpleField() {
		return simpleField;
	}

	public void setSimpleField(String simpleField) {
		this.simpleField = simpleField;
	}

}
