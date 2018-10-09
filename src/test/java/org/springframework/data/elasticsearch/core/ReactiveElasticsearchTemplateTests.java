/*
 * Copyright 2018. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.elasticsearch.core;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.assertj.core.api.Assertions.*;

import reactor.test.StepVerifier;

import java.util.List;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.entities.SampleEntity;

/**
 * @author Christoph Strobl
 * @currentRead Golden Fool - Robin Hobb
 */
public class ReactiveElasticsearchTemplateTests {

	private ElasticsearchRestTemplate restTemplate;
	private ReactiveElasticsearchTemplate template;

	@Before
	public void setUp() {

		restTemplate = new ElasticsearchRestTemplate(
				new RestHighLevelClient(RestClient.builder(HttpHost.create("http://localhost:9200"))));

		restTemplate.deleteIndex(SampleEntity.class);
		restTemplate.createIndex(SampleEntity.class);
		restTemplate.putMapping(SampleEntity.class);

		restTemplate.refresh(SampleEntity.class);

		template = new ReactiveElasticsearchTemplate(
				ReactiveElasticsearchClient.create("http://localhost:9201", "http://localhost:9200"));
	}

	@Test // DATAES-488
	public void indexWithIdShouldWork() {

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("foo bar")
				.version(System.currentTimeMillis()).build();

		template.index(sampleEntity).as(StepVerifier::create).expectNextCount(1).verifyComplete();

		restTemplate.refresh(SampleEntity.class);

		List<SampleEntity> result = restTemplate
				.queryForList(new CriteriaQuery(Criteria.where("message").is(sampleEntity.getMessage())), SampleEntity.class);
		assertThat(result).hasSize(1);
	}

	@Test // DATAES-488
	public void getShouldReturnEntity() {

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		restTemplate.index(indexQuery);
		restTemplate.refresh(SampleEntity.class);

		template.get(documentId, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(sampleEntity) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void getForNothing() {

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		restTemplate.index(indexQuery);
		restTemplate.refresh(SampleEntity.class);

		template.get("foo", SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void findShouldApplyCriteria() {

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		restTemplate.index(indexQuery);
		restTemplate.refresh(SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(Criteria.where("message").is("some message"));

		template.query(criteriaQuery, SampleEntity.class) //
				.as(StepVerifier::create) //
				.expectNext(sampleEntity) //
				.verifyComplete();
	}

	@Test // DATAES-488
	public void findShouldReturnEmptyFluxIfNothingFound() {

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		restTemplate.index(indexQuery);
		restTemplate.refresh(SampleEntity.class);

		CriteriaQuery criteriaQuery = new CriteriaQuery(Criteria.where("message").is("foo"));

		template.query(criteriaQuery, SampleEntity.class) //
				.as(StepVerifier::create) //
				.verifyComplete();
	}

	private IndexQuery getIndexQuery(SampleEntity sampleEntity) {
		return new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity)
				.withVersion(sampleEntity.getVersion()).build();
	}
}
