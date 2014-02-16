/*
 * Copyright 2013 the original author or authors.
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
import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.*;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.SampleEntity;
import org.springframework.data.elasticsearch.SampleEntityBuilder;
import org.springframework.data.elasticsearch.SampleMappingEntity;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateTests {

	private static final String INDEX_NAME = "test-index";
	private static final String TYPE_NAME = "test-type";

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
	}

	@Test
	public void shouldReturnCountForGivenSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		// when
		long count = elasticsearchTemplate.count(searchQuery, SampleEntity.class);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	@Test
	public void shouldReturnObjectForGivenId() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		// when
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		// then
		assertNotNull("not null....", sampleEntity1);
		assertEquals(sampleEntity, sampleEntity1);
	}

	@Test
	public void shouldReturnPageForGivenSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities, is(notNullValue()));
		assertThat(sampleEntities.getTotalElements(), greaterThanOrEqualTo(1L));
	}

	@Test
	public void shouldDoBulkIndex() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);

		indexQueries.add(indexQuery2);
		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements(), is(equalTo(2L)));
	}

	@Test
	public void shouldDeleteDocumentForGivenId() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		// when
		elasticsearchTemplate.delete(INDEX_NAME, TYPE_NAME, documentId);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldDeleteEntityForGivenId() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		// when
		elasticsearchTemplate.delete(SampleEntity.class, documentId);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldDeleteDocumentForGivenQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		// when
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(termQuery("id", documentId));
		elasticsearchTemplate.delete(deleteQuery, SampleEntity.class);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldFilterSearchResultsForGivenFilter() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withFilter(boolFilter().must(termFilter("id", documentId))).build();
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities.getTotalElements(), equalTo(1L));
	}

	@Test
	public void shouldSortResultsGivenSortCriteria() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("abc");
		sampleEntity1.setRate(10);
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("xyz");
		sampleEntity2.setRate(5);
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("xyz");
		sampleEntity3.setRate(15);
		sampleEntity3.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery3 = new IndexQuery();
		indexQuery3.setId(documentId3);
		indexQuery3.setObject(sampleEntity3);

		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);
		indexQueries.add(indexQuery3);

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withSort(new FieldSortBuilder("rate").ignoreUnmapped(true).order(SortOrder.ASC)).build();
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities.getTotalElements(), equalTo(3L));
		assertThat(sampleEntities.getContent().get(0).getRate(), is(sampleEntity2.getRate()));
	}

	@Test
	public void shouldExecuteStringQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		StringQuery stringQuery = new StringQuery(matchAllQuery().toString());
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities.getTotalElements(), equalTo(1L));
	}

	@Test
	public void shouldReturnPageableResultsGivenStringQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		StringQuery stringQuery = new StringQuery(matchAllQuery().toString(), new PageRequest(0, 10));
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	@Test
	@Ignore("By default, the search request will fail if there is no mapping associated with a field. The ignore_unmapped option allows to ignore fields that have no mapping and not sort by them")
	public void shouldReturnSortedPageableResultsGivenStringQuery() {
		// todo
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		StringQuery stringQuery = new StringQuery(matchAllQuery().toString(), new PageRequest(0, 10), new Sort(
				new Sort.Order(Sort.Direction.ASC, "messsage")));
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities.getTotalElements(), is(greaterThanOrEqualTo(1L)));
	}

	@Test
	public void shouldReturnObjectMatchingGivenStringQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		StringQuery stringQuery = new StringQuery(termQuery("id", documentId).toString());
		// when
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(stringQuery, SampleEntity.class);
		// then
		assertThat(sampleEntity1, is(notNullValue()));
		assertThat(sampleEntity1.getId(), is(equalTo(documentId)));
	}

	@Test
	public void shouldCreateIndexGivenEntityClass() {
		// when
		boolean created = elasticsearchTemplate.createIndex(SampleEntity.class);
		// then
		assertThat(created, is(true));
	}

	@Test
	public void shouldExecuteGivenCriteriaQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some test message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

		// when
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat(sampleEntity1, is(notNullValue()));
	}

	@Test
	public void shouldReturnSpecifiedFields() {
		// given
		String documentId = randomNumeric(5);
		String message = "some test message";
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage(message);
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME).withFields("message").build();
		// when
		Page<String> page = elasticsearchTemplate.queryForPage(searchQuery, String.class, new SearchResultMapper() {
			@Override
			public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<String> values = new ArrayList<String>();
				for (SearchHit searchHit : response.getHits()) {
					values.add((String) searchHit.field("message").value());
				}
				return new FacetedPageImpl<T>((List<T>) values);
			}
		});
		// then
		assertThat(page, is(notNullValue()));
		assertThat(page.getTotalElements(), is(equalTo(1L)));
		assertThat(page.getContent().get(0), is(message));
	}

	@Test
	public void shouldReturnSimilarResultsGivenMoreLikeThisQuery() {
		// given
		String sampleMessage = "So we build a web site or an application and want to add search to it, "
				+ "and then it hits us: getting search working is hard. We want our search solution to be fast,"
				+ " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, "
				+ "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, "
				+ "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";

		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId1);
		sampleEntity1.setMessage(sampleMessage);
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId1);
		indexQuery1.setObject(sampleEntity1);

		elasticsearchTemplate.index(indexQuery1);

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage(sampleMessage);
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);

		elasticsearchTemplate.index(indexQuery2);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		MoreLikeThisQuery moreLikeThisQuery = new MoreLikeThisQuery();
		moreLikeThisQuery.setId(documentId2);
		moreLikeThisQuery.addFields("message");
		moreLikeThisQuery.setMinDocFreq(1);
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.moreLikeThis(moreLikeThisQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements(), is(equalTo(1L)));
		assertThat(sampleEntities.getContent(), hasItem(sampleEntity1));
	}

	@Test
	public void shouldReturnResultsWithScanAndScroll() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME).withPageable(new PageRequest(0, 10)).build();

		String scrollId = elasticsearchTemplate.scan(searchQuery, 1000, false);
		List<SampleEntity> sampleEntities = new ArrayList<SampleEntity>();
		boolean hasRecords = true;
		while (hasRecords) {
			Page<SampleEntity> page = elasticsearchTemplate.scroll(scrollId, 5000L, SampleEntity.class);
			if (page.hasContent()) {
				sampleEntities.addAll(page.getContent());
			} else {
				hasRecords = false;
			}
		}
		assertThat(sampleEntities.size(), is(equalTo(30)));
	}

	@Test
	public void shouldReturnResultsForScanAndScrollWithCustomResultMapper() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME).withPageable(new PageRequest(0, 10)).build();

		String scrollId = elasticsearchTemplate.scan(searchQuery, 1000, false);
		List<SampleEntity> sampleEntities = new ArrayList<SampleEntity>();
		boolean hasRecords = true;
		while (hasRecords) {
			Page<SampleEntity> page = elasticsearchTemplate.scroll(scrollId, 5000L, new SearchResultMapper() {
				@Override
				public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
					List<SampleEntity> chunk = new ArrayList<SampleEntity>();
					for (SearchHit searchHit : response.getHits()) {
						if (response.getHits().getHits().length <= 0) {
							return null;
						}
						SampleEntity user = new SampleEntity();
						user.setId(searchHit.getId());
						user.setMessage((String) searchHit.getSource().get("message"));
						chunk.add(user);
					}
					if (chunk.size() > 0) {
						return new FacetedPageImpl<T>((List<T>) chunk);
					}
					return null;
				}
			});
			if (page != null) {
				sampleEntities.addAll(page.getContent());
			} else {
				hasRecords = false;
			}
		}
		assertThat(sampleEntities.size(), is(equalTo(30)));
	}

	private static List<IndexQuery> createSampleEntitiesWithMessage(String message, int numberOfEntities) {
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		for (int i = 0; i < numberOfEntities; i++) {
			String documentId = randomNumeric(5);
			SampleEntity sampleEntity = new SampleEntity();
			sampleEntity.setId(documentId);
			sampleEntity.setMessage(message);
			sampleEntity.setRate(2);
			sampleEntity.setVersion(System.currentTimeMillis());
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(documentId);
			indexQuery.setObject(sampleEntity);
			indexQueries.add(indexQuery);
		}
		return indexQueries;
	}

	@Test
	public void shouldReturnListForGivenCriteria() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("test message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test test");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);

		indexQueries.add(indexQuery2);

		// second document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("some message");
		sampleEntity3.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery3 = new IndexQuery();
		indexQuery3.setId(documentId3);
		indexQuery3.setObject(sampleEntity3);

		indexQueries.add(indexQuery3);
		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// when
		CriteriaQuery singleCriteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));
		CriteriaQuery multipleCriteriaQuery = new CriteriaQuery(new Criteria("message").contains("some").and("message")
				.contains("message"));
		List<SampleEntity> sampleEntitiesForSingleCriteria = elasticsearchTemplate.queryForList(singleCriteriaQuery,
				SampleEntity.class);
		List<SampleEntity> sampleEntitiesForAndCriteria = elasticsearchTemplate.queryForList(multipleCriteriaQuery,
				SampleEntity.class);
		// then
		assertThat(sampleEntitiesForSingleCriteria.size(), is(2));
		assertThat(sampleEntitiesForAndCriteria.size(), is(1));
	}

	@Test
	public void shouldReturnListForGivenStringQuery() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setId(documentId);
		sampleEntity1.setMessage("test message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId(documentId);
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setId(documentId2);
		sampleEntity2.setMessage("test test");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId(documentId2);
		indexQuery2.setObject(sampleEntity2);

		indexQueries.add(indexQuery2);

		// second document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setId(documentId3);
		sampleEntity3.setMessage("some message");
		sampleEntity3.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery3 = new IndexQuery();
		indexQuery3.setId(documentId3);
		indexQuery3.setObject(sampleEntity3);

		indexQueries.add(indexQuery3);
		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// when
		StringQuery stringQuery = new StringQuery(matchAllQuery().toString());
		List<SampleEntity> sampleEntities = elasticsearchTemplate.queryForList(stringQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities.size(), is(3));
	}

	@Test
	public void shouldPutMappingForGivenEntity() throws Exception {
		// given
		Class entity = SampleMappingEntity.class;
		elasticsearchTemplate.createIndex(entity);
		// when
		assertThat(elasticsearchTemplate.putMapping(entity), is(true));
	}

	@Test
	public void shouldDeleteIndexForGivenEntity() {
		// given
		Class clazz = SampleEntity.class;
		// when
		elasticsearchTemplate.deleteIndex(clazz);
		// then
		assertThat(elasticsearchTemplate.indexExists(clazz), is(false));
	}

	@Test
	public void shouldDoPartialUpdateForExistingDocument() {
		//given
		String documentId = randomNumeric(5);
		String messageBeforeUpdate = "some test message";
		String messageAfterUpdate = "test message";

		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage(messageBeforeUpdate);
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("message", messageAfterUpdate);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(documentId)
				.withClass(SampleEntity.class).withIndexRequest(indexRequest).build();
		// when
		elasticsearchTemplate.update(updateQuery);
		//then
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity indexedEntity = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(indexedEntity.getMessage(), is(messageAfterUpdate));
	}

	@Test(expected = DocumentMissingException.class)
	public void shouldThrowExceptionIfDocumentDoesNotExistWhileDoingPartialUpdate() {
		// when
		IndexRequest indexRequest = new IndexRequest();
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(randomNumeric(5))
				.withClass(SampleEntity.class).withIndexRequest(indexRequest).build();
		elasticsearchTemplate.update(updateQuery);
	}

	@Test
	public void shouldDoUpsertIfDocumentDoesNotExist() {
		//given
		String documentId = randomNumeric(5);
		String message = "test message";
		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("message", message);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(documentId)
				.withDoUpsert(true).withClass(SampleEntity.class)
				.withIndexRequest(indexRequest).build();
		//when
		elasticsearchTemplate.update(updateQuery);
		//then
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity indexedEntity = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(indexedEntity.getMessage(), is(message));
	}

	@Test
	public void shouldReturnHighlightedFieldsForGivenQueryAndFields() {

		//given
		String documentId = randomNumeric(5);
		String actualMessage = "some test message";
		String highlightedMessage = "some <em>test</em> message";

		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage(actualMessage);
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(termQuery("message", "test"))
				.withHighlightFields(new HighlightBuilder.Field("message"))
				.build();

		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class, new SearchResultMapper() {
			@Override
			public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<SampleEntity> chunk = new ArrayList<SampleEntity>();
				for (SearchHit searchHit : response.getHits()) {
					if (response.getHits().getHits().length <= 0) {
						return null;
					}
					SampleEntity user = new SampleEntity();
					user.setId(searchHit.getId());
					user.setMessage((String) searchHit.getSource().get("message"));
					user.setHighlightedMessage(searchHit.getHighlightFields().get("message").fragments()[0].toString());
					chunk.add(user);
				}
				if (chunk.size() > 0) {
					return new FacetedPageImpl<T>((List<T>) chunk);
				}
				return null;
			}
		});

		assertThat(sampleEntities.getContent().get(0).getHighlightedMessage(), is(highlightedMessage));
	}

	@Test
	public void shouldDeleteSpecifiedTypeFromAnIndex() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// when
		elasticsearchTemplate.deleteType(INDEX_NAME, TYPE_NAME);

		//then
		boolean typeExists = elasticsearchTemplate.typeExists(INDEX_NAME, TYPE_NAME);
		assertThat(typeExists, is(false));
	}

	@Test
	public void shouldDeleteDocumentBySpecifiedTypeUsingDeleteQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// when
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(termQuery("id", documentId));
		deleteQuery.setIndex(INDEX_NAME);
		deleteQuery.setType(TYPE_NAME);
		elasticsearchTemplate.delete(deleteQuery);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements(), equalTo(0L));
	}

	@Test
	public void shouldAddAlias() {
		// given
		elasticsearchTemplate.createIndex(SampleEntity.class);
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(INDEX_NAME)
				.withAliasName(aliasName).build();
		// when
		elasticsearchTemplate.addAlias(aliasQuery);
		// then
		Set<String> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.contains(aliasName), is(true));
	}

	@Test
	public void shouldRemoveAlias() {
		// given
		elasticsearchTemplate.createIndex(SampleEntity.class);
		String indexName = INDEX_NAME;
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(indexName)
				.withAliasName(aliasName).build();
		// when
		elasticsearchTemplate.addAlias(aliasQuery);
		Set<String> aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.contains(aliasName), is(true));
		// then
		elasticsearchTemplate.removeAlias(aliasQuery);
		aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.size(), is(0));
	}


	@Test
	public void shouldIndexDocumentForSpecifiedSource() {

		// given
		String documentSource = "{\"id\":\"2333343434\",\"type\":null,\"message\":\"some message\",\"rate\":0,\"available\":false,\"highlightedMessage\":null,\"version\":1385208779482}";
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId("2333343434");
		indexQuery.setSource(documentSource);
		indexQuery.setIndexName(INDEX_NAME);
		indexQuery.setType(TYPE_NAME);
		// when
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", indexQuery.getId()))
				.withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME)
				.build();
		// then
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class, new SearchResultMapper() {
			@Override
			public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<SampleEntity> values = new ArrayList<SampleEntity>();
				for (SearchHit searchHit : response.getHits()) {
					SampleEntity sampleEntity = new SampleEntity();
					sampleEntity.setId(searchHit.getId());
					sampleEntity.setMessage((String) searchHit.getSource().get("message"));
					values.add(sampleEntity);
				}
				return new FacetedPageImpl<T>((List<T>) values);
			}
		});
		assertThat(page, is(notNullValue()));
		assertThat(page.getContent().size(), is(1));
		assertThat(page.getContent().get(0).getId(), is(indexQuery.getId()));
	}

	@Test(expected = ElasticsearchException.class)
	public void shouldThrowElasticsearchExceptionWhenNoDocumentSpecified() {
		// given
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId("2333343434");
		indexQuery.setIndexName(INDEX_NAME);
		indexQuery.setType(TYPE_NAME);

		//when
		elasticsearchTemplate.index(indexQuery);
	}

	@Test
	public void shouldReturnIds() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(termQuery("message", "message"))
				.withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME)
				.withPageable(new PageRequest(0, 100))
				.build();
		// then
		List<String> ids = elasticsearchTemplate.queryForIds(searchQuery);
		assertThat(ids, is(notNullValue()));
		assertThat(ids.size(), is(30));
	}

	@Test
	public void shouldReturnDocumentAboveMinimalScoreGivenQuery() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();

		indexQueries.add(new SampleEntityBuilder("1").message("ab").buildIndex());
		indexQueries.add(new SampleEntityBuilder("2").message("bc").buildIndex());
		indexQueries.add(new SampleEntityBuilder("3").message("ac").buildIndex());

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(boolQuery().must(wildcardQuery("message", "*a*")).should(wildcardQuery("message", "*b*")))
				.withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME)
				.withMinScore(0.5F)
				.build();

		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		// then
		assertThat(page.getTotalElements(), is(1L));
		assertThat(page.getContent().get(0).getMessage(), is("ab"));
	}


	@Test
	public void shouldDoIndexWithoutId() {
		// given
		// document
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setObject(sampleEntity);
		// when
		String documentId = elasticsearchTemplate.index(indexQuery);
		// then
		assertThat(sampleEntity.getId(), is(equalTo(documentId)));

		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity result = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(result.getId(), is(equalTo(documentId)));
	}

	@Test
	public void shouldDoBulkIndexWithoutId() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setMessage("some message");
		sampleEntity1.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setObject(sampleEntity1);
		indexQueries.add(indexQuery1);

		// second document
		SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setMessage("some message");
		sampleEntity2.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setObject(sampleEntity2);
		indexQueries.add(indexQuery2);
		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements(), is(equalTo(2L)));

		assertThat(sampleEntities.getContent().get(0).getId(), is(notNullValue()));
		assertThat(sampleEntities.getContent().get(1).getId(), is(notNullValue()));
	}

	@Test
	public void shouldIndexMapWithIndexNameAndTypeAtRuntime() {
		//given
		Map<String, Object> person1 = new HashMap<String, Object>();
		person1.put("userId", "1");
		person1.put("email", "smhdiu@gmail.com");
		person1.put("title", "Mr");
		person1.put("firstName", "Mohsin");
		person1.put("lastName", "Husen");

		Map<String, Object> person2 = new HashMap<String, Object>();
		person2.put("userId", "2");
		person2.put("email", "akonczak@gmail.com");
		person2.put("title", "Mr");
		person2.put("firstName", "Artur");
		person2.put("lastName", "Konczak");

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId("1");
		indexQuery1.setObject(person1);
		indexQuery1.setIndexName(INDEX_NAME);
		indexQuery1.setType(TYPE_NAME);

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId("2");
		indexQuery2.setObject(person2);
		indexQuery2.setIndexName(INDEX_NAME);
		indexQuery2.setType(TYPE_NAME);

		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);

		//when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(INDEX_NAME, true);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME).withQuery(matchAllQuery()).build();
		Page<Map> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, Map.class, new SearchResultMapper() {
			@Override
			public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<Map> chunk = new ArrayList<Map>();
				for (SearchHit searchHit : response.getHits()) {
					if (response.getHits().getHits().length <= 0) {
						return null;
					}
					Map<String, Object> person = new HashMap<String, Object>();
					person.put("userId", searchHit.getSource().get("userId"));
					person.put("email", searchHit.getSource().get("email"));
					person.put("title", searchHit.getSource().get("title"));
					person.put("firstName", searchHit.getSource().get("firstName"));
					person.put("lastName", searchHit.getSource().get("lastName"));
					chunk.add(person);
				}
				if (chunk.size() > 0) {
					return new FacetedPageImpl<T>((List<T>) chunk);
				}
				return null;
			}
		});
		assertThat(sampleEntities.getTotalElements(), is(equalTo(2L)));
		assertThat(sampleEntities.getContent().get(0).get("userId"), is(person1.get("userId")));
		assertThat(sampleEntities.getContent().get(1).get("userId"), is(person2.get("userId")));
	}

	@Test
	public void shouldIndexSampleEntityWithIndexAndTypeAtRuntime() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setIndexName(INDEX_NAME);
		indexQuery.setType(TYPE_NAME);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(INDEX_NAME, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME).withQuery(matchAllQuery()).build();
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities, is(notNullValue()));
		assertThat(sampleEntities.getTotalElements(), greaterThanOrEqualTo(1L));
	}
}
