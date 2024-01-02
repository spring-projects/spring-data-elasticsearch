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
package org.springframework.data.elasticsearch.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Michael Wirth
 * @author Peter-Josef Meisch
 * @author Murali Chevuri
 */
@SpringIntegrationTest
abstract class ElasticsearchRepositoryIntegrationTests {

	@Autowired private SampleElasticsearchRepository repository;
	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping();
	}

	@Test
	@org.junit.jupiter.api.Order(Integer.MAX_VALUE)
	public void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test
	void shouldDoBulkIndexDocument() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		// when
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2));

		// then
		Optional<SampleEntity> entity1FromElasticSearch = repository.findById(documentId1);
		assertThat(entity1FromElasticSearch.isPresent()).isTrue();

		Optional<SampleEntity> entity2FromElasticSearch = repository.findById(documentId2);
		assertThat(entity2FromElasticSearch.isPresent()).isTrue();
	}

	@Test
	void shouldSaveDocument() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		// when
		repository.save(sampleEntity);

		// then
		Optional<SampleEntity> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch).isPresent();
	}

	@Test
	void throwExceptionWhenTryingToInsertWithVersionButWithoutId() {

		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		// when
		assertThatThrownBy(() -> repository.save(sampleEntity)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldFindDocumentById() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		Optional<SampleEntity> entityFromElasticSearch = repository.findById(documentId);

		// then
		assertThat(entityFromElasticSearch).isPresent();
		assertThat(entityFromElasticSearch.get()).isEqualTo(sampleEntity);
	}

	@Test
	void shouldReturnCountOfDocuments() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		Long count = repository.count();

		// then
		assertThat(count).isGreaterThanOrEqualTo(1L);
	}

	@Test
	void shouldFindAllDocuments() {

		// when
		Iterable<SampleEntity> results = repository.findAll();

		// then
		assertThat(results).isNotNull();
	}

	@Test
	void shouldDeleteDocument() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		repository.deleteById(documentId);

		// then
		Optional<SampleEntity> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch).isNotPresent();
	}

	@Test // DATAES-82, #2417
	void shouldFindAllByIdQuery() {

		// create more than 10 documents to see that the number of input ids is set as requested size
		int numEntities = 20;
		List<String> ids = new ArrayList<>(numEntities);
		List<SampleEntity> entities = new ArrayList<>(numEntities);
		for (int i = 0; i < numEntities; i++) {
			String documentId = nextIdAsString();
			ids.add(documentId);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage("hello world.");
			sampleEntity.setVersion(System.currentTimeMillis());
			entities.add(sampleEntity);
		}
		repository.saveAll(entities);

		Iterable<SampleEntity> sampleEntities = repository.findAllById(ids);

		assertThat(sampleEntities).isNotNull().hasSize(numEntities);
	}

	@Test
	void shouldSaveIterableEntities() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());

		Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity1, sampleEntity2);

		// when
		repository.saveAll(sampleEntities);

		// then
		Iterable<SampleEntity> entities = repository.findAll();
		assertThat(entities).hasSize(2);
	}

	@Test
	void shouldReturnTrueGivenDocumentWithIdExists() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertThat(exist).isTrue();
	}

	@Test // DATAES-363
	void shouldReturnFalseGivenDocumentWithIdDoesNotExist() {

		// given
		String documentId = nextIdAsString();

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertThat(exist).isFalse();
	}

	@Test
	void shouldDeleteAll() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		repository.deleteAll();

		// then
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).isEmpty();
	}

	@Test
	void shouldDeleteById() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		long result = repository.deleteSampleEntityById(documentId);

		// then
		Iterable<SampleEntity> sampleEntities = repository.searchById(documentId);
		assertThat(sampleEntities).isEmpty();
		assertThat(result).isEqualTo(1L);
	}

	@Test // DATAES-976
	void shouldDeleteAllById() {

		// given
		String id1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(id1);
		sampleEntity1.setMessage("hello world 1");
		sampleEntity1.setAvailable(true);
		sampleEntity1.setVersion(System.currentTimeMillis());

		String id2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(id2);
		sampleEntity2.setMessage("hello world 2");
		sampleEntity2.setAvailable(true);
		sampleEntity2.setVersion(System.currentTimeMillis());

		String id3 = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(id3);
		sampleEntity3.setMessage("hello world 3");
		sampleEntity3.setAvailable(false);
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		repository.deleteAllById(Arrays.asList(id1, id3));

		// then
		Assertions.assertThat(repository.findAll()).extracting(SampleEntity::getId).containsExactly(id2);
	}

	@Test
	void shouldDeleteByMessageAndReturnList() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world 1");
		sampleEntity1.setAvailable(true);
		sampleEntity1.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setMessage("hello world 2");
		sampleEntity2.setAvailable(true);
		sampleEntity2.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId);
		sampleEntity3.setMessage("hello world 3");
		sampleEntity3.setAvailable(false);
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		List<SampleEntity> result = repository.deleteByAvailable(true);

		// then
		assertThat(result).hasSize(2);
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(1);
	}

	@Test
	void shouldDeleteByListForMessage() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("hello world 1");
		sampleEntity1.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setMessage("hello world 2");
		sampleEntity2.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId);
		sampleEntity3.setMessage("hello world 3");
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		List<SampleEntity> result = repository.deleteByMessage("hello world 3");

		// then
		assertThat(result).hasSize(1);
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	void shouldDeleteByType() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setType("book");
		sampleEntity1.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId);
		sampleEntity2.setType("article");
		sampleEntity2.setVersion(System.currentTimeMillis());

		documentId = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId);
		sampleEntity3.setType("image");
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		repository.deleteByType("article");

		// then
		Iterable<SampleEntity> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	void shouldDeleteEntity() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		repository.delete(sampleEntity);

		// then
		Iterable<SampleEntity> sampleEntities = repository.searchById(documentId);
		assertThat(sampleEntities).isEmpty();
	}

	@Test
	void shouldReturnIterableEntities() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity1);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> sampleEntities = repository.searchById(documentId1);

		// then
		assertThat(sampleEntities).isNotNull();
	}

	@Test
	void shouldDeleteIterableEntities() {

		// given
		String documentId1 = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage("hello world.");
		sampleEntity1.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		Iterable<SampleEntity> sampleEntities = Arrays.asList(sampleEntity2, sampleEntity2);

		// when
		repository.deleteAll(sampleEntities);

		// then
		Assertions.assertThat(repository.findById(documentId1)).isNotPresent();
		Assertions.assertThat(repository.findById(documentId2)).isNotPresent();
	}

	@Test
	void shouldIndexEntity() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setVersion(System.currentTimeMillis());
		sampleEntity.setMessage("some message");

		// when
		repository.save(sampleEntity);

		// then
		Iterable<SampleEntity> entities = repository.findAll();
		assertThat(entities).hasSize(1);
	}

	@Test
	void shouldSortByGivenField() {

		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("world");
		repository.save(sampleEntity);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello");
		repository.save(sampleEntity2);

		// when
		Iterable<SampleEntity> sampleEntities = repository.findAll(Sort.by(Order.asc("message")));

		// then
		assertThat(sampleEntities).isNotNull();
	}

	@Test
	void shouldReturnSimilarEntities() {

		// given
		String sampleMessage = "So we build a web site or an application and want to add search to it, "
				+ "and then it hits us: getting search working is hard. We want our search solution to be fast,"
				+ " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, "
				+ "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, "
				+ "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";

		List<SampleEntity> sampleEntities = createSampleEntitiesWithMessage(sampleMessage, 30);
		repository.saveAll(sampleEntities);

		// when
		Page<SampleEntity> results = repository.searchSimilar(sampleEntities.get(0), new String[] { "message" },
				PageRequest.of(0, 5));

		// then
		assertThat(results.getTotalElements()).isGreaterThanOrEqualTo(1L);
	}

	@Test // DATAES-142
	void shouldIndexNotEmptyList() {
		// given
		List<SampleEntity> list = new ArrayList<>();
		String documentId = nextIdAsString();
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("world");
		list.add(sampleEntity1);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello");
		list.add(sampleEntity2);

		Iterable<SampleEntity> savedEntities = repository.saveAll(list);

		assertThat(savedEntities).containsExactlyElementsOf(list);
	}

	@Test // DATAES-142
	void shouldNotFailOnIndexingEmptyList() {
		Iterable<SampleEntity> savedEntities = repository.saveAll(Collections.emptyList());

		assertThat(savedEntities).hasSize(0);
	}

	@Test // DATAES-832
	void shouldNotReturnNullValuesInFindAllById() throws IOException {

		// given
		String documentId1 = "id-one";
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		repository.save(sampleEntity1);
		String documentId2 = "id-two";
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		repository.save(sampleEntity2);
		String documentId3 = "id-three";
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		repository.save(sampleEntity3);

		Iterable<SampleEntity> allById = repository
				.findAllById(Arrays.asList("id-one", "does-not-exist", "id-two", "where-am-i", "id-three"));
		List<SampleEntity> results = StreamUtils.createStreamFromIterator(allById.iterator()).collect(Collectors.toList());

		assertThat(results).hasSize(3);
		assertThat(results.stream().map(SampleEntity::getId).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("id-one", "id-two", "id-three");
	}

	private static List<SampleEntity> createSampleEntitiesWithMessage(String message, int numberOfEntities) {

		List<SampleEntity> sampleEntities = new ArrayList<>();
		long idBase = (long) (Math.random() * 100);
		long versionBase = System.currentTimeMillis();

		for (int i = 0; i < numberOfEntities; i++) {
			String documentId = String.valueOf(idBase + i);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage(message);
			sampleEntity.setRate(2);
			sampleEntity.setVersion(versionBase + i);
			sampleEntities.add(sampleEntity);
		}
		return sampleEntities;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text, store = true, fielddata = true) private String type;
		@Nullable
		@Field(type = FieldType.Text, store = true, fielddata = true) private String message;
		@Nullable private int rate;
		@Nullable private boolean available;
		@Nullable
		@Version private Long version;

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

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		@Nullable
		public java.lang.Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable java.lang.Long version) {
			this.version = version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			SampleEntity that = (SampleEntity) o;

			if (rate != that.rate)
				return false;
			if (available != that.available)
				return false;
			if (!Objects.equals(id, that.id))
				return false;
			if (!Objects.equals(type, that.type))
				return false;
			if (!Objects.equals(message, that.message))
				return false;
			return Objects.equals(version, that.version);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (message != null ? message.hashCode() : 0);
			result = 31 * result + rate;
			result = 31 * result + (available ? 1 : 0);
			result = 31 * result + (version != null ? version.hashCode() : 0);
			return result;
		}
	}

	interface SampleElasticsearchRepository extends ElasticsearchRepository<SampleEntity, String> {

		long deleteSampleEntityById(String id);

		List<SampleEntity> deleteByAvailable(boolean available);

		List<SampleEntity> deleteByMessage(String message);

		void deleteByType(String type);

		Iterable<SampleEntity> searchById(String id);
	}

}
