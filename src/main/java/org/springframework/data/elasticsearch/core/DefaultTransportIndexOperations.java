/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.IndexTemplateMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link IndexOperations} implementation using the TransportClient.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
class DefaultTransportIndexOperations extends AbstractDefaultIndexOperations implements IndexOperations {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTransportIndexOperations.class);

	private final Client client;

	public DefaultTransportIndexOperations(Client client, ElasticsearchConverter elasticsearchConverter,
			Class<?> boundClass) {
		super(elasticsearchConverter, boundClass);
		this.client = client;
	}

	public DefaultTransportIndexOperations(Client client, ElasticsearchConverter elasticsearchConverter,
			IndexCoordinates boundIndex) {
		super(elasticsearchConverter, boundIndex);
		this.client = client;
	}

	@Override
	protected boolean doCreate(IndexCoordinates index, @Nullable Document settings) {
		CreateIndexRequestBuilder createIndexRequestBuilder = requestFactory.createIndexRequestBuilder(client, index,
				settings);
		return createIndexRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	protected boolean doDelete(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for delete operation");

		if (doExists(index)) {
			DeleteIndexRequest deleteIndexRequest = requestFactory.deleteIndexRequest(index);
			return client.admin().indices().delete(deleteIndexRequest).actionGet().isAcknowledged();
		}
		return false;
	}

	@Override
	protected boolean doExists(IndexCoordinates index) {

		IndicesExistsRequest indicesExistsRequest = requestFactory.indicesExistsRequest(index);
		return client.admin().indices().exists(indicesExistsRequest).actionGet().isExists();
	}

	@Override
	protected boolean doPutMapping(IndexCoordinates index, Document mapping) {

		Assert.notNull(index, "No index defined for putMapping()");

		PutMappingRequestBuilder requestBuilder = requestFactory.putMappingRequestBuilder(client, index, mapping);
		return requestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	protected Map<String, Object> doGetMapping(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for getMapping()");

		GetMappingsRequest mappingsRequest = requestFactory.getMappingsRequest(client, index);

		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetadata>> mappings = client.admin().indices().getMappings( //
				mappingsRequest).actionGet() //
				.getMappings();

		if (mappings == null || mappings.size() == 0) {
			return Collections.emptyMap();
		}

		if (mappings.size() > 1) {
			LOGGER.warn("more than one mapping returned for " + index.getIndexName());
		}
		// we have at least one, take the first from the iterator
		return mappings.iterator().next().value.get(IndexCoordinates.TYPE).getSourceAsMap();
	}

	@Override
	protected boolean doAddAlias(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest.AliasActions aliasAction = requestFactory.aliasAction(query, index);
		return client.admin().indices().prepareAliases().addAliasAction(aliasAction).execute().actionGet().isAcknowledged();
	}

	@Override
	protected boolean doRemoveAlias(AliasQuery query, IndexCoordinates index) {

		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		IndicesAliasesRequestBuilder indicesAliasesRequestBuilder = requestFactory
				.indicesRemoveAliasesRequestBuilder(client, query, index);
		return indicesAliasesRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	protected List<AliasMetadata> doQueryForAlias(IndexCoordinates index) {

		GetAliasesRequest getAliasesRequest = requestFactory.getAliasesRequest(index);
		return client.admin().indices().getAliases(getAliasesRequest).actionGet().getAliases().get(index.getIndexName());
	}

	@Override
	protected Map<String, Set<AliasData>> doGetAliases(@Nullable String[] aliasNames, @Nullable String[] indexNames) {

		GetAliasesRequest getAliasesRequest = requestFactory.getAliasesRequest(aliasNames, indexNames);

		ImmutableOpenMap<String, List<AliasMetadata>> aliases = client.admin().indices().getAliases(getAliasesRequest)
				.actionGet().getAliases();

		Map<String, Set<AliasMetadata>> aliasesResponse = new LinkedHashMap<>();
		aliases.keysIt().forEachRemaining(index -> aliasesResponse.put(index, new HashSet<>(aliases.get(index))));
		return requestFactory.convertAliasesResponse(aliasesResponse);
	}

	@Override
	public boolean alias(AliasActions aliasActions) {

		IndicesAliasesRequestBuilder indicesAliasesRequestBuilder = requestFactory.indicesAliasesRequestBuilder(client,
				aliasActions);
		return indicesAliasesRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	protected Map<String, Object> doGetSettings(IndexCoordinates index, boolean includeDefaults) {

		Assert.notNull(index, "index must not be null");

		GetSettingsRequest getSettingsRequest = requestFactory.getSettingsRequest(index, includeDefaults);
		GetSettingsResponse response = client.admin() //
				.indices() //
				.getSettings(getSettingsRequest) //
				.actionGet();

		return requestFactory.fromSettingsResponse(response, index.getIndexName());
	}

	@Override
	protected void doRefresh(IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");

		RefreshRequest request = requestFactory.refreshRequest(index);
		client.admin().indices().refresh(request).actionGet();
	}

	@Override
	public boolean putTemplate(PutTemplateRequest putTemplateRequest) {

		Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

		PutIndexTemplateRequest putIndexTemplateRequest = requestFactory.putIndexTemplateRequest(client,
				putTemplateRequest);
		return client.admin().indices().putTemplate(putIndexTemplateRequest).actionGet().isAcknowledged();
	}

	@Override
	public TemplateData getTemplate(GetTemplateRequest getTemplateRequest) {

		Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

		GetIndexTemplatesRequest getIndexTemplatesRequest = requestFactory.getIndexTemplatesRequest(client,
				getTemplateRequest);
		GetIndexTemplatesResponse getIndexTemplatesResponse = client.admin().indices()
				.getTemplates(getIndexTemplatesRequest).actionGet();
		for (IndexTemplateMetadata indexTemplateMetadata : getIndexTemplatesResponse.getIndexTemplates()) {

			if (indexTemplateMetadata.getName().equals(getTemplateRequest.getTemplateName())) {

				Document settings = Document.create();
				Settings templateSettings = indexTemplateMetadata.settings();
				templateSettings.keySet().forEach(key -> settings.put(key, templateSettings.get(key)));

				Map<String, AliasData> aliases = new LinkedHashMap<>();
				ImmutableOpenMap<String, AliasMetadata> aliasesResponse = indexTemplateMetadata.aliases();
				Iterator<String> keysItAliases = aliasesResponse.keysIt();
				while (keysItAliases.hasNext()) {
					String key = keysItAliases.next();
					aliases.put(key, requestFactory.convertAliasMetadata(aliasesResponse.get(key)));
				}

				Map<String, String> mappingsDoc = new LinkedHashMap<>();
				ImmutableOpenMap<String, CompressedXContent> mappingsResponse = indexTemplateMetadata.mappings();
				Iterator<String> keysItMappings = mappingsResponse.keysIt();
				while (keysItMappings.hasNext()) {
					String key = keysItMappings.next();
					mappingsDoc.put(key, mappingsResponse.get(key).string());
				}
				String mappingsJson = mappingsDoc.get("_doc");
				Document mapping = null;
				if (mappingsJson != null) {
					try {
						mapping = Document.from((Map<String, ? extends Object>) Document.parse(mappingsJson).get("_doc"));
					} catch (Exception e) {
						LOGGER.warn("Got invalid mappings JSON: {}", mappingsJson);
					}
				}

				TemplateData templateData = TemplateData.builder()
						.withIndexPatterns(indexTemplateMetadata.patterns().toArray(new String[0])) //
						.withSettings(settings) //
						.withMapping(mapping) //
						.withAliases(aliases) //
						.withOrder(indexTemplateMetadata.order()) //
						.withVersion(indexTemplateMetadata.version()).build();

				return templateData;
			}
		}

		return null;
	}

	@Override
	public boolean existsTemplate(ExistsTemplateRequest existsTemplateRequest) {

		Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

		// client.admin().indices() has no method for checking the existence
		return getTemplate(new GetTemplateRequest(existsTemplateRequest.getTemplateName())) != null;
	}

	@Override
	public boolean deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {

		Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

		DeleteIndexTemplateRequest deleteIndexTemplateRequest = requestFactory.deleteIndexTemplateRequest(client,
				deleteTemplateRequest);
		return client.admin().indices().deleteTemplate(deleteIndexTemplateRequest).actionGet().isAcknowledged();
	}
}
