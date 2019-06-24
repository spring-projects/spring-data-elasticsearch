/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.utils.IndexBuilder.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.lang.Double;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.util.CloseableIterator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Base for testing rest/transport templates
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Abdul Mohammed
 * @author Kevin Leturc
 * @author Mason Chan
 * @author Chris White
 * @author Ilkang Na
 * @author Alen Turkovic
 * @author Sascha Woo
 * @author Jean-Baptiste Nizet
 * @author Zetang Zeng
 * @author Peter Nowak
 * @author Ivan Greene
 * @author Dmitriy Yakovlev
 * @author Peter-Josef Meisch
 * @author Martin Choraine
 * @author Farid Azaza
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateTests {

	private static final String INDEX_NAME_SAMPLE_ENTITY = "test-index-sample-core-template";
	private static final String INDEX_1_NAME = "test-index-1";
	private static final String INDEX_2_NAME = "test-index-2";
	private static final String INDEX_3_NAME = "test-index-3";
	private static final String TYPE_NAME = "test-type";

	private final SearchResultMapper searchResultMapper = new SearchResultMapperAdapter() {
		@Override
		public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {

			List<SampleEntity> result = new ArrayList<>();
			for (SearchHit searchHit : response.getHits()) {

				if (response.getHits().getHits().length <= 0) {
					return new AggregatedPageImpl<T>(Collections.emptyList(), response.getScrollId());
				}

				String message = (String) searchHit.getSourceAsMap().get("message");
				SampleEntity sampleEntity = new SampleEntity();
				sampleEntity.setId(searchHit.getId());
				sampleEntity.setMessage(message);
				result.add(sampleEntity);
			}

			if (result.size() > 0) {
				return new AggregatedPageImpl<T>((List<T>) result, response.getScrollId());
			}

			return new AggregatedPageImpl<T>(Collections.emptyList(), response.getScrollId());
		}
	};

	@Autowired protected ElasticsearchOperations elasticsearchTemplate;

	@Before
	public void before() {

		deleteIndices();

		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.putMapping(SampleEntity.class);

		elasticsearchTemplate.createIndex(SampleEntityUUIDKeyed.class);
		elasticsearchTemplate.putMapping(SampleEntityUUIDKeyed.class);
	}

	@After
	public void after() {

		deleteIndices();
	}

	private void deleteIndices() {

		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.deleteIndex(SampleEntityUUIDKeyed.class);
		elasticsearchTemplate.deleteIndex(UseServerConfigurationEntity.class);
		elasticsearchTemplate.deleteIndex(SampleMappingEntity.class);
		elasticsearchTemplate.deleteIndex(Book.class);
		elasticsearchTemplate.deleteIndex(INDEX_1_NAME);
		elasticsearchTemplate.deleteIndex(INDEX_2_NAME);
		elasticsearchTemplate.deleteIndex(INDEX_3_NAME);
	}

	@Test // DATAES-106
	public void shouldReturnCountForGivenCriteriaQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when

		long count = elasticsearchTemplate.count(criteriaQuery, SampleEntity.class);

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void shouldReturnCountForGivenSearchQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();

		// when

		long count = elasticsearchTemplate.count(searchQuery, SampleEntity.class);
		// then
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void shouldReturnObjectForGivenId() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();
		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);

		// when
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);

		// then
		assertThat(sampleEntity1).isNotNull();
		assertThat(sampleEntity1).isEqualTo(sampleEntity);
	}

	@Test
	public void shouldReturnObjectsForGivenIdsUsingMultiGet() {

		// given
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some message")
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		SearchQuery query = new NativeSearchQueryBuilder().withIds(Arrays.asList(documentId, documentId2)).build();
		List<SampleEntity> sampleEntities = elasticsearchTemplate.multiGet(query, SampleEntity.class);

		// then
		assertThat(sampleEntities).hasSize(2);
		assertThat(sampleEntities.get(0)).isEqualTo(sampleEntity1);
		assertThat(sampleEntities.get(1)).isEqualTo(sampleEntity2);
	}

	@Test
	public void shouldReturnObjectsForGivenIdsUsingMultiGetWithFields() {

		// given
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("some message").type("type1")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some message").type("type2")
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		SearchQuery query = new NativeSearchQueryBuilder().withIds(Arrays.asList(documentId, documentId2))
				.withFields("message", "type").build();
		List<SampleEntity> sampleEntities = elasticsearchTemplate.multiGet(query, SampleEntity.class,
				new MultiGetResultMapper() {
					@Override
					public <T> LinkedList<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
						LinkedList<T> list = new LinkedList<>();
						for (MultiGetItemResponse response : responses.getResponses()) {
							SampleEntity entity = new SampleEntity();
							entity.setId(response.getResponse().getId());
							entity.setMessage((String) response.getResponse().getSource().get("message"));
							entity.setType((String) response.getResponse().getSource().get("type"));
							list.add((T) entity);
						}
						return list;
					}
				});

		// then
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	public void shouldReturnPageForGivenSearchQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities).isNotNull();
		assertThat(sampleEntities.getTotalElements()).isGreaterThanOrEqualTo(1);
	}

	@Test // DATAES-595
	public void shouldReturnPageUsingLocalPreferenceForGivenSearchQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQueryWithValidPreference = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPreference("_local").build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQueryWithValidPreference,
				SampleEntity.class);

		// then
		assertThat(sampleEntities).isNotNull();
		assertThat(sampleEntities.getTotalElements()).isGreaterThanOrEqualTo(1);
	}

	@Test(expected = Exception.class) // DATAES-595
	public void shouldThrowExceptionWhenInvalidPreferenceForSearchQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQueryWithInvalidPreference = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPreference("_only_nodes:oops").build();

		// when
		elasticsearchTemplate.queryForPage(searchQueryWithInvalidPreference, SampleEntity.class);

		// then Throw IllegalArgumentException in case of ElasticsearchTemplate and ElasticsearchStatusException in case of
		// ElasticsearchRestTemplate
	}

	@Test // DATAES-422 - Add support for IndicesOptions in search queries
	public void shouldPassIndicesOptionsForGivenSearchQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery idxQuery = new IndexQueryBuilder().withIndexName(INDEX_1_NAME).withId(sampleEntity.getId())
				.withObject(sampleEntity).build();

		elasticsearchTemplate.index(idxQuery);
		elasticsearchTemplate.refresh(INDEX_1_NAME);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_1_NAME, INDEX_2_NAME).withIndicesOptions(IndicesOptions.lenientExpandOpen()).build();
		Page<SampleEntity> entities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(entities).isNotNull();
		assertThat(entities.getTotalElements()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldDoBulkIndex() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some message")
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2));

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(2);
	}

	@Test
	public void shouldDoBulkUpdate() {

		// given
		String documentId = randomNumeric(5);
		String messageBeforeUpdate = "some test message";
		String messageAfterUpdate = "test message";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("message", messageAfterUpdate);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(documentId).withClass(SampleEntity.class)
				.withIndexRequest(indexRequest).build();

		List<UpdateQuery> queries = new ArrayList<>();
		queries.add(updateQuery);

		// when
		elasticsearchTemplate.bulkUpdate(queries);

		// then
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity indexedEntity = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(indexedEntity.getMessage()).isEqualTo(messageAfterUpdate);
	}

	@Test
	public void shouldDeleteDocumentForGivenId() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);

		// when
		elasticsearchTemplate.delete(INDEX_NAME_SAMPLE_ENTITY, TYPE_NAME, documentId);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(0);
	}

	@Test
	public void shouldDeleteEntityForGivenId() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);

		// when
		elasticsearchTemplate.delete(SampleEntity.class, documentId);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(0);
	}

	@Test
	public void shouldDeleteDocumentForGivenQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(termQuery("id", documentId));
		elasticsearchTemplate.delete(deleteQuery, SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(0);
	}

	@Test // DATAES-547
	public void shouldDeleteAcrossIndex() {

		// given
		SampleEntity sampleEntity = SampleEntity.builder() //
				.message("foo") //
				.version(System.currentTimeMillis()) //
				.build();

		IndexQuery idxQuery1 = new IndexQueryBuilder().withIndexName(INDEX_1_NAME).withId(randomNumeric(5))
				.withObject(sampleEntity).build();

		elasticsearchTemplate.index(idxQuery1);
		elasticsearchTemplate.refresh(INDEX_1_NAME);

		IndexQuery idxQuery2 = new IndexQueryBuilder().withIndexName(INDEX_2_NAME).withId(randomNumeric(5))
				.withObject(sampleEntity).build();

		elasticsearchTemplate.index(idxQuery2);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		// when
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(typeQuery(TYPE_NAME));
		deleteQuery.setType(TYPE_NAME);
		deleteQuery.setIndex("test-index-*");

		elasticsearchTemplate.delete(deleteQuery);

		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("message", "foo"))
				.withIndices(INDEX_1_NAME, INDEX_2_NAME) //
				.build();

		assertThat(elasticsearchTemplate.count(searchQuery)).isEqualTo(0);
	}

	@Test // DATAES-547
	public void shouldDeleteAcrossIndexWhenNoMatchingDataPresent() {

		// given
		SampleEntity sampleEntity = SampleEntity.builder() //
				.message("positive") //
				.version(System.currentTimeMillis()) //
				.build();

		IndexQuery idxQuery1 = new IndexQueryBuilder().withIndexName(INDEX_1_NAME).withId(randomNumeric(5))
				.withObject(sampleEntity).build();

		elasticsearchTemplate.index(idxQuery1);
		elasticsearchTemplate.refresh(INDEX_1_NAME);

		IndexQuery idxQuery2 = new IndexQueryBuilder().withIndexName(INDEX_2_NAME).withId(randomNumeric(5))
				.withObject(sampleEntity).build();

		elasticsearchTemplate.index(idxQuery2);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		// when
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(termQuery("message", "negative"));
		deleteQuery.setType(TYPE_NAME);
		deleteQuery.setIndex("test-index-*");

		elasticsearchTemplate.delete(deleteQuery);

		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("message", "positive"))
				.withIndices(INDEX_1_NAME, INDEX_2_NAME) //
				.build();

		assertThat(elasticsearchTemplate.count(searchQuery)).isEqualTo(2);
	}

	@Test
	public void shouldFilterSearchResultsForGivenFilter() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withFilter(boolQuery().filter(termQuery("id", documentId))).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(1);
	}

	@Test
	public void shouldSortResultsGivenSortCriteria() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyz").rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withSort(new FieldSortBuilder("rate").order(SortOrder.ASC)).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(3);
		assertThat(sampleEntities.getContent().get(0).getRate()).isEqualTo(sampleEntity2.getRate());
	}

	@Test
	public void shouldSortResultsGivenMultipleSortCriteria() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyz").rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withSort(new FieldSortBuilder("rate").order(SortOrder.ASC))
				.withSort(new FieldSortBuilder("message").order(SortOrder.ASC)).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(3);
		assertThat(sampleEntities.getContent().get(0).getRate()).isEqualTo(sampleEntity2.getRate());
		assertThat(sampleEntities.getContent().get(1).getMessage()).isEqualTo(sampleEntity1.getMessage());
	}

	@Test // DATAES-312
	public void shouldSortResultsGivenNullFirstSortCriteria() {

		// given
		List<IndexQuery> indexQueries;

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(15)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).rate(10).version(System.currentTimeMillis())
				.build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPageable(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("message").nullsFirst()))).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(3);
		assertThat(sampleEntities.getContent().get(0).getRate()).isEqualTo(sampleEntity3.getRate());
		assertThat(sampleEntities.getContent().get(1).getMessage()).isEqualTo(sampleEntity1.getMessage());
	}

	@Test // DATAES-312
	public void shouldSortResultsGivenNullLastSortCriteria() {

		// given
		List<IndexQuery> indexQueries;

		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(15)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).rate(10).version(System.currentTimeMillis())
				.build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPageable(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("message").nullsLast()))).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(3);
		assertThat(sampleEntities.getContent().get(0).getRate()).isEqualTo(sampleEntity1.getRate());
		assertThat(sampleEntities.getContent().get(1).getMessage()).isEqualTo(sampleEntity2.getMessage());
	}

	@Test // DATAES-467
	public void shouldSortResultsByScore() {

		// given
		List<SampleEntity> entities = Arrays.asList( //
				SampleEntity.builder().id("1").message("abc").build(), //
				SampleEntity.builder().id("2").message("def").build(), //
				SampleEntity.builder().id("3").message("ghi").build());

		elasticsearchTemplate.bulkIndex(getIndexQueries(entities));
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPageable(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("_score")))).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(3);
	}

	@Test
	public void shouldExecuteStringQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		StringQuery stringQuery = new StringQuery(matchAllQuery().toString());

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(1);
	}

	@Test
	public void shouldUseScriptedFields() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setRate(2);
		sampleEntity.setMessage("some message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		Map<String, Object> params = new HashMap<>();
		params.put("factor", 2);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withScriptField(
				new ScriptField("scriptedRate", new Script(ScriptType.INLINE, "expression", "doc['rate'] * factor", params)))
				.build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(1);
		assertThat(sampleEntities.getContent().get(0).getScriptedRate()).isEqualTo(4.0);
	}

	@Test
	public void shouldReturnPageableResultsGivenStringQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		StringQuery stringQuery = new StringQuery(matchAllQuery().toString(), PageRequest.of(0, 10));

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldReturnSortedPageableResultsGivenStringQuery() {

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
		elasticsearchTemplate.refresh(SampleEntity.class);

		StringQuery stringQuery = new StringQuery(matchAllQuery().toString(), PageRequest.of(0, 10),
				Sort.by(Order.asc("message")));

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(stringQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void shouldReturnObjectMatchingGivenStringQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		StringQuery stringQuery = new StringQuery(termQuery("id", documentId).toString());

		// when
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(stringQuery, SampleEntity.class);

		// then
		assertThat(sampleEntity1).isNotNull();
		assertThat(sampleEntity1.getId()).isEqualTo(documentId);
	}

	@Test
	public void shouldCreateIndexGivenEntityClass() {

		// when
		boolean created = elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.putMapping(SampleEntity.class);
		Map setting = elasticsearchTemplate.getSetting(SampleEntity.class);

		// then
		assertThat(created).isTrue();
		assertThat(setting.get("index.number_of_shards")).isEqualTo("1");
		assertThat(setting.get("index.number_of_replicas")).isEqualTo("0");
	}

	@Test
	public void shouldExecuteGivenCriteriaQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

		// when
		SampleEntity sampleEntity1 = elasticsearchTemplate.queryForObject(criteriaQuery, SampleEntity.class);

		// then
		assertThat(sampleEntity1).isNotNull();
	}

	@Test
	public void shouldDeleteGivenCriteriaQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));

		// when
		elasticsearchTemplate.delete(criteriaQuery, SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		StringQuery stringQuery = new StringQuery(matchAllQuery().toString());
		List<SampleEntity> sampleEntities = elasticsearchTemplate.queryForList(stringQuery, SampleEntity.class);

		assertThat(sampleEntities).isEmpty();
	}

	@Test
	public void shouldReturnSpecifiedFields() {

		// given
		String documentId = randomNumeric(5);
		String message = "some test message";
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(message)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withFields("message").build();
		// when
		Page<String> page = elasticsearchTemplate.queryForPage(searchQuery, String.class, new SearchResultMapperAdapter() {
			@Override
			public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<String> values = new ArrayList<>();
				for (SearchHit searchHit : response.getHits()) {
					values.add((String) searchHit.getSourceAsMap().get("message"));
				}
				return new AggregatedPageImpl<>((List<T>) values);
			}
		});

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().get(0)).isEqualTo(message);
	}

	@Test
	public void shouldReturnFieldsBasedOnSourceFilter() {

		// given
		String documentId = randomNumeric(5);
		String message = "some test message";
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(message)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		FetchSourceFilterBuilder sourceFilter = new FetchSourceFilterBuilder();
		sourceFilter.withIncludes("message");

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withSourceFilter(sourceFilter.build()).build();

		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().get(0).getMessage()).isEqualTo(message);
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
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId1).message(sampleMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);

		String documentId2 = randomNumeric(5);

		elasticsearchTemplate.index(getIndexQuery(
				SampleEntity.builder().id(documentId2).message(sampleMessage).version(System.currentTimeMillis()).build()));
		elasticsearchTemplate.refresh(SampleEntity.class);

		MoreLikeThisQuery moreLikeThisQuery = new MoreLikeThisQuery();
		moreLikeThisQuery.setId(documentId2);
		moreLikeThisQuery.addFields("message");
		moreLikeThisQuery.setMinDocFreq(1);

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.moreLikeThis(moreLikeThisQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities.getTotalElements()).isEqualTo(1);
		assertThat(sampleEntities.getContent()).contains(sampleEntity);
	}

	@Test // DATAES-167
	public void shouldReturnResultsWithScanAndScrollForGivenCriteriaQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME_SAMPLE_ENTITY);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, criteriaQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scroll = elasticsearchTemplate.continueScroll(scroll.getScrollId(), 1000, SampleEntity.class);
		}
		elasticsearchTemplate.clearScroll(scroll.getScrollId());
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsWithScanAndScrollForGivenSearchQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withPageable(PageRequest.of(0, 10)).build();

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scroll = elasticsearchTemplate.continueScroll(scroll.getScrollId(), 1000, SampleEntity.class);
		}
		elasticsearchTemplate.clearScroll(scroll.getScrollId());
		assertThat(sampleEntities).hasSize(30);
	}

	@Test // DATAES-167
	public void shouldReturnResultsWithScanAndScrollForSpecifiedFieldsForCriteriaQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME_SAMPLE_ENTITY);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.addFields("message");
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, criteriaQuery, SampleEntity.class,
				searchResultMapper);
		String scrollId = scroll.getScrollId();
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scrollId = scroll.getScrollId();
			scroll = elasticsearchTemplate.continueScroll(scrollId, 1000, SampleEntity.class, searchResultMapper);
		}
		elasticsearchTemplate.clearScroll(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test // DATAES-84
	public void shouldReturnResultsWithScanAndScrollForSpecifiedFieldsForSearchCriteria() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withFields("message").withQuery(matchAllQuery())
				.withPageable(PageRequest.of(0, 10)).build();

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class,
				searchResultMapper);
		String scrollId = scroll.getScrollId();
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scrollId = scroll.getScrollId();
			scroll = elasticsearchTemplate.continueScroll(scrollId, 1000, SampleEntity.class, searchResultMapper);
		}
		elasticsearchTemplate.clearScroll(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test // DATAES-167
	public void shouldReturnResultsForScanAndScrollWithCustomResultMapperForGivenCriteriaQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME_SAMPLE_ENTITY);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, criteriaQuery, SampleEntity.class,
				searchResultMapper);
		String scrollId = scroll.getScrollId();
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scrollId = scroll.getScrollId();
			scroll = elasticsearchTemplate.continueScroll(scrollId, 1000, SampleEntity.class, searchResultMapper);
		}
		elasticsearchTemplate.clearScroll(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test
	public void shouldReturnResultsForScanAndScrollWithCustomResultMapperForGivenSearchQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withPageable(PageRequest.of(0, 10)).build();

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class,
				searchResultMapper);
		String scrollId = scroll.getScrollId();
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scrollId = scroll.getScrollId();
			scroll = elasticsearchTemplate.continueScroll(scrollId, 1000, SampleEntity.class, searchResultMapper);
		}
		elasticsearchTemplate.clearScroll(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test // DATAES-217
	public void shouldReturnResultsWithScanAndScrollForGivenCriteriaQueryAndClass() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, criteriaQuery, SampleEntity.class);
		String scrollId = scroll.getScrollId();
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scrollId = scroll.getScrollId();
			scroll = elasticsearchTemplate.continueScroll(scrollId, 1000, SampleEntity.class);
		}
		elasticsearchTemplate.clearScroll(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test // DATAES-217
	public void shouldReturnResultsWithScanAndScrollForGivenSearchQueryAndClass() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPageable(PageRequest.of(0, 10)).build();

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class);
		String scrollId = scroll.getScrollId();
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scrollId = scroll.getScrollId();
			scroll = elasticsearchTemplate.continueScroll(scrollId, 1000, SampleEntity.class);
		}
		elasticsearchTemplate.clearScroll(scrollId);
		assertThat(sampleEntities).hasSize(30);
	}

	@Test // DATAES-167
	public void shouldReturnResultsWithStreamForGivenCriteriaQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME_SAMPLE_ENTITY);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(criteriaQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (stream.hasNext()) {
			sampleEntities.add(stream.next());
		}
		assertThat(sampleEntities).hasSize(30);
	}

	private static List<IndexQuery> createSampleEntitiesWithMessage(String message, int numberOfEntities) {
		List<IndexQuery> indexQueries = new ArrayList<>();
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
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("test test").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("some message").rate(15)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		CriteriaQuery singleCriteriaQuery = new CriteriaQuery(new Criteria("message").contains("test"));
		CriteriaQuery multipleCriteriaQuery = new CriteriaQuery(
				new Criteria("message").contains("some").and("message").contains("message"));
		List<SampleEntity> sampleEntitiesForSingleCriteria = elasticsearchTemplate.queryForList(singleCriteriaQuery,
				SampleEntity.class);
		List<SampleEntity> sampleEntitiesForAndCriteria = elasticsearchTemplate.queryForList(multipleCriteriaQuery,
				SampleEntity.class);
		// then
		assertThat(sampleEntitiesForSingleCriteria).hasSize(2);
		assertThat(sampleEntitiesForAndCriteria).hasSize(1);
	}

	@Test
	public void shouldReturnListForGivenStringQuery() {

		// given
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("test message")
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("test test").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("some message").rate(15)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		StringQuery stringQuery = new StringQuery(matchAllQuery().toString());
		List<SampleEntity> sampleEntities = elasticsearchTemplate.queryForList(stringQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities).hasSize(3);
	}

	@Test
	public void shouldPutMappingForGivenEntity() throws Exception {

		// given
		Class entity = SampleMappingEntity.class;
		elasticsearchTemplate.deleteIndex(entity);
		elasticsearchTemplate.createIndex(entity);

		// when

		// then
		assertThat(elasticsearchTemplate.putMapping(entity)).isTrue();
	}

	@Test
	public void shouldDeleteIndexForGivenEntity() {

		// given
		Class<?> clazz = SampleEntity.class;

		// when
		elasticsearchTemplate.deleteIndex(clazz);

		// then
		assertThat(elasticsearchTemplate.indexExists(clazz)).isFalse();
	}

	@Test
	public void shouldDoPartialUpdateForExistingDocument() {

		// given
		String documentId = randomNumeric(5);
		String messageBeforeUpdate = "some test message";
		String messageAfterUpdate = "test message";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(messageBeforeUpdate)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("message", messageAfterUpdate);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(documentId).withClass(SampleEntity.class)
				.withIndexRequest(indexRequest).build();

		// when
		elasticsearchTemplate.update(updateQuery);

		// then
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity indexedEntity = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(indexedEntity.getMessage()).isEqualTo(messageAfterUpdate);
	}

	@Test
	public void shouldDoUpsertIfDocumentDoesNotExist() {

		// given
		String documentId = randomNumeric(5);
		String message = "test message";
		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("message", message);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(documentId).withDoUpsert(true)
				.withClass(SampleEntity.class).withIndexRequest(indexRequest).build();

		// when
		elasticsearchTemplate.update(updateQuery);

		// then
		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity indexedEntity = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(indexedEntity.getMessage()).isEqualTo(message);
	}

	@Test
	public void shouldReturnHighlightedFieldsForGivenQueryAndFields() {

		// given
		String documentId = randomNumeric(5);
		String actualMessage = "some test message";
		String highlightedMessage = "some <em>test</em> message";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(actualMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		// when
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		List<HighlightBuilder.Field> message = new HighlightBuilder().field("message").fields();
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("message", "test"))
				.withHighlightFields(message.toArray(new HighlightBuilder.Field[message.size()])).build();

		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class,
				new SearchResultMapperAdapter() {
					@Override
					public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
						List<SampleEntity> chunk = new ArrayList<>();
						for (SearchHit searchHit : response.getHits()) {
							if (response.getHits().getHits().length <= 0) {
								return null;
							}
							SampleEntity user = new SampleEntity();
							user.setId(searchHit.getId());
							user.setMessage((String) searchHit.getSourceAsMap().get("message"));
							user.setHighlightedMessage(searchHit.getHighlightFields().get("message").fragments()[0].toString());
							chunk.add(user);
						}
						if (chunk.size() > 0) {
							return new AggregatedPageImpl<>((List<T>) chunk);
						}
						return null;
					}
				});

		// then
		assertThat(sampleEntities.getContent().get(0).getHighlightedMessage()).isEqualTo(highlightedMessage);
	}

	@Test // DATAES-412
	public void shouldReturnMultipleHighlightFields() {

		// given
		String documentId = randomNumeric(5);
		String actualType = "some test type";
		String actualMessage = "some test message";
		String highlightedType = "some <em>test</em> type";
		String highlightedMessage = "some <em>test</em> message";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).type(actualType).message(actualMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(boolQuery().must(termQuery("type", "test")).must(termQuery("message", "test")))
				.withHighlightFields(new HighlightBuilder.Field("type"), new HighlightBuilder.Field("message")).build();

		// when
		elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class, new SearchResultMapperAdapter() {
			@Override
			public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				for (SearchHit searchHit : response.getHits()) {
					Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
					HighlightField highlightFieldType = highlightFields.get("type");
					HighlightField highlightFieldMessage = highlightFields.get("message");

					// then
					assertThat(highlightFieldType).isNotNull();
					assertThat(highlightFieldMessage).isNotNull();
					assertThat(highlightFieldType.fragments()[0].toString()).isEqualTo(highlightedType);
					assertThat(highlightFieldMessage.fragments()[0].toString()).isEqualTo(highlightedMessage);
				}
				return null;
			}
		});
	}

	@Test // DATAES-479
	public void shouldHonorTheHighlightBuilderOptions() {

		// given
		String documentId = randomNumeric(5);
		String actualMessage = "some test message with <html> unsafe <script> text";
		String highlightedMessage = "some <em>test</em> message with &lt;html&gt; unsafe &lt;script&gt; text";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(actualMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("message", "test"))
				.withHighlightBuilder(new HighlightBuilder().encoder("html"))
				.withHighlightFields(new HighlightBuilder.Field("message")).build();

		// when
		elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class, new SearchResultMapperAdapter() {
			@Override
			public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				for (SearchHit searchHit : response.getHits()) {
					Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
					HighlightField highlightFieldMessage = highlightFields.get("message");

					// then
					assertThat(highlightFieldMessage).isNotNull();
					assertThat(highlightFieldMessage.fragments()[0].toString()).isEqualTo(highlightedMessage);
				}
				return null;
			}
		});
	}

	@Test // DATAES-479
	public void shouldHighlightIfBuilderSetEvenIfFieldsNotSet() {

		// given
		String documentId = randomNumeric(5);
		String actualMessage = "some test message text";
		String highlightedMessage = "some <em>test</em> message text";

		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message(actualMessage)
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("message", "test"))
				.withHighlightBuilder(new HighlightBuilder().field("message")).build();

		// when
		elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class, new SearchResultMapper() {
			@Override
			public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				for (SearchHit searchHit : response.getHits()) {
					Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
					HighlightField highlightFieldMessage = highlightFields.get("message");

					// then
					assertThat(highlightFieldMessage).isNotNull();
					assertThat(highlightFieldMessage.fragments()[0].toString()).isEqualTo(highlightedMessage);
				}
				return null;
			}

			@Override
			public <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {
				return null;
			}
		});
	}

	@Test // DATAES-487
	public void shouldReturnSameEntityForMultiSearch() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac").build()));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		List<SearchQuery> queries = new ArrayList<>();

		queries.add(new NativeSearchQueryBuilder().withQuery(termQuery("message", "ab")).build());
		queries.add(new NativeSearchQueryBuilder().withQuery(termQuery("message", "bc")).build());
		queries.add(new NativeSearchQueryBuilder().withQuery(termQuery("message", "ac")).build());

		// then
		List<Page<SampleEntity>> sampleEntities = elasticsearchTemplate.queryForPage(queries, SampleEntity.class);
		for (Page<SampleEntity> sampleEntity : sampleEntities) {
			assertThat(sampleEntity.getTotalElements()).isEqualTo(1);
		}
	}

	@Test // DATAES-487
	public void shouldReturnDifferentEntityForMultiSearch() {

		// given
		Class<Book> clazz = Book.class;
		elasticsearchTemplate.deleteIndex(clazz);
		elasticsearchTemplate.createIndex(clazz);
		elasticsearchTemplate.putMapping(clazz);
		elasticsearchTemplate.refresh(clazz);

		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab").build()));
		indexQueries.add(buildIndex(Book.builder().id("2").description("bc").build()));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);
		elasticsearchTemplate.refresh(clazz);

		// when
		List<SearchQuery> queries = new ArrayList<>();

		queries.add(new NativeSearchQueryBuilder().withQuery(termQuery("message", "ab")).build());
		queries.add(new NativeSearchQueryBuilder().withQuery(termQuery("description", "bc")).build());

		// then
		List<Page<?>> pages = elasticsearchTemplate.queryForPage(queries, Lists.newArrayList(SampleEntity.class, clazz));
		Page<?> page0 = pages.get(0);
		assertThat(page0.getTotalElements()).isEqualTo(1L);
		assertThat(page0.getContent().get(0).getClass()).isEqualTo(SampleEntity.class);
		Page<?> page1 = pages.get(1);
		assertThat(page1.getTotalElements()).isEqualTo(1L);
		assertThat(page1.getContent().get(0).getClass()).isEqualTo(clazz);
	}

	@Test
	public void shouldDeleteDocumentBySpecifiedTypeUsingDeleteQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(termQuery("id", documentId));
		deleteQuery.setIndex(INDEX_NAME_SAMPLE_ENTITY);
		deleteQuery.setType(TYPE_NAME);
		elasticsearchTemplate.delete(deleteQuery);
		elasticsearchTemplate.refresh(INDEX_NAME_SAMPLE_ENTITY);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", documentId)).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(0);
	}

	@Test
	public void shouldIndexDocumentForSpecifiedSource() {

		// given
		String documentSource = "{\"id\":\"2333343434\",\"type\":null,\"message\":\"some message\",\"rate\":0,\"available\":false,\"highlightedMessage\":null,\"version\":1385208779482}";
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId("2333343434");
		indexQuery.setSource(documentSource);
		indexQuery.setIndexName(INDEX_NAME_SAMPLE_ENTITY);
		indexQuery.setType(TYPE_NAME);

		// when
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("id", indexQuery.getId()))
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).build();

		// then
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class,
				new SearchResultMapperAdapter() {
					@Override
					public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
						List<SampleEntity> values = new ArrayList<>();
						for (SearchHit searchHit : response.getHits()) {
							SampleEntity sampleEntity = new SampleEntity();
							sampleEntity.setId(searchHit.getId());
							sampleEntity.setMessage((String) searchHit.getSourceAsMap().get("message"));
							values.add(sampleEntity);
						}
						return new AggregatedPageImpl<>((List<T>) values);
					}
				});
		assertThat(page).isNotNull();
		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getContent().get(0).getId()).isEqualTo(indexQuery.getId());
	}

	@Test(expected = ElasticsearchException.class)
	public void shouldThrowElasticsearchExceptionWhenNoDocumentSpecified() {

		// given
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId("2333343434");
		indexQuery.setIndexName(INDEX_NAME_SAMPLE_ENTITY);
		indexQuery.setType(TYPE_NAME);

		// when
		elasticsearchTemplate.index(indexQuery);
	}

	@Test
	public void shouldReturnIds() {
		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 30);
		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("message", "message"))
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withPageable(PageRequest.of(0, 100)).build();
		// then
		List<String> ids = elasticsearchTemplate.queryForIds(searchQuery);
		assertThat(ids).hasSize(30);
	}

	@Test
	public void shouldReturnDocumentAboveMinimalScoreGivenQuery() {
		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac").build()));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(boolQuery().must(wildcardQuery("message", "*a*")).should(wildcardQuery("message", "*b*")))
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withMinScore(2.0F).build();

		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().get(0).getMessage()).isEqualTo("ab");
	}

	@Test // DATAES-462
	public void shouldReturnScores() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab xz").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac xz hi").build()));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("message", "xz"))
				.withSort(SortBuilders.fieldSort("message")).withTrackScores(true).build();

		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(page).isInstanceOf(AggregatedPage.class);
		assertThat(((AggregatedPage) page).getMaxScore()).isGreaterThan(0f);
		assertThat(page.getContent().get(0).getScore()).isGreaterThan(0f);
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
		assertThat(sampleEntity.getId()).isEqualTo(documentId);

		GetQuery getQuery = new GetQuery();
		getQuery.setId(documentId);
		SampleEntity result = elasticsearchTemplate.queryForObject(getQuery, SampleEntity.class);
		assertThat(result.getId()).isEqualTo(documentId);
	}

	@Test
	public void shouldDoBulkIndexWithoutId() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
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
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(2);

		List<SampleEntity> content = sampleEntities.getContent();
		assertThat(content.get(0).getId()).isNotNull();
		assertThat(content.get(1).getId()).isNotNull();
	}

	@Test
	public void shouldIndexMapWithIndexNameAndTypeAtRuntime() {

		// given
		Map<String, Object> person1 = new HashMap<>();
		person1.put("userId", "1");
		person1.put("email", "smhdiu@gmail.com");
		person1.put("title", "Mr");
		person1.put("firstName", "Mohsin");
		person1.put("lastName", "Husen");

		Map<String, Object> person2 = new HashMap<>();
		person2.put("userId", "2");
		person2.put("email", "akonczak@gmail.com");
		person2.put("title", "Mr");
		person2.put("firstName", "Artur");
		person2.put("lastName", "Konczak");

		IndexQuery indexQuery1 = new IndexQuery();
		indexQuery1.setId("1");
		indexQuery1.setObject(person1);
		indexQuery1.setIndexName(INDEX_NAME_SAMPLE_ENTITY);
		indexQuery1.setType(TYPE_NAME);

		IndexQuery indexQuery2 = new IndexQuery();
		indexQuery2.setId("2");
		indexQuery2.setObject(person2);
		indexQuery2.setIndexName(INDEX_NAME_SAMPLE_ENTITY);
		indexQuery2.setType(TYPE_NAME);

		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(indexQuery1);
		indexQueries.add(indexQuery2);

		// when
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(INDEX_NAME_SAMPLE_ENTITY);

		// then
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME)
				.withQuery(matchAllQuery()).build();
		Page<Map> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, Map.class,
				new SearchResultMapperAdapter() {
					@Override
					public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
						List<Map> chunk = new ArrayList<>();
						for (SearchHit searchHit : response.getHits()) {
							if (response.getHits().getHits().length <= 0) {
								return null;
							}
							Map<String, Object> person = new HashMap<>();
							person.put("userId", searchHit.getSourceAsMap().get("userId"));
							person.put("email", searchHit.getSourceAsMap().get("email"));
							person.put("title", searchHit.getSourceAsMap().get("title"));
							person.put("firstName", searchHit.getSourceAsMap().get("firstName"));
							person.put("lastName", searchHit.getSourceAsMap().get("lastName"));
							chunk.add(person);
						}
						if (chunk.size() > 0) {
							return new AggregatedPageImpl<>((List<T>) chunk);
						}
						return null;
					}
				});

		assertThat(sampleEntities.getTotalElements()).isEqualTo(2);
		List<Map> content = sampleEntities.getContent();
		assertThat(content.get(0).get("userId")).isEqualTo(person1.get("userId"));
		assertThat(content.get(1).get("userId")).isEqualTo(person2.get("userId"));
	}

	@Test // DATAES-523
	public void shouldIndexGteEntityWithVersionType() {

		// given
		String documentId = randomNumeric(5);
		GTEVersionEntity entity = GTEVersionEntity.builder().id(documentId).name("FooBar")
				.version(System.currentTimeMillis()).build();

		IndexQueryBuilder indexQueryBuilder = new IndexQueryBuilder().withId(documentId)
				.withIndexName(INDEX_NAME_SAMPLE_ENTITY).withType(TYPE_NAME).withVersion(entity.getVersion())
				.withObject(entity);

		elasticsearchTemplate.index(indexQueryBuilder.build());
		elasticsearchTemplate.refresh(INDEX_NAME_SAMPLE_ENTITY);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME)
				.withQuery(matchAllQuery()).build();
		// when
		Page<GTEVersionEntity> entities = elasticsearchTemplate.queryForPage(searchQuery, GTEVersionEntity.class);
		// then
		assertThat(entities).isNotNull();
		assertThat(entities.getTotalElements()).isGreaterThanOrEqualTo(1);

		// reindex with same version
		elasticsearchTemplate.index(indexQueryBuilder.build());
		elasticsearchTemplate.refresh(INDEX_NAME_SAMPLE_ENTITY);

		// reindex with version one below
		assertThatThrownBy(() -> {
			elasticsearchTemplate.index(indexQueryBuilder.withVersion(entity.getVersion() - 1).build());
		}).hasMessageContaining("version").hasMessageContaining("conflict");
	}

	@Test
	public void shouldIndexSampleEntityWithIndexAndTypeAtRuntime() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder().withId(documentId).withIndexName(INDEX_NAME_SAMPLE_ENTITY)
				.withType(TYPE_NAME).withObject(sampleEntity).build();

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(INDEX_NAME_SAMPLE_ENTITY);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME)
				.withQuery(matchAllQuery()).build();

		// when
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities).isNotNull();
		assertThat(sampleEntities.getTotalElements()).isGreaterThanOrEqualTo(1);
	}

	@Test // DATAES-106
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexUsingCriteriaQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME_SAMPLE_ENTITY);

		// when
		long count = elasticsearchTemplate.count(criteriaQuery);

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexUsingSearchQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).build();

		// when
		long count = elasticsearchTemplate.count(searchQuery);

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-106
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexAndTypeUsingCriteriaQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_NAME_SAMPLE_ENTITY);
		criteriaQuery.addTypes(TYPE_NAME);

		// when

		long count = elasticsearchTemplate.count(criteriaQuery);
		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexAndTypeUsingSearchQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).build();

		// when
		long count = elasticsearchTemplate.count(searchQuery);

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-106
	public void shouldReturnCountForGivenCriteriaQueryWithGivenMultiIndices() {

		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withIndexName(INDEX_1_NAME)
				.withObject(sampleEntity1).build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withIndexName(INDEX_2_NAME)
				.withObject(sampleEntity2).build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_1_NAME, INDEX_2_NAME);

		// when
		long count = elasticsearchTemplate.count(criteriaQuery);

		// then
		assertThat(count).isEqualTo(2);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenMultiIndices() {

		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withIndexName(INDEX_1_NAME)
				.withObject(sampleEntity1).build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withIndexName(INDEX_2_NAME)
				.withObject(sampleEntity2).build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_1_NAME, INDEX_2_NAME).build();

		// when
		long count = elasticsearchTemplate.count(searchQuery);

		// then
		assertThat(count).isEqualTo(2);
	}

	private void cleanUpIndices() {
		elasticsearchTemplate.deleteIndex(INDEX_1_NAME);
		elasticsearchTemplate.deleteIndex(INDEX_2_NAME);
		elasticsearchTemplate.createIndex(INDEX_1_NAME);
		elasticsearchTemplate.createIndex(INDEX_2_NAME);
		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);
	}

	@Test // DATAES-71
	public void shouldCreatedIndexWithSpecifiedIndexName() {

		// given
		elasticsearchTemplate.deleteIndex(INDEX_3_NAME);

		// when
		elasticsearchTemplate.createIndex(INDEX_3_NAME);

		// then
		assertThat(elasticsearchTemplate.indexExists(INDEX_3_NAME)).isTrue();
	}

	@Test // DATAES-72
	public void shouldDeleteIndexForSpecifiedIndexName() {

		// given
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		elasticsearchTemplate.deleteIndex(INDEX_3_NAME);

		// then
		assertThat(elasticsearchTemplate.indexExists(INDEX_3_NAME)).isFalse();
	}

	@Test // DATAES-106
	public void shouldReturnCountForGivenCriteriaQueryWithGivenIndexNameForSpecificIndex() {

		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withIndexName(INDEX_1_NAME)
				.withObject(sampleEntity1).build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withIndexName(INDEX_2_NAME)
				.withObject(sampleEntity2).build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
		criteriaQuery.addIndices(INDEX_1_NAME);

		// when
		long count = elasticsearchTemplate.count(criteriaQuery);

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test // DATAES-67
	public void shouldReturnCountForGivenSearchQueryWithGivenIndexNameForSpecificIndex() {

		// given
		cleanUpIndices();
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withIndexName(INDEX_1_NAME)
				.withObject(sampleEntity1).build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withIndexName(INDEX_2_NAME)
				.withObject(sampleEntity2).build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withIndices(INDEX_1_NAME)
				.build();

		// when
		long count = elasticsearchTemplate.count(searchQuery);

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowAnExceptionForGivenCriteriaQueryWhenNoIndexSpecifiedForCountQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());

		// when
		long count = elasticsearchTemplate.count(criteriaQuery);

		// then
		assertThat(count).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class) // DATAES-67
	public void shouldThrowAnExceptionForGivenSearchQueryWhenNoIndexSpecifiedForCountQuery() {

		// given
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = getIndexQuery(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleEntity.class);
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();

		// when
		elasticsearchTemplate.count(searchQuery);
	}

	@Test // DATAES-71
	public void shouldCreateIndexWithGivenSettings() {

		// given
		String settings = "{\n" + "        \"index\": {\n" + "            \"number_of_shards\": \"1\",\n"
				+ "            \"number_of_replicas\": \"0\",\n" + "            \"analysis\": {\n"
				+ "                \"analyzer\": {\n" + "                    \"emailAnalyzer\": {\n"
				+ "                        \"type\": \"custom\",\n"
				+ "                        \"tokenizer\": \"uax_url_email\"\n" + "                    }\n"
				+ "                }\n" + "            }\n" + "        }\n" + "}";

		elasticsearchTemplate.deleteIndex(INDEX_3_NAME);

		// when
		elasticsearchTemplate.createIndex(INDEX_3_NAME, settings);

		// then
		Map map = elasticsearchTemplate.getSetting(INDEX_3_NAME);
		assertThat(elasticsearchTemplate.indexExists(INDEX_3_NAME)).isTrue();
		assertThat(map.containsKey("index.analysis.analyzer.emailAnalyzer.tokenizer")).isTrue();
		assertThat(map.get("index.analysis.analyzer.emailAnalyzer.tokenizer")).isEqualTo("uax_url_email");
	}

	@Test // DATAES-71
	public void shouldCreateGivenSettingsForGivenIndex() {

		// given
		// delete , create and apply mapping in before method

		// then
		Map map = elasticsearchTemplate.getSetting(SampleEntity.class);
		assertThat(elasticsearchTemplate.indexExists(SampleEntity.class)).isTrue();
		assertThat(map.containsKey("index.refresh_interval")).isTrue();
		assertThat(map.containsKey("index.number_of_replicas")).isTrue();
		assertThat(map.containsKey("index.number_of_shards")).isTrue();
		assertThat(map.containsKey("index.store.type")).isTrue();
		assertThat(map.get("index.refresh_interval")).isEqualTo("-1");
		assertThat(map.get("index.number_of_replicas")).isEqualTo("0");
		assertThat(map.get("index.number_of_shards")).isEqualTo("1");
		assertThat(map.get("index.store.type")).isEqualTo("fs");
	}

	@Test // DATAES-88
	public void shouldCreateIndexWithGivenClassAndSettings() {

		// given
		String settings = "{\n" + "        \"index\": {\n" + "            \"number_of_shards\": \"1\",\n"
				+ "            \"number_of_replicas\": \"0\",\n" + "            \"analysis\": {\n"
				+ "                \"analyzer\": {\n" + "                    \"emailAnalyzer\": {\n"
				+ "                        \"type\": \"custom\",\n"
				+ "                        \"tokenizer\": \"uax_url_email\"\n" + "                    }\n"
				+ "                }\n" + "            }\n" + "        }\n" + "}";

		// when
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class, settings);
		elasticsearchTemplate.putMapping(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		Map map = elasticsearchTemplate.getSetting(SampleEntity.class);
		assertThat(elasticsearchTemplate.indexExists(INDEX_NAME_SAMPLE_ENTITY)).isTrue();
		assertThat(map.containsKey("index.number_of_replicas")).isTrue();
		assertThat(map.containsKey("index.number_of_shards")).isTrue();
		assertThat((String) map.get("index.number_of_replicas")).isEqualTo("0");
		assertThat((String) map.get("index.number_of_shards")).isEqualTo("1");
	}

	@Test
	public void shouldTestResultsAcrossMultipleIndices() {

		// given
		String documentId1 = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId1).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery1 = new IndexQueryBuilder().withId(sampleEntity1.getId()).withIndexName(INDEX_1_NAME)
				.withObject(sampleEntity1).build();

		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("some test message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery2 = new IndexQueryBuilder().withId(sampleEntity2.getId()).withIndexName(INDEX_2_NAME)
				.withObject(sampleEntity2).build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(indexQuery1, indexQuery2));
		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_1_NAME, INDEX_2_NAME).build();

		// when
		List<SampleEntity> sampleEntities = elasticsearchTemplate.queryForList(searchQuery, SampleEntity.class);

		// then
		assertThat(sampleEntities).hasSize(2);
	}

	@Test
	/*
	 * This is basically a demonstration to show composing entities out of heterogeneous indexes.
	 */
	public void shouldComposeObjectsReturnedFromHeterogeneousIndexes() {

		// given
		HetroEntity1 entity1 = new HetroEntity1(randomNumeric(3), "aFirstName");
		HetroEntity2 entity2 = new HetroEntity2(randomNumeric(4), "aLastName");

		IndexQuery idxQuery1 = new IndexQueryBuilder().withIndexName(INDEX_1_NAME).withId(entity1.getId())
				.withObject(entity1).build();
		IndexQuery idxQuery2 = new IndexQueryBuilder().withIndexName(INDEX_2_NAME).withId(entity2.getId())
				.withObject(entity2).build();

		elasticsearchTemplate.bulkIndex(Arrays.asList(idxQuery1, idxQuery2));
		elasticsearchTemplate.refresh(INDEX_1_NAME);
		elasticsearchTemplate.refresh(INDEX_2_NAME);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withTypes("hetro")
				.withIndices(INDEX_1_NAME, INDEX_2_NAME).build();
		Page<ResultAggregator> page = elasticsearchTemplate.queryForPage(searchQuery, ResultAggregator.class,
				new SearchResultMapperAdapter() {
					@Override
					public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
						List<ResultAggregator> values = new ArrayList<>();
						for (SearchHit searchHit : response.getHits()) {
							String id = String.valueOf(searchHit.getSourceAsMap().get("id"));
							String firstName = StringUtils.isNotEmpty((String) searchHit.getSourceAsMap().get("firstName"))
									? (String) searchHit.getSourceAsMap().get("firstName")
									: "";
							String lastName = StringUtils.isNotEmpty((String) searchHit.getSourceAsMap().get("lastName"))
									? (String) searchHit.getSourceAsMap().get("lastName")
									: "";
							values.add(new ResultAggregator(id, firstName, lastName));
						}
						return new AggregatedPageImpl<>((List<T>) values);
					}
				});

		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	public void shouldCreateIndexUsingServerDefaultConfiguration() {

		// given

		// when
		boolean created = elasticsearchTemplate.createIndex(UseServerConfigurationEntity.class);

		// then
		assertThat(created).isTrue();
		Map setting = elasticsearchTemplate.getSetting(UseServerConfigurationEntity.class);
		assertThat(setting.get("index.number_of_shards")).isEqualTo("5");
		assertThat(setting.get("index.number_of_replicas")).isEqualTo("1");
	}

	@Test
	public void shouldReadFileFromClasspathRetainingNewlines() {

		// given
		String settingsFile = "/settings/test-settings.yml";

		// when
		String content = ElasticsearchTemplate.readFileFromClasspath(settingsFile);

		// then
		assertThat(content).isEqualTo("index:\n" + "  number_of_shards: 1\n" + "  number_of_replicas: 0\n" + "  analysis:\n"
				+ "    analyzer:\n" + "      emailAnalyzer:\n" + "        type: custom\n" + "        tokenizer: uax_url_email");
	}

	@Test // DATAES-531
	public void shouldReturnMappingForGivenEntityClass() {

		// given

		// when
		boolean created = elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.putMapping(SampleEntity.class);
		Map<String, Object> mapping = elasticsearchTemplate.getMapping(SampleEntity.class);

		// then
		assertThat(created).isTrue();
		assertThat(mapping).isNotNull();
		assertThat(((Map<String, Object>) ((Map<String, Object>) mapping.get("properties")).get("message")).get("type"))
				.isEqualTo("text");
	}

	@Test // DATAES-525
	public void shouldDeleteOnlyDocumentsMatchedByDeleteQuery() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// document to be deleted
		String documentIdToDelete = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(documentIdToDelete).message("some message")
				.version(System.currentTimeMillis()).build()));

		// remaining document
		String remainingDocumentId = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(remainingDocumentId).message("some other message")
				.version(System.currentTimeMillis()).build()));
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(idsQuery().addIds(documentIdToDelete));
		elasticsearchTemplate.delete(deleteQuery, SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		// document with id "remainingDocumentId" should still be indexed
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(1);
		assertThat(sampleEntities.getContent().get(0).getId()).isEqualTo(remainingDocumentId);
	}

	@Test // DATAES-525
	public void shouldDeleteOnlyDocumentsMatchedByCriteriaQuery() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		// given
		// document to be deleted
		String documentIdToDelete = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(documentIdToDelete).message("some message")
				.version(System.currentTimeMillis()).build()));

		// remaining document
		String remainingDocumentId = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(remainingDocumentId).message("some other message")
				.version(System.currentTimeMillis()).build()));
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(documentIdToDelete));
		elasticsearchTemplate.delete(criteriaQuery, SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		// document with id "remainingDocumentId" should still be indexed
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(1);
		assertThat(sampleEntities.getContent().get(0).getId()).isEqualTo(remainingDocumentId);
	}

	@Test // DATAES-525
	public void shouldDeleteDocumentForGivenIdOnly() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// document to be deleted
		String documentIdToDelete = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(documentIdToDelete).message("some message")
				.version(System.currentTimeMillis()).build()));

		// remaining document
		String remainingDocumentId = UUID.randomUUID().toString();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(remainingDocumentId).message("some other message")
				.version(System.currentTimeMillis()).build()));
		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		elasticsearchTemplate.delete(SampleEntity.class, documentIdToDelete);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		// document with id "remainingDocumentId" should still be indexed
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);
		assertThat(sampleEntities.getTotalElements()).isEqualTo(1L);
		assertThat(sampleEntities.getContent().get(0).getId()).isEqualTo(remainingDocumentId);
	}

	@Test // DATAES-525
	public void shouldApplyCriteriaQueryToScanAndScrollForGivenCriteriaQuery() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some message that should be found by the scroll query").version(System.currentTimeMillis()).build()));
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some other message that should be found by the scroll query").version(System.currentTimeMillis())
				.build()));
		String notFindableMessage = "this entity must not be found by the scroll query";
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString()).message(notFindableMessage)
				.version(System.currentTimeMillis()).build()));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("message"));
		criteriaQuery.addIndices(INDEX_NAME_SAMPLE_ENTITY);
		criteriaQuery.addTypes(TYPE_NAME);
		criteriaQuery.setPageable(PageRequest.of(0, 10));

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, criteriaQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scroll = elasticsearchTemplate.continueScroll(scroll.getScrollId(), 1000, SampleEntity.class);
		}
		elasticsearchTemplate.clearScroll(scroll.getScrollId());

		// then
		assertThat(sampleEntities).hasSize(2);
		assertThat(sampleEntities.stream().map(SampleEntity::getMessage).collect(Collectors.toList()))
				.doesNotContain(notFindableMessage);
	}

	@Test // DATAES-525
	public void shouldApplySearchQueryToScanAndScrollForGivenSearchQuery() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some message that should be found by the scroll query").version(System.currentTimeMillis()).build()));
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString())
				.message("some other message that should be found by the scroll query").version(System.currentTimeMillis())
				.build()));
		String notFindableMessage = "this entity must not be found by the scroll query";
		indexQueries.add(getIndexQuery(SampleEntity.builder().id(UUID.randomUUID().toString()).message(notFindableMessage)
				.version(System.currentTimeMillis()).build()));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// when
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("message", "message"))
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withPageable(PageRequest.of(0, 10)).build();

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scroll = elasticsearchTemplate.continueScroll(scroll.getScrollId(), 1000, SampleEntity.class);
		}
		elasticsearchTemplate.clearScroll(scroll.getScrollId());

		// then
		assertThat(sampleEntities).hasSize(2);
		assertThat(sampleEntities.stream().map(SampleEntity::getMessage).collect(Collectors.toList()))
				.doesNotContain(notFindableMessage);
	}

	@Test // DATAES-565
	public void shouldRespectSourceFilterWithScanAndScrollForGivenSearchQuery() {

		// given
		List<IndexQuery> entities = createSampleEntitiesWithMessage("Test message", 3);

		// when
		elasticsearchTemplate.bulkIndex(entities);
		elasticsearchTemplate.refresh(SampleEntity.class);

		// then
		SourceFilter sourceFilter = new FetchSourceFilter(new String[] { "id" }, new String[] {});

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withPageable(PageRequest.of(0, 10))
				.withSourceFilter(sourceFilter).build();

		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scroll = elasticsearchTemplate.continueScroll(scroll.getScrollId(), 1000, SampleEntity.class);
		}
		elasticsearchTemplate.clearScroll(scroll.getScrollId());
		assertThat(sampleEntities).hasSize(3);
		assertThat(sampleEntities.stream().map(SampleEntity::getId).collect(Collectors.toList()))
				.doesNotContain((String) null);
		assertThat(sampleEntities.stream().map(SampleEntity::getMessage).collect(Collectors.toList()))
				.containsOnly((String) null);
	}

	@Test // DATAES-457
	public void shouldSortResultsGivenSortCriteriaWithScanAndScroll() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyz").rate(10)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withSort(new FieldSortBuilder("rate").order(SortOrder.ASC))
				.withSort(new FieldSortBuilder("message").order(SortOrder.DESC)).withPageable(PageRequest.of(0, 10)).build();

		// when
		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scroll = elasticsearchTemplate.continueScroll(scroll.getScrollId(), 1000, SampleEntity.class);
		}

		// then
		assertThat(sampleEntities).hasSize(3);
		assertThat(sampleEntities.get(0).getRate()).isEqualTo(sampleEntity2.getRate());
		assertThat(sampleEntities.get(1).getRate()).isEqualTo(sampleEntity3.getRate());
		assertThat(sampleEntities.get(1).getMessage()).isEqualTo(sampleEntity3.getMessage());
		assertThat(sampleEntities.get(2).getRate()).isEqualTo(sampleEntity1.getRate());
		assertThat(sampleEntities.get(2).getMessage()).isEqualTo(sampleEntity1.getMessage());
	}

	@Test // DATAES-457
	public void shouldSortResultsGivenSortCriteriaFromPageableWithScanAndScroll() {

		// given
		List<IndexQuery> indexQueries = new ArrayList<>();
		// first document
		String documentId = randomNumeric(5);
		SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(10)
				.version(System.currentTimeMillis()).build();

		// second document
		String documentId2 = randomNumeric(5);
		SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
				.version(System.currentTimeMillis()).build();

		// third document
		String documentId3 = randomNumeric(5);
		SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyz").rate(10)
				.version(System.currentTimeMillis()).build();

		indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPageable(
						PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "rate").and(Sort.by(Sort.Direction.DESC, "message"))))
				.build();

		// when
		ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class);
		List<SampleEntity> sampleEntities = new ArrayList<>();
		while (scroll.hasContent()) {
			sampleEntities.addAll(scroll.getContent());
			scroll = elasticsearchTemplate.continueScroll(scroll.getScrollId(), 1000, SampleEntity.class);
		}

		// then
		assertThat(sampleEntities).hasSize(3);
		assertThat(sampleEntities.get(0).getRate()).isEqualTo(sampleEntity2.getRate());
		assertThat(sampleEntities.get(1).getRate()).isEqualTo(sampleEntity3.getRate());
		assertThat(sampleEntities.get(1).getMessage()).isEqualTo(sampleEntity3.getMessage());
		assertThat(sampleEntities.get(2).getRate()).isEqualTo(sampleEntity1.getRate());
		assertThat(sampleEntities.get(2).getMessage()).isEqualTo(sampleEntity1.getMessage());
	}

	@Test // DATAES-593
	public void shouldReturnDocumentWithCollapsedField() {

		// given
		SampleEntity sampleEntity = SampleEntity.builder().id(randomNumeric(5)).message("message 1").rate(1)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntity2 = SampleEntity.builder().id(randomNumeric(5)).message("message 2").rate(2)
				.version(System.currentTimeMillis()).build();
		SampleEntity sampleEntit3 = SampleEntity.builder().id(randomNumeric(5)).message("message 1").rate(1)
				.version(System.currentTimeMillis()).build();

		List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity, sampleEntity2, sampleEntit3));

		elasticsearchTemplate.bulkIndex(indexQueries);
		elasticsearchTemplate.refresh(SampleEntity.class);

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(INDEX_NAME_SAMPLE_ENTITY).withTypes(TYPE_NAME).withCollapseField("rate").build();

		// when
		Page<SampleEntity> page = elasticsearchTemplate.queryForPage(searchQuery, SampleEntity.class);

		// then
		assertThat(page).isNotNull();
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getContent()).hasSize(2);
		assertThat(page.getContent().get(0).getMessage()).isEqualTo("message 1");
		assertThat(page.getContent().get(1).getMessage()).isEqualTo("message 2");
	}

	private IndexQuery getIndexQuery(SampleEntity sampleEntity) {
		return new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity)
				.withVersion(sampleEntity.getVersion()).build();
	}

	private List<IndexQuery> getIndexQueries(List<SampleEntity> sampleEntities) {
		List<IndexQuery> indexQueries = new ArrayList<>();
		for (SampleEntity sampleEntity : sampleEntities) {
			indexQueries.add(getIndexQuery(sampleEntity));
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

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode(exclude = "score")
	@Builder
	@Document(indexName = INDEX_NAME_SAMPLE_ENTITY, type = "test-type", shards = 1, replicas = 0, refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@org.springframework.data.elasticsearch.annotations.Field(type = Text, store = true,
				fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		@ScriptedField private Double scriptedRate;
		private boolean available;
		private String highlightedMessage;
		private GeoPoint location;
		@Version private Long version;
		@Score private float score;
	}

	/**
	 * @author Gad Akuka
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 */
	@Data
	@AllArgsConstructor
	@Builder
	@Document(indexName = "test-index-uuid-keyed-core-template", type = "test-type-uuid-keyed", shards = 1, replicas = 0,
			refreshInterval = "-1")
	private static class SampleEntityUUIDKeyed {

		@Id private UUID id;
		private String type;
		@Field(type = FieldType.Text, fielddata = true) private String message;
		private int rate;
		@ScriptedField private Long scriptedRate;
		private boolean available;
		private String highlightedMessage;

		private GeoPoint location;

		@Version private Long version;

	}

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	@Document(indexName = "test-index-book-core-template", type = "book", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class Book {

		@Id private String id;
		private String name;
		@Field(type = FieldType.Object) private Author author;
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;
	}

	@Data
	static class Author {

		private String id;
		private String name;
	}

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	@Document(indexName = "test-index-version-core-template", type = "test-type", shards = 1, replicas = 0,
			refreshInterval = "-1", versionType = VersionType.EXTERNAL_GTE)
	private static class GTEVersionEntity {

		@Version private Long version;

		@Id private String id;

		private String name;
	}

	@Data
	@Document(indexName = "test-index-hetro1-core-template", type = "hetro", replicas = 0, shards = 1)
	static class HetroEntity1 {

		@Id private String id;
		private String firstName;
		@Version private Long version;

		HetroEntity1(String id, String firstName) {
			this.id = id;
			this.firstName = firstName;
			this.version = System.currentTimeMillis();
		}
	}

	@Data
	@Document(indexName = "test-index-hetro2-core-template", type = "hetro", replicas = 0, shards = 1)
	static class HetroEntity2 {

		@Id private String id;
		private String lastName;
		@Version private Long version;

		HetroEntity2(String id, String lastName) {
			this.id = id;
			this.lastName = lastName;
			this.version = System.currentTimeMillis();
		}
	}

	@Data
	@Document(indexName = "test-index-server-configuration", type = "test-type", useServerConfiguration = true,
			shards = 10, replicas = 10, refreshInterval = "-1")
	private static class UseServerConfigurationEntity {

		@Id private String id;
		private String val;

	}

	@Data
	@Document(indexName = "test-index-sample-mapping", type = "mapping", shards = 1, replicas = 0, refreshInterval = "-1")
	static class SampleMappingEntity {

		@Id private String id;

		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		static class NestedEntity {

			@Field(type = Text) private String someField;

			public String getSomeField() {
				return someField;
			}

			public void setSomeField(String someField) {
				this.someField = someField;
			}
		}
	}

}
