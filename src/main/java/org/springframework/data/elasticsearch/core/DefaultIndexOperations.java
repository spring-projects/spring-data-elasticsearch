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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.client.support.AliasData;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link IndexOperations} implementation using the RestClient.
 * 
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
class DefaultIndexOperations extends AbstractDefaultIndexOperations implements IndexOperations {

	private RestHighLevelClient client;

	public DefaultIndexOperations(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {
		super(elasticsearchConverter);
		this.client = client;
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {
		CreateIndexRequest request = requestFactory.createIndexRequest(indexName, settings);
		try {
			return client.indices().create(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Error for creating index: " + request.toString(), e);
		}
	}

	@Override
	public boolean deleteIndex(String indexName) {

		Assert.notNull(indexName, "No index defined for delete operation");

		if (indexExists(indexName)) {
			DeleteIndexRequest request = new DeleteIndexRequest(indexName);
			try {
				return client.indices().delete(request, RequestOptions.DEFAULT).isAcknowledged();
			} catch (IOException e) {
				throw new ElasticsearchException("Error while deleting index request: " + request.toString(), e);
			}
		}
		return false;
	}

	@Override
	public boolean indexExists(String indexName) {
		GetIndexRequest request = new GetIndexRequest(indexName);
		try {
			return client.indices().exists(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("Error while for indexExists request: " + request.toString(), e);
		}
	}

	@Override
	public boolean putMapping(IndexCoordinates index, Object mapping) {

		Assert.notNull(index, "No index defined for putMapping()");

		PutMappingRequest request = requestFactory.putMappingRequest(index, mapping);
		try {
			return client.indices().putMapping(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("Failed to put mapping for " + index.getIndexName(), e);
		}
	}

	@Override
	public Map<String, Object> getMapping(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for getMapping()");

		RestClient restClient = client.getLowLevelClient();
		try {
			Request request = new Request("GET",
					'/' + index.getIndexName() + "/_mapping/" + index.getTypeName() + "?include_type_name=true");
			Response response = restClient.performRequest(request);
			return convertMappingResponse(EntityUtils.toString(response.getEntity()), index.getTypeName());
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + index.getIndexName()
					+ " type : " + index.getTypeName() + ' ', e);
		}
	}

	@Override
	public boolean addAlias(AliasQuery query, IndexCoordinates index) {
		IndicesAliasesRequest request = requestFactory.indicesAddAliasesRequest(query, index);
		try {
			return client.indices().updateAliases(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException("failed to update aliases with request: " + request, e);
		}
	}

	@Override
	public boolean removeAlias(AliasQuery query, IndexCoordinates index) {

		Assert.notNull(index, "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");

		IndicesAliasesRequest indicesAliasesRequest = requestFactory.indicesRemoveAliasesRequest(query, index);
		try {
			return client.indices().updateAliases(indicesAliasesRequest, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new ElasticsearchException(
					"failed to update aliases with indicesRemoveAliasesRequest: " + indicesAliasesRequest, e);
		}
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		List<AliasMetaData> aliases = null;
		RestClient restClient = client.getLowLevelClient();
		Response response;
		String aliasResponse;

		try {
			response = restClient.performRequest(new Request("GET", '/' + indexName + "/_alias/*"));
			aliasResponse = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + indexName, e);
		}

		return convertAliasResponse(aliasResponse);
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

		try {
			GetSettingsResponse response = client.indices() //
					.getSettings(request, RequestOptions.DEFAULT);

			return convertSettingsResponseToMap(response, indexName);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to get settings for index: " + indexName, e);
		}
	}

	@Override
	public void refresh(IndexCoordinates index) {

		Assert.notNull(index, "No index defined for refresh()");

		try {
			client.indices().refresh(refreshRequest(index.getIndexNames()), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to refresh index: " + index, e);
		}
	}

	// region Helper methods
	private Map<String, Object> convertMappingResponse(String mappingResponse, String type) {
		ObjectMapper mapper = new ObjectMapper();

		try {
			Map<String, Object> result = null;
			JsonNode node = mapper.readTree(mappingResponse);

			node = node.findValue("mappings").findValue(type);
			result = mapper.readValue(mapper.writeValueAsString(node), HashMap.class);

			return result;
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + mappingResponse, e);
		}
	}

	/**
	 * It takes two steps to create a List<AliasMetadata> from the elasticsearch http response because the aliases field
	 * is actually a Map by alias name, but the alias name is on the AliasMetadata.
	 *
	 * @param aliasResponse
	 * @return
	 */
	private List<AliasMetaData> convertAliasResponse(String aliasResponse) {
		ObjectMapper mapper = new ObjectMapper();

		try {
			JsonNode node = mapper.readTree(aliasResponse);
			node = node.findValue("aliases");

			if (node == null) {
				return Collections.emptyList();
			}

			Map<String, AliasData> aliasData = mapper.readValue(mapper.writeValueAsString(node),
					new TypeReference<Map<String, AliasData>>() {});

			Iterable<Map.Entry<String, AliasData>> aliasIter = aliasData.entrySet();
			List<AliasMetaData> aliasMetaDataList = new ArrayList<AliasMetaData>();

			for (Map.Entry<String, AliasData> aliasentry : aliasIter) {
				AliasData data = aliasentry.getValue();
				aliasMetaDataList.add(AliasMetaData.newAliasMetaDataBuilder(aliasentry.getKey()).filter(data.getFilter())
						.routing(data.getRouting()).searchRouting(data.getSearch_routing()).indexRouting(data.getIndex_routing())
						.build());
			}
			return aliasMetaDataList;
		} catch (IOException e) {
			throw new ElasticsearchException("Could not map alias response : " + aliasResponse, e);
		}
	}

	// endregion
}
