package org.springframework.data.elasticsearch.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.jest.ExtendedSearchResult;
import org.springframework.data.elasticsearch.core.jest.MultiDocumentResult;
import org.springframework.data.elasticsearch.core.jest.ScrollSearchResult;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexBoost;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.ScriptField;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.ClearScroll;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import io.searchbox.core.Delete;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.core.MultiGet;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import io.searchbox.core.Update;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.Refresh;
import io.searchbox.indices.Refresh.Builder;
import io.searchbox.indices.aliases.AddAliasMapping;
import io.searchbox.indices.aliases.GetAliases;
import io.searchbox.indices.aliases.ModifyAliases;
import io.searchbox.indices.aliases.RemoveAliasMapping;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.indices.settings.GetSettings;
import io.searchbox.indices.type.TypeExist;
import io.searchbox.params.Parameters;
import io.searchbox.params.SearchType;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.elasticsearch.index.VersionType.EXTERNAL;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Jest implementation of ElasticsearchOperations.
 *
 * @author Roy Julien
 * @author Robert Gruendler
 */
public class JestElasticsearchTemplate implements ElasticsearchOperations, ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(JestElasticsearchTemplate.class);

	private final JestClient client;
	private final ElasticsearchConverter elasticsearchConverter;
	private final JestResultsMapper resultsMapper;

	public JestElasticsearchTemplate(JestClient client) {
		this(client, null, null);
	}

	public JestElasticsearchTemplate(JestClient client, JestResultsMapper resultMapper) {
		this(client, null, resultMapper);
	}

	public JestElasticsearchTemplate(JestClient client, ElasticsearchConverter elasticsearchConverter, JestResultsMapper resultsMapper) {
		this.client = client;
		this.elasticsearchConverter = (elasticsearchConverter == null) ? new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()) : elasticsearchConverter;
		this.resultsMapper = (resultsMapper == null) ? new DefaultJestResultsMapper(this.elasticsearchConverter.getMappingContext()) : resultsMapper;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (elasticsearchConverter instanceof ApplicationContextAware) {
			((ApplicationContextAware) elasticsearchConverter).setApplicationContext(context);
		}
	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return elasticsearchConverter;
	}

	@Override
	public Client getClient() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz) {
		return createIndexIfNotCreated(clazz);
	}

	@Override
	public boolean createIndex(String indexName) {
		JestResult result = execute(new CreateIndex.Builder(indexName).build());
		return result.isSucceeded();
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {

		CreateIndex.Builder createIndexBuilder = new CreateIndex.Builder(indexName);

		if (settings instanceof String) {
			createIndexBuilder.settings(String.valueOf(settings));
		} else if (settings instanceof Map) {
			createIndexBuilder.settings(settings);
		} else if (settings instanceof XContentBuilder) {
			createIndexBuilder.settings(settings);
		}

		return executeWithAcknowledge(createIndexBuilder.build());
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz, Object settings) {
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz) {
		logger.warn("putMapping(Class<T> clazz) not implemented yet in jest client");
		return false;
	}

	@Override
	public boolean putMapping(String indexName, String type, Object mapping) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");

		Object source = null;
		if (mapping instanceof String) {
			source = String.valueOf(mapping);
		} else if (mapping instanceof Map) {
			source = mapping;
		} else if (mapping instanceof XContentBuilder) {
			source = mapping;
		}

		PutMapping.Builder requestBuilder = new PutMapping.Builder(indexName, type, source);

		return executeWithAcknowledge(requestBuilder.build());
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz, Object mapping) {
		return putMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType(), mapping);
	}

	@Override
	public <T> Map getMapping(Class<T> clazz) {
		return getMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType());
	}

	@Override
	public Map getMapping(String indexName, String type) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");
		Map mappings = new HashMap();

		try {

			GetMapping.Builder getMappingBuilder = new GetMapping.Builder();
			getMappingBuilder.addIndex(indexName).addType(type);

			JestResult result = execute(getMappingBuilder.build());

			JsonObject rawProperties = result.getJsonObject().get(indexName).getAsJsonObject()
					.get("mappings").getAsJsonObject()
					.get(type).getAsJsonObject().get("properties").getAsJsonObject();

			Map<String, String> properties = new HashMap<String, String>();
			for (Map.Entry<String, JsonElement> entry : rawProperties.entrySet()) {
				properties.put(entry.getKey(), entry.getValue().getAsJsonObject().get("type").getAsString());
			}

			mappings.put("properties", properties);
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + indexName + " type : " + type + " " + e.getMessage());
		}
		return mappings;
	}

	@Override
	public Map getSetting(String indexName) {
		Assert.notNull(indexName, "No index defined for getSettings");

		GetSettings.Builder getSettingsBuilder = new GetSettings.Builder();
		getSettingsBuilder.addIndex(indexName);

		JestResult result = execute(getSettingsBuilder.build());

		Set<Map.Entry<String, JsonElement>> entries = result.getJsonObject()
				.get(indexName).getAsJsonObject()
				.get("settings").getAsJsonObject()
				.get("index").getAsJsonObject().entrySet();

		HashMap<String, String> mappings = new HashMap<String, String>();
		// flatten the settings hash to a "index.property.nestedProperty.etc -> value" map
		for(Map.Entry<String, JsonElement> entry : entries) {

			StringBuilder builder = new StringBuilder();
			builder.append("index");
			String value = null;
			if (entry.getValue().isJsonPrimitive()) {
				value = entry.getValue().getAsString();
				builder.append(".").append(entry.getKey());
				mappings.put(builder.toString(), value);
			} else if (entry.getValue().isJsonArray()) {
				// TODO
			} else if (entry.getValue().isJsonObject()) {
				builder.append(".").append(entry.getKey());
				appendJsonObjectKeyValue((JsonObject) entry.getValue(), builder, mappings);
			}

			if (value != null) {
			} else {
				logger.warn("Unable to get value for setting " + builder.toString());
			}
		}
		return mappings;
	}

	private void appendJsonObjectKeyValue(JsonObject jsonObject, StringBuilder builder, Map<String, String> mappings) {

		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			if (entry.getValue().isJsonObject()) {
				StringBuilder newBuilder = new StringBuilder(builder.toString());
				newBuilder.append(".").append(entry.getKey());
				appendJsonObjectKeyValue((JsonObject) entry.getValue(), newBuilder, mappings);
			} else if (entry.getValue().isJsonArray()) {
				// TODO
			} else {
				String key = builder.toString() + "." + entry.getKey();
				mappings.put(key, entry.getValue().getAsString());
			}
		}
	}

	@Override
	public <T> Map getSetting(Class<T> clazz) {
		return getSetting(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz) {
		return queryForObject(query, clazz, resultsMapper);
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {
		return queryForObject(query, clazz, resultsMapper);
	}


	public <T> T queryForObject(GetQuery query, Class<T> clazz, JestGetResultMapper mapper) {
		return queryForObject(null, query, clazz, mapper);
	}

	public <T> T queryForObject(String indexName, GetQuery query, Class<T> clazz) {
		return queryForObject(indexName, query, clazz, resultsMapper);
	}

	public <T> T queryForObject(String indexName, GetQuery query, Class<T> clazz, JestGetResultMapper mapper) {

		ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
		String index = indexName == null ? persistentEntity.getIndexName() : indexName;
		Get.Builder build = new Get.Builder(index, query.getId()).type(persistentEntity.getIndexType());
		DocumentResult result = execute(build.build());

		if (result.getResponseCode() == 404) {
			return null;
		}

		return mapper.mapResult(result, clazz);
	}

	@Override
	public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public <T> T queryForObject(StringQuery query, Class<T> clazz) {
		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
		return queryForPage(query, clazz, resultsMapper);
	}


	@Override
	public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
		// TODO
		throw new UnsupportedOperationException();
	}

	public <T> T query(SearchQuery query, JestResultsExtractor<T> resultsExtractor) {
		SearchResult response = doSearch(prepareSearch(query), query);
		return resultsExtractor.extract(response);
	}

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
	public <T> List<String> queryForIds(SearchQuery query) {
		SearchSourceBuilder search = prepareSearch(query).query(query.getQuery()).noFields();
		if (query.getFilter() != null) {
			search.postFilter(query.getFilter());
		}

		SearchResult result = executeSearch(query, search);
		return extractIds(result);
	}

	@Override
	public <T> Page<T> queryForPage(CriteriaQuery criteriaQuery, Class<T> clazz) {
        QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());

        SearchSourceBuilder searchRequestBuilder = prepareSearch(criteriaQuery, clazz);

        if (elasticsearchQuery != null) {
            searchRequestBuilder.query(elasticsearchQuery);
        } else {
            searchRequestBuilder.query(QueryBuilders.matchAllQuery());
        }

        if (criteriaQuery.getMinScore() > 0) {
            searchRequestBuilder.minScore(criteriaQuery.getMinScore());
        }

        if (elasticsearchFilter != null)
            searchRequestBuilder.postFilter(elasticsearchFilter);

		SearchResult response = executeSearch(criteriaQuery, searchRequestBuilder);
		return resultsMapper.mapResults(response, clazz, criteriaQuery.getPageable(), null);
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {
		return queryForPage(query, clazz, resultsMapper);
	}

	public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz, JestSearchResultMapper mapper) {
        SearchResult response = doSearch(prepareSearch(query, clazz), query);
        return mapper.mapResults(response, clazz, query.getPageable(), query);
	}

	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, JestSearchResultMapper mapper) {
		SearchResult response = executeSearch(null, prepareSearch(query, clazz).query(query.getSource()));
		return mapper.mapResults(response, clazz, query.getPageable(), null);
	}

	@Override
	public <T> CloseableIterator<T> stream(CriteriaQuery query, Class<T> clazz) {
		final long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		final String initScrollId = scan(query, scrollTimeInMillis, false, clazz);
		return doStream(initScrollId, scrollTimeInMillis, clazz, resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz) {
		return stream(query, clazz, resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
		return stream(query, clazz, resultsMapper);
	}

	private <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz, JestScrollResultMapper mapper) {
		final long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		final String initScrollId = scan(query, scrollTimeInMillis, false, clazz);
		return doStream(initScrollId, scrollTimeInMillis, clazz, mapper);
	}

	private <T> CloseableIterator<T> doStream(final String initScrollId, final long scrollTimeInMillis, final Class<T> clazz, final JestScrollResultMapper mapper) {

		return new CloseableIterator<T>() {

			/** As we couldn't retrieve single result with scroll, store current hits. */
			private volatile Iterator<T> currentHits;

			/** The scroll id. */
			private volatile String scrollId = initScrollId;

			/** If stream is finished (ie: cluster returns no results. */
			private volatile boolean finished;

			@Override
			public boolean hasNext() {
				// Test if stream is finished
				if (finished) {
					return false;
				}
				// Test if it remains hits
				if (currentHits == null || !currentHits.hasNext()) {

					SearchScroll scroll = new SearchScroll.Builder(scrollId, scrollTimeInMillis+"ms").build();
					ScrollSearchResult response = new ScrollSearchResult(execute(scroll));

					// Save hits and scroll id
					currentHits = mapper.mapResults(response, clazz).iterator();
					finished = !currentHits.hasNext();
					scrollId = response.getScrollId();
				}
				return currentHits.hasNext();
			}

			@Override
			public T next() {
				if (hasNext()) {
					return currentHits.next();
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove");
			}

			@Override
			public void close() {
				try {
					// Clear scroll on cluster only in case of error (cause elasticsearch auto clear scroll when it's done)
					if (!finished && scrollId != null && currentHits != null && currentHits.hasNext()) {
						execute(new ClearScroll.Builder(scrollId).build());
					}
				} finally {
					currentHits = null;
					scrollId = null;
				}
			}
		};
	}

	@Override
	public <T> long count(CriteriaQuery criteriaQuery, Class<T> clazz) {
        QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());

        if (elasticsearchFilter == null) {
            return doCount(prepareCount(criteriaQuery, clazz), elasticsearchQuery);
        } else {
            // filter could not be set into CountRequestBuilder, convert request into search request
            return doCount(prepareSearch(criteriaQuery, clazz), elasticsearchQuery, elasticsearchFilter);
        }
	}

	@Override
	public <T> long count(SearchQuery searchQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = searchQuery.getQuery();
		QueryBuilder elasticsearchFilter = searchQuery.getFilter();

		if (elasticsearchFilter == null) {
			return doCount(prepareCount(searchQuery, clazz), elasticsearchQuery);
		} else {
			// filter could not be set into CountRequestBuilder, convert request into search request
			return doCount(prepareSearch(searchQuery, clazz), elasticsearchQuery, elasticsearchFilter);
		}
	}

	@Override
	public <T> long count(CriteriaQuery query) {
		return count(query, null);
	}

	@Override
	public <T> long count(SearchQuery query) {
		return count(query, null);
	}

	private long doCount(Count.Builder countRequestBuilder, QueryBuilder elasticsearchQuery) {
		if (elasticsearchQuery != null) {
			countRequestBuilder.query(new SearchSourceBuilder().query(elasticsearchQuery).toString());
		}

		CountResult result = execute(countRequestBuilder.build());
		return result.getCount().longValue();
	}

	private long doCount(SearchSourceBuilder searchRequestBuilder, QueryBuilder elasticsearchQuery, QueryBuilder elasticsearchFilter) {
		if (elasticsearchQuery != null) {
			searchRequestBuilder.query(elasticsearchQuery);
		} else {
			searchRequestBuilder.query(QueryBuilders.matchAllQuery());
		}
		if (elasticsearchFilter != null) {
			searchRequestBuilder.postFilter(elasticsearchFilter);
		}

		CountResult result = execute(new Count.Builder().query(searchRequestBuilder.toString()).build());
		return result.getCount().longValue();
	}

	private <T> Count.Builder prepareCount(Query query, Class<T> clazz) {
		String indexName[] = !isEmpty(query.getIndices()) ? query.getIndices().toArray(new String[query.getIndices().size()]) : retrieveIndexNameFromPersistentEntity(clazz);
		String types[] = !isEmpty(query.getTypes()) ? query.getTypes().toArray(new String[query.getTypes().size()]) : retrieveTypeFromPersistentEntity(clazz);

		Assert.notNull(indexName, "No index defined for Query");

		Count.Builder countRequestBuilder = new Count.Builder().addIndex(Arrays.asList(indexName));
		if (types != null) {
			countRequestBuilder.addType(Arrays.asList(types));
		}
		return countRequestBuilder;
	}

	@Override
	public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper getResultMapper) {
		return multiGet(searchQuery, clazz, resultsMapper);
	}

	public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz, JestMultiGetResultMapper getResultMapper) {
		return getResultMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	@Override
	public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz) {
		return resultsMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	private <T> MultiDocumentResult getMultiResponse(Query searchQuery, Class<T> clazz) {

		String indexName = !isEmpty(searchQuery.getIndices()) ? searchQuery.getIndices().get(0) : getPersistentEntityFor(clazz).getIndexName();
		String type = !isEmpty(searchQuery.getTypes()) ? searchQuery.getTypes().get(0) : getPersistentEntityFor(clazz).getIndexType();

		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notEmpty(searchQuery.getIds(), "No Id define for Query");

		MultiGet.Builder.ById builder = new MultiGet.Builder.ById(indexName, type).addId(searchQuery.getIds());

		return new MultiDocumentResult(execute(builder.build()));
	}

	@Override
	public String index(IndexQuery query) {

		String documentId = execute(prepareIndex(query)).getId();

		// We should call this because we are not going through a mapper.
		if (query.getObject() != null) {
			setPersistentEntityId(query.getObject(), documentId);
		}
		return documentId;
	}

	@Override
	public UpdateResponse update(UpdateQuery updateQuery) {
		DocumentResult result = execute(prepareUpdate(updateQuery));
		JsonObject jsonResult = result.getJsonObject();
		int successful = jsonResult.get("_shards").getAsJsonObject().get("successful").getAsInt();
		return new UpdateResponse(
				jsonResult.get("_index").getAsString(),
				jsonResult.get("_type").getAsString(),
				jsonResult.get("_id").getAsString(),
				jsonResult.get("_version").getAsLong(),
				successful > 0
		);
	}

	@Override
	public void bulkIndex(List<IndexQuery> queries) {
		Bulk.Builder bulk = new Bulk.Builder();

		for (IndexQuery query : queries) {
			bulk.addAction(prepareIndex(query));
		}

		BulkResult bulkResult = new BulkResult(execute(bulk.build()));
		if (!bulkResult.isSucceeded()) {
			Map<String, String> failedDocuments = new HashMap<String, String>();
			for (BulkResult.BulkResultItem item : bulkResult.getFailedItems()) {
				failedDocuments.put(item.id, item.error);
			}
			throw new ElasticsearchException(
					"Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + "]", failedDocuments
			);
		}
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries) {

		Bulk.Builder bulk = new Bulk.Builder();

		for (UpdateQuery query : queries) {
			bulk.addAction(prepareUpdate(query));
		}

		BulkResult bulkResult = new BulkResult(execute(bulk.build()));
		if (!bulkResult.isSucceeded()) {
			Map<String, String> failedDocuments = new HashMap<String, String>();
			for (BulkResult.BulkResultItem item : bulkResult.getFailedItems()) {
				failedDocuments.put(item.id, item.error);
			}
			throw new ElasticsearchException(
					"Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + "]", failedDocuments
			);
		}
	}

	@Override
	public String delete(String indexName, String type, String id) {
		return execute(new Delete.Builder(id).index(indexName).type(type).build()).getId();
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
	public <T> String delete(Class<T> clazz, String id) {
		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
		return delete(persistentEntity.getIndexName(), persistentEntity.getIndexType(), id);
	}

	@Override
	public <T> void delete(DeleteQuery query, Class<T> clazz) {
		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);

		DeleteByQuery deleteQuery = new DeleteByQuery.Builder(query.getQuery().toString()).
				addIndex(persistentEntity.getIndexName()).
				addType(persistentEntity.getIndexType()).build();

		execute(deleteQuery);
	}

	@Override
	public void delete(DeleteQuery query) {
		Assert.notNull(query.getIndex(), "No index defined for Query");
		Assert.notNull(query.getType(), "No type define for Query");

		DeleteByQuery deleteQuery = new DeleteByQuery.Builder(query.getQuery().toString()).
				addIndex(query.getIndex()).
				addType(query.getType()).build();

		execute(deleteQuery);

	}

	@Override
	public <T> boolean deleteIndex(Class<T> clazz) {
		return deleteIndex(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public boolean deleteIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for delete operation");
		if (indexExists(indexName)) {
			return executeWithAcknowledge(new DeleteIndex.Builder(indexName).build());
		}
		return false;
	}

	@Override
	public <T> boolean indexExists(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public boolean indexExists(String indexName) {
		return executeWithAcknowledge(new IndicesExists.Builder(indexName).build());
	}

	@Override
	public boolean typeExists(String index, String type) {
		return executeWithAcknowledge(new TypeExist.Builder(index).addType(type).build());
	}

	@Override
	public void refresh(String indexName) {
		Assert.notNull(indexName, "No index defined for refresh()");
		Refresh refresh = new Builder().addIndex(indexName).build();
		execute(refresh);
	}

	@Override
	public <T> void refresh(Class<T> clazz) {
		refresh(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public String scan(CriteriaQuery criteriaQuery, long scrollTimeInMillis, boolean noFields) {
		Assert.notNull(criteriaQuery.getIndices(), "No index defined for Query");
		Assert.notNull(criteriaQuery.getTypes(), "No type define for Query");
		Assert.notNull(criteriaQuery.getPageable(), "Query.pageable is required for scan & scroll");

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(elasticsearchQuery != null ? elasticsearchQuery : QueryBuilders.matchAllQuery());

		if (elasticsearchFilter != null) {
			searchSourceBuilder.postFilter(elasticsearchFilter);
		}

		if (!isEmpty(criteriaQuery.getFields())) {
			searchSourceBuilder.fields(criteriaQuery.getFields());
		}

		if (noFields) {
			searchSourceBuilder.noFields();
		}

		Search.Builder search = new Search.Builder(searchSourceBuilder.toString()).
				addType(criteriaQuery.getTypes()).
				addIndex(criteriaQuery.getIndices()).
				setSearchType(SearchType.SCAN).
				setParameter(Parameters.SIZE, criteriaQuery.getPageable().getPageSize()).
				setParameter(Parameters.SCROLL, scrollTimeInMillis+"ms");

		return new ExtendedSearchResult(execute(search.build())).getScrollId();
	}

	@Override
	public <T> String scan(CriteriaQuery query, long scrollTimeInMillis, boolean noFields, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return scan(query, scrollTimeInMillis, noFields);
	}

	@Override
	public String scan(SearchQuery searchQuery, long scrollTimeInMillis, boolean noFields) {
		Assert.notNull(searchQuery.getIndices(), "No index defined for Query");
		Assert.notNull(searchQuery.getTypes(), "No type define for Query");
		Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scan & scroll");

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(searchQuery.getQuery());

		if (!isEmpty(searchQuery.getFields())) {
			searchSourceBuilder.fields(searchQuery.getFields());
		}

		if (noFields) {
			searchSourceBuilder.noFields();
		}

		if (searchQuery.getFilter() != null) {
			searchSourceBuilder.postFilter(searchQuery.getFilter());
		}

		Search.Builder search = new Search.Builder(searchSourceBuilder.toString()).
			addType(searchQuery.getTypes()).
			addIndex(searchQuery.getIndices()).
			setSearchType(SearchType.SCAN).
			setParameter(Parameters.SIZE, searchQuery.getPageable().getPageSize()).
			setParameter(Parameters.SCROLL, scrollTimeInMillis+"ms");

		return new ExtendedSearchResult(execute(search.build())).getScrollId();
	}

	@Override
	public <T> String scan(SearchQuery query, long scrollTimeInMillis, boolean noFields, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return scan(query, scrollTimeInMillis, noFields);
	}

	@Override
	public <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
		SearchScroll scroll = new SearchScroll.Builder(scrollId, scrollTimeInMillis+"ms").build();
		ScrollSearchResult response = getScrollSearchResult(execute(scroll));
		return resultsMapper.mapResults(response, clazz);
	}

	@Override
	public <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, SearchResultMapper mapper) {
		return scroll(scrollId, scrollTimeInMillis, resultsMapper);
	}

	@VisibleForTesting
	public ScrollSearchResult getScrollSearchResult(JestResult jestResult) {
		return new ScrollSearchResult(jestResult);
	}

	@Override
	public <T> void clearScroll(String scrollId) {
		execute(new ClearScroll.Builder(scrollId).build());
	}

	public <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, JestScrollResultMapper mapper) {
		SearchScroll scroll = new SearchScroll.Builder(scrollId, scrollTimeInMillis+"ms").build();
		ScrollSearchResult response = new ScrollSearchResult(execute(scroll));
		return mapper.mapResults(response, null);
	}

	@Override
	public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public Boolean addAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		AddAliasMapping.Builder aliasAction = new AddAliasMapping.Builder(query.getIndexName(), query.getAliasName());
		if (query.getFilterBuilder() != null) {
			//TODO(setFilter on alias)
//            aliasAction.setFilter(query.getFilterBuilder());
		} else if (query.getFilter() != null) {
			aliasAction.setFilter(query.getFilter());
		} else if (isNotBlank(query.getRouting())) {
			aliasAction.addRouting(query.getRouting());
		} else if (isNotBlank(query.getSearchRouting())) {
			aliasAction.addSearchRouting(query.getSearchRouting());
		} else if (isNotBlank(query.getIndexRouting())) {
			aliasAction.addIndexRouting(query.getIndexRouting());
		}
		return executeWithAcknowledge(new ModifyAliases.Builder(aliasAction.build()).build());
	}

	@Override
	public Boolean removeAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		RemoveAliasMapping removeAlias = new RemoveAliasMapping.Builder(query.getIndexName(), query.getAliasName()).build();
		return executeWithAcknowledge(new ModifyAliases.Builder(removeAlias).build());
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {

		GetAliases getAliases = new GetAliases.Builder().addIndex(indexName).build();
		JestResult result = execute(getAliases);
		if (!result.isSucceeded()) {
			return Collections.emptyList();
		}

		Set<Map.Entry<String, JsonElement>> entries = result.getJsonObject().getAsJsonObject(indexName).getAsJsonObject("aliases").entrySet();

		List<AliasMetaData> aliases = new ArrayList<AliasMetaData>(entries.size());
		for(Map.Entry<String, JsonElement> entry : entries) {
			aliases.add(AliasMetaData.builder(entry.getKey()).build());
		}
		return aliases;

	}

	@VisibleForTesting
	public <T extends JestResult> T execute(Action<T> action) {
		try {
			return client.execute(action);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to execute action", e);
		}
	}

	private boolean executeWithAcknowledge(Action<?> action) {
		try {
			JestResult jestResult = client.execute(action);
			return jestResult.isSucceeded();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to execute action", e);
		}
	}

	private <T> SearchSourceBuilder prepareSearch(Query query, Class<T> clazz) {
		if (query.getIndices().isEmpty()) {
			query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
		}
		if (query.getTypes().isEmpty()) {
			query.addTypes(retrieveTypeFromPersistentEntity(clazz));
		}
		return prepareSearch(query);
	}

	private SearchSourceBuilder prepareSearch(Query query) {
		Assert.notNull(query.getIndices(), "No index defined for Query");
		Assert.notNull(query.getTypes(), "No type defined for Query");

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		int startRecord = 0;

		if (query.getPageable() != null) {
			startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
			searchSourceBuilder.size(query.getPageable().getPageSize());
		}
		searchSourceBuilder.from(startRecord);

		if (!query.getFields().isEmpty()) {
			searchSourceBuilder.fields(query.getFields());
		}

		if (query.getSort() != null) {
			for (Sort.Order order : query.getSort()) {
				searchSourceBuilder.sort(order.getProperty(), order.getDirection() == Sort.Direction.DESC ? SortOrder.DESC : SortOrder.ASC);
			}
		}

		if (query.getMinScore() > 0) {
			searchSourceBuilder.minScore(query.getMinScore());
		}
		return searchSourceBuilder;
	}


	private SearchResult doSearch(SearchSourceBuilder searchSourceBuilder, SearchQuery searchQuery) {
		if (searchQuery.getFilter() != null) {
			searchSourceBuilder.postFilter(searchQuery.getFilter());
		}

		if (!isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder sort : searchQuery.getElasticsearchSorts()) {
				searchSourceBuilder.sort(sort);
			}
		}

		if (searchQuery.getHighlightFields() != null) {
			HighlightBuilder highlighter = searchSourceBuilder.highlighter();
			for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
				highlighter.field(highlightField);
			}
		}

		if (!isEmpty(searchQuery.getAggregations())) {
			for (AbstractAggregationBuilder aggregationBuilder : searchQuery.getAggregations()) {
				searchSourceBuilder.aggregation(aggregationBuilder);
			}
		}
		if (!isEmpty(searchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : searchQuery.getIndicesBoost()) {
				searchSourceBuilder.indexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!searchQuery.getScriptFields().isEmpty()) {
			searchSourceBuilder.field("_source");
			for (ScriptField scriptedField : searchQuery.getScriptFields()) {
				searchSourceBuilder.scriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		return executeSearch(searchQuery, searchSourceBuilder.query(searchQuery.getQuery()));
	}

	private SearchResult executeSearch(Query query, SearchSourceBuilder request) {

		Search.Builder search = new Search.Builder(request.toString());
		if (query != null) {
			search.
					addType(query.getTypes()).
					addIndex(query.getIndices()).
					setSearchType(SearchType.valueOf(query.getSearchType().name()));
		}

		return new ExtendedSearchResult(execute(search.build()));
	}

	private Index prepareIndex(IndexQuery query) {
		try {
			String indexName = isBlank(query.getIndexName()) ? retrieveIndexNameFromPersistentEntity(query.getObject()
					.getClass())[0] : query.getIndexName();
			String type = isBlank(query.getType()) ? retrieveTypeFromPersistentEntity(query.getObject().getClass())[0]
					: query.getType();

			Index.Builder indexBuilder;

			if (query.getObject() != null) {
				String entityId = null;
				if (isDocument(query.getObject().getClass())) {
					entityId = getPersistentEntityId(query.getObject());
				}

				indexBuilder = new Index.Builder(resultsMapper.getEntityMapper().mapToString(query.getObject()));

				// If we have a query id and a document id, do not ask ES to generate one.
				if (query.getId() != null && entityId != null) {
					indexBuilder.index(indexName).type(type).id(query.getId());
				} else {
					indexBuilder.index(indexName).type(type);
				}
			} else if (query.getSource() != null) {
				indexBuilder = new Index.Builder(query.getSource()).index(indexName).type(type).id(query.getId());
			} else {
				throw new ElasticsearchException("object or source is null, failed to index the document [id: " + query.getId() + "]");
			}
			if (query.getVersion() != null) {
				indexBuilder.setParameter(Parameters.VERSION, query.getVersion());
				indexBuilder.setParameter(Parameters.VERSION_TYPE, EXTERNAL);
			}

			if (query.getParentId() != null) {
				indexBuilder.setParameter(Parameters.PARENT, query.getParentId());
			}

			return indexBuilder.build();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
		}
	}

	private Update prepareUpdate(UpdateQuery query) {
		String indexName = isNotBlank(query.getIndexName()) ? query.getIndexName() : getPersistentEntityFor(query.getClazz()).getIndexName();
		String type = isNotBlank(query.getType()) ? query.getType() : getPersistentEntityFor(query.getClazz()).getIndexType();
		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notNull(query.getId(), "No Id define for Query");
		Assert.notNull(query.getUpdateRequest(), "No IndexRequest define for Query");

		Map<String, Object> payLoadMap = new HashMap<String, Object>();

		if (query.getUpdateRequest().script() == null) {

			Map<String, Object> sourceAsMap = null;

			if (query.getUpdateRequest().doc().source() != null) {
				sourceAsMap = query.getUpdateRequest().doc().sourceAsMap();
			}

			// doc
			if (query.DoUpsert()) {
				payLoadMap.put("doc_as_upsert", Boolean.TRUE);
				payLoadMap.put("doc", sourceAsMap);
			} else {
				payLoadMap.put("doc", sourceAsMap);
			}

		} else {
			Script script = query.getUpdateRequest().script();
			payLoadMap.put("script", script.getScript());
			payLoadMap.put("lang", script.getLang());
			payLoadMap.put("params", script.getParams());
		}

		try {
			String payload = resultsMapper.getEntityMapper().mapToString(payLoadMap);
			Update.Builder updateBuilder = new Update.Builder(payload).index(indexName).type(type).id(query.getId());
			return updateBuilder.build();

		} catch (IOException e) {
			throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
		}
	}

	private <T> Map getDefaultSettings(ElasticsearchPersistentEntity<T> persistentEntity) {
		return new MapBuilder<String, String>().put("index.number_of_shards", String.valueOf(persistentEntity.getShards()))
				.put("index.number_of_replicas", String.valueOf(persistentEntity.getReplicas()))
				.put("index.refresh_interval", persistentEntity.getRefreshInterval())
				.put("index.store.type", persistentEntity.getIndexStoreType()).map();
	}

	private <T> boolean createIndexIfNotCreated(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName()) || createIndexWithSettings(clazz);
	}

	private <T> boolean createIndexWithSettings(Class<T> clazz) {
		if (clazz.isAnnotationPresent(Setting.class)) {
			String settingPath = clazz.getAnnotation(Setting.class).settingPath();
			if (isNotBlank(settingPath)) {
				String settings = readFileFromClasspath(settingPath);
				if (isNotBlank(settings)) {
					return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
				}
			} else {
				logger.info("settingPath in @Setting has to be defined. Using default instead.");
			}
		}
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), getDefaultSettings(getPersistentEntityFor(clazz)));
	}

	private boolean isDocument(Class clazz) {
		return clazz.isAnnotationPresent(Document.class);
	}

	public ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz) {
		Assert.isTrue(clazz.isAnnotationPresent(Document.class), "Unable to identify index name. " + clazz.getSimpleName()
				+ " is not a Document. Make sure the document class is annotated with @Document(indexName=\"foo\")");
		return elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
	}

	private String getPersistentEntityId(Object entity) {
		PersistentProperty idProperty = getPersistentEntityFor(entity.getClass()).getIdProperty();
		if (idProperty != null) {
			Method getter = idProperty.getGetter();
			if (getter != null) {
				try {
					Object id = getter.invoke(entity);
					if (id != null) {
						return String.valueOf(id);
					}
				} catch (Throwable t) {
					logger.error(t.getMessage(), t);
				}
			}
		}
		return null;
	}

	private void setPersistentEntityIndexAndType(Query query, Class clazz) {
		if (query.getIndices().isEmpty()) {
			query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
		}
		if (query.getTypes().isEmpty()) {
			query.addTypes(retrieveTypeFromPersistentEntity(clazz));
		}
	}

	private void setPersistentEntityId(Object entity, String id) {
		PersistentProperty idProperty = getPersistentEntityFor(entity.getClass()).getIdProperty();
		// Only deal with String because ES generated Ids are strings !
		if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
			Method setter = idProperty.getSetter();
			if (setter != null) {
				try {
					setter.invoke(entity, id);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	private String[] retrieveIndexNameFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[]{getPersistentEntityFor(clazz).getIndexName()};
		}
		return null;
	}

	private String[] retrieveTypeFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[]{getPersistentEntityFor(clazz).getIndexType()};
		}
		return null;
	}

	private List<String> extractIds(SearchResult result) {
		List<String> ids = new ArrayList<String>();
		for (SearchResult.Hit<JsonObject, Void> hit : result.getHits(JsonObject.class)) {
			if (hit != null) {
				ids.add(hit.source.get(JestResult.ES_METADATA_ID).toString());
			}
		}
		return ids;
	}

	private static String readFileFromClasspath(String url) {
		StringBuilder stringBuilder = new StringBuilder();

		BufferedReader bufferedReader = null;

		try {
			ClassPathResource classPathResource = new ClassPathResource(url);
			InputStreamReader inputStreamReader = new InputStreamReader(classPathResource.getInputStream());
			bufferedReader = new BufferedReader(inputStreamReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		} catch (Exception e) {
			logger.debug(String.format("Failed to load file from url: %s: %s", url, e.getMessage()));
			return null;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					logger.debug(String.format("Unable to close buffered reader.. %s", e.getMessage()));
				}
			}
		}

		return stringBuilder.toString();
	}
}
