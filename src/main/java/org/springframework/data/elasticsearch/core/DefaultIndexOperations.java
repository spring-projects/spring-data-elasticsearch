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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * {@link IndexOperations} implementation using the RestClient.
 * 
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
class DefaultIndexOperations extends AbstractDefaultIndexOperations implements IndexOperations {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIndexOperations.class);

	private final ElasticsearchRestTemplate restTemplate;

	public DefaultIndexOperations(ElasticsearchRestTemplate restTemplate, Class<?> boundClass) {
		super(restTemplate.getElasticsearchConverter(), boundClass);
		this.restTemplate = restTemplate;
	}

	public DefaultIndexOperations(ElasticsearchRestTemplate restTemplate, IndexCoordinates boundIndex) {
		super(restTemplate.getElasticsearchConverter(), boundIndex);
		this.restTemplate = restTemplate;
	}

	@Override
	protected boolean doCreate(IndexCoordinates index, @Nullable Document settings) {
		CreateIndexRequest request = requestFactory.createIndexRequest(index, settings);
		return restTemplate.execute(client -> client.indices().create(request, RequestOptions.DEFAULT).isAcknowledged());
	}

	@Override
	protected boolean doDelete(IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");

		if (doExists(index)) {
			DeleteIndexRequest deleteIndexRequest = requestFactory.deleteIndexRequest(index);
			return restTemplate
					.execute(client -> client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT).isAcknowledged());
		}
		return false;
	}

	@Override
	protected boolean doExists(IndexCoordinates index) {

		GetIndexRequest getIndexRequest = requestFactory.getIndexRequest(index);
		return restTemplate.execute(client -> client.indices().exists(getIndexRequest, RequestOptions.DEFAULT));
	}

	@Override
	protected boolean doPutMapping(IndexCoordinates index, Document mapping) {

		Assert.notNull(index, "No index defined for putMapping()");

		PutMappingRequest request = requestFactory.putMappingRequest(index, mapping);
		return restTemplate
				.execute(client -> client.indices().putMapping(request, RequestOptions.DEFAULT).isAcknowledged());
	}

	@Override
	protected Map<String, Object> doGetMapping(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for getMapping()");

		GetMappingsRequest mappingsRequest = requestFactory.getMappingsRequest(index);

		return restTemplate.execute(client -> {
			Map<String, MappingMetadata> mappings = client.indices() //
					.getMapping(mappingsRequest, RequestOptions.DEFAULT) //
					.mappings(); //

			if (mappings == null || mappings.size() == 0) {
				return Collections.emptyMap();
			}

			if (mappings.size() > 1) {
				LOGGER.warn("more than one mapping returned for " + index.getIndexName());
			}
			// we have at least one, take the first from the iterator
			return mappings.entrySet().iterator().next().getValue().getSourceAsMap();
		});
	}

	@Override
	protected boolean doAddAlias(AliasQuery query, IndexCoordinates index) {

		IndicesAliasesRequest request = requestFactory.indicesAddAliasesRequest(query, index);
		return restTemplate
				.execute(client -> client.indices().updateAliases(request, RequestOptions.DEFAULT).isAcknowledged());
	}

	@Override
	protected boolean doRemoveAlias(AliasQuery query, IndexCoordinates index) {

		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		IndicesAliasesRequest indicesAliasesRequest = requestFactory.indicesRemoveAliasesRequest(query, index);
		return restTemplate.execute(
				client -> client.indices().updateAliases(indicesAliasesRequest, RequestOptions.DEFAULT).isAcknowledged());
	}

	@Override
	protected List<AliasMetadata> doQueryForAlias(IndexCoordinates index) {

		GetAliasesRequest getAliasesRequest = requestFactory.getAliasesRequest(index);

		return restTemplate.execute(client -> {
			GetAliasesResponse alias = client.indices().getAlias(getAliasesRequest, RequestOptions.DEFAULT);
			// we only return data for the first index name that was requested (always have done so)
			String index1 = getAliasesRequest.indices()[0];
			return new ArrayList<>(alias.getAliases().get(index1));
		});
	}

	@Override
	protected Map<String, Set<AliasData>> doGetAliases(@Nullable String[] aliasNames, @Nullable String[] indexNames) {

		GetAliasesRequest getAliasesRequest = requestFactory.getAliasesRequest(aliasNames, indexNames);

		return restTemplate.execute(client -> requestFactory
				.convertAliasesResponse(client.indices().getAlias(getAliasesRequest, RequestOptions.DEFAULT).getAliases()));
	}

	@Override
	public boolean alias(AliasActions aliasActions) {

		IndicesAliasesRequest request = requestFactory.indicesAliasesRequest(aliasActions);
		return restTemplate
				.execute(client -> client.indices().updateAliases(request, RequestOptions.DEFAULT).isAcknowledged());
	}

	@Override
	protected Map<String, Object> doGetSettings(IndexCoordinates index, boolean includeDefaults) {

		Assert.notNull(index, "index must not be null");

		GetSettingsRequest getSettingsRequest = requestFactory.getSettingsRequest(index, includeDefaults);
		GetSettingsResponse response = restTemplate.execute(client -> client.indices() //
				.getSettings(getSettingsRequest, RequestOptions.DEFAULT));

		return requestFactory.fromSettingsResponse(response, getSettingsRequest.indices()[0]);
	}

	@Override
	protected void doRefresh(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for refresh()");

		RefreshRequest refreshRequest = requestFactory.refreshRequest(index);
		restTemplate.execute(client -> client.indices().refresh(refreshRequest, RequestOptions.DEFAULT));
	}

	@Override
	public boolean putTemplate(PutTemplateRequest putTemplateRequest) {

		Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

		PutIndexTemplateRequest putIndexTemplateRequest = requestFactory.putIndexTemplateRequest(putTemplateRequest);
		return restTemplate.execute(
				client -> client.indices().putTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT).isAcknowledged());
	}

	@Override
	public TemplateData getTemplate(GetTemplateRequest getTemplateRequest) {

		Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

		// getIndexTemplate throws an error on non-existing template names
		if (!existsTemplate(new ExistsTemplateRequest(getTemplateRequest.getTemplateName()))) {
			return null;
		}

		GetIndexTemplatesRequest getIndexTemplatesRequest = requestFactory.getIndexTemplatesRequest(getTemplateRequest);
		GetIndexTemplatesResponse getIndexTemplatesResponse = restTemplate
				.execute(client -> client.indices().getIndexTemplate(getIndexTemplatesRequest, RequestOptions.DEFAULT));
		return requestFactory.getTemplateData(getIndexTemplatesResponse, getTemplateRequest.getTemplateName());
	}

	@Override
	public boolean existsTemplate(ExistsTemplateRequest existsTemplateRequest) {

		Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

		IndexTemplatesExistRequest putIndexTemplateRequest = requestFactory
				.indexTemplatesExistsRequest(existsTemplateRequest);
		return restTemplate
				.execute(client -> client.indices().existsTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT));
	}

	@Override
	public boolean deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {

		Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

		DeleteIndexTemplateRequest deleteIndexTemplateRequest = requestFactory
				.deleteIndexTemplateRequest(deleteTemplateRequest);
		return restTemplate.execute(
				client -> client.indices().deleteTemplate(deleteIndexTemplateRequest, RequestOptions.DEFAULT).isAcknowledged());
	}

	// endregion
}
