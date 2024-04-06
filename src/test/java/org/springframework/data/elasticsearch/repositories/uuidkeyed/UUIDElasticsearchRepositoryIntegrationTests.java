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
package org.springframework.data.elasticsearch.repositories.uuidkeyed;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Gad Akuka
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Michael Wirth
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class UUIDElasticsearchRepositoryIntegrationTests {

	@Autowired private SampleUUIDKeyedElasticsearchRepository repository;

	@Autowired ElasticsearchOperations operations;
    @Autowired IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
        operations.indexOps(SampleEntityUUIDKeyed.class).createWithMapping();
	}

	@Test
	@org.junit.jupiter.api.Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
	}

	@Test
	public void shouldDoBulkIndexDocument() {

		// given
		UUID documentId1 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId1);
		sampleEntityUUIDKeyed1.setMessage("some message");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("some message");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		// when
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2));

		// then
		Optional<SampleEntityUUIDKeyed> entity1FromElasticSearch = repository.findById(documentId1);
		assertThat(entity1FromElasticSearch).isPresent();

		Optional<SampleEntityUUIDKeyed> entity2FromElasticSearch = repository.findById(documentId2);
		assertThat(entity2FromElasticSearch).isPresent();
	}

	@Test
	public void shouldSaveDocument() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());

		// when
		repository.save(sampleEntityUUIDKeyed);

		// then
		Optional<SampleEntityUUIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch).isPresent();
	}

	@Test
	public void shouldFindDocumentById() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		// when
		Optional<SampleEntityUUIDKeyed> entityFromElasticSearch = repository.findById(documentId);

		// then
		assertThat(entityFromElasticSearch).isPresent();
		assertThat(entityFromElasticSearch.get()).isEqualTo(sampleEntityUUIDKeyed);
	}

	@Test
	public void shouldReturnCountOfDocuments() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		// when
		Long count = repository.count();

		// then
		assertThat(count).isGreaterThanOrEqualTo(1L);
	}

	@Test
	public void shouldFindAllDocuments() {

		// when
		Iterable<SampleEntityUUIDKeyed> results = repository.findAll();

		// then
		assertThat(results).isNotNull();
	}

	@Test
	public void shouldDeleteDocument() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("some message");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		// when
		repository.deleteById(documentId);

		// then
		Optional<SampleEntityUUIDKeyed> entityFromElasticSearch = repository.findById(documentId);
		assertThat(entityFromElasticSearch).isNotPresent();
	}

	@Test // DATAES-82
	public void shouldFindAllByIdQuery() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed2);

		// when
		List<UUID> docIds = Arrays.asList(documentId, documentId2);
		List<SampleEntityUUIDKeyed> sampleEntities = (List<SampleEntityUUIDKeyed>) repository.findAllById(docIds);

		// then
		assertThat(sampleEntities).isNotNull().hasSize(2);
		assertThat(sampleEntities.get(0).getId()).isIn(docIds);
		assertThat(sampleEntities.get(1).getId()).isIn(docIds);
	}

	@Test
	public void shouldSaveIterableEntities() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setMessage("hello world.");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		Iterable<SampleEntityUUIDKeyed> sampleEntities = Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2);

		// when
		repository.saveAll(sampleEntities);

		// then
		Iterable<SampleEntityUUIDKeyed> entities = repository.findAll();
		assertThat(entities).hasSize(2);
	}

	@Test
	public void shouldReturnTrueGivenDocumentWithIdExists() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertThat(exist).isTrue();
	}

	@Test // DATAES-363
	public void shouldReturnFalseGivenDocumentWithIdDoesNotExist() {

		// given
		UUID documentId = UUID.randomUUID();

		// when
		boolean exist = repository.existsById(documentId);

		// then
		assertThat(exist).isFalse();
	}

	@Test
	public void shouldDeleteAll() {

		// when
		repository.deleteAll();

		// then
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.findAll();
		assertThat(sampleEntities).isEmpty();
	}

	@Test
	public void shouldDeleteById() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		// when
		long result = repository.deleteSampleEntityUUIDKeyedById(documentId);

		// then
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.findAll();
		assertThat(sampleEntities).isEmpty();
		assertThat(result).isEqualTo(1L);
	}

	@Test
	public void shouldDeleteByMessageAndReturnList() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setMessage("hello world 1");
		sampleEntityUUIDKeyed1.setAvailable(true);
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId);
		sampleEntityUUIDKeyed2.setMessage("hello world 2");
		sampleEntityUUIDKeyed2.setAvailable(true);
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed3 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed3.setId(documentId);
		sampleEntityUUIDKeyed3.setMessage("hello world 3");
		sampleEntityUUIDKeyed3.setAvailable(false);
		sampleEntityUUIDKeyed3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed3));

		// when
		List<SampleEntityUUIDKeyed> result = repository.deleteByAvailable(true);

		// then
		assertThat(result).hasSize(2);
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(1);
	}

	@Test
	public void shouldDeleteByListForMessage() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setMessage("hello world 1");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId);
		sampleEntityUUIDKeyed2.setMessage("hello world 2");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed3 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed3.setId(documentId);
		sampleEntityUUIDKeyed3.setMessage("hello world 3");
		sampleEntityUUIDKeyed3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed3));

		// when
		List<SampleEntityUUIDKeyed> result = repository.deleteByMessage("hello world 3");

		// then
		assertThat(result).hasSize(1);
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	public void shouldDeleteByType() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId);
		sampleEntityUUIDKeyed1.setType("book");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId);
		sampleEntityUUIDKeyed2.setType("article");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());

		documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed3 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed3.setId(documentId);
		sampleEntityUUIDKeyed3.setType("image");
		sampleEntityUUIDKeyed3.setVersion(System.currentTimeMillis());
		repository.saveAll(Arrays.asList(sampleEntityUUIDKeyed1, sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed3));

		// when
		repository.deleteByType("article");

		// then
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.findAll();
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	public void shouldDeleteEntity() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("hello world.");
		sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed);

		// when
		repository.delete(sampleEntityUUIDKeyed);

		// then
		Optional<SampleEntityUUIDKeyed> sampleEntities = repository.findById(documentId);
		assertThat(sampleEntities).isEmpty();
	}

	@Test
	public void shouldReturnIterableEntities() {

		// given
		UUID documentId1 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId1);
		sampleEntityUUIDKeyed1.setMessage("hello world.");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed1);

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed2);

		// when
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.searchById(documentId1);

		// then
		assertThat(sampleEntities).isNotNull();
	}

	@Test
	public void shouldDeleteIterableEntities() {

		// given
		UUID documentId1 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed1 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed1.setId(documentId1);
		sampleEntityUUIDKeyed1.setMessage("hello world.");
		sampleEntityUUIDKeyed1.setVersion(System.currentTimeMillis());

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello world.");
		sampleEntityUUIDKeyed2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntityUUIDKeyed2);

		Iterable<SampleEntityUUIDKeyed> sampleEntities = Arrays.asList(sampleEntityUUIDKeyed2, sampleEntityUUIDKeyed2);

		// when
		repository.deleteAll(sampleEntities);

		// then
		assertThat(repository.findById(documentId1)).isNotPresent();
		assertThat(repository.findById(documentId2)).isNotPresent();
	}

	@Test
	public void shouldSortByGivenField() {

		// given
		UUID documentId = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed.setId(documentId);
		sampleEntityUUIDKeyed.setMessage("world");
		repository.save(sampleEntityUUIDKeyed);

		UUID documentId2 = UUID.randomUUID();
		SampleEntityUUIDKeyed sampleEntityUUIDKeyed2 = new SampleEntityUUIDKeyed();
		sampleEntityUUIDKeyed2.setId(documentId2);
		sampleEntityUUIDKeyed2.setMessage("hello");
		repository.save(sampleEntityUUIDKeyed2);

		// when
		Iterable<SampleEntityUUIDKeyed> sampleEntities = repository.findAll(Sort.by(Order.asc("message")));

		// then
		assertThat(sampleEntities).isNotNull();
	}

	@Test
	public void shouldReturnSimilarEntities() {

		// given
		String sampleMessage = "So we build a web site or an application and want to add search to it, "
				+ "and then it hits us: getting search working is hard. We want our search solution to be fast,"
				+ " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, "
				+ "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, "
				+ "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";

		List<SampleEntityUUIDKeyed> sampleEntities = createSampleEntitiesWithMessage(sampleMessage, 30);
		repository.saveAll(sampleEntities);

		// when
		Page<SampleEntityUUIDKeyed> results = repository.searchSimilar(sampleEntities.get(0), new String[] { "message" },
				PageRequest.of(0, 5));

		// then
		assertThat(results.getTotalElements()).isGreaterThanOrEqualTo(1);
	}

	private static List<SampleEntityUUIDKeyed> createSampleEntitiesWithMessage(String message, int numberOfEntities) {
		List<SampleEntityUUIDKeyed> sampleEntities = new ArrayList<>();
		for (int i = 0; i < numberOfEntities; i++) {
			UUID documentId = UUID.randomUUID();
			SampleEntityUUIDKeyed sampleEntityUUIDKeyed = new SampleEntityUUIDKeyed();
			sampleEntityUUIDKeyed.setId(documentId);
			sampleEntityUUIDKeyed.setMessage(message);
			sampleEntityUUIDKeyed.setRate(2);
			sampleEntityUUIDKeyed.setVersion(System.currentTimeMillis());
			sampleEntities.add(sampleEntityUUIDKeyed);
		}
		return sampleEntities;
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntityUUIDKeyed {
		@Nullable
		@Id private UUID id;
		@Nullable private String type;
		@Nullable
		@Field(type = FieldType.Text, fielddata = true) private String message;
		@Nullable private int rate;
		@Nullable
		@ScriptedField private Long scriptedRate;
		@Nullable private boolean available;
		@Nullable private String highlightedMessage;
		@Nullable private GeoPoint location;
		@Nullable
		@Version private Long version;

		@Nullable
		public UUID getId() {
			return id;
		}

		public void setId(@Nullable UUID id) {
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
		public Long getScriptedRate() {
			return scriptedRate;
		}

		public void setScriptedRate(@Nullable Long scriptedRate) {
			this.scriptedRate = scriptedRate;
		}

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		@Nullable
		public String getHighlightedMessage() {
			return highlightedMessage;
		}

		public void setHighlightedMessage(@Nullable String highlightedMessage) {
			this.highlightedMessage = highlightedMessage;
		}

		@Nullable
		public GeoPoint getLocation() {
			return location;
		}

		public void setLocation(@Nullable GeoPoint location) {
			this.location = location;
		}

		@Nullable
		public Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable Long version) {
			this.version = version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof SampleEntityUUIDKeyed that))
				return false;

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
			if (!Objects.equals(scriptedRate, that.scriptedRate))
				return false;
			if (!Objects.equals(highlightedMessage, that.highlightedMessage))
				return false;
			if (!Objects.equals(location, that.location))
				return false;
			return Objects.equals(version, that.version);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (message != null ? message.hashCode() : 0);
			result = 31 * result + rate;
			result = 31 * result + (scriptedRate != null ? scriptedRate.hashCode() : 0);
			result = 31 * result + (available ? 1 : 0);
			result = 31 * result + (highlightedMessage != null ? highlightedMessage.hashCode() : 0);
			result = 31 * result + (location != null ? location.hashCode() : 0);
			result = 31 * result + (version != null ? version.hashCode() : 0);
			return result;
		}
	}

	/**
	 * @author Gad Akuka
	 * @author Christoph Strobl
	 */
	interface SampleUUIDKeyedElasticsearchRepository extends ElasticsearchRepository<SampleEntityUUIDKeyed, UUID> {

		List<SampleEntityUUIDKeyed> searchById(UUID uuid);

		long deleteSampleEntityUUIDKeyedById(UUID id);

		List<SampleEntityUUIDKeyed> deleteByAvailable(boolean available);

		List<SampleEntityUUIDKeyed> deleteByMessage(String message);

		void deleteByType(String type);

	}
}
