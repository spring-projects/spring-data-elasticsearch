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
import static org.springframework.data.elasticsearch.utils.IdGenerator.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
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

/**
 * @author Ezequiel Antúnez Camacho
 * @since 5.1
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
	@Order(Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	@Test // #2418
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setDocumentId(documentId2);
		Optional<SampleEntity> entityFromElasticSearch = repository.findOne(Example.of(probe));

		// then
		assertThat(entityFromElasticSearch).contains(sampleEntity2);

	}

	@Test // #2418
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("some message");
		final Example<SampleEntity> example = Example.of(probe);
		AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy = assertThatThrownBy(
				() -> repository.findOne(example));

		// then
		assertThatThrownBy.isInstanceOf(IncorrectResultSizeDataAccessException.class);

	}

	@Test // #2418
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2));

		// when
		SampleEntity.SampleNestedEntity sampleNestedEntityProbe = new SampleEntity.SampleNestedEntity();
		sampleNestedEntityProbe.setNestedData("sampleNestedData");
		SampleEntity probe = new SampleEntity();
		probe.setSampleNestedEntity(sampleNestedEntityProbe);

		Optional<SampleEntity> entityFromElasticSearch = repository.findOne(Example.of(probe));

		// then
		assertThat(entityFromElasticSearch).contains(sampleEntity);

	}

	@Test // #2418
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("hello world");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(2);
	}

	@Test // #2418
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

		repository.saveAll(List.of(sampleEntityWithRate11, sampleEntityWithRate13, sampleEntityWithRate22));

		// when
		SampleEntity probe = new SampleEntity();
		final Iterable<SampleEntity> all = repository.findAll();
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe), Sort.by(Sort.Direction.DESC, "rate"));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(3).containsExactly(sampleEntityWithRate22, sampleEntityWithRate13,
				sampleEntityWithRate11);
	}

	@Test // #2418
	void shouldFindAllWithPageable() {
		// given
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setDocumentId(nextIdAsString());
		sampleEntity.setMessage("hello world");
		sampleEntity.setRate(1);
		sampleEntity.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(nextIdAsString());
		sampleEntity2.setMessage("hello world");
		sampleEntity2.setRate(3);
		sampleEntity2.setVersion(System.currentTimeMillis());

		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setDocumentId(nextIdAsString());
		sampleEntity3.setMessage("hello world");
		sampleEntity3.setRate(2);
		sampleEntity3.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3));

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
		sampleEntity.setDocumentId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		String documentId2 = nextIdAsString();
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setDocumentId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		repository.saveAll(List.of(sampleEntity, sampleEntity2));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setDocumentId(documentId2);
		final long count = repository.count(Example.of(probe));

		// then
		assertThat(count).isPositive();
	}

	@Test // #2418
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setDocumentId(documentId2);
		boolean exists = repository.exists(Example.of(probe));

		// then
		assertThat(exists).isTrue();
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("hello world");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe, ExampleMatcher.matching()
				.withMatcher("message", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.DEFAULT))));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(1);
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("bye world");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe, ExampleMatcher.matching()
				.withMatcher("message", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.EXACT))));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(1);
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("h");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe, ExampleMatcher.matching()
				.withMatcher("message", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.STARTING))));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(2);
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("world");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe, ExampleMatcher.matching()
				.withMatcher("message", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.ENDING))));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(2);
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

		repository.saveAll(List.of(sampleEntity, sampleEntity2, sampleEntity3));

		// when
		SampleEntity probe = new SampleEntity();
		probe.setMessage("[(hello)(hola)].*");
		Iterable<SampleEntity> sampleEntities = repository.findAll(Example.of(probe, ExampleMatcher.matching()
				.withMatcher("message", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.REGEX))));

		// then
		assertThat(sampleEntities).isNotNull().hasSize(2);
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
		@Field(type = FieldType.Nested, store = true, fielddata = true) private SampleNestedEntity sampleNestedEntity;
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

				SampleNestedEntity that = (SampleNestedEntity) o;

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

	interface SampleElasticsearchRepository
			extends ElasticsearchRepository<SampleEntity, String>, QueryByExampleExecutor<SampleEntity> {}

}
