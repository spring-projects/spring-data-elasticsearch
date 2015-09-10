package org.springframework.data.elasticsearch.builder;

import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.entities.SourceExcludedEntity;

public class SourceExcludedEntityBuilder {

	private SourceExcludedEntity result;

	public SourceExcludedEntityBuilder(String id) {
		this.result = new SourceExcludedEntity();
		this.result.setId(id);
	}

	public SourceExcludedEntityBuilder indexOnlyField(String value) {
		this.result.setIndexOnlyField(value);
		return this;
	}

	public SourceExcludedEntityBuilder simpleField(String value) {
		this.result.setSimpleField(value);
		return this;
	}

	public SourceExcludedEntity build() {
		return result;
	}

	public IndexQuery buildIndex() {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(result.getId());
		indexQuery.setObject(result);
		return indexQuery;
	}

}
