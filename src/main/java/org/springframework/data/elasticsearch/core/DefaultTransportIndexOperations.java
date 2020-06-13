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

import static org.elasticsearch.client.Requests.*;

import java.util.List;
import java.util.Map;

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
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
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

		return client.admin().indices().getMappings( //
				mappingsRequest).actionGet() //
				.getMappings().get(mappingsRequest.indices()[0]).get(IndexCoordinates.TYPE) //
				.getSourceAsMap();
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
	protected List<AliasMetaData> doQueryForAlias(IndexCoordinates index) {

		GetAliasesRequest getAliasesRequest = requestFactory.getAliasesRequest(index);
		return client.admin().indices().getAliases(getAliasesRequest).actionGet().getAliases().get(index.getIndexName());
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

}
