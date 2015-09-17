/*
 * Copyright 2014-2015 the original author or authors.
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

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
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
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.builder.SampleEntityBuilder;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.entities.HetroEntity1;
import org.springframework.data.elasticsearch.entities.HetroEntity2;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.data.elasticsearch.entities.SampleMappingEntity;
import org.springframework.data.util.CloseableIterator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Abdul Mohammed
 * @author Kevin Leturc
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateTests {

	private static final String INDEX_NAME = "test-index";
	private static final String INDEX_1_NAME = "test-index-1";
	private static final String INDEX_2_NAME = "test-index-2";
	private static final String TYPE_NAME = "test-type";

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.deleteIndex(INDEX_1_NAME);
		elasticsearchTemplate.deleteIndex(INDEX_2_NAME);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldReturnCountForGivenCriteriaQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		// when
		long count = elasticsearchTemplate.count(criteriaQuery, SampleEntity.class);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	@Test
	public void shouldReturnCountForGivenSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();
		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		// when
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		// then
		assertNotNull("entity can't be null....", sampleEntity1);
		assertEquals(sampleEntity, sampleEntity1);
	}

	@Test
	public void shouldReturnObjectsForGivenIdsUsingMultiGet() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2).message("some message")
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		// when
		SearchQuery query = new NativeSearchQueryBuilder().withIds(Arrays.asList(documentId, documentId2)).build();
		LinkedList<SampleEntity> sampleEntities = elasticsearchTemplate.multiGet(query, SampleEntity.class);
		// then
		assertThat(sampleEntities.size(), is(equalTo(2)));
		assertEquals(sampleEntities.get(0), sampleEntity1);
		assertEquals(sampleEntities.get(1), sampleEntity2);
	}

	@Test
	public void shouldReturnObjectsForGivenIdsUsingMultiGetWithFields() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId)
				.message("some message")
				.type("type1")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2)
				.message("some message")
				.type("type2")
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		// when
		SearchQuery query = new NativeSearchQueryBuilder()
				.withIds(Arrays.asList(documentId, documentId2))
				.withFields("message", "type")
				.build();
		LinkedList<SampleEntity> sampleEntities = elasticsearchTemplate.multiGet(query, SampleEntity.class, new MultiGetResultMapper() {
			@Override
			public <T> LinkedList<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
				LinkedList<T> list = new LinkedList<T>();
				for (MultiGetItemResponse response : responses.getResponses()) {
					SampleEntity entity = new SampleEntity();
					entity.setId(response.getResponse().getId());
					entity.setMessage((String) response.getResponse().getField("message").getValue());
					entity.setType((String) response.getResponse().getField("type").getValue());
					list.add((T) entity);
				}
				return list;
			}
		});
		// then
		assertThat(sampleEntities.size(), is(equalTo(2)));
	}

	@Test
	public void shouldReturnPageForGivenSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2).message("some message")
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements(), is(equalTo(2L)));
	}


	@Test
	public void shouldDoBulkUpdate() {
		//given
		String documentId = randomNumeric(5);
		String messageBeforeUpdate = "some test message";
		String messageAfterUpdate = "test message";

		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("message", messageAfterUpdate);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(documentId)
				.withClass(SampleEntity.class).withIndexRequest(indexRequest).build();


		List<UpdateQuery> queries = new ArrayList<UpdateQuery>();
		queries.add(updateQuery);

		// when
		elasticsearchTemplate.bulkUpdate(queries);
		//then
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity indexedEntity = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(indexedEntity.getMessage(), is(messageAfterUpdate));
	}

	@Test
	public void shouldDeleteDocumentForGivenId() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
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
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId)
				.message("abc")
				.rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2)
				.message("xyz")
				.rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntityBuilder(documentId3)
				.message("xyz")
				.rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

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
	public void shouldSortResultsGivenMultipleSortCriteria() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId)
				.message("abc")
				.rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2)
				.message("xyz")
				.rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntityBuilder(documentId3)
				.message("xyz")
				.rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withSort(new FieldSortBuilder("rate").ignoreUnmapped(true).order(SortOrder.ASC))
				.withSort(new FieldSortBuilder("message").ignoreUnmapped(true).order(SortOrder.ASC)).build();
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		// then
		assertThat(sampleEntities.getTotalElements(), equalTo(3L));
		assertThat(sampleEntities.getContent().get(0).getRate(), is(sampleEntity2.getRate()));
		assertThat(sampleEntities.getContent().get(1).getMessage(), is(sampleEntity1.getMessage()));
	}

	@Test
	public void shouldExecuteStringQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

		// when
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);
		// then
		assertThat(sampleEntity1, is(notNullValue()));
	}

	@Test
	public void shouldDeleteGivenCriteriaQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

		// when
		elasticsearchTemplate.delete(criteriaQuery, SampleEntity.class);
		// then
		StringQuery stringQuery = new StringQuery(matchAllQuery().toString());
		List<SampleEntity> sampleEntities = elasticsearchTemplate.queryForList(stringQuery, SampleEntity.class);

		assertThat(sampleEntities.size(), is(0));
	}

	@Test
	public void shouldReturnSpecifiedFields() {
		// given
		String documentId = randomNumeric(5);
		String message = "some test message";
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message(message)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId1).message(sampleMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);

		String documentId2 = randomNumeric(5);

		elasticsearchTemplate.index(getIndexQuery(new SampleEntityBuilder(documentId2).message(sampleMessage)
				.version(System.currentTimeMillis()).build()));
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		MoreLikeThisQuery moreLikeThisQuery = new MoreLikeThisQuery();
		moreLikeThisQuery.setId(documentId2);
		moreLikeThisQuery.addFields("message");
		moreLikeThisQuery.setMinDocFreq(1);
		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.moreLikeThis(moreLikeThisQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements(), is(equalTo(1L)));
		assertThat(sampleEntities.getContent(), hasItem(sampleEntity));
	}

	/*
	DATAES-167
	 */
	@Test
	public void shouldReturnResultsWithScanAndScrollForGivenCriteriaQuery() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.setPageable(new PageRequest(0, 10));

		String scrollId = elasticsearchTemplate.scan(criteriaQuery, 1000, false);
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
	public void shouldReturnResultsWithScanAndScrollForGivenSearchQuery() {
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

	/*
	DATAES-167
	*/
	@Test
	public void shouldReturnResultsWithScanAndScrollForSpecifiedFieldsForCriteriaCriteria() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.addFields("message");
		criteriaQuery.setPageable(new PageRequest(0, 10));

		String scrollId = elasticsearchTemplate.scan(criteriaQuery, 5000, false);
		List<SampleEntity> sampleEntities = new ArrayList<SampleEntity>();
		boolean hasRecords = true;
		while (hasRecords) {
			Page<SampleEntity> page = elasticsearchTemplate.scroll(scrollId, 5000L, new SearchResultMapper() {
				@Override
				public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
					List<SampleEntity> result = new ArrayList<SampleEntity>();
					for (SearchHit searchHit : response.getHits()) {
						String message = searchHit.getFields().get("message").getValue();
						SampleEntity sampleEntity = new SampleEntity();
						sampleEntity.setId(searchHit.getId());
						sampleEntity.setMessage(message);
						result.add(sampleEntity);
					}

					if (result.size() > 0) {
						return new FacetedPageImpl<T>((List<T>) result);
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

	/*
	DATAES-84
	*/
	@Test
	public void shouldReturnResultsWithScanAndScrollForSpecifiedFieldsForSearchCriteria() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME)
				.withTypes(TYPE_NAME)
				.withFields("message")
				.withQuery(matchAllQuery())
				.withPageable(new PageRequest(0, 10))
				.build();

		String scrollId = elasticsearchTemplate.scan(searchQuery, 10000, false);
		List<SampleEntity> sampleEntities = new ArrayList<SampleEntity>();
		boolean hasRecords = true;
		while (hasRecords) {
			Page<SampleEntity> page = elasticsearchTemplate.scroll(scrollId, 10000L, new SearchResultMapper() {
				@Override
				public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
					List<SampleEntity> result = new ArrayList<SampleEntity>();
					for (SearchHit searchHit : response.getHits()) {
						String message = searchHit.getFields().get("message").getValue();
						SampleEntity sampleEntity = new SampleEntity();
						sampleEntity.setId(searchHit.getId());
						sampleEntity.setMessage(message);
						result.add(sampleEntity);
					}

					if (result.size() > 0) {
						return new FacetedPageImpl<T>((List<T>) result);
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

	/*
	DATAES-167
	 */
	@Test
	public void shouldReturnResultsForScanAndScrollWithCustomResultMapperForGivenCriteriaQuery() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.setPageable(new PageRequest(0, 10));

		String scrollId = elasticsearchTemplate.scan(criteriaQuery, 5000, false);
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

	@Test
	public void shouldReturnResultsForScanAndScrollWithCustomResultMapperForGivenSearchQuery() {
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

	/*
	DATAES-167
	 */
	@Test
	public void shouldReturnResultsWithStreamForGivenCriteriaQuery() {
		//given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// then

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.setPageable(new PageRequest(0, 10));

		CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(criteriaQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<SampleEntity>();
		while (stream.hasNext()) {
			sampleEntities.add(stream.next());
		}
		assertThat(sampleEntities.size(), is(equalTo(30)));
	}

	private static List<IndexQuery> createSampleEntitiesWithMessage(String message, int numberOfEntities) {
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		for (int i = 0; i < numberOfEntities; i++) {
			String documentId = UUID.randomUUID().toString();
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
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId)
				.message("test message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2)
				.message("test test")
				.rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntityBuilder(documentId3)
				.message("some message")
				.rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

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
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId)
				.message("test message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2)
				.message("test test")
				.rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = new SampleEntityBuilder(documentId3)
				.message("some message")
				.rate(15)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

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

		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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

		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message(actualMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		// when
		elasticsearchTemplate.deleteType(INDEX_NAME, TYPE_NAME);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		//then
		boolean typeExists = elasticsearchTemplate.typeExists(INDEX_NAME, TYPE_NAME);
		assertThat(typeExists, is(false));
	}

	@Test
	public void shouldDeleteDocumentBySpecifiedTypeUsingDeleteQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

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
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder().withId(documentId)
				.withIndexName(INDEX_NAME).withType(TYPE_NAME)
				.withObject(sampleEntity).build();

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

	/*
	DATAES-106
	 */
	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexUsingCriteriaQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices("test-index");
		// when
		long count = elasticsearchTemplate.count(criteriaQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-67
	 */
	@Test
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexUsingSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withIndices("test-index")
				.build();
		// when
		long count = elasticsearchTemplate.count(searchQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexAndTypeUsingCriteriaQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices("test-index");
		criteriaQuery.addTypes("test-type");
		// when
		long count = elasticsearchTemplate.count(criteriaQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-67
	 */
	@Test
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexAndTypeUsingSearchQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withIndices("test-index")
				.withTypes("test-type")
				.build();
		// when
		long count = elasticsearchTemplate.count(searchQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenMultiIndices() {
		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId())
				.withIndexName("test-index-1")
				.withObject(sampleEntity1)
				.build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId())
				.withIndexName("test-index-2")
				.withObject(sampleEntity2)
				.build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh("test-index-1", true);
		elasticsearchTemplate.refresh("test-index-2", true);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices("test-index-1", "test-index-2");
		// when
		long count = elasticsearchTemplate.count(criteriaQuery);
		// then
		assertThat(count, is(equalTo(2L)));
	}

	/*
	DATAES-67
	 */
	@Test
	public void shouldReturnCountForGivenSearchQueryWithGivenMultiIndices() {
		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId())
				.withIndexName("test-index-1")
				.withObject(sampleEntity1)
				.build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId())
				.withIndexName("test-index-2")
				.withObject(sampleEntity2)
				.build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh("test-index-1", true);
		elasticsearchTemplate.refresh("test-index-2", true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withIndices("test-index-1", "test-index-2")
				.build();
		// when
		long count = elasticsearchTemplate.count(searchQuery);
		// then
		assertThat(count, is(equalTo(2L)));
	}

	private void cleanUpIndices() {
		elasticsearchTemplate.deleteIndex("test-index-1");
		elasticsearchTemplate.deleteIndex("test-index-2");
		elasticsearchTemplate.createIndex("test-index-1");
		elasticsearchTemplate.createIndex("test-index-2");
		elasticsearchTemplate.refresh("test-index-1", true);
		elasticsearchTemplate.refresh("test-index-2", true);
	}

	/*
	DATAES-71
	*/
	@Test
	public void shouldCreatedIndexWithSpecifiedIndexName() {
		// given
		elasticsearchTemplate.deleteIndex("test-index");
		// when
		elasticsearchTemplate.createIndex("test-index");
		// then
		assertThat(elasticsearchTemplate.indexExists("test-index"), is(true));
	}

	/*
	DATAES-72
	*/
	@Test
	public void shouldDeleteIndexForSpecifiedIndexName() {
		// given
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		// when
		elasticsearchTemplate.deleteIndex("test-index");
		// then
		assertThat(elasticsearchTemplate.indexExists("test-index"), is(false));
	}

	/*
	DATAES-106
	 */
	@Test
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexNameForSpecificIndex() {
		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId())
				.withIndexName("test-index-1")
				.withObject(sampleEntity1)
				.build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId())
				.withIndexName("test-index-2")
				.withObject(sampleEntity2)
				.build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh("test-index-1", true);
		elasticsearchTemplate.refresh("test-index-2", true);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices("test-index-1");
		// when
		long count = elasticsearchTemplate.count(criteriaQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-67
	*/
	@Test
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexNameForSpecificIndex() {
		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId())
				.withIndexName("test-index-1")
				.withObject(sampleEntity1)
				.build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId())
				.withIndexName("test-index-2")
				.withObject(sampleEntity2)
				.build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh("test-index-1", true);
		elasticsearchTemplate.refresh("test-index-2", true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withIndices("test-index-1")
				.build();
		// when
		long count = elasticsearchTemplate.count(searchQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowAnExceptionForGivenCriteriaQueryWhenNoIndexSpecifiedForCountQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		// when
		long count = elasticsearchTemplate.count(criteriaQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-67
	*/
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowAnExceptionForGivenSearchQueryWhenNoIndexSpecifiedForCountQuery() {
		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.build();
		// when
		long count = elasticsearchTemplate.count(searchQuery);
		// then
		assertThat(count, is(equalTo(1L)));
	}

	/*
	DATAES-71
	*/
	@Test
	public void shouldCreateIndexWithGivenSettings() {
		// given
		String settings = "{\n" +
				"        \"index\": {\n" +
				"            \"number_of_shards\": \"1\",\n" +
				"            \"number_of_replicas\": \"0\",\n" +
				"            \"analysis\": {\n" +
				"                \"analyzer\": {\n" +
				"                    \"emailAnalyzer\": {\n" +
				"                        \"type\": \"custom\",\n" +
				"                        \"tokenizer\": \"uax_url_email\"\n" +
				"                    }\n" +
				"                }\n" +
				"            }\n" +
				"        }\n" +
				"}";

		elasticsearchTemplate.deleteIndex("test-index");
		// when
		elasticsearchTemplate.createIndex("test-index", settings);
		// then
		Map map = elasticsearchTemplate.getSetting("test-index");
		boolean hasAnalyzer = map.containsKey("index.analysis.analyzer.emailAnalyzer.tokenizer");
		String emailAnalyzer = (String) map.get("index.analysis.analyzer.emailAnalyzer.tokenizer");
		assertThat(elasticsearchTemplate.indexExists("test-index"), is(true));
		assertThat(hasAnalyzer, is(true));
		assertThat(emailAnalyzer, is("uax_url_email"));
	}

	/*
	DATAES-71
	*/
	@Test
	public void shouldCreateGivenSettingsForGivenIndex() {
		//given
		//delete , create and apply mapping in before method

		// then
		Map map = elasticsearchTemplate.getSetting(SampleEntity.class);
		assertThat(elasticsearchTemplate.indexExists("test-index"), is(true));
		assertThat(map.containsKey("index.refresh_interval"), is(true));
		assertThat(map.containsKey("index.number_of_replicas"), is(true));
		assertThat(map.containsKey("index.number_of_shards"), is(true));
		assertThat(map.containsKey("index.store.type"), is(true));
		assertThat((String) map.get("index.refresh_interval"), is("-1"));
		assertThat((String) map.get("index.number_of_replicas"), is("0"));
		assertThat((String) map.get("index.number_of_shards"), is("1"));
		assertThat((String) map.get("index.store.type"), is("memory"));
	}

	/*
	DATAES-88
	*/
	@Test
	public void shouldCreateIndexWithGivenClassAndSettings() {
		//given
		String settings = "{\n" +
				"        \"index\": {\n" +
				"            \"number_of_shards\": \"1\",\n" +
				"            \"number_of_replicas\": \"0\",\n" +
				"            \"analysis\": {\n" +
				"                \"analyzer\": {\n" +
				"                    \"emailAnalyzer\": {\n" +
				"                        \"type\": \"custom\",\n" +
				"                        \"tokenizer\": \"uax_url_email\"\n" +
				"                    }\n" +
				"                }\n" +
				"            }\n" +
				"        }\n" +
				"}";

		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class, settings);
		elasticsearchTemplate.refresh(SampleEntity.class, true);

		// then
		Map map = elasticsearchTemplate.getSetting(SampleEntity.class);
		assertThat(elasticsearchTemplate.indexExists("test-index"), is(true));
		assertThat(map.containsKey("index.number_of_replicas"), is(true));
		assertThat(map.containsKey("index.number_of_shards"), is(true));
		assertThat((String) map.get("index.number_of_replicas"), is("0"));
		assertThat((String) map.get("index.number_of_shards"), is("1"));
	}

	@Test
	public void shouldTestResultsAcrossMultipleIndices() {
		// given
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = new SampleEntityBuilder(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId())
				.withIndexName("test-index-1")
				.withObject(sampleEntity1)
				.build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = new SampleEntityBuilder(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId())
				.withIndexName("test-index-2")
				.withObject(sampleEntity2)
				.build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh("test-index-1", true);
		elasticsearchTemplate.refresh("test-index-2", true);

		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withIndices("test-index-1", "test-index-2")
				.build();
		// when
		List<SampleEntity> sampleEntities = elasticsearchTemplate.queryForList(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.size(), is(equalTo(2)));
	}

	@Test
	/**
	 * This is basically a demonstration to show composing entities out of heterogeneous indexes.
	 */
	public void shouldComposeObjectsReturnedFromHeterogeneousIndexes() {

		// Given

		HetroEntity1 entity1 = new HetroEntity1(randomNumeric(3), "aFirstName");
		HetroEntity2 entity2 = new HetroEntity2(randomNumeric(4), "aLastName");

		IndexQuery idxQuery1 = new IndexQueryBuilder().withIndexName(INDEX_1_NAME).withId(entity1.getId()).withObject(entity1).build();
		IndexQuery idxQuery2 = new IndexQueryBuilder().withIndexName(INDEX_2_NAME).withId(entity2.getId()).withObject(entity2).build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(idxQuery1, idxQuery2));
		elasticsearchTemplate.refresh(INDEX_1_NAME, true);
		elasticsearchTemplate.refresh(INDEX_2_NAME, true);

		// When

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withTypes("hetro").withIndices(INDEX_1_NAME, INDEX_2_NAME).build();
		Page<ResultAggregator> page = elasticsearchTemplate.queryForPage(searchQuery, ResultAggregator.class, new SearchResultMapper() {
			@Override
			public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<ResultAggregator> values = new ArrayList<ResultAggregator>();
				for (SearchHit searchHit : response.getHits()) {
					String id = String.valueOf(searchHit.getSource().get("id"));
					String firstName = StringUtils.isNotEmpty((String) searchHit.getSource().get("firstName")) ? (String) searchHit.getSource().get("firstName") : "";
					String lastName = StringUtils.isNotEmpty((String) searchHit.getSource().get("lastName")) ? (String) searchHit.getSource().get("lastName") : "";
					values.add(new ResultAggregator(id, firstName, lastName));
				}
				return new FacetedPageImpl<T>((List<T>) values);
			}
		});

		assertThat(page.getTotalElements(), is(2l));
	}


	private IndexQuery getIndexQuery(SampleEntity sampleEntity) {
		return new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity).build();
	}

	private List<IndexQuery> getIndexQueries(List<SampleEntity> sampleEntities) {
		List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
		for (SampleEntity sampleEntity : sampleEntities) {
			indexQueries.add(new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity).build());
		}
		return indexQueries;
	}

	@Document(indexName = INDEX_2_NAME, replicas = 0, shards = 1)
	class ResultAggregator {

		private String id;
		private String firstName;
		private String lastName;

		ResultAggregator(String id, String firstName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}
}
