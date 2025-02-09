/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.SearchTemplateQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;

public class ReactiveRepositorySearchTemplateQueryUnitTests extends ReactiveRepositoryQueryUnitTestsBase {

	@Test // #2997
	@DisplayName("should set searchtemplate id")
	void shouldSetSearchTemplateId() throws NoSuchMethodException {

		var query = createQuery("searchWithArgs", "answer", 42);

		assertThat(query).isInstanceOf(org.springframework.data.elasticsearch.core.query.SearchTemplateQuery.class);
		var searchTemplateQuery = (org.springframework.data.elasticsearch.core.query.SearchTemplateQuery) query;

		assertThat(searchTemplateQuery.getId()).isEqualTo("searchtemplate-42");
	}

	@Test // #2997
	@DisplayName("should set searchtemplate parameters")
	void shouldSetSearchTemplateParameters() throws NoSuchMethodException {

		var query = createQuery("searchWithArgs", "answer", 42);

		assertThat(query).isInstanceOf(org.springframework.data.elasticsearch.core.query.SearchTemplateQuery.class);
		var searchTemplateQuery = (org.springframework.data.elasticsearch.core.query.SearchTemplateQuery) query;

		var params = searchTemplateQuery.getParams();
		assertThat(params).isNotNull().hasSize(2);
		assertThat(params.get("stringArg")).isEqualTo("answer");
		assertThat(params.get("intArg")).isEqualTo(42);
	}

	// region helper methods
	private Query createQuery(String methodName, Object... args) throws NoSuchMethodException {
		Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
		ReactiveElasticsearchQueryMethod queryMethod = getQueryMethod(SampleRepository.class, methodName, argTypes);

		ReactiveRepositorySearchTemplateQuery repositorySearchTemplateQuery = queryForMethod(queryMethod);

		return repositorySearchTemplateQuery.createQuery(new ElasticsearchParametersParameterAccessor(queryMethod, args));
	}

	private ReactiveRepositorySearchTemplateQuery queryForMethod(ReactiveElasticsearchQueryMethod queryMethod) {
		return new ReactiveRepositorySearchTemplateQuery(queryMethod, operations, ValueExpressionDelegate.create(),
				queryMethod.getAnnotatedSearchTemplateQuery().id());
	}
	// endregion

	// region test data
	private interface SampleRepository extends ElasticsearchRepository<SampleEntity, String> {
		@SearchTemplateQuery(id = "searchtemplate-42")
		SearchHits<SampleEntity> searchWithArgs(String stringArg, Integer intArg);

		@SearchTemplateQuery(id = "searchtemplate-42")
		SearchHits<SampleEntity> searchWithArgsAndSort(String stringArg, Integer intArg, Sort sort);
	}

	@Document(indexName = "not-relevant")
	static class SampleEntity {
		@Nullable
		@Id String id;
		@Nullable String data;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getData() {
			return data;
		}

		public void setData(@Nullable String data) {
			this.data = data;
		}
	}
	// endregion
}
