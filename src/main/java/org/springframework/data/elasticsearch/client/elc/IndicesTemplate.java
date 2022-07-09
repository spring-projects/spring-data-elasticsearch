/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static org.springframework.util.StringUtils.*;

import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.endpoints.BooleanResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ResourceUtil;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link IndexOperations} interface using en {@link ElasticsearchIndicesClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class IndicesTemplate extends ChildTemplate<ElasticsearchIndicesClient> implements IndexOperations {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndicesTemplate.class);

	protected final ElasticsearchConverter elasticsearchConverter;
	@Nullable protected final Class<?> boundClass;
	@Nullable protected final IndexCoordinates boundIndex;

	public IndicesTemplate(ElasticsearchIndicesClient client, ElasticsearchConverter elasticsearchConverter,
			Class<?> boundClass) {
		super(client, elasticsearchConverter);

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null");
		Assert.notNull(boundClass, "boundClass may not be null");

		this.elasticsearchConverter = elasticsearchConverter;
		this.boundClass = boundClass;
		this.boundIndex = null;

	}

	public IndicesTemplate(ElasticsearchIndicesClient client, ElasticsearchConverter elasticsearchConverter,
			IndexCoordinates boundIndex) {
		super(client, elasticsearchConverter);

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null");
		Assert.notNull(boundIndex, "boundIndex must not be null");

		this.elasticsearchConverter = elasticsearchConverter;
		this.boundClass = null;
		this.boundIndex = boundIndex;

	}

	protected Class<?> checkForBoundClass() {
		if (boundClass == null) {
			throw new InvalidDataAccessApiUsageException("IndexOperations are not bound");
		}
		return boundClass;
	}

	@Override
	public boolean create() {

		Settings settings = boundClass != null ? createSettings(boundClass) : new Settings();
		return doCreate(getIndexCoordinates(), settings, null);
	}

	@Override
	public boolean create(Map<String, Object> settings) {

		Assert.notNull(settings, "settings must not be null");

		return doCreate(getIndexCoordinates(), settings, null);
	}

	@Override
	public boolean create(Map<String, Object> settings, Document mapping) {

		Assert.notNull(settings, "settings must not be null");
		Assert.notNull(mapping, "mapping must not be null");

		return doCreate(getIndexCoordinates(), settings, mapping);
	}

	@Override
	public boolean createWithMapping() {
		return doCreate(getIndexCoordinates(), createSettings(), createMapping());
	}

	protected boolean doCreate(IndexCoordinates indexCoordinates, Map<String, Object> settings,
			@Nullable Document mapping) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");
		Assert.notNull(settings, "settings must not be null");

		CreateIndexRequest createIndexRequest = requestConverter.indicesCreateRequest(indexCoordinates, settings, mapping);
		CreateIndexResponse createIndexResponse = execute(client -> client.create(createIndexRequest));
		return Boolean.TRUE.equals(createIndexResponse.acknowledged());
	}

	@Override
	public boolean delete() {
		return doDelete(getIndexCoordinates());
	}

	private boolean doDelete(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		if (doExists(indexCoordinates)) {
			DeleteIndexRequest deleteIndexRequest = requestConverter.indicesDeleteRequest(indexCoordinates);
			DeleteIndexResponse deleteIndexResponse = execute(client -> client.delete(deleteIndexRequest));
			return deleteIndexResponse.acknowledged();
		}

		return false;
	}

	@Override
	public boolean exists() {
		return doExists(getIndexCoordinates());
	}

	private boolean doExists(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		ExistsRequest existsRequest = requestConverter.indicesExistsRequest(indexCoordinates);
		BooleanResponse existsResponse = execute(client -> client.exists(existsRequest));
		return existsResponse.value();
	}

	@Override
	public void refresh() {

		RefreshRequest refreshRequest = requestConverter.indicesRefreshRequest(getIndexCoordinates());
		execute(client -> client.refresh(refreshRequest));
	}

	@Override
	public Document createMapping() {
		return createMapping(checkForBoundClass());
	}

	@Override
	public Document createMapping(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		// load mapping specified in Mapping annotation if present
		// noinspection DuplicatedCode
		Mapping mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Mapping.class);

		if (mappingAnnotation != null) {
			String mappingPath = mappingAnnotation.mappingPath();

			if (hasText(mappingPath)) {
				String mappings = ResourceUtil.readFileFromClasspath(mappingPath);

				if (hasText(mappings)) {
					return Document.parse(mappings);
				}
			}
		}

		// build mapping from field annotations
		try {
			String mapping = new MappingBuilder(elasticsearchConverter).buildPropertyMapping(clazz);
			return Document.parse(mapping);
		} catch (Exception e) {
			throw new UncategorizedElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}

	@Override
	public boolean putMapping(Document mapping) {

		Assert.notNull(mapping, "mapping must not be null");

		PutMappingRequest putMappingRequest = requestConverter.indicesPutMappingRequest(getIndexCoordinates(), mapping);
		PutMappingResponse putMappingResponse = execute(client -> client.putMapping(putMappingRequest));
		return putMappingResponse.acknowledged();
	}

	@Override
	public Map<String, Object> getMapping() {

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		GetMappingRequest getMappingRequest = requestConverter.indicesGetMappingRequest(indexCoordinates);
		GetMappingResponse getMappingResponse = execute(client -> client.getMapping(getMappingRequest));

		Document mappingResponse = responseConverter.indicesGetMapping(getMappingResponse, indexCoordinates);
		return mappingResponse;
	}

	@Override
	public Settings createSettings() {
		return createSettings(checkForBoundClass());
	}

	@Override
	public Settings createSettings(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		ElasticsearchPersistentEntity<?> persistentEntity = getRequiredPersistentEntity(clazz);
		String settingPath = persistentEntity.settingPath();
		return hasText(settingPath) //
				? Settings.parse(ResourceUtil.readFileFromClasspath(settingPath)) //
				: persistentEntity.getDefaultSettings();

	}

	@Override
	public Settings getSettings() {
		return getSettings(false);
	}

	@Override
	public Settings getSettings(boolean includeDefaults) {

		GetIndicesSettingsRequest getIndicesSettingsRequest = requestConverter
				.indicesGetSettingsRequest(getIndexCoordinates(), includeDefaults);
		GetIndicesSettingsResponse getIndicesSettingsResponse = execute(
				client -> client.getSettings(getIndicesSettingsRequest));
		return responseConverter.indicesGetSettings(getIndicesSettingsResponse, getIndexCoordinates().getIndexName());
	}

	@Override
	public boolean alias(AliasActions aliasActions) {

		Assert.notNull(aliasActions, "aliasActions must not be null");

		UpdateAliasesRequest updateAliasesRequest = requestConverter.indicesUpdateAliasesRequest(aliasActions);
		UpdateAliasesResponse updateAliasesResponse = execute(client -> client.updateAliases(updateAliasesRequest));
		return updateAliasesResponse.acknowledged();
	}

	@Override
	public Map<String, Set<AliasData>> getAliases(String... aliasNames) {

		Assert.notNull(aliasNames, "aliasNames must not be null");

		GetAliasRequest getAliasRequest = requestConverter.indicesGetAliasRequest(aliasNames, null);
		var getAliasResponse = execute(client -> client.getAlias(getAliasRequest));
		return responseConverter.indicesGetAliasData(getAliasResponse);
	}

	@Override
	public Map<String, Set<AliasData>> getAliasesForIndex(String... indexNames) {

		Assert.notNull(indexNames, "indexNames must not be null");

		GetAliasRequest getAliasRequest = requestConverter.indicesGetAliasRequest(null, indexNames);
		var getAliasResponse = execute(client -> client.getAlias(getAliasRequest));
		return responseConverter.indicesGetAliasData(getAliasResponse);
	}

	@Override
	public boolean putTemplate(PutTemplateRequest putTemplateRequest) {

		Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.PutTemplateRequest putTemplateRequestES = requestConverter
				.indicesPutTemplateRequest(putTemplateRequest);
		return execute(client -> client.putTemplate(putTemplateRequestES)).acknowledged();
	}

	@Override
	public TemplateData getTemplate(GetTemplateRequest getTemplateRequest) {

		Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.GetTemplateRequest getTemplateRequestES = requestConverter
				.indicesGetTemplateRequest(getTemplateRequest);
		GetTemplateResponse getTemplateResponse = execute(client -> client.getTemplate(getTemplateRequestES));

		return responseConverter.indicesGetTemplateData(getTemplateResponse, getTemplateRequest.getTemplateName());
	}

	@Override
	public boolean existsTemplate(ExistsTemplateRequest existsTemplateRequest) {

		Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.ExistsTemplateRequest existsTemplateRequestSO = requestConverter
				.indicesExistsTemplateRequest(existsTemplateRequest);
		return execute(client -> client.existsTemplate(existsTemplateRequestSO)).value();
	}

	@Override
	public boolean deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {

		Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.DeleteTemplateRequest deleteTemplateRequestES = requestConverter
				.indicesDeleteTemplateRequest(deleteTemplateRequest);
		return execute(client -> client.deleteTemplate(deleteTemplateRequestES)).acknowledged();
	}

	@Override
	public List<IndexInformation> getInformation(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		GetIndexRequest getIndexRequest = requestConverter.indicesGetIndexRequest(indexCoordinates);
		GetIndexResponse getIndexResponse = execute(client -> client.get(getIndexRequest));
		return responseConverter.indicesGetIndexInformations(getIndexResponse);
	}

	// region Helper functions
	ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	@Override
	public IndexCoordinates getIndexCoordinates() {
		return (boundClass != null) ? getIndexCoordinatesFor(boundClass) : Objects.requireNonNull(boundIndex);
	}

	public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return getRequiredPersistentEntity(clazz).getIndexCoordinates();
	}
	// endregion
}
