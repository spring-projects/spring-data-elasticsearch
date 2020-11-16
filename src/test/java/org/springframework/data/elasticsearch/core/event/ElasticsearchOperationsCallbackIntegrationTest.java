/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import lombok.Data;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.JoinTypeRelation;
import org.springframework.data.elasticsearch.annotations.JoinTypeRelations;
import org.springframework.data.elasticsearch.core.AbstractElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
@SpringIntegrationTest
abstract class ElasticsearchOperationsCallbackIntegrationTest {

	private static final String INDEX = "test-operations-callback";
	@Autowired private ElasticsearchOperations originalOperations;
	// need a spy here on the abstract implementation class
	private AbstractElasticsearchTemplate operations;

	@Nullable private static SeqNoPrimaryTerm seqNoPrimaryTerm = null;

	@Configuration
	static class Config {

		@Component
		static class SampleEntityBeforeConvertCallback implements BeforeConvertCallback<SampleEntity> {
			@Override
			public SampleEntity onBeforeConvert(SampleEntity entity, IndexCoordinates index) {
				entity.setText("converted");
				JoinField<String> joinField = new JoinField<>("answer", "42");
				entity.setJoinField(joinField);
				if (seqNoPrimaryTerm != null) {
					entity.setSeqNoPrimaryTerm(seqNoPrimaryTerm);
				}
				return entity;
			}
		}
	}

	@BeforeEach
	void setUp() {
		seqNoPrimaryTerm = null;
		operations = (AbstractElasticsearchTemplate) spy(originalOperations);

		IndexOperations indexOps = operations.indexOps(SampleEntity.class);
		indexOps.delete();
		indexOps.create();
		indexOps.putMapping(SampleEntity.class);

		// store one entity to have a seq_no and primary_term
		final SampleEntity initial = new SampleEntity("1", "initial");
		final SampleEntity saved = operations.save(initial);
		seqNoPrimaryTerm = saved.getSeqNoPrimaryTerm();
	}

	@AfterEach
	void tearDown() {
		IndexOperations indexOps = operations.indexOps(SampleEntity.class);
		indexOps.delete();
	}

	@Test // DATAES-68
	void shouldCallBeforeConvertCallback() {
		SampleEntity entity = new SampleEntity("1", "test");

		SampleEntity saved = operations.save(entity);

		assertThat(saved.getText()).isEqualTo("converted");
	}

	@Test // DATAES-972
	@DisplayName("should apply conversion result to IndexQuery on save")
	void shouldApplyConversionResultToIndexQueryOnSave() {

		SampleEntity entity = new SampleEntity("1", "test");

		operations.save(entity);

		ArgumentCaptor<IndexQuery> indexQueryCaptor = ArgumentCaptor.forClass(IndexQuery.class);
		verify(operations, times(2)).doIndex(indexQueryCaptor.capture(), any());

		final IndexQuery capturedIndexQuery = indexQueryCaptor.getValue();
		SampleEntity convertedEntity = (SampleEntity) capturedIndexQuery.getObject();

		final JoinField<String> joinField = convertedEntity.getJoinField();
		assertThat(joinField.getName()).isEqualTo("answer");
		assertThat(joinField.getParent()).isEqualTo("42");
		assertThat(capturedIndexQuery.getRouting()).isEqualTo("42");
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.getPrimaryTerm());
	}

	@Test // DATAES-972
	@DisplayName("should apply conversion result to IndexQuery when not set ")
	void shouldApplyConversionResultToIndexQueryWhenNotSet() {

		SampleEntity entity = new SampleEntity("1", "test");

		final IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(entity.getId());
		indexQuery.setObject(entity);

		operations.index(indexQuery, IndexCoordinates.of(INDEX));

		ArgumentCaptor<IndexQuery> indexQueryCaptor = ArgumentCaptor.forClass(IndexQuery.class);
		verify(operations, times(2)).doIndex(indexQueryCaptor.capture(), any());

		final IndexQuery capturedIndexQuery = indexQueryCaptor.getValue();
		SampleEntity convertedEntity = (SampleEntity) capturedIndexQuery.getObject();

		final JoinField<String> joinField = convertedEntity.getJoinField();
		assertThat(joinField.getName()).isEqualTo("answer");
		assertThat(joinField.getParent()).isEqualTo("42");
		assertThat(capturedIndexQuery.getRouting()).isEqualTo("42");
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.getPrimaryTerm());
	}

	@Test // DATAES-972
	@DisplayName("should not apply conversion result to IndexQuery when already set ")
	void shouldNotApplyConversionResultToIndexQueryWhenAlreadySet() {

		SeqNoPrimaryTerm seqNoPrimaryTermOriginal = seqNoPrimaryTerm;
		seqNoPrimaryTerm = new SeqNoPrimaryTerm(7, 8);

		SampleEntity entity = new SampleEntity("1", "test");

		final IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(entity.getId());
		indexQuery.setObject(entity);
		indexQuery.setRouting("12");
		indexQuery.setSeqNo(seqNoPrimaryTermOriginal.getSequenceNumber());
		indexQuery.setPrimaryTerm(seqNoPrimaryTermOriginal.getPrimaryTerm());

		operations.index(indexQuery, IndexCoordinates.of(INDEX));

		ArgumentCaptor<IndexQuery> indexQueryCaptor = ArgumentCaptor.forClass(IndexQuery.class);
		verify(operations, times(2)).doIndex(indexQueryCaptor.capture(), any());

		final IndexQuery capturedIndexQuery = indexQueryCaptor.getValue();
		SampleEntity convertedEntity = (SampleEntity) capturedIndexQuery.getObject();

		final JoinField<String> joinField = convertedEntity.getJoinField();
		assertThat(joinField.getName()).isEqualTo("answer");
		assertThat(joinField.getParent()).isEqualTo("42");
		assertThat(capturedIndexQuery.getRouting()).isEqualTo("12");
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTermOriginal.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTermOriginal.getPrimaryTerm());
	}

	@Test // DATAES-972
	@DisplayName("should apply conversion result to IndexQuery in bulkIndex")
	void shouldApplyConversionResultToIndexQueryInBulkIndex() {

		SampleEntity entity = new SampleEntity("1", "test");

		final IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(entity.getId());
		indexQuery.setObject(entity);

		operations.bulkIndex(Collections.singletonList(indexQuery), SampleEntity.class);

		ArgumentCaptor<List<IndexQuery>> indexQueryListCaptor = ArgumentCaptor.forClass(List.class);
		verify(operations, times(1)).bulkOperation(indexQueryListCaptor.capture(), any(), any());

		final List<IndexQuery> capturedIndexQueries = indexQueryListCaptor.getValue();
		assertThat(capturedIndexQueries).hasSize(1);
		final IndexQuery capturedIndexQuery = capturedIndexQueries.get(0);
		SampleEntity convertedEntity = (SampleEntity) capturedIndexQuery.getObject();

		final JoinField<String> joinField = convertedEntity.getJoinField();
		assertThat(joinField.getName()).isEqualTo("answer");
		assertThat(joinField.getParent()).isEqualTo("42");
		assertThat(capturedIndexQuery.getRouting()).isEqualTo("42");
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.getSequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.getPrimaryTerm());
	}

	@Data
	@Document(indexName = INDEX)
	static class SampleEntity {
		@Id private String id;
		private String text;

		@JoinTypeRelations(relations = { @JoinTypeRelation(parent = "question",
				children = { "answer" }) }) @Nullable private JoinField<String> joinField;

		private SeqNoPrimaryTerm seqNoPrimaryTerm;

		public SampleEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
