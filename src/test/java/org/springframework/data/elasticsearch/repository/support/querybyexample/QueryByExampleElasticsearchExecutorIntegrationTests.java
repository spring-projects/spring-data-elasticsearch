/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support.querybyexample;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.elasticsearch.utils.IdGenerator.nextIdAsString;

/**
 * @author Ezequiel Antúnez Camacho
 */
@SpringIntegrationTest
abstract class QueryByExampleElasticsearchExecutorIntegrationTests {

	@Autowired private SampleElasticsearchRepository repository;
	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping();
	}

	@Test // #2418
	@org.junit.jupiter.api.Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #2418
	void shouldFindOne() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		SampleEntity probe = new SampleEntity();
		sampleEntity.setId(documentId);
		Optional<SampleEntity> entityFromElasticSearch = repository.findOne(Example.of(probe));

		// then
		assertThat(entityFromElasticSearch).contains(sampleEntity);

	}

	@Test // #2418
	void shouldThrowExceptionIfMoreThanOneResultInFindOne() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		// when
		SampleEntity probe = new SampleEntity();
		AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy = assertThatThrownBy(
				() -> repository.findOne(Example.of(probe)));

		// then
		assertThatThrownBy.isInstanceOf(IncorrectResultSizeDataAccessException.class);

	}

	@Test // #2418
	void shouldFindOneWithNestedField() {
		// given
		SampleEntity.SampleNestedEntity sampleNestedEntity = new SampleEntity.SampleNestedEntity();
		sampleNestedEntity.setNestedData("sampleNestedData");
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setSampleNestedEntity(sampleNestedEntity);
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		SampleEntity probe = new SampleEntity();
		sampleEntity.setSampleNestedEntity(sampleNestedEntity);
		Optional<SampleEntity> entityFromElasticSearch = repository.findOne(Example.of(probe));

		// then
		assertThat(entityFromElasticSearch).contains(sampleEntity);

	}

	@Test // #2418
	void shouldFindAll() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("hello world.");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(2);
	}

	@Test // #2418
	void shouldFindAllWithMatchers() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("hola mundo.");
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity3);

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("world");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe, ExampleMatcher.matching()
				.withMatcher("message", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.CONTAINING))));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(2);
	}

	@Test // #2418
	void shouldFindAllWithSort() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setRate(1);
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setRate(3);
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("hello world.");
		sampleEntity3.setRate(2);
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity3);

		// when
		SampleEntity probe = new SampleEntity();
		final Iterable<SampleEntity> all = repository.findAll();
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe), Sort.by(Sort.Direction.DESC, "rate"));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(3).containsExactly(sampleEntity2, sampleEntity3, sampleEntity);
	}

	@Test // #2418
	void shouldFindAllWithPageable() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setRate(1);
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setRate(3);
		sampleEntity2.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity2);

		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("hello world.");
		sampleEntity3.setRate(2);
		sampleEntity3.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity3);

		// when
		SampleEntity probe = new SampleEntity();
		Iterable<SampleEntity> page1 = repository.findAll(Example.of(probe),
				PageRequest.of(0, 2, Sort.Direction.DESC, "rate"));
		Iterable<SampleEntity> page2 = repository.findAll(Example.of(probe),
				PageRequest.of(1, 2, Sort.Direction.DESC, "rate"));

		// then
		assertThat(page1).isNotNull().hasSize(2).containsExactly(sampleEntity2, sampleEntity3);
		assertThat(page2).isNotNull().hasSize(1).containsExactly(sampleEntity);
	}

	@Test // #2418
	void shouldCount() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		Long count = repository.count(Example.of(sampleEntity));

		// then
		assertThat(count).isPositive();
	}

	@Test // #2418
	void shouldExists() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity);

		// when
		boolean exists = repository.exists(Example.of(sampleEntity));

		// then
		assertThat(exists).isTrue();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text, store = true, fielddata = true) private String type;
		@Nullable
		@Field(type = FieldType.Text, store = true, fielddata = true) private String message;
		@Nullable private Integer rate;
		@Nullable private Boolean available;
		@Nullable
		@Field(type = FieldType.Nested, store = true, fielddata = true) private SampleNestedEntity sampleNestedEntity;
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

		@Nullable
		public Integer getRate() {
			return rate;
		}

		public void setRate(Integer rate) {
			this.rate = rate;
		}

		@Nullable
		public Boolean isAvailable() {
			return available;
		}

		public void setAvailable(Boolean available) {
			this.available = available;
		}

		@Nullable
		public SampleNestedEntity getSampleNestedEntity() {
			return sampleNestedEntity;
		}

		public void setSampleNestedEntity(SampleNestedEntity sampleNestedEntity) {
			this.sampleNestedEntity = sampleNestedEntity;
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
			if (!Objects.equals(sampleNestedEntity, that.sampleNestedEntity))
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
			result = 31 * result + (sampleNestedEntity != null ? sampleNestedEntity.hashCode() : 0);
			result = 31 * result + (version != null ? version.hashCode() : 0);
			return result;
		}

		static class SampleNestedEntity {

			@Nullable
			@Field(type = FieldType.Text, store = true, fielddata = true) private String nestedData;

			@Nullable
			public String getNestedData() {
				return nestedData;
			}

			public void setNestedData(@Nullable String nestedData) {
				this.nestedData = nestedData;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;

				SampleNestedEntity that = (SampleNestedEntity) o;

				return Objects.equals(nestedData, that.nestedData);
			}

			@Override
			public int hashCode() {
				return nestedData != null ? nestedData.hashCode() : 0;
			}
		}
	}

	interface SampleElasticsearchRepository
			extends ElasticsearchRepository<SampleEntity, String>, QueryByExampleExecutor<SampleEntity> {}

}
