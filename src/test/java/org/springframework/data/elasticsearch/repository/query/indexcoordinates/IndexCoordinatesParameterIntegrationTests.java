package org.springframework.data.elasticsearch.repository.query.indexcoordinates;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;

@SpringIntegrationTest
abstract class IndexCoordinatesParameterIntegrationTests {
	@Autowired ElasticsearchOperations operations;
	@Autowired IndexNameProvider indexNameProvider;
	@Autowired RecordRepository recordRepository;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #2506
	@DisplayName("should use indexcoordinates passes as repository query argument")
	void shouldUseIndexCoordinatesPassesAsRepositoryQueryArgument() {

		var record1 = new Record("1", "one");
		var indexName1 = indexNameProvider.indexName();
		var indexCoordinates1 = IndexCoordinates.of(indexName1);
		operations.save(record1, indexCoordinates1);

		var record2 = new Record("2", "two");
		var indexName2 = indexName1 + "second";
		var indexCoordinates2 = IndexCoordinates.of(indexName2);
		operations.save(record2, indexCoordinates2);

		// search for record1
		var searchHits = recordRepository.findByText("one");
		assert searchHits.getTotalHits() == 1;
		searchHits = recordRepository.findByText("one", indexCoordinates2);
		assert searchHits.getTotalHits() == 0;

		// search for record2
		searchHits = recordRepository.findByText("two");
		assert searchHits.getTotalHits() == 0;
		searchHits = recordRepository.findByText("two", indexCoordinates2);
		assert searchHits.getTotalHits() == 1;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Record {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Keyword) private String text;

		public Record(@Nullable String id, @Nullable String text) {
			this.id = id;
			this.text = text;
		}

		public @Nullable String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		public @Nullable String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}

	interface RecordRepository extends ElasticsearchRepository<Record, String> {
		SearchHits<Record> findByText(String text);

		SearchHits<Record> findByText(String text, IndexCoordinates index);
	}
}
