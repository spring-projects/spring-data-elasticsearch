/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.query;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import java.lang.Long;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 * @author James Bodkin
 */
@SpringIntegrationTest
public abstract class CriteriaQueryIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // DATAES-706
	public void shouldPerformAndOperationOnCriteriaEntries() {

		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(nextIdAsString());
		sampleEntity1.setMessage("some test message");
		operations.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(nextIdAsString());
		sampleEntity2.setMessage("some other message");
		operations.save(sampleEntity2);

		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("test").and("message").contains("some"));
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo(sampleEntity1.id);
	}

	@Test // DATAES-706
	public void shouldPerformOrOperationOnCriteriaEntries() {

		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(nextIdAsString());
		sampleEntity1.setMessage("some test message");
		operations.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(nextIdAsString());
		sampleEntity2.setMessage("some other message");
		operations.save(sampleEntity2);

		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("test").or("message").contains("other"));
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getSearchHits().stream().map(SearchHit::getId)).containsExactlyInAnyOrder(sampleEntity1.id,
				sampleEntity2.id);
	}

	@Test // DATAES-706
	public void shouldPerformAndOperationWithinCriteria() {

		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(nextIdAsString());
		sampleEntity1.setMessage("some test message");
		operations.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(nextIdAsString());
		sampleEntity2.setMessage("some other message");
		operations.save(sampleEntity2);

		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("test").and(new Criteria("message").contains("some")));
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test // DATAES-706
	public void shouldPerformOrOperationWithinCriteria() {

		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(nextIdAsString());
		sampleEntity1.setMessage("some test message");
		operations.save(sampleEntity1);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(nextIdAsString());
		sampleEntity2.setMessage("some other message");
		operations.save(sampleEntity2);

		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("test").or(new Criteria("message").contains("other")));
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getSearchHits().stream().map(SearchHit::getId)).containsExactlyInAnyOrder(sampleEntity1.id,
				sampleEntity2.id);
	}

	@Test
	public void shouldPerformIsOperation() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		indexQueries.add(indexQuery);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("some message"));

		// when
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		// then
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
		SearchHit<SampleEntity> searchHit = searchHits.getSearchHit(0);
		assertThat(searchHit.getId()).isEqualTo(searchHit.getId());
	}

	@Test
	public void shouldPerformMultipleIsOperations() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("some message"));

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	@Test
	public void shouldPerformEndsWithOperation() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message end");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		Criteria criteria = new Criteria("message").endsWith("end");
		CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class);

		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformStartsWithOperation() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("start some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		Criteria criteria = new Criteria("message").startsWith("start");
		CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class);

		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformContainsOperation() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("contains some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("contains"));

		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class);

		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldExecuteExpression() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("elasticsearch search");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").expression("+elasticsearch || test"));

		// when
		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class);

		// then
		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldExecuteCriteriaChain() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message search");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test test message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").startsWith("some").endsWith("search").contains("message").is("some message search"));

		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class);

		assertThat(criteriaQuery.getCriteria().getField().getName()).isEqualTo("message");
		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformIsNotOperation() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("foo").not());

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(criteriaQuery.getCriteria().isNegating()).isTrue();
		assertThat(searchHits).isNotNull();
		assertThat(searchHits.iterator().next().getContent().getMessage()).doesNotContain("foo");
	}

	@Test
	public void shouldPerformBetweenOperation() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(100);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(200);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(100, 150));

		SearchHit<SampleEntity> sampleEntity = operations.searchOne(criteriaQuery, SampleEntity.class);

		assertThat(sampleEntity).isNotNull();
	}

	@Test
	public void shouldPerformBetweenOperationWithoutUpperBound() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(300);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(400);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(350, null));

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformBetweenOperationWithoutLowerBound() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(500);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(600);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").between(null, 550));

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformLessThanEqualOperation() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(700);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(800);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").lessThanEqual(750));

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformGreaterThanEquals() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setRate(900);
		sampleEntity1.setMessage("bar");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setRate(1000);
		sampleEntity2.setMessage("foo");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);

		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("rate").greaterThanEqual(950));

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits).isNotNull();
		assertThat(searchHits.getTotalHits()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldPerformBoostOperation() {

		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setRate(700);
		sampleEntity.setMessage("foo");
		sampleEntity.setVersion(System.currentTimeMillis());

		operations.save(sampleEntity);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("foo").boost(2));

		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getScore()).isEqualTo(2.0f);
	}

	@Test
	public void shouldReturnDocumentAboveMinimalScoreGivenCriteria() {

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(buildIndex(new SampleEntity("1", "ab")));
		indexQueries.add(buildIndex(new SampleEntity("2", "bc")));
		indexQueries.add(buildIndex(new SampleEntity("3", "ac")));
		operations.bulkIndex(indexQueries, SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("a").or(new Criteria("message").contains("b")));
		criteriaQuery.setMinScore(2.0F);
		SearchHits<SampleEntity> searchHits = operations.search(criteriaQuery, SampleEntity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getContent().getMessage()).isEqualTo("ab");
	}

	@Test // DATAES-213
	public void shouldEscapeValue() {

		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("Hello World!");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").is("Hello World!"));

		SearchHit<SampleEntity> sampleEntity1 = operations.searchOne(criteriaQuery, SampleEntity.class);

		assertThat(sampleEntity1).isNotNull();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Nullable
		@Field(type = Text, store = true, fielddata = true) private String message;
		@Nullable private int rate;
		@Nullable
		@Version private Long version;

		public SampleEntity() {}

		public SampleEntity(@Nullable String id, @Nullable String message) {
			this.id = id;
			this.message = message;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		public int getRate() {
			return rate;
		}

		public void setRate(int rate) {
			this.rate = rate;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}
	}
}
