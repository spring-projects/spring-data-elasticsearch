/*
 * Copyright 2019 the original author or authors.
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
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
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

	public DefaultTransportIndexOperations(Client client, ElasticsearchConverter elasticsearchConverter) {
		super(elasticsearchConverter);
		this.client = client;
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {
		CreateIndexRequestBuilder createIndexRequestBuilder = requestFactory.createIndexRequestBuilder(client, indexName,
				settings);
		return createIndexRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	public boolean deleteIndex(String indexName) {

		Assert.notNull(indexName, "No index defined for delete operation");

		if (indexExists(indexName)) {
			return client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet().isAcknowledged();
		}
		return false;
	}

	@Override
	public boolean indexExists(String indexName) {
		return client.admin().indices().exists(indicesExistsRequest(indexName)).actionGet().isExists();
	}

	@Override
	public boolean putMapping(IndexCoordinates index, Object mapping) {

		Assert.notNull(index, "No index defined for putMapping()");

		PutMappingRequestBuilder requestBuilder = requestFactory.putMappingRequestBuilder(client, index, mapping);
		return requestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	public Map<String, Object> getMapping(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for putMapping()");

		try {
			return client.admin().indices()
					.getMappings(new GetMappingsRequest().indices(index.getIndexNames()).types(index.getTypeNames())).actionGet()
					.getMappings().get(index.getIndexName()).get(index.getTypeName()).getSourceAsMap();
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + index.getIndexName()
					+ " type : " + index.getTypeName() + ' ' + e.getMessage());
		}
	}

	@Override
	public boolean addAlias(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest.AliasActions aliasAction = requestFactory.aliasAction(query, index);
		return client.admin().indices().prepareAliases().addAliasAction(aliasAction).execute().actionGet().isAcknowledged();
	}

	@Override
	public boolean removeAlias(AliasQuery query, IndexCoordinates index) {

		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		return client.admin().indices().prepareAliases().removeAlias(index.getIndexName(), query.getAliasName()).execute()
				.actionGet().isAcknowledged();
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		return client.admin().indices().getAliases(new GetAliasesRequest().indices(indexName)).actionGet().getAliases()
				.get(indexName);
	}

	@Override
	public Map<String, Object> getSettings(String indexName) {
		return getSettings(indexName, false);
	}

	@Override
	public Map<String, Object> getSettings(String indexName, boolean includeDefaults) {

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
	public void refresh(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for refresh()");

		client.admin().indices().refresh(refreshRequest(index.getIndexNames())).actionGet();
	}

}
