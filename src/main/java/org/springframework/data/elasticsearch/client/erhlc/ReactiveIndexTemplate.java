/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import static org.elasticsearch.client.Requests.*;
import static org.springframework.util.StringUtils.*;

import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.ReactiveResourceUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ReactiveMappingBuilder;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @author George Popides
 * @since 4.1
 * @deprecated since 5.0
 */
@Deprecated
class ReactiveIndexTemplate implements ReactiveIndexOperations {

	private static final Log LOGGER = LogFactory.getLog(ReactiveIndexTemplate.class);

	@Nullable private final Class<?> boundClass;
	private final IndexCoordinates boundIndex;
	private final RequestFactory requestFactory;
	private final ReactiveElasticsearchOperations operations;
	private final ElasticsearchConverter converter;

	public ReactiveIndexTemplate(ReactiveElasticsearchOperations operations, IndexCoordinates index) {

		Assert.notNull(operations, "operations must not be null");
		Assert.notNull(index, "index must not be null");

		this.operations = operations;
		this.converter = operations.getElasticsearchConverter();
		this.requestFactory = new RequestFactory(operations.getElasticsearchConverter());
		this.boundClass = null;
		this.boundIndex = index;
	}

	public ReactiveIndexTemplate(ReactiveElasticsearchOperations operations, Class<?> clazz) {

		Assert.notNull(operations, "operations must not be null");
		Assert.notNull(clazz, "clazz must not be null");

		this.operations = operations;
		this.converter = operations.getElasticsearchConverter();
		this.requestFactory = new RequestFactory(operations.getElasticsearchConverter());
		this.boundClass = clazz;
		this.boundIndex = getIndexCoordinatesFor(clazz);
	}

	// region index management
	@Override
	public Mono<Boolean> create() {

		IndexCoordinates index = getIndexCoordinates();

		if (boundClass != null) {
			return createSettings(boundClass).flatMap(settings -> doCreate(index, settings, null));
		} else {
			return doCreate(index, new Settings(), null);
		}
	}

	@Override
	public Mono<Boolean> createWithMapping() {
		return createSettings() //
				.flatMap(settings -> //
				createMapping().flatMap(mapping -> //
				doCreate(getIndexCoordinates(), settings, mapping))); //
	}

	@Override
	public Mono<Boolean> create(Map<String, Object> settings) {

		Assert.notNull(settings, "settings must not be null");

		return doCreate(getIndexCoordinates(), settings, null);
	}

	@Override
	public Mono<Boolean> create(Map<String, Object> settings, Document mapping) {

		Assert.notNull(settings, "settings must not be null");
		Assert.notNull(mapping, "mapping must not be null");

		return doCreate(getIndexCoordinates(), settings, mapping);
	}

	private Mono<Boolean> doCreate(IndexCoordinates index, Map<String, Object> settings, @Nullable Document mapping) {

		CreateIndexRequest request = requestFactory.createIndexRequest(index, settings, mapping);
		return Mono.from(operations.executeWithIndicesClient(client -> client.createIndex(request)));
	}

	@Override
	public Mono<Boolean> delete() {

		return exists() //
				.flatMap(exists -> {

					if (exists) {
						DeleteIndexRequest request = requestFactory.deleteIndexRequest(getIndexCoordinates());
						return Mono.from(operations.executeWithIndicesClient(client -> client.deleteIndex(request)))
								.onErrorResume(NoSuchIndexException.class, e -> Mono.just(false));
					} else {
						return Mono.just(false);
					}
				});
	}

	@Override
	public Mono<Boolean> exists() {

		GetIndexRequest request = requestFactory.getIndexRequest(getIndexCoordinates());
		return Mono.from(operations.executeWithIndicesClient(client -> client.existsIndex(request)));
	}

	@Override
	public Mono<Void> refresh() {
		return Mono.from(operations.executeWithIndicesClient(
				client -> client.refreshIndex(refreshRequest(getIndexCoordinates().getIndexNames()))));
	}
	// endregion

	// region mappings
	@Override
	public Mono<Document> createMapping() {
		return createMapping(checkForBoundClass());
	}

	@Override
	public Mono<Document> createMapping(Class<?> clazz) {

		// noinspection DuplicatedCode
		Mapping mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Mapping.class);

		if (mappingAnnotation != null) {
			String mappingPath = mappingAnnotation.mappingPath();

			if (hasText(mappingPath)) {
				return ReactiveResourceUtil.loadDocument(mappingAnnotation.mappingPath(), "@Mapping");
			}
		}

		return new ReactiveMappingBuilder(converter).buildReactivePropertyMapping(clazz).map(Document::parse);
	}

	@Override
	public Mono<Boolean> putMapping(Mono<Document> mapping) {
		return mapping.map(document -> requestFactory.putMappingRequest(getIndexCoordinates(), document)) //
				.flatMap(request -> Mono.from(operations.executeWithIndicesClient(client -> client.putMapping(request))));
	}

	@Override
	public Mono<Document> getMapping() {

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		GetMappingsRequest request = requestFactory.getMappingsRequest(indexCoordinates);

		return Mono.from(operations.executeWithIndicesClient(client -> client.getMapping(request)))
				.flatMap(getMappingsResponse -> {
					Map<String, Object> source = getMappingsResponse.mappings().get(indexCoordinates.getIndexName())
							.getSourceAsMap();
					Document document = Document.from(source);
					return Mono.just(document);
				});
	}
	// endregion

	// region settings

	@Override
	public Mono<Settings> createSettings() {
		return createSettings(checkForBoundClass());
	}

	@Override
	public Mono<Settings> createSettings(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		ElasticsearchPersistentEntity<?> persistentEntity = getRequiredPersistentEntity(clazz);
		String settingPath = persistentEntity.settingPath();
		return hasText(settingPath) //
				? ReactiveResourceUtil.loadDocument(settingPath, "@Setting") //
						.map(Settings::new) //
				: Mono.just(persistentEntity.getDefaultSettings());
	}

	@Override
	public Mono<Settings> getSettings(boolean includeDefaults) {

		String indexName = getIndexCoordinates().getIndexName();
		GetSettingsRequest request = requestFactory.getSettingsRequest(indexName, includeDefaults);

		return Mono.from(operations.executeWithIndicesClient(client -> client.getSettings(request)))
				.map(getSettingsResponse -> ResponseConverter.fromSettingsResponse(getSettingsResponse, indexName));
	}

	// endregion

	// region aliases
	@Override
	public Mono<Boolean> alias(AliasActions aliasActions) {

		IndicesAliasesRequest request = requestFactory.indicesAliasesRequest(aliasActions);
		return Mono.from(operations.executeWithIndicesClient(client -> client.updateAliases(request)));
	}

	@Override
	public Mono<Map<String, Set<AliasData>>> getAliases(String... aliasNames) {
		return getAliases(aliasNames, null);
	}

	@Override
	public Mono<Map<String, Set<AliasData>>> getAliasesForIndex(String... indexNames) {
		return getAliases(null, indexNames);
	}

	private Mono<Map<String, Set<AliasData>>> getAliases(@Nullable String[] aliasNames, @Nullable String[] indexNames) {

		GetAliasesRequest getAliasesRequest = requestFactory.getAliasesRequest(aliasNames, indexNames);
		return Mono.from(operations.executeWithIndicesClient(client -> client.getAliases(getAliasesRequest)))
				.map(GetAliasesResponse::getAliases).map(ResponseConverter::aliasDatas);
	}
	// endregion

	// region templates
	@Override
	public Mono<Boolean> putTemplate(PutTemplateRequest putTemplateRequest) {

		Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

		PutIndexTemplateRequest putIndexTemplateRequest = requestFactory.putIndexTemplateRequest(putTemplateRequest);
		return Mono.from(operations.executeWithIndicesClient(client -> client.putTemplate(putIndexTemplateRequest)));
	}

	@Override
	public Mono<TemplateData> getTemplate(GetTemplateRequest getTemplateRequest) {

		Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

		GetIndexTemplatesRequest getIndexTemplatesRequest = requestFactory.getIndexTemplatesRequest(getTemplateRequest);
		return Mono.from(operations.executeWithIndicesClient(client -> client.getTemplate(getIndexTemplatesRequest)))
				.flatMap(response -> {
					if (response != null) {
						TemplateData templateData = ResponseConverter.getTemplateData(response,
								getTemplateRequest.getTemplateName());
						if (templateData != null) {
							return Mono.just(templateData);
						}
					}
					return Mono.empty();
				});
	}

	@Override
	public Mono<Boolean> existsTemplate(ExistsTemplateRequest existsTemplateRequest) {

		Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

		IndexTemplatesExistRequest indexTemplatesExistRequest = requestFactory
				.indexTemplatesExistsRequest(existsTemplateRequest);
		return Mono.from(operations.executeWithIndicesClient(client -> client.existsTemplate(indexTemplatesExistRequest)));
	}

	@Override
	public Mono<Boolean> deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {

		Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

		DeleteIndexTemplateRequest deleteIndexTemplateRequest = requestFactory
				.deleteIndexTemplateRequest(deleteTemplateRequest);
		return Mono.from(operations.executeWithIndicesClient(client -> client.deleteTemplate(deleteIndexTemplateRequest)));
	}

	// endregion

	// region helper functions
	@Override
	public IndexCoordinates getIndexCoordinates() {
		return (boundClass != null) ? getIndexCoordinatesFor(boundClass) : boundIndex;
	}

	@Override
	public Flux<IndexInformation> getInformation(IndexCoordinates index) {

		Assert.notNull(index, "index must not be null");

		org.elasticsearch.client.indices.GetIndexRequest getIndexRequest = requestFactory.getIndexRequest(index);
		return Mono
				.from(operations.executeWithIndicesClient(
						client -> client.getIndex(getIndexRequest).map(ResponseConverter::getIndexInformations)))
				.flatMapMany(Flux::fromIterable);
	}

	private IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return operations.getElasticsearchConverter().getMappingContext().getRequiredPersistentEntity(clazz)
				.getIndexCoordinates();
	}

	private ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return converter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	private Class<?> checkForBoundClass() {
		if (boundClass == null) {
			throw new InvalidDataAccessApiUsageException("IndexOperations are not bound");
		}
		return boundClass;
	}

	// endregion

}
