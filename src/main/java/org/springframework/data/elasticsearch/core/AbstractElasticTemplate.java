/*
 * Copyright 2013-2018 the original author or authors.
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

import org.elasticsearch.action.admin.indices.create.*;
import org.elasticsearch.action.admin.indices.refresh.*;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.*;
import org.elasticsearch.common.*;
import org.elasticsearch.common.collect.*;
import org.elasticsearch.common.unit.*;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.*;
import org.elasticsearch.search.fetch.subphase.highlight.*;
import org.elasticsearch.search.sort.*;
import org.elasticsearch.search.suggest.*;
import org.slf4j.*;
import org.springframework.context.*;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.*;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.aggregation.*;
import org.springframework.data.elasticsearch.core.convert.*;
import org.springframework.data.elasticsearch.core.mapping.*;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.utils.*;
import org.springframework.data.util.*;
import org.springframework.util.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static org.elasticsearch.client.Requests.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.core.MappingBuilder.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.*;

/**
 * @author Nikita Guchakov
 */
public abstract class AbstractElasticTemplate implements ElasticsearchOperations, ApplicationContextAware {
	private static final Logger log = LoggerFactory.getLogger(AbstractElasticTemplate.class);
	private static final String FIELD_SCORE = "_score";

	private final ResultsMapper resultsMapper;
	private ElasticsearchConverter elasticsearchConverter;

	AbstractElasticTemplate(ElasticsearchConverter elasticsearchConverter, ResultsMapper resultsMapper) {
		Assert.notNull(elasticsearchConverter, "ElasticsearchConverter must not be null!");
		this.elasticsearchConverter = elasticsearchConverter;
		Assert.notNull(resultsMapper, "ResultsMapper must not be null!");
		this.resultsMapper = resultsMapper;
	}

	@Deprecated // use ClassPathUtils.readFileFromClasspath directly
	public static String readFileFromClasspath(String url) {
		return ClassPathUtils.readFileFromClasspath(url);
	}

	@Override
	public boolean createIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for Query");
		try {
			return createIndex(Requests.createIndexRequest(indexName));
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to create index " + indexName, e);
		}
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz) {
		return createIndexIfNotCreated(clazz);
	}

	abstract boolean createIndex(CreateIndexRequest indexRequest) throws IOException;

	@Override
	public <T> boolean putMapping(Class<T> clazz) {
		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();
			if (!StringUtils.isEmpty(mappingPath)) {
				String mappings = ClassPathUtils.readFileFromClasspath(mappingPath);
				if (!StringUtils.isEmpty(mappings)) {
					return putMapping(clazz, mappings);
				}
			}
		}

		log.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
		ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
		try {
			ElasticsearchPersistentProperty property = persistentEntity.getRequiredIdProperty();
			XContentBuilder xContentBuilder = buildMapping(clazz, persistentEntity.getIndexType(), property.getFieldName(),
					persistentEntity.getParentType());
			return putMapping(clazz, xContentBuilder);
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz, Object mapping) {
		return putMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType(),
				mapping);
	}

	@Override
	public <T> Map getMapping(Class<T> clazz) {
		return getMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType());
	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return elasticsearchConverter;
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz) {
		return queryForObject(query, clazz, resultsMapper);
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {
		ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
		GetResponse response = queryForObject(query, persistentEntity.getIndexName(), persistentEntity.getIndexType());
		return mapper.mapResult(response, clazz);
	}

	@Override
	public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> T queryForObject(StringQuery query, Class<T> clazz) {
		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public abstract <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz,
			SearchResultMapper resultsMapper);

	@Override
	public <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<T> queryForList(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> CloseableIterator<T> stream(CriteriaQuery query, Class<T> clazz) {
		final long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		return doStream(scrollTimeInMillis, (ScrolledPage<T>) startScroll(scrollTimeInMillis, query, clazz), clazz,
				resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz) {
		return stream(query, clazz, resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, final Class<T> clazz, final SearchResultMapper mapper) {
		final long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		return doStream(scrollTimeInMillis, (ScrolledPage<T>) startScroll(scrollTimeInMillis, query, clazz, mapper), clazz,
				mapper);
	}

	private <T> CloseableIterator<T> doStream(final long scrollTimeInMillis, final ScrolledPage<T> page,
			final Class<T> clazz, final SearchResultMapper mapper) {
		return new PageIterator<>(page,
				nextScrollId -> (ScrolledPage<T>) continueScroll(nextScrollId, scrollTimeInMillis, clazz, mapper),
				this::clearScroll);
	}

	@Override
	public <T> void refresh(Class<T> clazz) {
		refresh(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public void refresh(String indexName) {
		Assert.notNull(indexName, "No index defined for refresh()");
		refresh(refreshRequest(indexName));
	}

	HighlightBuilder highlightFromQuery(SearchQuery searchQuery) {
		HighlightBuilder highlightBuilder = searchQuery.getHighlightBuilder();
		if (highlightBuilder == null) {
			highlightBuilder = new HighlightBuilder();
		}
		if (searchQuery.getHighlightFields() == null) {
			return highlightBuilder;
		}
		for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
			highlightBuilder.field(highlightField);
		}
		return highlightBuilder;
	}

	SortBuilder toSortBuilder(Sort.Order order) {
		SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC;
		if (FIELD_SCORE.equals(order.getProperty())) {
			return SortBuilders.scoreSort().order(sortOrder);
		} else {
			FieldSortBuilder fieldSortBuilder = SortBuilders.fieldSort(order.getProperty()).order(sortOrder);

			if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
				fieldSortBuilder.missing("_first");
			} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
				fieldSortBuilder.missing("_last");
			}
			return fieldSortBuilder;
		}
	}

	@Override
	public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {

		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
		String indexName = StringUtils.isEmpty(query.getIndexName()) ? persistentEntity.getIndexName()
				: query.getIndexName();
		String type = StringUtils.isEmpty(query.getType()) ? persistentEntity.getIndexType() : query.getType();

		Assert.notNull(indexName, "No 'indexName' defined for MoreLikeThisQuery");
		Assert.notNull(type, "No 'type' defined for MoreLikeThisQuery");
		Assert.notNull(query.getId(), "No document id defined for MoreLikeThisQuery");

		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = moreLikeThisQuery(
				toArray(new MoreLikeThisQueryBuilder.Item(indexName, type, query.getId())));

		if (query.getMinTermFreq() != null) {
			moreLikeThisQueryBuilder.minTermFreq(query.getMinTermFreq());
		}
		if (query.getMaxQueryTerms() != null) {
			moreLikeThisQueryBuilder.maxQueryTerms(query.getMaxQueryTerms());
		}
		if (!isEmpty(query.getStopWords())) {
			moreLikeThisQueryBuilder.stopWords(toArray(query.getStopWords()));
		}
		if (query.getMinDocFreq() != null) {
			moreLikeThisQueryBuilder.minDocFreq(query.getMinDocFreq());
		}
		if (query.getMaxDocFreq() != null) {
			moreLikeThisQueryBuilder.maxDocFreq(query.getMaxDocFreq());
		}
		if (query.getMinWordLen() != null) {
			moreLikeThisQueryBuilder.minWordLength(query.getMinWordLen());
		}
		if (query.getMaxWordLen() != null) {
			moreLikeThisQueryBuilder.maxWordLength(query.getMaxWordLen());
		}
		if (query.getBoostTerms() != null) {
			moreLikeThisQueryBuilder.boostTerms(query.getBoostTerms());
		}

		return queryForPage(new NativeSearchQueryBuilder().withQuery(moreLikeThisQueryBuilder).build(), clazz);
	}

	@Override
	public <T> Map getSetting(Class<T> clazz) {
		return getSetting(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz) {
		Assert.isTrue(clazz.isAnnotationPresent(Document.class), "Unable to identify index name. " + clazz.getSimpleName()
				+ " is not a Document. Make sure the document class is annotated with @Document(indexName=\"foo\")");
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	SearchResponse doScroll(Class<?> clazz, long scrollTimeInMillis, CriteriaQuery criteriaQuery) {
		Assert.notNull(criteriaQuery.getIndices(), "No index defined for Query");
		Assert.notNull(criteriaQuery.getTypes(), "No type define for Query");
		Assert.notNull(criteriaQuery.getPageable(), "Query.pageable is required for scan & scroll");

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor()
				.createFilterFromCriteria(criteriaQuery.getCriteria());

		return scroll(clazz, scrollTimeInMillis, criteriaQuery, elasticsearchQuery, elasticsearchFilter);
	}

	@Override
	public <T> String delete(Class<T> clazz, String id) {
		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
		return delete(persistentEntity.getIndexName(), persistentEntity.getIndexType(), id);
	}

	@Override
	public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper getResultMapper) {
		return getResultMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	@Override
	public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz) {
		return getResultsMapper().mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	private <T> MultiGetResponse getMultiResponse(Query searchQuery, Class<T> clazz) {

		String indexName = !isEmpty(searchQuery.getIndices()) ? searchQuery.getIndices().get(0)
				: getPersistentEntityFor(clazz).getIndexName();
		String type = !isEmpty(searchQuery.getTypes()) ? searchQuery.getTypes().get(0)
				: getPersistentEntityFor(clazz).getIndexType();

		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notEmpty(searchQuery.getIds(), "No Id define for Query");

		if (searchQuery.getFields() != null && !searchQuery.getFields().isEmpty()) {
			searchQuery.addSourceFilter(new FetchSourceFilter(toArray(searchQuery.getFields()), null));
		}

		Stream<MultiGetRequest.Item> items = searchQuery.getIds().stream()
				.map(id -> new MultiGetRequest.Item(indexName, type, id)).map(item -> item.routing(searchQuery.getRoute()));
		return executeMultiRequest(items);
	}

	@Override
	public <T> List<String> queryForIds(SearchQuery query) {
		return extractIds(getIdsResponse(query));
	}

	abstract SearchResponse getIdsResponse(SearchQuery query);

	@Override
	public <T> long count(CriteriaQuery query) {
		return count(query, null);
	}

	@Override
	public <T> long count(SearchQuery query) {
		return count(query, null);
	}

	abstract MultiGetResponse executeMultiRequest(Stream<MultiGetRequest.Item> items);

	@Override
	public <T> boolean deleteIndex(Class<T> clazz) {
		return deleteIndex(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public <T> boolean indexExists(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public void delete(DeleteQuery deleteQuery) {
		Assert.notNull(deleteQuery.getIndex(), "No index defined for Query");
		Assert.notNull(deleteQuery.getType(), "No type define for Query");
		delete(deleteQuery, null);
	}

	@Override
	public <T> void delete(CriteriaQuery criteriaQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		Assert.notNull(elasticsearchQuery, "Query can not be null.");
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(elasticsearchQuery);
		delete(deleteQuery, clazz);
	}

	@Override
	public <T> void delete(DeleteQuery deleteQuery, Class<T> clazz) {

		String indexName = hasText(deleteQuery.getIndex()) ? deleteQuery.getIndex()
				: getPersistentEntityFor(clazz).getIndexName();
		String typeName = hasText(deleteQuery.getType()) ? deleteQuery.getType()
				: getPersistentEntityFor(clazz).getIndexType();
		int pageSize = deleteQuery.getPageSize() != null ? deleteQuery.getPageSize() : 1000;
		long scrollTimeInMillis = deleteQuery.getScrollTimeInMillis() != null ? deleteQuery.getScrollTimeInMillis()
				: 10000L;

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(deleteQuery.getQuery()).withIndices(indexName)
				.withTypes(typeName).withPageable(PageRequest.of(0, pageSize)).build();

		SearchResultMapper onlyIdResultMapper = new IdResultMapper();
		List<String> ids = new ArrayList<>();
		Page<String> scrolledResult = startScroll(scrollTimeInMillis, searchQuery, String.class, onlyIdResultMapper);
		do {
			ids.addAll(scrolledResult.getContent());
			scrolledResult = continueScroll(((ScrolledPage<T>) scrolledResult).getScrollId(), scrollTimeInMillis,
					String.class, onlyIdResultMapper);
		} while (!scrolledResult.getContent().isEmpty());

		bulkDelete(indexName, typeName, ids);

		clearScroll(((ScrolledPage<T>) scrolledResult).getScrollId());
	}

	SearchResponse doScroll(Class<?> clazz, long scrollTimeInMillis, SearchQuery searchQuery) {
		Assert.notNull(searchQuery.getIndices(), "No index defined for Query");
		Assert.notNull(searchQuery.getTypes(), "No type define for Query");
		Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scan & scroll");
		return scroll(clazz, scrollTimeInMillis, searchQuery);
	}

	@Override
	public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz) {
		SearchResponse response = doScroll(clazz, scrollTimeInMillis, searchQuery);
		return resultsMapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz) {
		SearchResponse response = doScroll(clazz, scrollTimeInMillis, criteriaQuery);
		return resultsMapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery searchQuery, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchResponse response = doScroll(clazz, scrollTimeInMillis, searchQuery);
		return mapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz,
			SearchResultMapper mapper) {
		SearchResponse response = doScroll(clazz, scrollTimeInMillis, criteriaQuery);
		return mapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> Page<T> continueScroll(@Nullable String scrollId, long scrollTimeInMillis, Class<T> clazz) {
		return continueScroll(scrollId, scrollTimeInMillis, clazz, resultsMapper);
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz, Object settings) {
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
	}

	String getPersistentEntityId(Object entity) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(entity.getClass());
		Object identifier = persistentEntity.getIdentifierAccessor(entity).getIdentifier();

		return identifier != null ? identifier.toString() : null;

	}

	void setPersistentEntityId(Object entity, String id) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(entity.getClass());
		ElasticsearchPersistentProperty idProperty = persistentEntity.getIdProperty();

		// Only deal with text because ES generated Ids are strings !

		if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
			persistentEntity.getPropertyAccessor(entity).setProperty(idProperty, id);
		}
	}

	void setPersistentEntityIndexAndType(Query query, Class clazz) {
		if (query.getIndices().isEmpty()) {
			query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
		}
		if (query.getTypes().isEmpty()) {
			query.addTypes(retrieveTypeFromPersistentEntity(clazz));
		}
	}

	String[] retrieveIndexNameFromPersistentEntity(Class clazz) {
		return clazz != null ? new String[] { getPersistentEntityFor(clazz).getIndexName() } : null;
	}

	private List<String> extractIds(SearchResponse response) {
		List<String> ids = new ArrayList<>();
		for (SearchHit hit : response.getHits()) {
			if (hit != null) {
				ids.add(hit.getId());
			}
		}
		return ids;
	}

	String[] retrieveTypeFromPersistentEntity(Class clazz) {
		return clazz != null ? new String[] { getPersistentEntityFor(clazz).getIndexType() } : null;
	}

	ResultsMapper getResultsMapper() {
		return resultsMapper;
	}

	public SearchResponse suggest(SuggestBuilder suggestion, Class clazz) {
		return suggest(suggestion, retrieveIndexNameFromPersistentEntity(clazz));
	}

	public void setApplicationContext(ApplicationContext context) {
		if (getElasticsearchConverter() instanceof ApplicationContextAware) {
			((ApplicationContextAware) getElasticsearchConverter()).setApplicationContext(context);
		}
	}

	private <T> boolean createIndexIfNotCreated(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName()) || createIndexWithSettings(clazz);
	}

	private <T> boolean createIndexWithSettings(Class<T> clazz) {
		if (clazz.isAnnotationPresent(Setting.class)) {
			String settingPath = clazz.getAnnotation(Setting.class).settingPath();
			if (!StringUtils.isEmpty(settingPath)) {
				String settings = ClassPathUtils.readFileFromClasspath(settingPath);
				if (!StringUtils.isEmpty(settings)) {
					return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
				}
			} else {
				log.info("settingPath in @Setting has to be defined. Using default instead.");
			}
		}
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), getDefaultSettings(getPersistentEntityFor(clazz)));
	}

	private <T> Map getDefaultSettings(ElasticsearchPersistentEntity<T> persistentEntity) {

		if (persistentEntity.isUseServerConfiguration())
			return new HashMap();

		return new MapBuilder<String, String>().put("index.number_of_shards", String.valueOf(persistentEntity.getShards()))
				.put("index.number_of_replicas", String.valueOf(persistentEntity.getReplicas()))
				.put("index.refresh_interval", persistentEntity.getRefreshInterval())
				.put("index.store.type", persistentEntity.getIndexStoreType()).map();
	}

	static String[] toArray(List<String> values) {
		return values.toArray(new String[0]);
	}

	private static MoreLikeThisQueryBuilder.Item[] toArray(MoreLikeThisQueryBuilder.Item... values) {
		return values;
	}

	@Override
	public abstract void clearScroll(String scrollId);

	public abstract SearchResponse suggest(SuggestBuilder suggestion, String... indices);

	abstract SearchResponse scroll(Class<?> clazz, long scrollTimeInMillis, SearchQuery searchQuery);

	abstract SearchResponse scroll(Class<?> clazz, long scrollTimeInMillis, CriteriaQuery criteriaQuery,
			QueryBuilder elasticsearchQuery, QueryBuilder elasticsearchFilter);

	abstract void bulkDelete(String indexName, String typeName, List<String> ids);

	abstract GetResponse queryForObject(GetQuery query, String indexName, String indexType);

	abstract void refresh(RefreshRequest refreshRequest);

}
