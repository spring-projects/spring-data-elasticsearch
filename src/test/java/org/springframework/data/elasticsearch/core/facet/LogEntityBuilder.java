package org.springframework.data.elasticsearch.core.facet;

import java.util.Date;

import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * User: Artur Konczak
 * Date: 18/10/13
 * Time: 17:33
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
