/*
 * Copyright 2020-2024 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
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
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
@SpringIntegrationTest
abstract class CallbackIntegrationTests {

	@Autowired private ElasticsearchOperations originalOperations;
	// need a spy here on the abstract implementation class
	private AbstractElasticsearchTemplate operations;
	@Autowired private IndexNameProvider indexNameProvider;

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

		@Component
		static class SampleEntityAfterLoadCallback implements AfterLoadCallback<SampleEntity> {

			@Override
			public org.springframework.data.elasticsearch.core.document.Document onAfterLoad(
					org.springframework.data.elasticsearch.core.document.Document document, Class<SampleEntity> type,
					IndexCoordinates indexCoordinates) {

				document.put("className", document.get("_class"));
				return document;
			}
		}
	}

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		seqNoPrimaryTerm = null;
		operations = (AbstractElasticsearchTemplate) spy(originalOperations);

		IndexOperations indexOps = operations.indexOps(SampleEntity.class);
		indexOps.createWithMapping();

		// store one entity to have a seq_no and primary_term
		seqNoPrimaryTerm = operations.save(new SampleEntity("1", "initial")).getSeqNoPrimaryTerm();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		originalOperations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
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
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.sequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.primaryTerm());
	}

	@Test // DATAES-972
	@DisplayName("should apply conversion result to IndexQuery when not set ")
	void shouldApplyConversionResultToIndexQueryWhenNotSet() {

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
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.sequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.primaryTerm());
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
		indexQuery.setSeqNo(seqNoPrimaryTermOriginal.sequenceNumber());
		indexQuery.setPrimaryTerm(seqNoPrimaryTermOriginal.primaryTerm());

		operations.index(indexQuery, IndexCoordinates.of(indexNameProvider.indexName()));

		ArgumentCaptor<IndexQuery> indexQueryCaptor = ArgumentCaptor.forClass(IndexQuery.class);
		verify(operations, times(2)).doIndex(indexQueryCaptor.capture(), any());

		final IndexQuery capturedIndexQuery = indexQueryCaptor.getValue();
		SampleEntity convertedEntity = (SampleEntity) capturedIndexQuery.getObject();

		final JoinField<String> joinField = convertedEntity.getJoinField();
		assertThat(joinField.getName()).isEqualTo("answer");
		assertThat(joinField.getParent()).isEqualTo("42");
		assertThat(capturedIndexQuery.getRouting()).isEqualTo("12");
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTermOriginal.sequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTermOriginal.primaryTerm());
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
		assertThat(capturedIndexQuery.getSeqNo()).isEqualTo(seqNoPrimaryTerm.sequenceNumber());
		assertThat(capturedIndexQuery.getPrimaryTerm()).isEqualTo(seqNoPrimaryTerm.primaryTerm());
	}

	@Test // #2009
	@DisplayName("should invoke after load callback")
	void shouldInvokeAfterLoadCallback() {

		SampleEntity entity = new SampleEntity("1", "test");
		operations.save(entity);

		SampleEntity loaded = operations.get(entity.getId(), SampleEntity.class);

		assertThat(loaded).isNotNull();
		assertThat(loaded.className).isEqualTo(SampleEntity.class.getName());
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable private String text;

//		@ReadOnlyProperty
		@Nullable private String className;

		@Nullable
		@JoinTypeRelations(relations = {
				@JoinTypeRelation(parent = "question", children = { "answer" }) }) private JoinField<String> joinField;

		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;

		public SampleEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public JoinField<String> getJoinField() {
			return joinField;
		}

		public void setJoinField(@Nullable JoinField<String> joinField) {
			this.joinField = joinField;
		}

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}
}
