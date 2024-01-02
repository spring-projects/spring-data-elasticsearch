/*
 * Copyright 2023-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.*;
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import reactor.test.StepVerifier;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
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

/**
 * @author Ezequiel Antúnez Camacho
 * @since 5.1
 */
@SpringIntegrationTest
abstract class ReactiveQueryByExampleElasticsearchExecutorIntegrationTests {

	@Autowired private SampleReactiveElasticsearchRepository repository;
	@Autowired private ReactiveElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void before() {
		indexNameProvider.increment();
		operations.indexOps(SampleEntity.class).createWithMapping().as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // #2418
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
	}

	@Test
	void shouldFindOne() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2)) //
				.as(StepVerifier::create) //
				.expectNextCount(2L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setDocumentId(documentId2);
		repository.findOne(Example.of(probe))
				// then
				.as(StepVerifier::create) //
				.consumeNextWith(entityFromElasticSearch -> assertThat(entityFromElasticSearch).isEqualTo(sampleEntity2)) //
				.verifyComplete();
	}

	@Test
	void shouldThrowExceptionIfMoreThanOneResultInFindOne() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2)) //
				.as(StepVerifier::create) //
				.expectNextCount(2L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		repository.findOne(Example.of(probe))
				// then
				.as(StepVerifier::create) //
				.expectError(IncorrectResultSizeDataAccessException.class) //
				.verify();
	}

	@Test
	void shouldFindOneWithNestedField() {
		// given
		SampleEntity.SampleNestedEntity sampleNestedEntity = new SampleEntity.SampleNestedEntity();
		sampleNestedEntity.setNestedData("sampleNestedData");
		sampleNestedEntity.setAnotherNestedData("sampleAnotherNestedData");
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("some message");
		sampleEntity.setSampleNestedEntity(sampleNestedEntity);
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity.SampleNestedEntity sampleNestedEntity2 = new SampleEntity.SampleNestedEntity();
		sampleNestedEntity2.setNestedData("sampleNestedData2");
		sampleNestedEntity2.setAnotherNestedData("sampleAnotherNestedData2");
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("some message");
		sampleEntity2.setSampleNestedEntity(sampleNestedEntity2);
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2)) //
				.as(StepVerifier::create) //
				.expectNextCount(2L) //
				.verifyComplete();

		// when
		SampleEntity.SampleNestedEntity sampleNestedEntityProbe = new SampleEntity.SampleNestedEntity();
		sampleNestedEntityProbe.setNestedData("sampleNestedData");
		SampleEntity probe = new SampleEntity();
		probe.setSampleNestedEntity(sampleNestedEntityProbe);
		repository.findOne(Example.of(probe))
				// then
				.as(StepVerifier::create) //
				.expectNext(sampleEntity) //
				.verifyComplete();
	}

	@Test
	void shouldFindAll() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("hello world");
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("hello world");
		sampleEntity2.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setDocumentId(nextIdAsString());
		sampleEntity3.setMessage("bye world");
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3)) //
				.as(StepVerifier::create) //
				.expectNextCount(3L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("hello world");
		repository.findAll(Example.of(probe))
				// then
				.as(StepVerifier::create) //
				.expectNextSequence(List.of(sampleEntity, sampleEntity2)) //
				.verifyComplete();
	}

	@Test
	void shouldFindAllWithSort() {
		// given
		SampleEntity sampleEntityWithRate11 = new SampleEntity();
		sampleEntityWithRate11.setDocumentId(nextIdAsString());
		sampleEntityWithRate11.setMessage("hello world");
		sampleEntityWithRate11.setRate(11);
		sampleEntityWithRate11.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntityWithRate13 = new SampleEntity();
		sampleEntityWithRate13.setDocumentId(nextIdAsString());
		sampleEntityWithRate13.setMessage("hello world");
		sampleEntityWithRate13.setRate(13);
		sampleEntityWithRate13.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntityWithRate22 = new SampleEntity();
		sampleEntityWithRate22.setDocumentId(nextIdAsString());
		sampleEntityWithRate22.setMessage("hello world");
		sampleEntityWithRate22.setRate(22);
		sampleEntityWithRate22.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntityWithRate11, sampleEntityWithRate13, sampleEntityWithRate22)) //
				.as(StepVerifier::create) //
				.expectNextCount(3L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		repository.findAll(Example.of(probe), Sort.by(Sort.Direction.DESC, "rate"))
				// then
				.as(StepVerifier::create) //
				.expectNextSequence(List.of(sampleEntityWithRate22, sampleEntityWithRate13, sampleEntityWithRate11)) //
				.verifyComplete();
	}

	@Test
	void shouldCount() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2)) //
				.as(StepVerifier::create) //
				.expectNextCount(2L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setDocumentId(documentId2);
		repository.count(Example.of(probe))
				// then
				.as(StepVerifier::create) //
				.expectNext(1L) //
				.verifyComplete();
	}

	@Test
	void shouldExists() {
		// given
		String documentId = nextIdAsString();
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2)) //
				.as(StepVerifier::create) //
				.expectNextCount(2L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setDocumentId(documentId2);
		repository.exists(Example.of(probe))
				// then
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();
	}

	@Test // #2418
	void defaultStringMatcherShouldWork() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("hello world");
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("bye world");
		sampleEntity2.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setDocumentId(nextIdAsString());
		sampleEntity3.setMessage("hola mundo");
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3)) //
				.as(StepVerifier::create) //
				.expectNextCount(3L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("hello world");
		repository
				.findAll(Example.of(probe,
						ExampleMatcher.matching().withMatcher("message",
								ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.DEFAULT))))
				// then
				.as(StepVerifier::create) //
				.expectNext(sampleEntity) //
				.verifyComplete();
	}

	@Test // #2418
	void exactStringMatcherShouldWork() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("hello world");
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("bye world");
		sampleEntity2.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setDocumentId(nextIdAsString());
		sampleEntity3.setMessage("hola mundo");
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3)) //
				.as(StepVerifier::create) //
				.expectNextCount(3L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("bye world");
		repository
				.findAll(Example.of(probe,
						ExampleMatcher.matching().withMatcher("message",
								ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.EXACT))))
				// then
				.as(StepVerifier::create) //
				.expectNext(sampleEntity2) //
				.verifyComplete();
	}

	@Test // #2418
	void startingStringMatcherShouldWork() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("hello world");
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("bye world");
		sampleEntity2.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setDocumentId(nextIdAsString());
		sampleEntity3.setMessage("hola mundo");
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3)) //
				.as(StepVerifier::create) //
				.expectNextCount(3L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("h");
		repository
				.findAll(Example.of(probe,
						ExampleMatcher.matching().withMatcher("message",
								ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.STARTING))))
				// then
				.as(StepVerifier::create) //
				.expectNextSequence(List.of(sampleEntity, sampleEntity3)) //
				.verifyComplete();
	}

	@Test // #2418
	void endingStringMatcherShouldWork() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("hello world");
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("bye world");
		sampleEntity2.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setDocumentId(nextIdAsString());
		sampleEntity3.setMessage("hola mundo");
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3)) //
				.as(StepVerifier::create) //
				.expectNextCount(3L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("world");
		repository
				.findAll(Example.of(probe,
						ExampleMatcher.matching().withMatcher("message",
								ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.ENDING))))
				// then
				.as(StepVerifier::create) //
				.expectNextSequence(List.of(sampleEntity, sampleEntity2)) //
				.verifyComplete();
	}

	@Test // #2418
	void regexStringMatcherShouldWork() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("hello world");
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("bye world");
		sampleEntity2.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setDocumentId(nextIdAsString());
		sampleEntity3.setMessage("hola mundo");
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3)) //
				.as(StepVerifier::create) //
				.expectNextCount(3L) //
				.verifyComplete();

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("[(hello)(hola)].*");
		repository
				.findAll(Example.of(probe,
						ExampleMatcher.matching().withMatcher("message",
								ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.REGEX))))
				// then
				.as(StepVerifier::create) //
				.expectNextSequence(List.of(sampleEntity, sampleEntity3)) //
				.verifyComplete();
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {
		@Nullable
		@Id private String documentId;
		@Nullable
		@Field(type = FieldType.Text, store = true, fielddata = true) private String type;
		@Nullable
		@Field(type = FieldType.Keyword, store = true) private String message;
		@Nullable private Integer rate;
		@Nullable private Boolean available;
		@Nullable
		@Field(type = FieldType.Nested, store = true,
				fielddata = true) private SampleEntity.SampleNestedEntity sampleNestedEntity;
		@Nullable
		@Version private Long version;

		@Nullable
		public String getDocumentId() {
			return documentId;
		}

		public void setDocumentId(@Nullable String documentId) {
			this.documentId = documentId;
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
		public SampleEntity.SampleNestedEntity getSampleNestedEntity() {
			return sampleNestedEntity;
		}

		public void setSampleNestedEntity(SampleEntity.SampleNestedEntity sampleNestedEntity) {
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

			if (!Objects.equals(rate, that.rate))
				return false;
			if (available != that.available)
				return false;
			if (!Objects.equals(documentId, that.documentId))
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
			int result = documentId != null ? documentId.hashCode() : 0;
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
			@Field(type = FieldType.Text, store = true, fielddata = true) private String anotherNestedData;

			@Nullable
			public String getNestedData() {
				return nestedData;
			}

			public void setNestedData(@Nullable String nestedData) {
				this.nestedData = nestedData;
			}

			@Nullable
			public String getAnotherNestedData() {
				return anotherNestedData;
			}

			public void setAnotherNestedData(@Nullable String anotherNestedData) {
				this.anotherNestedData = anotherNestedData;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;

				SampleEntity.SampleNestedEntity that = (SampleEntity.SampleNestedEntity) o;

				return Objects.equals(nestedData, that.nestedData) && Objects.equals(anotherNestedData, that.anotherNestedData);
			}

			@Override
			public int hashCode() {
				int result = nestedData != null ? nestedData.hashCode() : 0;
				result = 31 * result + (anotherNestedData != null ? anotherNestedData.hashCode() : 0);
				return result;
			}
		}
	}

	interface SampleReactiveElasticsearchRepository
			extends ReactiveElasticsearchRepository<SampleEntity, String>, ReactiveQueryByExampleExecutor<SampleEntity> {}

}
