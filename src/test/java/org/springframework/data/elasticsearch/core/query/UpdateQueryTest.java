package org.springframework.data.elasticsearch.core.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.document.Document;

class UpdateQueryTest {

	@Test // #3205
	@DisplayName("should build query with only a script")
	void shouldBuildQueryWithOnlyAScript() {

		UpdateQuery.builder("id")
				.withScript("script")
				.build();
	}

	@Test // #3205
	@DisplayName("should build query with only a scriptname")
	void shouldBuildQueryWithOnlyAScriptName() {

		UpdateQuery.builder("id")
				.withScriptName("scriptname")
				.build();
	}

	@Test // #3205
	@DisplayName("should build query with only a document")
	void shouldBuildQueryWithOnlyASDocument() {

		UpdateQuery.builder("id")
				.withDocument(Document.create())
				.build();
	}

	@Test // #3205
	@DisplayName("should build query with only a query")
	void shouldBuildQueryWithOnlyAQuery() {

		Query query = StringQuery.builder("StrignQuery").build();

		UpdateQuery.builder(query)
				.build();
	}
}
