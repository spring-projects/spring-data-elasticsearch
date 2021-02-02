/*
 * Copyright 2019-2021 the original author or authors.
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

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.util.CollectionUtils.*;

import java.util.*;

import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.IndexTemplateMetadata;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.index.reindex.UpdateByQueryAction;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequestBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory class to create Elasticsearch request instances from Spring Data Elasticsearch query objects.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author Roman Puchkovskiy
 * @author Subhobrata Dey
 * @author Farid Faoudi
 * @since 4.0
 */
class RequestFactory {

	// the default max result window size of Elasticsearch
	static final Integer INDEX_MAX_RESULT_WINDOW = 10_000;

	private final ElasticsearchConverter elasticsearchConverter;

	public RequestFactory(ElasticsearchConverter elasticsearchConverter) {
		this.elasticsearchConverter = elasticsearchConverter;
	}

	// region alias
	public IndicesAliasesRequest.AliasActions aliasAction(AliasQuery query, IndexCoordinates index) {

		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		String[] indexNames = index.getIndexNames();
		IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.add()
				.alias(query.getAliasName()).indices(indexNames);

		if (query.getFilterBuilder() != null) {
			aliasAction.filter(query.getFilterBuilder());
		} else if (query.getFilter() != null) {
			aliasAction.filter(query.getFilter());
		}

		if (!StringUtils.isEmpty(query.getRouting())) {
			aliasAction.routing(query.getRouting());
		}

		if (!StringUtils.isEmpty(query.getSearchRouting())) {
			aliasAction.searchRouting(query.getSearchRouting());
		}

		if (!StringUtils.isEmpty(query.getIndexRouting())) {
			aliasAction.indexRouting(query.getIndexRouting());
		}

		return aliasAction;
	}

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

	public IndicesAliasesRequest indicesAddAliasesRequest(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest.AliasActions aliasAction = aliasAction(query, index);
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(aliasAction);
		return request;
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

	public IndicesAliasesRequestBuilder indicesAliasesRequestBuilder(Client client, AliasActions aliasActions) {

		IndicesAliasesRequestBuilder requestBuilder = client.admin().indices().prepareAliases();
		indicesAliasesRequest(aliasActions).getAliasActions().forEach(requestBuilder::addAliasAction);
		return requestBuilder;
	}

	public IndicesAliasesRequest indicesRemoveAliasesRequest(AliasQuery query, IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		IndicesAliasesRequest.AliasActions aliasAction = IndicesAliasesRequest.AliasActions.remove() //
				.indices(indexNames) //
				.alias(query.getAliasName());

		return Requests.indexAliasesRequest() //
				.addAliasAction(aliasAction);
	}

	IndicesAliasesRequestBuilder indicesRemoveAliasesRequestBuilder(Client client, AliasQuery query,
			IndexCoordinates index) {

		String indexName = index.getIndexName();
		return client.admin().indices().prepareAliases().removeAlias(indexName, query.getAliasName());
	}

	// endregion

	// region bulk
	public BulkRequest bulkRequest(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {
		BulkRequest bulkRequest = new BulkRequest();

		if (bulkOptions.getTimeout() != null) {
			bulkRequest.timeout(bulkOptions.getTimeout());
		}

		if (bulkOptions.getRefreshPolicy() != null) {
			bulkRequest.setRefreshPolicy(bulkOptions.getRefreshPolicy());
		}

		if (bulkOptions.getWaitForActiveShards() != null) {
			bulkRequest.waitForActiveShards(bulkOptions.getWaitForActiveShards());
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

	public BulkRequestBuilder bulkRequestBuilder(Client client, List<?> queries, BulkOptions bulkOptions,
			IndexCoordinates index) {
		BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

		if (bulkOptions.getTimeout() != null) {
			bulkRequestBuilder.setTimeout(bulkOptions.getTimeout());
		}

		if (bulkOptions.getRefreshPolicy() != null) {
			bulkRequestBuilder.setRefreshPolicy(bulkOptions.getRefreshPolicy());
		}

		if (bulkOptions.getWaitForActiveShards() != null) {
			bulkRequestBuilder.setWaitForActiveShards(bulkOptions.getWaitForActiveShards());
		}

		if (bulkOptions.getPipeline() != null) {
			bulkRequestBuilder.pipeline(bulkOptions.getPipeline());
		}

		if (bulkOptions.getRoutingId() != null) {
			bulkRequestBuilder.routing(bulkOptions.getRoutingId());
		}

		queries.forEach(query -> {

			if (query instanceof IndexQuery) {
				bulkRequestBuilder.add(indexRequestBuilder(client, (IndexQuery) query, index));
			} else if (query instanceof UpdateQuery) {
				bulkRequestBuilder.add(updateRequestBuilderFor(client, (UpdateQuery) query, index));
			}
		});

		return bulkRequestBuilder;
	}

	// endregion

	// region index management
	/**
	 * creates a CreateIndexRequest from the rest-high-level-client library.
	 *
	 * @param index name of the index
	 * @param settings optional settings
	 * @return request
	 */
	public CreateIndexRequest createIndexRequest(IndexCoordinates index, @Nullable Document settings) {
		CreateIndexRequest request = new CreateIndexRequest(index.getIndexName());

		if (settings != null && !settings.isEmpty()) {
			request.settings(settings);
		}
		return request;
	}

	/**
	 * creates a CreateIndexRequest from the elasticsearch library, used by the reactive methods.
	 *
	 * @param indexName name of the index
	 * @param settings optional settings
	 * @return request
	 */
	public org.elasticsearch.action.admin.indices.create.CreateIndexRequest createIndexRequestReactive(String indexName,
			@Nullable Document settings) {

		org.elasticsearch.action.admin.indices.create.CreateIndexRequest request = new org.elasticsearch.action.admin.indices.create.CreateIndexRequest(
				indexName);
		request.index(indexName);

		if (settings != null && !settings.isEmpty()) {
			request.settings(settings);
		}
		return request;
	}

	public CreateIndexRequestBuilder createIndexRequestBuilder(Client client, IndexCoordinates index,
			@Nullable Document settings) {

		String indexName = index.getIndexName();
		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

		if (settings != null) {
			createIndexRequestBuilder.setSettings(settings);
		}
		return createIndexRequestBuilder;
	}

	/**
	 * creates a GetIndexRequest from the rest-high-level-client library.
	 *
	 * @param index name of the index
	 * @return request
	 */
	public GetIndexRequest getIndexRequest(IndexCoordinates index) {
		return new GetIndexRequest(index.getIndexNames());
	}

	/**
	 * creates a CreateIndexRequest from the elasticsearch library, used by the reactive methods.
	 *
	 * @param indexName name of the index
	 * @return request
	 */
	public org.elasticsearch.action.admin.indices.get.GetIndexRequest getIndexRequestReactive(String indexName) {

		org.elasticsearch.action.admin.indices.get.GetIndexRequest request = new org.elasticsearch.action.admin.indices.get.GetIndexRequest();
		request.indices(indexName);
		return request;
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

	public org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest putMappingRequestReactive(
			IndexCoordinates index, Document mapping) {
		org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest request = new org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest(
				index.getIndexName());
		request.type("not-used-but-must-be-there");
		request.source(mapping);
		return request;
	}

	public PutMappingRequestBuilder putMappingRequestBuilder(Client client, IndexCoordinates index, Document mapping) {

		String[] indexNames = index.getIndexNames();
		PutMappingRequestBuilder requestBuilder = client.admin().indices().preparePutMapping(indexNames)
				.setType(IndexCoordinates.TYPE);
		requestBuilder.setSource(mapping);
		return requestBuilder;
	}

	public org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest getMappingRequestReactive(
			IndexCoordinates index) {
		org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest request = new org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest();
		request.indices(index.getIndexName());
		return request;
	}

	public GetSettingsRequest getSettingsRequest(String indexName, boolean includeDefaults) {
		return new GetSettingsRequest().indices(indexName).includeDefaults(includeDefaults);
	}

	public GetMappingsRequest getMappingsRequest(IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		return new GetMappingsRequest().indices(indexNames);
	}

	public org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest getMappingsRequest(
			@SuppressWarnings("unused") Client client, IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		return new org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest().indices(indexNames);
	}

	public Map<String, Set<AliasData>> convertAliasesResponse(Map<String, Set<AliasMetadata>> aliasesResponse) {
		Map<String, Set<AliasData>> converted = new LinkedHashMap<>();
		aliasesResponse.forEach((index, aliasMetaDataSet) -> {
			Set<AliasData> aliasDataSet = new LinkedHashSet<>();
			aliasMetaDataSet.forEach(aliasMetaData -> aliasDataSet.add(convertAliasMetadata(aliasMetaData)));
			converted.put(index, aliasDataSet);
		});
		return converted;
	}

	public AliasData convertAliasMetadata(AliasMetadata aliasMetaData) {
		Document filter = null;
		CompressedXContent aliasMetaDataFilter = aliasMetaData.getFilter();
		if (aliasMetaDataFilter != null) {
			filter = Document.parse(aliasMetaDataFilter.string());
		}
		AliasData aliasData = AliasData.of(aliasMetaData.alias(), filter, aliasMetaData.indexRouting(),
				aliasMetaData.getSearchRouting(), aliasMetaData.writeIndex(), aliasMetaData.isHidden());
		return aliasData;
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

	/**
	 * The version for the transport client needs a different PutIndexTemplateRequest class
	 */
	public org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest putIndexTemplateRequest(
			@SuppressWarnings("unused") Client client, PutTemplateRequest putTemplateRequest) {
		org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest request = new org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest(
				putTemplateRequest.getName()).patterns(Arrays.asList(putTemplateRequest.getIndexPatterns()));

		if (putTemplateRequest.getSettings() != null) {
			request.settings(putTemplateRequest.getSettings());
		}

		if (putTemplateRequest.getMappings() != null) {
			request.mapping("_doc", putTemplateRequest.getMappings());
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

	@Nullable
	public TemplateData getTemplateData(GetIndexTemplatesResponse getIndexTemplatesResponse, String templateName) {
		for (IndexTemplateMetadata indexTemplateMetadata : getIndexTemplatesResponse.getIndexTemplates()) {

			if (indexTemplateMetadata.name().equals(templateName)) {

				Document settings = Document.create();
				Settings templateSettings = indexTemplateMetadata.settings();
				templateSettings.keySet().forEach(key -> settings.put(key, templateSettings.get(key)));

				Map<String, AliasData> aliases = new LinkedHashMap<>();

				ImmutableOpenMap<String, AliasMetadata> aliasesResponse = indexTemplateMetadata.aliases();
				Iterator<String> keysIt = aliasesResponse.keysIt();
				while (keysIt.hasNext()) {
					String key = keysIt.next();
					aliases.put(key, convertAliasMetadata(aliasesResponse.get(key)));
				}
				TemplateData templateData = TemplateData.builder()
						.withIndexPatterns(indexTemplateMetadata.patterns().toArray(new String[0])) //
						.withSettings(settings) //
						.withMapping(Document.from(indexTemplateMetadata.mappings().getSourceAsMap())) //
						.withAliases(aliases) //
						.withOrder(indexTemplateMetadata.order()) //
						.withVersion(indexTemplateMetadata.version()).build();

				return templateData;
			}
		}
		return null;
	}

	public org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest getIndexTemplatesRequest(
			Client client, GetTemplateRequest getTemplateRequest) {
		return new org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest(
				getTemplateRequest.getTemplateName());
	}

	public IndexTemplatesExistRequest indexTemplatesExistsRequest(ExistsTemplateRequest existsTemplateRequest) {
		return new IndexTemplatesExistRequest(existsTemplateRequest.getTemplateName());
	}

	public DeleteIndexTemplateRequest deleteIndexTemplateRequest(DeleteTemplateRequest deleteTemplateRequest) {
		return new DeleteIndexTemplateRequest(deleteTemplateRequest.getTemplateName());
	}

	public org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest deleteIndexTemplateRequest(
			Client client, DeleteTemplateRequest deleteTemplateRequest) {
		return new org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest(
				deleteTemplateRequest.getTemplateName());
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

	public DeleteRequestBuilder deleteRequestBuilder(Client client, String id, @Nullable String routing,
			IndexCoordinates index) {
		String indexName = index.getIndexName();
		DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete();
		deleteRequestBuilder.setIndex(indexName);
		deleteRequestBuilder.setId(id);

		if (routing != null) {
			deleteRequestBuilder.setRouting(routing);
		}

		return deleteRequestBuilder;
	}

	public DeleteByQueryRequestBuilder deleteByQueryRequestBuilder(Client client, Query query, Class<?> clazz,
			IndexCoordinates index) {
		SearchRequest searchRequest = searchRequest(query, clazz, index);
		DeleteByQueryRequestBuilder requestBuilder = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE) //
				.source(index.getIndexNames()) //
				.filter(searchRequest.source().query()) //
				.abortOnVersionConflict(false) //
				.refresh(true);

		SearchRequestBuilder source = requestBuilder.source();

		if (query.isLimiting()) {
			// noinspection ConstantConditions
			source.setSize(query.getMaxResults());
		}

		if (query.hasScrollTime()) {
			// noinspection ConstantConditions
			source.setScroll(TimeValue.timeValueMillis(query.getScrollTime().toMillis()));
		}

		if (query.getRoute() != null) {
			source.setRouting(query.getRoute());
		}

		return requestBuilder;
	}
	// endregion

	// region get
	public GetRequest getRequest(String id, @Nullable String routing, IndexCoordinates index) {
		GetRequest getRequest = new GetRequest(index.getIndexName(), id);
		getRequest.routing(routing);
		return getRequest;
	}

	public GetRequestBuilder getRequestBuilder(Client client, String id, @Nullable String routing,
			IndexCoordinates index) {
		GetRequestBuilder getRequestBuilder = client.prepareGet(index.getIndexName(), null, id);
		getRequestBuilder.setRouting(routing);
		return getRequestBuilder;
	}

	public MultiGetRequest multiGetRequest(Query query, Class<?> clazz, IndexCoordinates index) {

		MultiGetRequest multiGetRequest = new MultiGetRequest();
		getMultiRequestItems(query, clazz, index).forEach(multiGetRequest::add);
		return multiGetRequest;
	}

	public MultiGetRequestBuilder multiGetRequestBuilder(Client client, Query searchQuery, Class<?> clazz,
			IndexCoordinates index) {

		MultiGetRequestBuilder multiGetRequestBuilder = client.prepareMultiGet();
		getMultiRequestItems(searchQuery, clazz, index).forEach(multiGetRequestBuilder::add);
		return multiGetRequestBuilder;
	}

	private List<MultiGetRequest.Item> getMultiRequestItems(Query searchQuery, Class<?> clazz, IndexCoordinates index) {

		elasticsearchConverter.updateQuery(searchQuery, clazz);
		List<MultiGetRequest.Item> items = new ArrayList<>();

		FetchSourceContext fetchSourceContext = getFetchSourceContext(searchQuery);

		if (!isEmpty(searchQuery.getIds())) {
			String indexName = index.getIndexName();
			for (String id : searchQuery.getIds()) {
				MultiGetRequest.Item item = new MultiGetRequest.Item(indexName, id);

				if (searchQuery.getRoute() != null) {
					item = item.routing(searchQuery.getRoute());
				}

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

		String indexName = index.getIndexName();
		IndexRequest indexRequest;

		if (query.getObject() != null) {
			String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
			// If we have a query id and a document id, do not ask ES to generate one.
			if (id != null) {
				indexRequest = new IndexRequest(indexName).id(id);
			} else {
				indexRequest = new IndexRequest(indexName);
			}
			indexRequest.source(elasticsearchConverter.mapObject(query.getObject()).toJson(), Requests.INDEX_CONTENT_TYPE);
		} else if (query.getSource() != null) {
			indexRequest = new IndexRequest(indexName).id(query.getId()).source(query.getSource(),
					Requests.INDEX_CONTENT_TYPE);
		} else {
			throw new InvalidDataAccessApiUsageException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}

		if (query.getVersion() != null) {
			indexRequest.version(query.getVersion());
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(query.getObject().getClass());
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

	public IndexRequestBuilder indexRequestBuilder(Client client, IndexQuery query, IndexCoordinates index) {
		String indexName = index.getIndexName();
		String type = IndexCoordinates.TYPE;

		IndexRequestBuilder indexRequestBuilder;

		if (query.getObject() != null) {
			String id = StringUtils.isEmpty(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
			// If we have a query id and a document id, do not ask ES to generate one.
			if (id != null) {
				indexRequestBuilder = client.prepareIndex(indexName, type, id);
			} else {
				indexRequestBuilder = client.prepareIndex(indexName, type);
			}
			indexRequestBuilder.setSource(elasticsearchConverter.mapObject(query.getObject()).toJson(),
					Requests.INDEX_CONTENT_TYPE);
		} else if (query.getSource() != null) {
			indexRequestBuilder = client.prepareIndex(indexName, type, query.getId()).setSource(query.getSource(),
					Requests.INDEX_CONTENT_TYPE);
		} else {
			throw new InvalidDataAccessApiUsageException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}

		if (query.getVersion() != null) {
			indexRequestBuilder.setVersion(query.getVersion());
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(query.getObject().getClass());
			indexRequestBuilder.setVersionType(versionType);
		}

		if (query.getSeqNo() != null) {
			indexRequestBuilder.setIfSeqNo(query.getSeqNo());
		}

		if (query.getPrimaryTerm() != null) {
			indexRequestBuilder.setIfPrimaryTerm(query.getPrimaryTerm());
		}

		if (query.getRouting() != null) {
			indexRequestBuilder.setRouting(query.getRouting());
		}

		return indexRequestBuilder;
	}
	// endregion

	// region search
	@Nullable
	public HighlightBuilder highlightBuilder(Query query) {
		HighlightBuilder highlightBuilder = query.getHighlightQuery().map(HighlightQuery::getHighlightBuilder).orElse(null);

		if (highlightBuilder == null) {

			if (query instanceof NativeSearchQuery) {
				NativeSearchQuery searchQuery = (NativeSearchQuery) query;

				if (searchQuery.getHighlightFields() != null || searchQuery.getHighlightBuilder() != null) {
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

	public SearchRequestBuilder searchRequestBuilder(Client client, SuggestBuilder suggestion, IndexCoordinates index) {
		String[] indexNames = index.getIndexNames();
		return client.prepareSearch(indexNames).suggest(suggestion);
	}

	public SearchRequest searchRequest(Query query, @Nullable Class<?> clazz, IndexCoordinates index) {

		elasticsearchConverter.updateQuery(query, clazz);
		SearchRequest searchRequest = prepareSearchRequest(query, clazz, index);
		QueryBuilder elasticsearchQuery = getQuery(query);
		QueryBuilder elasticsearchFilter = getFilter(query);

		searchRequest.source().query(elasticsearchQuery);

		if (elasticsearchFilter != null) {
			searchRequest.source().postFilter(elasticsearchFilter);
		}

		return searchRequest;

	}

	public SearchRequestBuilder searchRequestBuilder(Client client, Query query, @Nullable Class<?> clazz,
			IndexCoordinates index) {

		elasticsearchConverter.updateQuery(query, clazz);
		SearchRequestBuilder searchRequestBuilder = prepareSearchRequestBuilder(query, client, clazz, index);
		QueryBuilder elasticsearchQuery = getQuery(query);
		QueryBuilder elasticsearchFilter = getFilter(query);

		searchRequestBuilder.setQuery(elasticsearchQuery);

		if (elasticsearchFilter != null) {
			searchRequestBuilder.setPostFilter(elasticsearchFilter);
		}

		return searchRequestBuilder;
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

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			sourceBuilder.fetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable().isPaged()) {
			sourceBuilder.from((int) query.getPageable().getOffset());
			sourceBuilder.size(query.getPageable().getPageSize());
		} else {
			sourceBuilder.from(0);
			sourceBuilder.size(INDEX_MAX_RESULT_WINDOW);
		}

		if (!query.getFields().isEmpty()) {
			sourceBuilder.fetchSource(query.getFields().toArray(new String[0]), null);
		}

		if (query.getIndicesOptions() != null) {
			request.indicesOptions(query.getIndicesOptions());
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

		request.searchType(query.getSearchType());

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

		TimeValue timeout = query.getTimeout();
		if (timeout != null) {
			sourceBuilder.timeout(timeout);
		}

		sourceBuilder.explain(query.getExplain());

		request.source(sourceBuilder);
		return request;
	}

	private SearchRequestBuilder prepareSearchRequestBuilder(Query query, Client client, @Nullable Class<?> clazz,
			IndexCoordinates index) {

		String[] indexNames = index.getIndexNames();
		Assert.notNull(indexNames, "No index defined for Query");
		Assert.notEmpty(indexNames, "No index defined for Query");

		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexNames) //
				.setSearchType(query.getSearchType()) //
				.setVersion(true) //
				.setTrackScores(query.getTrackScores());
		if (hasSeqNoPrimaryTermProperty(clazz)) {
			searchRequestBuilder.seqNoAndPrimaryTerm(true);
		}

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			searchRequestBuilder.setFetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable().isPaged()) {
			searchRequestBuilder.setFrom((int) query.getPageable().getOffset());
			searchRequestBuilder.setSize(query.getPageable().getPageSize());
		} else {
			searchRequestBuilder.setFrom(0);
			searchRequestBuilder.setSize(INDEX_MAX_RESULT_WINDOW);
		}

		if (!query.getFields().isEmpty()) {
			searchRequestBuilder.setFetchSource(query.getFields().toArray(new String[0]), null);
		}

		if (query.getIndicesOptions() != null) {
			searchRequestBuilder.setIndicesOptions(query.getIndicesOptions());
		}

		if (query.isLimiting()) {
			// noinspection ConstantConditions
			searchRequestBuilder.setSize(query.getMaxResults());
		}

		if (query.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(query.getMinScore());
		}

		if (query.getPreference() != null) {
			searchRequestBuilder.setPreference(query.getPreference());
		}

		prepareSort(query, searchRequestBuilder, getPersistentEntity(clazz));

		HighlightBuilder highlightBuilder = highlightBuilder(query);

		if (highlightBuilder != null) {
			searchRequestBuilder.highlighter(highlightBuilder);
		}

		if (query instanceof NativeSearchQuery) {
			prepareNativeSearch(searchRequestBuilder, (NativeSearchQuery) query);
		}

		if (query.getTrackTotalHits() != null) {
			searchRequestBuilder.setTrackTotalHits(query.getTrackTotalHits());
		} else if (query.getTrackTotalHitsUpTo() != null) {
			searchRequestBuilder.setTrackTotalHitsUpTo(query.getTrackTotalHitsUpTo());
		}

		if (StringUtils.hasLength(query.getRoute())) {
			searchRequestBuilder.setRouting(query.getRoute());
		}

		TimeValue timeout = query.getTimeout();
		if (timeout != null) {
			searchRequestBuilder.setTimeout(timeout);
		}

		searchRequestBuilder.setExplain(query.getExplain());

		return searchRequestBuilder;
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
			for (AbstractAggregationBuilder<?> aggregationBuilder : query.getAggregations()) {
				sourceBuilder.aggregation(aggregationBuilder);
			}
		}

	}

	private void prepareNativeSearch(SearchRequestBuilder searchRequestBuilder, NativeSearchQuery nativeSearchQuery) {
		if (!isEmpty(nativeSearchQuery.getScriptFields())) {
			for (ScriptField scriptedField : nativeSearchQuery.getScriptFields()) {
				searchRequestBuilder.addScriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		if (nativeSearchQuery.getCollapseBuilder() != null) {
			searchRequestBuilder.setCollapse(nativeSearchQuery.getCollapseBuilder());
		}

		if (!isEmpty(nativeSearchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : nativeSearchQuery.getIndicesBoost()) {
				searchRequestBuilder.addIndexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(nativeSearchQuery.getAggregations())) {
			for (AbstractAggregationBuilder<?> aggregationBuilder : nativeSearchQuery.getAggregations()) {
				searchRequestBuilder.addAggregation(aggregationBuilder);
			}
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
			List<SortBuilder<?>> sorts = nativeSearchQuery.getElasticsearchSorts();
			if (sorts != null) {
				sorts.forEach(sourceBuilder::sort);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void prepareSort(Query query, SearchRequestBuilder searchRequestBuilder,
			@Nullable ElasticsearchPersistentEntity<?> entity) {
		if (query.getSort() != null) {
			query.getSort().forEach(order -> searchRequestBuilder.addSort(getSortBuilder(order, entity)));
		}

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery nativeSearchQuery = (NativeSearchQuery) query;
			List<SortBuilder<?>> sorts = nativeSearchQuery.getElasticsearchSorts();
			if (sorts != null) {
				sorts.forEach(searchRequestBuilder::addSort);
			}
		}
	}

	private SortBuilder<?> getSortBuilder(Sort.Order order, @Nullable ElasticsearchPersistentEntity<?> entity) {
		SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC;

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
				sort.ignoreUnmapped(geoDistanceOrder.getIgnoreUnmapped());
				sort.sortMode(SortMode.fromString(geoDistanceOrder.getMode().name()));
				sort.unit(DistanceUnit.fromString(geoDistanceOrder.getUnit()));
				return sort;
			} else {
				FieldSortBuilder sort = SortBuilders //
						.fieldSort(fieldName) //
						.order(sortOrder);

				if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
					sort.missing("_first");
				} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
					sort.missing("_last");
				}
				return sort;
			}
		}
	}
	// endregion

	// region update
	public UpdateRequest updateRequest(UpdateQuery query, IndexCoordinates index) {

		String indexName = index.getIndexName();
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
			updateRequest.setRefreshPolicy(RequestFactory.toElasticsearchRefreshPolicy(query.getRefreshPolicy()));
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

	public UpdateRequestBuilder updateRequestBuilderFor(Client client, UpdateQuery query, IndexCoordinates index) {

		String indexName = index.getIndexName();
		UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(indexName, IndexCoordinates.TYPE, query.getId());

		if (query.getScript() != null) {
			Map<String, Object> params = query.getParams();

			if (params == null) {
				params = new HashMap<>();
			}
			Script script = new Script(getScriptType(query.getScriptType()), query.getLang(), query.getScript(), params);
			updateRequestBuilder.setScript(script);
		}

		if (query.getDocument() != null) {
			updateRequestBuilder.setDoc(query.getDocument());
		}

		if (query.getUpsert() != null) {
			updateRequestBuilder.setUpsert(query.getUpsert());
		}

		if (query.getRouting() != null) {
			updateRequestBuilder.setRouting(query.getRouting());
		}

		if (query.getScriptedUpsert() != null) {
			updateRequestBuilder.setScriptedUpsert(query.getScriptedUpsert());
		}

		if (query.getDocAsUpsert() != null) {
			updateRequestBuilder.setDocAsUpsert(query.getDocAsUpsert());
		}

		if (query.getFetchSource() != null) {
			updateRequestBuilder.setFetchSource(query.getFetchSource());
		}

		if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
			List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
					: Collections.emptyList();
			List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
					: Collections.emptyList();
			updateRequestBuilder.setFetchSource(includes.toArray(new String[0]), excludes.toArray(new String[0]));
		}

		if (query.getIfSeqNo() != null) {
			updateRequestBuilder.setIfSeqNo(query.getIfSeqNo());
		}

		if (query.getIfPrimaryTerm() != null) {
			updateRequestBuilder.setIfPrimaryTerm(query.getIfPrimaryTerm());
		}

		if (query.getRefreshPolicy() != null) {
			updateRequestBuilder.setRefreshPolicy(RequestFactory.toElasticsearchRefreshPolicy(query.getRefreshPolicy()));
		}

		if (query.getRetryOnConflict() != null) {
			updateRequestBuilder.setRetryOnConflict(query.getRetryOnConflict());
		}

		if (query.getTimeout() != null) {
			updateRequestBuilder.setTimeout(query.getTimeout());
		}

		if (query.getWaitForActiveShards() != null) {
			updateRequestBuilder.setWaitForActiveShards(ActiveShardCount.parseString(query.getWaitForActiveShards()));
		}

		return updateRequestBuilder;
	}

	public UpdateByQueryRequest updateByQueryRequest(UpdateQuery query, IndexCoordinates index) {

		String indexName = index.getIndexName();
		final UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(indexName);
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
				updateByQueryRequest.setIndicesOptions(queryQuery.getIndicesOptions());
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

	public UpdateByQueryRequestBuilder updateByQueryRequestBuilder(Client client, UpdateQuery query,
			IndexCoordinates index) {

		String indexName = index.getIndexName();

		final UpdateByQueryRequestBuilder updateByQueryRequestBuilder = new UpdateByQueryRequestBuilder(client,
				UpdateByQueryAction.INSTANCE);

		updateByQueryRequestBuilder.source(indexName);
		updateByQueryRequestBuilder.script(getScript(query));

		if (query.getAbortOnVersionConflict() != null) {
			updateByQueryRequestBuilder.abortOnVersionConflict(query.getAbortOnVersionConflict());
		}

		if (query.getBatchSize() != null) {
			updateByQueryRequestBuilder.source().setSize(query.getBatchSize());
		}

		if (query.getQuery() != null) {
			final Query queryQuery = query.getQuery();
			updateByQueryRequestBuilder.filter(getQuery(queryQuery));

			if (queryQuery.getIndicesOptions() != null) {
				updateByQueryRequestBuilder.source().setIndicesOptions(queryQuery.getIndicesOptions());
			}

			if (queryQuery.getScrollTime() != null) {
				updateByQueryRequestBuilder.source()
						.setScroll(TimeValue.timeValueMillis(queryQuery.getScrollTime().toMillis()));
			}
		}

		if (query.getMaxDocs() != null) {
			updateByQueryRequestBuilder.maxDocs(query.getMaxDocs());
		}

		if (query.getMaxRetries() != null) {
			updateByQueryRequestBuilder.setMaxRetries(query.getMaxRetries());
		}

		if (query.getPipeline() != null) {
			updateByQueryRequestBuilder.setPipeline(query.getPipeline());
		}

		if (query.getRefreshPolicy() != null) {
			updateByQueryRequestBuilder.refresh(query.getRefreshPolicy() == RefreshPolicy.IMMEDIATE);
		}

		if (query.getRequestsPerSecond() != null) {
			updateByQueryRequestBuilder.setRequestsPerSecond(query.getRequestsPerSecond());
		}

		if (query.getRouting() != null) {
			updateByQueryRequestBuilder.source().setRouting(query.getRouting());
		}

		if (query.getShouldStoreResult() != null) {
			updateByQueryRequestBuilder.setShouldStoreResult(query.getShouldStoreResult());
		}

		if (query.getSlices() != null) {
			updateByQueryRequestBuilder.setSlices(query.getSlices());
		}

		if (query.getTimeout() != null) {
			updateByQueryRequestBuilder.source()
					.setTimeout(TimeValue.parseTimeValue(query.getTimeout(), getClass().getSimpleName() + ".timeout"));
		}

		return updateByQueryRequestBuilder;
	}
	// endregion

	// region helper functions
	@Nullable
	private QueryBuilder getQuery(Query query) {
		QueryBuilder elasticsearchQuery;

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery searchQuery = (NativeSearchQuery) query;
			elasticsearchQuery = searchQuery.getQuery();
		} else if (query instanceof CriteriaQuery) {
			CriteriaQuery criteriaQuery = (CriteriaQuery) query;
			elasticsearchQuery = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria());
		} else if (query instanceof StringQuery) {
			StringQuery stringQuery = (StringQuery) query;
			elasticsearchQuery = wrapperQuery(stringQuery.getSource());
		} else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return elasticsearchQuery;
	}

	@Nullable
	private QueryBuilder getFilter(Query query) {
		QueryBuilder elasticsearchFilter;

		if (query instanceof NativeSearchQuery) {
			NativeSearchQuery searchQuery = (NativeSearchQuery) query;
			elasticsearchFilter = searchQuery.getFilter();
		} else if (query instanceof CriteriaQuery) {
			CriteriaQuery criteriaQuery = (CriteriaQuery) query;
			elasticsearchFilter = new CriteriaFilterProcessor().createFilter(criteriaQuery.getCriteria());
		} else if (query instanceof StringQuery) {
			elasticsearchFilter = null;
		} else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return elasticsearchFilter;
	}

	public static WriteRequest.RefreshPolicy toElasticsearchRefreshPolicy(RefreshPolicy refreshPolicy) {
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

	private FetchSourceContext getFetchSourceContext(Query searchQuery) {
		FetchSourceContext fetchSourceContext = null;
		SourceFilter sourceFilter = searchQuery.getSourceFilter();

		if (!isEmpty(searchQuery.getFields())) {
			if (sourceFilter == null) {
				sourceFilter = new FetchSourceFilter(toArray(searchQuery.getFields()), null);
			} else {
				ArrayList<String> arrayList = new ArrayList<>();
				Collections.addAll(arrayList, sourceFilter.getIncludes());
				sourceFilter = new FetchSourceFilter(toArray(arrayList), null);
			}

			fetchSourceContext = new FetchSourceContext(true, sourceFilter.getIncludes(), sourceFilter.getExcludes());
		} else if (sourceFilter != null) {
			fetchSourceContext = new FetchSourceContext(true, sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}
		return fetchSourceContext;
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

	private VersionType retrieveVersionTypeFromPersistentEntity(Class<?> clazz) {

		MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext = elasticsearchConverter
				.getMappingContext();

		ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(clazz);

		VersionType versionType = null;

		if (persistentEntity != null) {
			versionType = persistentEntity.getVersionType();
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

	private org.elasticsearch.script.ScriptType getScriptType(@Nullable ScriptType scriptType) {

		if (scriptType == null || ScriptType.INLINE.equals(scriptType)) {
			return org.elasticsearch.script.ScriptType.INLINE;
		} else {
			return org.elasticsearch.script.ScriptType.STORED;
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

	// region response stuff

	/**
	 * extract the index settings information for a given index
	 *
	 * @param response the Elasticsearch response
	 * @param indexName the index name
	 * @return settings as {@link Document}
	 */
	public Document fromSettingsResponse(GetSettingsResponse response, String indexName) {

		Document settings = Document.create();

		if (!response.getIndexToDefaultSettings().isEmpty()) {
			Settings defaultSettings = response.getIndexToDefaultSettings().get(indexName);
			for (String key : defaultSettings.keySet()) {
				settings.put(key, defaultSettings.get(key));
			}
		}

		if (!response.getIndexToSettings().isEmpty()) {
			Settings customSettings = response.getIndexToSettings().get(indexName);
			for (String key : customSettings.keySet()) {
				settings.put(key, customSettings.get(key));
			}
		}

		return settings;
	}

	// endregion
}
