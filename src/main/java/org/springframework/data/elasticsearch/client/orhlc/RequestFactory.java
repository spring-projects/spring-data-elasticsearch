/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.orhlc;

import static org.opensearch.common.unit.TimeValue.*;
import static org.opensearch.index.query.QueryBuilders.*;
import static org.opensearch.index.reindex.RemoteInfo.*;
import static org.opensearch.script.Script.*;
import static org.springframework.util.CollectionUtils.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.admin.indices.alias.Alias;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.MultiGetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchType;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.Requests;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.GetIndexTemplatesRequest;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.client.indices.IndexTemplatesExistRequest;
import org.opensearch.client.indices.PutIndexTemplateRequest;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.geo.GeoDistance;
import org.opensearch.common.unit.DistanceUnit;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.VersionType;
import org.opensearch.index.query.MoreLikeThisQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.opensearch.index.reindex.RemoteInfo;
import org.opensearch.index.reindex.UpdateByQueryRequest;
import org.opensearch.script.Script;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.FetchSourceContext;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.rescore.QueryRescoreMode;
import org.opensearch.search.rescore.QueryRescorerBuilder;
import org.opensearch.search.slice.SliceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.GeoDistanceSortBuilder;
import org.opensearch.search.sort.ScoreSortBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortMode;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.search.suggest.SuggestBuilder;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.common.xcontent.XContentBuilder;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.ScriptType;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.query.RescorerQuery.ScoreMode;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest.Dest;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest.Slice;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest.Source;
import org.springframework.data.elasticsearch.core.reindex.Remote;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory class to create Opensearch request instances from Spring Data Opensearch query objects.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author Roman Puchkovskiy
 * @author Subhobrata Dey
 * @author Farid Faoudi
 * @author Peer Mueller
 * @author Sijia Liu
 * @author Peter Nowak
 * @author Andriy Redko
 * @since 5.0
 */
class RequestFactory {

	// the default max result window size of Elasticsearch
	static final Integer INDEX_MAX_RESULT_WINDOW = 10_000;

	private final ElasticsearchConverter elasticsearchConverter;

	public RequestFactory(ElasticsearchConverter elasticsearchConverter) {
		this.elasticsearchConverter = elasticsearchConverter;
	}

	// region alias
	public GetAliasesRequest getAliasesRequest(IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		return new GetAliasesRequest().indices(indexNames);
	}

	public GetAliasesRequest getAliasesRequest(@Nullable String[] aliasNames, @Nullable String[] indexNames) {
		GetAliasesRequest getAliasesRequest = new GetAliasesRequest(aliasNames);

		if (indexNames != null) {
			getAliasesRequest.indices(indexNames);
		}
		return getAliasesRequest;
	}

	public IndicesAliasesRequest indicesAliasesRequest(AliasActions aliasActions) {

		IndicesAliasesRequest request = new IndicesAliasesRequest();
		aliasActions.getActions().forEach(aliasAction -> {

			IndicesAliasesRequest.AliasActions aliasActionsES = null;

			if (aliasAction instanceof AliasAction.Add) {
				AliasAction.Add add = (AliasAction.Add) aliasAction;
				IndicesAliasesRequest.AliasActions addES = IndicesAliasesRequest.AliasActions.add();

				AliasActionParameters parameters = add.getParameters();
				addES.indices(parameters.getIndices());
				addES.aliases(parameters.getAliases());
				addES.routing(parameters.getRouting());
				addES.indexRouting(parameters.getIndexRouting());
				addES.searchRouting(parameters.getSearchRouting());
				addES.isHidden(parameters.getHidden());
				addES.writeIndex(parameters.getWriteIndex());

				Query filterQuery = parameters.getFilterQuery();

				if (filterQuery != null) {
					elasticsearchConverter.updateQuery(filterQuery, parameters.getFilterQueryClass());
					QueryBuilder queryBuilder = getFilter(filterQuery);

					if (queryBuilder == null) {
						queryBuilder = getQuery(filterQuery);
					}

					addES.filter(queryBuilder);
				}

				aliasActionsES = addES;
			} else if (aliasAction instanceof AliasAction.Remove) {
				AliasAction.Remove remove = (AliasAction.Remove) aliasAction;
				IndicesAliasesRequest.AliasActions removeES = IndicesAliasesRequest.AliasActions.remove();

				AliasActionParameters parameters = remove.getParameters();
				removeES.indices(parameters.getIndices());
				removeES.aliases(parameters.getAliases());

				aliasActionsES = removeES;
			} else if (aliasAction instanceof AliasAction.RemoveIndex) {
				AliasAction.RemoveIndex removeIndex = (AliasAction.RemoveIndex) aliasAction;
				IndicesAliasesRequest.AliasActions removeIndexES = IndicesAliasesRequest.AliasActions.removeIndex();

				AliasActionParameters parameters = removeIndex.getParameters();
				removeIndexES.indices(parameters.getIndices()[0]);

				aliasActionsES = removeIndexES;
			}

			if (aliasActionsES != null) {
				request.addAliasAction(aliasActionsES);
			}
		});

		return request;
	}

	// endregion

	// region bulk
	public BulkRequest bulkRequest(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		BulkRequest bulkRequest = new BulkRequest();

		if (bulkOptions.getTimeout() != null) {
			bulkRequest.timeout(TimeValue.timeValueMillis(bulkOptions.getTimeout().toMillis()));
		}

		if (bulkOptions.getRefreshPolicy() != null) {
			bulkRequest.setRefreshPolicy(toOpensearchRefreshPolicy(bulkOptions.getRefreshPolicy()));
		}

		if (bulkOptions.getWaitForActiveShards() != null) {
			bulkRequest.waitForActiveShards(ActiveShardCount.from(bulkOptions.getWaitForActiveShards().value()));
		}

		if (bulkOptions.getPipeline() != null) {
			bulkRequest.pipeline(bulkOptions.getPipeline());
		}

		if (bulkOptions.getRoutingId() != null) {
			bulkRequest.routing(bulkOptions.getRoutingId());
		}

		queries.forEach(query -> {

			if (query instanceof IndexQuery) {
				bulkRequest.add(indexRequest((IndexQuery) query, index));
			} else if (query instanceof UpdateQuery) {
				bulkRequest.add(updateRequest((UpdateQuery) query, index));
			}
		});
		return bulkRequest;
	}

	// endregion

	// region index management

	public CreateIndexRequest createIndexRequest(IndexCoordinates index, Map<String, Object> settings,
			@Nullable Document mapping) {

		Assert.notNull(index, "index must not be null");
		Assert.notNull(settings, "settings must not be null");

		CreateIndexRequest request = new CreateIndexRequest(index.getIndexName());

		if (!settings.isEmpty()) {
			request.settings(settings);
		}

		if (mapping != null && !mapping.isEmpty()) {
			request.mapping(mapping);
		}

		return request;
	}

	public GetIndexRequest getIndexRequest(IndexCoordinates index) {
		return new GetIndexRequest(index.getIndexNames()).humanReadable(false);
	}

	public IndicesExistsRequest indicesExistsRequest(IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		return new IndicesExistsRequest(indexNames);
	}

	public DeleteIndexRequest deleteIndexRequest(IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		return new DeleteIndexRequest(indexNames);
	}

	public RefreshRequest refreshRequest(IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		return new RefreshRequest(indexNames);
	}

	public GetSettingsRequest getSettingsRequest(IndexCoordinates index, boolean includeDefaults) {

		String[] indexNames = index.getIndexNames();
		return new GetSettingsRequest().indices(indexNames).includeDefaults(includeDefaults);
	}

	public PutMappingRequest putMappingRequest(IndexCoordinates index, Document mapping) {

		PutMappingRequest request = new PutMappingRequest(index.getIndexNames());
		request.source(mapping);
		return request;
	}

	public GetSettingsRequest getSettingsRequest(String indexName, boolean includeDefaults) {
		return new GetSettingsRequest().indices(indexName).includeDefaults(includeDefaults);
	}

	public GetMappingsRequest getMappingsRequest(IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		return new GetMappingsRequest().indices(indexNames);
	}

	public PutIndexTemplateRequest putIndexTemplateRequest(PutTemplateRequest putTemplateRequest) {

		PutIndexTemplateRequest request = new PutIndexTemplateRequest(putTemplateRequest.getName())
				.patterns(Arrays.asList(putTemplateRequest.getIndexPatterns()));

		if (putTemplateRequest.getSettings() != null) {
			request.settings(putTemplateRequest.getSettings());
		}

		if (putTemplateRequest.getMappings() != null) {
			request.mapping(putTemplateRequest.getMappings());
		}

		request.order(putTemplateRequest.getOrder()).version(putTemplateRequest.getVersion());

		AliasActions aliasActions = putTemplateRequest.getAliasActions();

		if (aliasActions != null) {
			aliasActions.getActions().forEach(aliasAction -> {
				AliasActionParameters parameters = aliasAction.getParameters();
				String[] parametersAliases = parameters.getAliases();

				if (parametersAliases != null) {
					for (String aliasName : parametersAliases) {
						Alias alias = new Alias(aliasName);

						if (parameters.getRouting() != null) {
							alias.routing(parameters.getRouting());
						}

						if (parameters.getIndexRouting() != null) {
							alias.indexRouting(parameters.getIndexRouting());
						}

						if (parameters.getSearchRouting() != null) {
							alias.searchRouting(parameters.getSearchRouting());
						}

						if (parameters.getHidden() != null) {
							alias.isHidden(parameters.getHidden());
						}

						if (parameters.getWriteIndex() != null) {
							alias.writeIndex(parameters.getWriteIndex());
						}

						Query filterQuery = parameters.getFilterQuery();

						if (filterQuery != null) {
							elasticsearchConverter.updateQuery(filterQuery, parameters.getFilterQueryClass());
							QueryBuilder queryBuilder = getFilter(filterQuery);

							if (queryBuilder == null) {
								queryBuilder = getQuery(filterQuery);
							}

							alias.filter(queryBuilder);
						}

						request.alias(alias);
					}
				}
			});
		}

		return request;
	}

	public GetIndexTemplatesRequest getIndexTemplatesRequest(GetTemplateRequest getTemplateRequest) {
		return new GetIndexTemplatesRequest(getTemplateRequest.getTemplateName());
	}

	public IndexTemplatesExistRequest indexTemplatesExistsRequest(ExistsTemplateRequest existsTemplateRequest) {
		return new IndexTemplatesExistRequest(existsTemplateRequest.getTemplateName());
	}

	public DeleteIndexTemplateRequest deleteIndexTemplateRequest(DeleteTemplateRequest deleteTemplateRequest) {
		return new DeleteIndexTemplateRequest(deleteTemplateRequest.getTemplateName());
	}

	public org.opensearch.index.reindex.ReindexRequest reindexRequest(ReindexRequest reindexRequest) {
		final org.opensearch.index.reindex.ReindexRequest request = new org.opensearch.index.reindex.ReindexRequest();

		if (reindexRequest.getConflicts() != null) {
			request.setConflicts(reindexRequest.getConflicts().getEsName());
		}

		if (reindexRequest.getMaxDocs() != null) {
			request.setMaxDocs(Math.toIntExact(reindexRequest.getMaxDocs()));
		}
		// region source build
		final Source source = reindexRequest.getSource();
		request.setSourceIndices(source.getIndexes().getIndexNames());

		// source query will build from RemoteInfo if remote exist
		if (source.getQuery() != null && source.getRemote() == null) {
			request.setSourceQuery(getQuery(source.getQuery()));
		}

		if (source.getSize() != null) {
			request.setSourceBatchSize(source.getSize());
		}

		if (source.getRemote() != null) {
			Remote remote = source.getRemote();
			QueryBuilder queryBuilder = source.getQuery() == null ? QueryBuilders.matchAllQuery()
					: getQuery(source.getQuery());
			BytesReference query;
			try {
				XContentBuilder builder = XContentBuilder.builder(QUERY_CONTENT_TYPE).prettyPrint();
				query = BytesReference.bytes(queryBuilder.toXContent(builder, ToXContent.EMPTY_PARAMS));
			} catch (IOException e) {
				throw new IllegalArgumentException("Error parsing the source query content", e);
			}
			request.setRemoteInfo(new RemoteInfo( //
					remote.getScheme(), //
					remote.getHost(), //
					remote.getPort(), //
					remote.getPathPrefix(), //
					query, //
					remote.getUsername(), //
					remote.getPassword(), //
					Collections.emptyMap(), //
					remote.getSocketTimeout() == null ? DEFAULT_SOCKET_TIMEOUT
							: timeValueSeconds(remote.getSocketTimeout().getSeconds()), //
					remote.getConnectTimeout() == null ? DEFAULT_CONNECT_TIMEOUT
							: timeValueSeconds(remote.getConnectTimeout().getSeconds()))); //
		}

		final Slice slice = source.getSlice();
		if (slice != null) {
			request.getSearchRequest().source().slice(new SliceBuilder(slice.getId(), slice.getMax()));
		}

		final SourceFilter sourceFilter = source.getSourceFilter();
		if (sourceFilter != null) {
			request.getSearchRequest().source().fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}
		// endregion

		// region dest build
		final Dest dest = reindexRequest.getDest();
		request.setDestIndex(dest.getIndex().getIndexName()).setDestRouting(dest.getRouting())
				.setDestPipeline(dest.getPipeline());

		final org.springframework.data.elasticsearch.annotations.Document.VersionType versionType = dest.getVersionType();
		if (versionType != null) {
			request.setDestVersionType(VersionType.fromString(versionType.getEsName()));
		}

		final IndexQuery.OpType opType = dest.getOpType();
		if (opType != null) {
			request.setDestOpType(opType.getEsName());
		}
		// endregion

		// region script build
		final ReindexRequest.Script script = reindexRequest.getScript();
		if (script != null) {
			request.setScript(new Script(DEFAULT_SCRIPT_TYPE, script.getLang(), script.getSource(), Collections.emptyMap()));
		}
		// endregion

		// region query parameters build
		final Duration timeout = reindexRequest.getTimeout();
		if (timeout != null) {
			request.setTimeout(timeValueSeconds(timeout.getSeconds()));
		}

		if (reindexRequest.getRefresh() != null) {
			request.setRefresh(reindexRequest.getRefresh());
		}

		if (reindexRequest.getRequireAlias() != null) {
			request.setRequireAlias(reindexRequest.getRequireAlias());
		}

		if (reindexRequest.getRequestsPerSecond() != null) {
			request.setRequestsPerSecond(reindexRequest.getRequestsPerSecond());
		}

		final Duration scroll = reindexRequest.getScroll();
		if (scroll != null) {
			request.setScroll(timeValueSeconds(scroll.getSeconds()));
		}

		if (reindexRequest.getWaitForActiveShards() != null) {
			request.setWaitForActiveShards(ActiveShardCount.parseString(reindexRequest.getWaitForActiveShards()));
		}

		if (reindexRequest.getSlices() != null) {
			request.setSlices(Math.toIntExact(reindexRequest.getSlices()));
		}
		// endregion
		return request;
	}

	// endregion

	// region delete
	public DeleteByQueryRequest deleteByQueryRequest(Query query, Class<?> clazz, IndexCoordinates index) {
		SearchRequest searchRequest = searchRequest(query, clazz, index);
		DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index.getIndexNames()) //
				.setQuery(searchRequest.source().query()) //
				.setAbortOnVersionConflict(false) //
				.setRefresh(true);

		if (query.isLimiting()) {
			// noinspection ConstantConditions
			deleteByQueryRequest.setBatchSize(query.getMaxResults());
		}

		if (query.hasScrollTime()) {
			// noinspection ConstantConditions
			deleteByQueryRequest.setScroll(TimeValue.timeValueMillis(query.getScrollTime().toMillis()));
		}

		if (query.getRoute() != null) {
			deleteByQueryRequest.setRouting(query.getRoute());
		}

		return deleteByQueryRequest;
	}

	public DeleteRequest deleteRequest(String id, @Nullable String routing, IndexCoordinates index) {
		String indexName = index.getIndexName();
		DeleteRequest deleteRequest = new DeleteRequest(indexName, id);

		if (routing != null) {
			deleteRequest.routing(routing);
		}

		return deleteRequest;
	}

	// endregion

	// region get
	public GetRequest getRequest(String id, @Nullable String routing, IndexCoordinates index) {
		GetRequest getRequest = new GetRequest(index.getIndexName(), id);
		getRequest.routing(routing);
		return getRequest;
	}

	public MultiGetRequest multiGetRequest(Query query, Class<?> clazz, IndexCoordinates index) {

		MultiGetRequest multiGetRequest = new MultiGetRequest();
		getMultiRequestItems(query, clazz, index).forEach(multiGetRequest::add);
		return multiGetRequest;
	}

	private List<MultiGetRequest.Item> getMultiRequestItems(Query searchQuery, Class<?> clazz, IndexCoordinates index) {

		elasticsearchConverter.updateQuery(searchQuery, clazz);
		List<MultiGetRequest.Item> items = new ArrayList<>();

		FetchSourceContext fetchSourceContext = getFetchSourceContext(searchQuery);

		if (!isEmpty(searchQuery.getIdsWithRouting())) {
			String indexName = index.getIndexName();

			for (Query.IdWithRouting idWithRouting : searchQuery.getIdsWithRouting()) {
				MultiGetRequest.Item item = new MultiGetRequest.Item(indexName, idWithRouting.id());
				if (idWithRouting.routing() != null) {
					item = item.routing(idWithRouting.routing());
				}

				// note: multiGet does not have fields, need to set sourceContext to filter
				if (fetchSourceContext != null) {
					item.fetchSourceContext(fetchSourceContext);
				}

				items.add(item);
			}
		}
		return items;
	}

	// endregion

	// region indexing
	public IndexRequest indexRequest(IndexQuery query, IndexCoordinates index) {

		String indexName = query.getIndexName() != null ? query.getIndexName() : index.getIndexName();
		IndexRequest indexRequest;

		Object queryObject = query.getObject();

		if (queryObject != null) {
			String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(queryObject) : query.getId();
			// If we have a query id and a document id, do not ask ES to generate one.
			if (id != null) {
				indexRequest = new IndexRequest(indexName).id(id);
			} else {
				indexRequest = new IndexRequest(indexName);
			}
			indexRequest.source(elasticsearchConverter.mapObject(queryObject).toJson(), Requests.INDEX_CONTENT_TYPE);
		} else if (query.getSource() != null) {
			indexRequest = new IndexRequest(indexName).id(query.getId()).source(query.getSource(),
					Requests.INDEX_CONTENT_TYPE);
		} else {
			throw new InvalidDataAccessApiUsageException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}

		if (query.getVersion() != null) {
			indexRequest.version(query.getVersion());
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(
					queryObject != null ? queryObject.getClass() : null);
			indexRequest.versionType(versionType);
		}

		if (query.getSeqNo() != null) {
			indexRequest.setIfSeqNo(query.getSeqNo());
		}

		if (query.getPrimaryTerm() != null) {
			indexRequest.setIfPrimaryTerm(query.getPrimaryTerm());
		}

		if (query.getRouting() != null) {
			indexRequest.routing(query.getRouting());
		}

		if (query.getOpType() != null) {
			switch (query.getOpType()) {
				case INDEX:
					indexRequest.opType(DocWriteRequest.OpType.INDEX);
					break;
				case CREATE:
					indexRequest.opType(DocWriteRequest.OpType.CREATE);
			}
		}

		return indexRequest;
	}

	// endregion

	// region search
	@Nullable
	public HighlightBuilder highlightBuilder(Query query) {
		HighlightBuilder highlightBuilder = query.getHighlightQuery()
				.map(highlightQuery -> new HighlightQueryBuilder(elasticsearchConverter.getMappingContext())
						.getHighlightBuilder(highlightQuery.getHighlight(), highlightQuery.getType()))
				.orElse(null);

		if (highlightBuilder == null) {

			if (query instanceof NativeSearchQuery) {
				NativeSearchQuery searchQuery = (NativeSearchQuery) query;

				if ((searchQuery.getHighlightFields() != null && searchQuery.getHighlightFields().length > 0)
						|| searchQuery.getHighlightBuilder() != null) {
					highlightBuilder = searchQuery.getHighlightBuilder();

					if (highlightBuilder == null) {
						highlightBuilder = new HighlightBuilder();
					}

					if (searchQuery.getHighlightFields() != null) {
						for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
							highlightBuilder.field(highlightField);
						}
					}
				}
			}
		}
		return highlightBuilder;
	}

	public MoreLikeThisQueryBuilder moreLikeThisQueryBuilder(MoreLikeThisQuery query, IndexCoordinates index) {

		String indexName = index.getIndexName();
		MoreLikeThisQueryBuilder.Item item = new MoreLikeThisQueryBuilder.Item(indexName, query.getId());

		String[] fields = query.getFields().toArray(new String[] {});

		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = QueryBuilders.moreLikeThisQuery(fields, null,
				new MoreLikeThisQueryBuilder.Item[] { item });

		if (query.getMinTermFreq() != null) {
			moreLikeThisQueryBuilder.minTermFreq(query.getMinTermFreq());
		}

		if (query.getMaxQueryTerms() != null) {
			moreLikeThisQueryBuilder.maxQueryTerms(query.getMaxQueryTerms());
		}

		if (!isEmpty(query.getStopWords())) {
			moreLikeThisQueryBuilder.stopWords(query.getStopWords());
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

		return moreLikeThisQueryBuilder;
	}

	public SearchRequest searchRequest(SuggestBuilder suggestion, IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		SearchRequest searchRequest = new SearchRequest(indexNames);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.suggest(suggestion);
		searchRequest.source(sourceBuilder);
		return searchRequest;
	}

	public SearchRequest searchRequest(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {

		elasticsearchConverter.updateQuery(query, clazz);
		SearchRequest searchRequest = prepareSearchRequest(query, clazz, index);
		QueryBuilder opensearchQuery = getQuery(query);
		QueryBuilder opensearchFilter = getFilter(query);

		searchRequest.source().query(opensearchQuery);

		if (opensearchFilter != null) {
			searchRequest.source().postFilter(opensearchFilter);
		}

		return searchRequest;

	}

	private SearchRequest prepareSearchRequest(Query query, @Nullable Class<?> clazz, IndexCoordinates indexCoordinates) {

		String[] indexNames = indexCoordinates.getIndexNames();
		Assert.notNull(indexNames, "No index defined for Query");
		Assert.notEmpty(indexNames, "No index defined for Query");

		SearchRequest request = new SearchRequest(indexNames);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.version(true);
		sourceBuilder.trackScores(query.getTrackScores());
		if (hasSeqNoPrimaryTermProperty(clazz)) {
			sourceBuilder.seqNoAndPrimaryTerm(true);
		}

		if (query.getPageable().isPaged()) {
			sourceBuilder.from((int) query.getPageable().getOffset());
			sourceBuilder.size(query.getPageable().getPageSize());
		} else {
			sourceBuilder.from(0);
			sourceBuilder.size(INDEX_MAX_RESULT_WINDOW);
		}

		if (query.getSourceFilter() != null) {
			sourceBuilder.fetchSource(getFetchSourceContext(query));
			SourceFilter sourceFilter = query.getSourceFilter();
			sourceBuilder.fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (!query.getFields().isEmpty()) {
			query.getFields().forEach(sourceBuilder::fetchField);
		}

		if (!isEmpty(query.getStoredFields())) {
			sourceBuilder.storedFields(query.getStoredFields());
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(toOpensearchIndicesOptions(query.getIndicesOptions()));
		}

		if (query.isLimiting()) {
			// noinspection ConstantConditions
			sourceBuilder.size(query.getMaxResults());
		}

		if (query.getMinScore() > 0) {
			sourceBuilder.minScore(query.getMinScore());
		}

		if (query.getPreference() != null) {
			request.preference(query.getPreference());
		}

		request.searchType(SearchType.fromString(query.getSearchType().name().toLowerCase()));

		prepareSort(query, sourceBuilder, getPersistentEntity(clazz));

		HighlightBuilder highlightBuilder = highlightBuilder(query);

		if (highlightBuilder != null) {
			sourceBuilder.highlighter(highlightBuilder);
		}

		if (query instanceof NativeSearchQuery) {
			prepareNativeSearch((NativeSearchQuery) query, sourceBuilder);
		}

		if (query.getTrackTotalHits() != null) {
			sourceBuilder.trackTotalHits(query.getTrackTotalHits());
		} else if (query.getTrackTotalHitsUpTo() != null) {
			sourceBuilder.trackTotalHitsUpTo(query.getTrackTotalHitsUpTo());
		}

		if (StringUtils.hasLength(query.getRoute())) {
			request.routing(query.getRoute());
		}

		Duration timeout = query.getTimeout();
		if (timeout != null) {
			sourceBuilder.timeout(new TimeValue(timeout.toMillis()));
		}

		sourceBuilder.explain(query.getExplain());

		if (query.getSearchAfter() != null) {
			sourceBuilder.searchAfter(query.getSearchAfter().toArray());
		}

		query.getRescorerQueries().forEach(rescorer -> sourceBuilder.addRescorer(getQueryRescorerBuilder(rescorer)));

		if (query.getRequestCache() != null) {
			request.requestCache(query.getRequestCache());
		}

		if (query.getScrollTime() != null) {
			request.scroll(TimeValue.timeValueMillis(query.getScrollTime().toMillis()));
		}

		request.source(sourceBuilder);
		return request;
	}

	private void prepareNativeSearch(NativeSearchQuery query, SearchSourceBuilder sourceBuilder) {

		if (!query.getScriptFields().isEmpty()) {
			for (ScriptField scriptedField : query.getScriptFields()) {
				sourceBuilder.scriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		if (query.getCollapseBuilder() != null) {
			sourceBuilder.collapse(query.getCollapseBuilder());
		}

		if (!isEmpty(query.getIndicesBoost())) {
			for (IndexBoost indexBoost : query.getIndicesBoost()) {
				sourceBuilder.indexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(query.getAggregations())) {
			query.getAggregations().forEach(sourceBuilder::aggregation);
		}

		if (!isEmpty(query.getPipelineAggregations())) {
			query.getPipelineAggregations().forEach(sourceBuilder::aggregation);
		}

		if (query.getSuggestBuilder() != null) {
			sourceBuilder.suggest(query.getSuggestBuilder());
		}

		if (!isEmpty(query.getSearchExtBuilders())) {
			sourceBuilder.ext(query.getSearchExtBuilders());
		}
	}

	@SuppressWarnings("rawtypes")
	private void prepareSort(Query query, SearchSourceBuilder sourceBuilder,
			@Nullable ElasticsearchPersistentEntity<?> entity) {

		if (query.getSort() != null) {
			query.getSort().forEach(order -> sourceBuilder.sort(getSortBuilder(order, entity)));
		}

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery nativeSearchQuery = (NativeSearchQuery) query;
			List<SortBuilder<?>> sorts = nativeSearchQuery.getOpensearchSorts();
			if (sorts != null) {
				sorts.forEach(sourceBuilder::sort);
			}
		}
	}

	private SortBuilder<?> getSortBuilder(Sort.Order order, @Nullable ElasticsearchPersistentEntity<?> entity) {
		SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC;

		Order.Mode mode = Order.DEFAULT_MODE;
		String unmappedType = null;

		if (order instanceof Order) {
			Order o = (Order) order;
			mode = o.getMode();
			unmappedType = o.getUnmappedType();
		}

		if (ScoreSortBuilder.NAME.equals(order.getProperty())) {
			return SortBuilders //
					.scoreSort() //
					.order(sortOrder);
		} else {
			ElasticsearchPersistentProperty property = (entity != null) //
					? entity.getPersistentProperty(order.getProperty()) //
					: null;
			String fieldName = property != null ? property.getFieldName() : order.getProperty();

			if (order instanceof GeoDistanceOrder) {
				GeoDistanceOrder geoDistanceOrder = (GeoDistanceOrder) order;

				GeoDistanceSortBuilder sort = SortBuilders.geoDistanceSort(fieldName, geoDistanceOrder.getGeoPoint().getLat(),
						geoDistanceOrder.getGeoPoint().getLon());

				sort.geoDistance(GeoDistance.fromString(geoDistanceOrder.getDistanceType().name()));
				sort.sortMode(SortMode.fromString(mode.name()));
				sort.unit(DistanceUnit.fromString(geoDistanceOrder.getUnit()));

				if (geoDistanceOrder.getIgnoreUnmapped() != GeoDistanceOrder.DEFAULT_IGNORE_UNMAPPED) {
					sort.ignoreUnmapped(geoDistanceOrder.getIgnoreUnmapped());
				}

				return sort;
			} else {
				FieldSortBuilder sort = SortBuilders //
						.fieldSort(fieldName) //
						.order(sortOrder) //
						.sortMode(SortMode.fromString(mode.name()));

				if (unmappedType != null) {
					sort.unmappedType(unmappedType);
				}

				if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
					sort.missing("_first");
				} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
					sort.missing("_last");
				}
				return sort;
			}
		}
	}

	private QueryRescorerBuilder getQueryRescorerBuilder(RescorerQuery rescorerQuery) {

		QueryBuilder queryBuilder = getQuery(rescorerQuery.getQuery());
		Assert.notNull("queryBuilder", "Could not build query for rescorerQuery");

		QueryRescorerBuilder builder = new QueryRescorerBuilder(queryBuilder);

		if (rescorerQuery.getScoreMode() != ScoreMode.Default) {
			builder.setScoreMode(QueryRescoreMode.valueOf(rescorerQuery.getScoreMode().name()));
		}

		if (rescorerQuery.getQueryWeight() != null) {
			builder.setQueryWeight(rescorerQuery.getQueryWeight());
		}

		if (rescorerQuery.getRescoreQueryWeight() != null) {
			builder.setRescoreQueryWeight(rescorerQuery.getRescoreQueryWeight());
		}

		if (rescorerQuery.getWindowSize() != null) {
			builder.windowSize(rescorerQuery.getWindowSize());
		}

		return builder;

	}
	// endregion

	// region update
	public UpdateRequest updateRequest(UpdateQuery query, IndexCoordinates index) {

		String indexName = query.getIndexName() != null ? query.getIndexName() : index.getIndexName();
		UpdateRequest updateRequest = new UpdateRequest(indexName, query.getId());

		if (query.getScript() != null) {
			Map<String, Object> params = query.getParams();

			if (params == null) {
				params = new HashMap<>();
			}
			Script script = new Script(getScriptType(query.getScriptType()), query.getLang(), query.getScript(), params);
			updateRequest.script(script);
		}

		if (query.getDocument() != null) {
			updateRequest.doc(query.getDocument());
		}

		if (query.getUpsert() != null) {
			updateRequest.upsert(query.getUpsert());
		}

		if (query.getRouting() != null) {
			updateRequest.routing(query.getRouting());
		}

		if (query.getScriptedUpsert() != null) {
			updateRequest.scriptedUpsert(query.getScriptedUpsert());
		}

		if (query.getDocAsUpsert() != null) {
			updateRequest.docAsUpsert(query.getDocAsUpsert());
		}

		if (query.getFetchSource() != null) {
			updateRequest.fetchSource(query.getFetchSource());
		}

		if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
			List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
					: Collections.emptyList();
			List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
					: Collections.emptyList();
			updateRequest.fetchSource(includes.toArray(new String[0]), excludes.toArray(new String[0]));
		}

		if (query.getIfSeqNo() != null) {
			updateRequest.setIfSeqNo(query.getIfSeqNo());
		}

		if (query.getIfPrimaryTerm() != null) {
			updateRequest.setIfPrimaryTerm(query.getIfPrimaryTerm());
		}

		if (query.getRefreshPolicy() != null) {
			updateRequest.setRefreshPolicy(RequestFactory.toOpensearchRefreshPolicy(query.getRefreshPolicy()));
		}

		if (query.getRetryOnConflict() != null) {
			updateRequest.retryOnConflict(query.getRetryOnConflict());
		}

		if (query.getTimeout() != null) {
			updateRequest.timeout(query.getTimeout());
		}

		if (query.getWaitForActiveShards() != null) {
			updateRequest.waitForActiveShards(ActiveShardCount.parseString(query.getWaitForActiveShards()));
		}

		return updateRequest;
	}

	public UpdateByQueryRequest updateByQueryRequest(UpdateQuery query, IndexCoordinates index) {

		final UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(index.getIndexNames());
		updateByQueryRequest.setScript(getScript(query));

		if (query.getAbortOnVersionConflict() != null) {
			updateByQueryRequest.setAbortOnVersionConflict(query.getAbortOnVersionConflict());
		}

		if (query.getBatchSize() != null) {
			updateByQueryRequest.setBatchSize(query.getBatchSize());
		}

		if (query.getQuery() != null) {
			final Query queryQuery = query.getQuery();

			updateByQueryRequest.setQuery(getQuery(queryQuery));

			if (queryQuery.getIndicesOptions() != null) {
				updateByQueryRequest.setIndicesOptions(toOpensearchIndicesOptions(queryQuery.getIndicesOptions()));
			}

			if (queryQuery.getScrollTime() != null) {
				updateByQueryRequest.setScroll(TimeValue.timeValueMillis(queryQuery.getScrollTime().toMillis()));
			}
		}

		if (query.getMaxDocs() != null) {
			updateByQueryRequest.setMaxDocs(query.getMaxDocs());
		}

		if (query.getMaxRetries() != null) {
			updateByQueryRequest.setMaxRetries(query.getMaxRetries());
		}

		if (query.getPipeline() != null) {
			updateByQueryRequest.setPipeline(query.getPipeline());
		}

		if (query.getRefreshPolicy() != null) {
			updateByQueryRequest.setRefresh(query.getRefreshPolicy() == RefreshPolicy.IMMEDIATE);
		}

		if (query.getRequestsPerSecond() != null) {
			updateByQueryRequest.setRequestsPerSecond(query.getRequestsPerSecond());
		}

		if (query.getRouting() != null) {
			updateByQueryRequest.setRouting(query.getRouting());
		}

		if (query.getShouldStoreResult() != null) {
			updateByQueryRequest.setShouldStoreResult(query.getShouldStoreResult());
		}

		if (query.getSlices() != null) {
			updateByQueryRequest.setSlices(query.getSlices());
		}

		if (query.getTimeout() != null) {
			updateByQueryRequest.setTimeout(query.getTimeout());
		}

		if (query.getWaitForActiveShards() != null) {
			updateByQueryRequest.setWaitForActiveShards(ActiveShardCount.parseString(query.getWaitForActiveShards()));
		}

		return updateByQueryRequest;
	}

	// endregion

	// region helper functions
	@Nullable
	private QueryBuilder getQuery(Query query) {
		QueryBuilder opensearchQuery;

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery searchQuery = (NativeSearchQuery) query;
			opensearchQuery = searchQuery.getQuery();
		} else if (query instanceof CriteriaQuery) {
			CriteriaQuery criteriaQuery = (CriteriaQuery) query;
			opensearchQuery = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria());
		} else if (query instanceof StringQuery) {
			StringQuery stringQuery = (StringQuery) query;
			opensearchQuery = wrapperQuery(stringQuery.getSource());
		} else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return opensearchQuery;
	}

	@Nullable
	private QueryBuilder getFilter(Query query) {
		QueryBuilder opensearchFilter;

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery searchQuery = (NativeSearchQuery) query;
			opensearchFilter = searchQuery.getFilter();
		} else if (query instanceof CriteriaQuery) {
			CriteriaQuery criteriaQuery = (CriteriaQuery) query;
			opensearchFilter = new CriteriaFilterProcessor().createFilter(criteriaQuery.getCriteria());
		} else if (query instanceof StringQuery) {
		    opensearchFilter = null;
		} else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return opensearchFilter;
	}

	public static WriteRequest.RefreshPolicy toOpensearchRefreshPolicy(RefreshPolicy refreshPolicy) {
		switch (refreshPolicy) {
			case IMMEDIATE:
				return WriteRequest.RefreshPolicy.IMMEDIATE;
			case WAIT_UNTIL:
				return WriteRequest.RefreshPolicy.WAIT_UNTIL;
			case NONE:
			default:
				return WriteRequest.RefreshPolicy.NONE;
		}
	}

	@Nullable
	private FetchSourceContext getFetchSourceContext(Query searchQuery) {

		SourceFilter sourceFilter = searchQuery.getSourceFilter();

		if (sourceFilter != null) {
			return new FetchSourceContext(true, sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		return null;
	}

	public org.opensearch.action.support.IndicesOptions toOpensearchIndicesOptions(IndicesOptions indicesOptions) {

		Assert.notNull(indicesOptions, "indicesOptions must not be null");

		Set<org.opensearch.action.support.IndicesOptions.Option> options = indicesOptions.getOptions().stream()
				.map(it -> org.opensearch.action.support.IndicesOptions.Option.valueOf(it.name().toUpperCase()))
				.collect(Collectors.toSet());

		Set<org.opensearch.action.support.IndicesOptions.WildcardStates> wildcardStates = indicesOptions
				.getExpandWildcards().stream()
				.map(it -> org.opensearch.action.support.IndicesOptions.WildcardStates.valueOf(it.name().toUpperCase()))
				.collect(Collectors.toSet());

		return new org.opensearch.action.support.IndicesOptions(
				options.isEmpty() ? EnumSet.noneOf(org.opensearch.action.support.IndicesOptions.Option.class)
						: EnumSet.copyOf(options),
				wildcardStates.isEmpty() ? EnumSet.noneOf(org.opensearch.action.support.IndicesOptions.WildcardStates.class)
						: EnumSet.copyOf(wildcardStates));
	}
	// endregion

	@Nullable
	private ElasticsearchPersistentEntity<?> getPersistentEntity(@Nullable Class<?> clazz) {
		return clazz != null ? elasticsearchConverter.getMappingContext().getPersistentEntity(clazz) : null;
	}

	@Nullable
	private String getPersistentEntityId(Object entity) {

		MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext = elasticsearchConverter
				.getMappingContext();

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entity.getClass());

		if (persistentEntity != null) {
			Object identifier = persistentEntity //
					.getIdentifierAccessor(entity).getIdentifier();

			if (identifier != null) {
				return identifier.toString();
			}
		}

		return null;
	}

	private VersionType retrieveVersionTypeFromPersistentEntity(@Nullable Class<?> clazz) {

		MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext = elasticsearchConverter
				.getMappingContext();

		ElasticsearchPersistentEntity<?> persistentEntity = clazz != null ? mappingContext.getPersistentEntity(clazz)
				: null;

		VersionType versionType = null;

		if (persistentEntity != null) {
			org.springframework.data.elasticsearch.annotations.Document.VersionType entityVersionType = persistentEntity
					.getVersionType();

			if (entityVersionType != null) {
				versionType = VersionType.fromString(entityVersionType.name().toLowerCase());
			}
		}

		return versionType != null ? versionType : VersionType.EXTERNAL;
	}

	private String[] toArray(List<String> values) {
		String[] valuesAsArray = new String[values.size()];
		return values.toArray(valuesAsArray);
	}
	// endregion

	private boolean hasSeqNoPrimaryTermProperty(@Nullable Class<?> entityClass) {

		if (entityClass == null) {
			return false;
		}

		if (!elasticsearchConverter.getMappingContext().hasPersistentEntityFor(entityClass)) {
			return false;
		}

		ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.getMappingContext()
				.getRequiredPersistentEntity(entityClass);
		return entity.hasSeqNoPrimaryTermProperty();
	}

	private org.opensearch.script.ScriptType getScriptType(@Nullable ScriptType scriptType) {

		if (scriptType == null || ScriptType.INLINE.equals(scriptType)) {
			return org.opensearch.script.ScriptType.INLINE;
		} else {
			return org.opensearch.script.ScriptType.STORED;
		}
	}

	@Nullable
	private Script getScript(UpdateQuery query) {
		if (ScriptType.STORED.equals(query.getScriptType()) && query.getScriptName() != null) {
			final Map<String, Object> params = Optional.ofNullable(query.getParams()).orElse(new HashMap<>());
			return new Script(getScriptType(ScriptType.STORED), null, query.getScriptName(), params);
		}

		if (ScriptType.INLINE.equals(query.getScriptType()) && query.getScript() != null) {
			final Map<String, Object> params = Optional.ofNullable(query.getParams()).orElse(new HashMap<>());
			return new Script(getScriptType(ScriptType.INLINE), query.getLang(), query.getScript(), params);
		}

		return null;
	}

	// endregion
}
