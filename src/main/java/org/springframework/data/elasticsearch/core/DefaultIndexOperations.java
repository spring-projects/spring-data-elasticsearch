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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
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
			GetMappingsResponse mapping = client.indices().getMapping(mappingsRequest, RequestOptions.DEFAULT);
			// we only return data for the first index name that was requested (always have done so)
			String index1 = mappingsRequest.indices()[0];
			Map<String, Object> result = new LinkedHashMap<>();
			return mapping.mappings().get(index1).getSourceAsMap();
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

		return convertSettingsResponseToMap(response, getSettingsRequest.indices()[0]);
	}

	@Override
	protected void doRefresh(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for refresh()");

		RefreshRequest refreshRequest = requestFactory.refreshRequest(index);
		restTemplate.execute(client -> client.indices().refresh(refreshRequest, RequestOptions.DEFAULT));
	}

	// endregion
}
