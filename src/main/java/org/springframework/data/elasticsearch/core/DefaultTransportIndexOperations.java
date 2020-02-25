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
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.elasticsearch.ElasticsearchException;
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
	protected boolean doCreate(String indexName, @Nullable Document settings) {
		CreateIndexRequestBuilder createIndexRequestBuilder = requestFactory.createIndexRequestBuilder(client, indexName,
				settings);
		return createIndexRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	protected boolean doDelete(String indexName) {

		Assert.notNull(indexName, "No index defined for delete operation");

		if (doExists(indexName)) {
			return client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet().isAcknowledged();
		}
		return false;
	}

	@Override
	protected boolean doExists(String indexName) {
		return client.admin().indices().exists(indicesExistsRequest(indexName)).actionGet().isExists();
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

		try {
			return client.admin().indices().getMappings( //
					new GetMappingsRequest().indices(index.getIndexNames())).actionGet() //
					.getMappings().get(index.getIndexName()).get(IndexCoordinates.TYPE) //
					.getSourceAsMap();
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + index.getIndexName(), e);
		}
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

		return client.admin().indices().prepareAliases().removeAlias(index.getIndexName(), query.getAliasName()).execute()
				.actionGet().isAcknowledged();
	}

	@Override
	protected List<AliasMetaData> doQueryForAlias(String indexName) {
		return client.admin().indices().getAliases(new GetAliasesRequest().indices(indexName)).actionGet().getAliases()
				.get(indexName);
	}

	@Override
	protected Map<String, Object> doGetSettings(String indexName, boolean includeDefaults) {

		Assert.notNull(indexName, "No index defined for getSettings");

		GetSettingsRequest request = new GetSettingsRequest() //
				.indices(indexName) //
				.includeDefaults(includeDefaults);

		GetSettingsResponse response = client.admin() //
				.indices() //
				.getSettings(request) //
				.actionGet();

		return convertSettingsResponseToMap(response, indexName);
	}

	@Override
	protected void doRefresh(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for refresh()");

		client.admin().indices().refresh(refreshRequest(index.getIndexNames())).actionGet();
	}

}
