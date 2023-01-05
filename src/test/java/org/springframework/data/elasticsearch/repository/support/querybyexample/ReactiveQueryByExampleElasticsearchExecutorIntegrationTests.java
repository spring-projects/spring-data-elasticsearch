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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.lang.Nullable;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.elasticsearch.utils.IdGenerator.nextIdAsString;

/**
 * @author Ezequiel Antúnez Camacho
 */
@SpringIntegrationTest
abstract class ReactiveQueryByExampleElasticsearchExecutorIntegrationTests {

	@Autowired private SampleReactiveElasticsearchRepository repository;
	@Autowired private ReactiveElasticsearchOperations operations;
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

	@Test
	void shouldFindOne() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		SampleEntity probe = new SampleEntity();
		sampleEntity.setId(documentId);
		repository.save(sampleEntity).as(StepVerifier::create).expectNextCount(1L).verifyComplete();
		// when
		repository.findOne(Example.of(probe))
				// then
				.as(StepVerifier::create)
				.consumeNextWith(entityFromElasticSearch -> assertThat(entityFromElasticSearch).isEqualTo(sampleEntity)) //
				.verifyComplete();
	}

	@Test
	void shouldThrowExceptionIfMoreThanOneResultInFindOne() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(Arrays.asList(sampleEntity, sampleEntity2)).as(StepVerifier::create).expectNextCount(2L)
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		repository.findOne(Example.of(probe))
				// then
				.as(StepVerifier::create).expectError(IncorrectResultSizeDataAccessException.class).verify();
	}

	@Test
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
		repository.save(sampleEntity).as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		sampleEntity.setSampleNestedEntity(sampleNestedEntity);
		repository.findOne(Example.of(probe))
				// then
				.as(StepVerifier::create)
				.consumeNextWith(entityFromElasticSearch -> assertThat(entityFromElasticSearch).isEqualTo(sampleEntity)) //
				.verifyComplete();
	}

	@Test
	void shouldFindAll() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(Arrays.asList(sampleEntity, sampleEntity2)).as(StepVerifier::create).expectNextCount(2L)
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("hello world.");
		repository.findAll(Example.of(probe))
				// then
				.as(StepVerifier::create).expectNextSequence(Arrays.asList(sampleEntity, sampleEntity2)).verifyComplete();
	}

	@Test
	void shouldFindAllWithMatchers() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("bye world.");
		sampleEntity2.setVersion(System.currentTimeMillis());

		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("hola mundo.");
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(Arrays.asList(sampleEntity, sampleEntity2, sampleEntity3)).as(StepVerifier::create)
				.expectNextCount(3L).verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("world");
		repository
				.findAll(Example.of(probe,
						ExampleMatcher.matching().withMatcher("message",
								ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.CONTAINING))))
				// then
				.as(StepVerifier::create).expectNextSequence(Arrays.asList(sampleEntity, sampleEntity2)).verifyComplete();
	}

	@Test
	void shouldFindAllWithSort() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("hello world.");
		sampleEntity.setRate(1);
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("hello world.");
		sampleEntity2.setRate(3);
		sampleEntity2.setVersion(System.currentTimeMillis());

		String documentId3 = nextIdAsString();
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("hello world.");
		sampleEntity3.setRate(2);
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(Arrays.asList(sampleEntity, sampleEntity2, sampleEntity3)).as(StepVerifier::create)
				.expectNextCount(3L).verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		repository.findAll(Example.of(probe), Sort.by(Sort.Direction.DESC, "rate"))
				// then
				.as(StepVerifier::create).expectNextSequence(Arrays.asList(sampleEntity2, sampleEntity3, sampleEntity))
				.verifyComplete();
		;
	}

	@Test
	void shouldCount() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity).as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		// when
		repository.count(Example.of(sampleEntity))
				// then
				.as(StepVerifier::create).expectNext(1L).verifyComplete();
	}

	@Test
	void shouldExists() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		repository.save(sampleEntity).as(StepVerifier::create).expectNextCount(1L).verifyComplete();

		// when
		repository.exists(Example.of(sampleEntity))
				// then
				.as(StepVerifier::create).expectNext(true).verifyComplete();
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
			result = 31 * result + (rate != null ? rate.hashCode() : 0);
			result = 31 * result + (available != null ? available.hashCode() : 0);
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

	interface SampleReactiveElasticsearchRepository
			extends ReactiveElasticsearchRepository<SampleEntity, String>, ReactiveQueryByExampleExecutor<SampleEntity> {}

}
