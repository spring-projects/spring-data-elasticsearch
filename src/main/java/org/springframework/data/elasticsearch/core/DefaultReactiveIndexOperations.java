/*
 * Copyright 2020 the original author or authors.
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
import static org.springframework.util.StringUtils.*;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 4.1
 */
class DefaultReactiveIndexOperations implements ReactiveIndexOperations {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReactiveIndexOperations.class);

	@Nullable private final Class<?> boundClass;
	private final IndexCoordinates boundIndex;
	private final RequestFactory requestFactory;
	private final ReactiveElasticsearchOperations operations;
	private final ElasticsearchConverter converter;

	public DefaultReactiveIndexOperations(ReactiveElasticsearchOperations operations, IndexCoordinates index) {

		Assert.notNull(operations, "operations must not be null");
		Assert.notNull(index, "index must not be null");

		this.operations = operations;
		this.converter = operations.getElasticsearchConverter();
		this.requestFactory = new RequestFactory(operations.getElasticsearchConverter());
		this.boundClass = null;
		this.boundIndex = index;
	}

	public DefaultReactiveIndexOperations(ReactiveElasticsearchOperations operations, Class<?> clazz) {

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

		String indexName = getIndexCoordinates().getIndexName();

		if (boundClass != null) {
			return createSettings(boundClass).flatMap(settings -> doCreate(indexName, settings));
		} else {
			return doCreate(indexName, null);
		}
	}

	@Override
	public Mono<Boolean> create(Document settings) {
		return doCreate(getIndexCoordinates().getIndexName(), settings);
	}

	private Mono<Boolean> doCreate(String indexName, @Nullable Document settings) {

		CreateIndexRequest request = requestFactory.createIndexRequestReactive(indexName, settings);
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

		GetIndexRequest request = requestFactory.getIndexRequestReactive(getIndexCoordinates().getIndexName());
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

        Mapping mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Mapping.class);

        if (mappingAnnotation != null) {
            return loadDocument(mappingAnnotation.mappingPath(), "@Mapping");
        }

		String mapping = new MappingBuilder(converter).buildPropertyMapping(clazz);
		return Mono.just(Document.parse(mapping));
	}

	@Override
	public Mono<Boolean> putMapping(Mono<Document> mapping) {
		return mapping.map(document -> requestFactory.putMappingRequestReactive(getIndexCoordinates(), document)) //
				.flatMap(request -> Mono.from(operations.executeWithIndicesClient(client -> client.putMapping(request))));
	}

	@Override
	public Mono<Document> getMapping() {

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		GetMappingsRequest request = requestFactory.getMappingRequestReactive(indexCoordinates);

		return Mono.from(operations.executeWithIndicesClient(client -> client.getMapping(request)))
				.flatMap(getMappingsResponse -> {
					Document document = Document.create();
					document.put("properties",
							getMappingsResponse.mappings().get(indexCoordinates.getIndexName()).get("properties").getSourceAsMap());
					return Mono.just(document);
				});
	}
	// endregion

	// region settings

	@Override
	public Mono<Document> createSettings() {
		return createSettings(checkForBoundClass());
	}

	@Override
	public Mono<Document> createSettings(Class<?> clazz) {

        Setting setting = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);

        if (setting != null) {
            return loadDocument(setting.settingPath(), "@Setting");
        }

		return Mono.just(getRequiredPersistentEntity(clazz).getDefaultSettings());
	}

	@Override
	public Mono<Document> getSettings(boolean includeDefaults) {

		String indexName = getIndexCoordinates().getIndexName();
		GetSettingsRequest request = requestFactory.getSettingsRequest(indexName, includeDefaults);

		return Mono.from(operations.executeWithIndicesClient(client -> client.getSettings(request)))
				.map(getSettingsResponse -> requestFactory.fromSettingsResponse(getSettingsResponse, indexName));
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
				.map(GetAliasesResponse::getAliases).map(requestFactory::convertAliasesResponse);
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
						TemplateData templateData = requestFactory.getTemplateData(response, getTemplateRequest.getTemplateName());
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

	private IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return operations.getElasticsearchConverter().getMappingContext().getRequiredPersistentEntity(clazz)
				.getIndexCoordinates();
	}

	private ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return converter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	private Mono<Document> loadDocument(String path, String annotation) {

		if (hasText(path)) {
			return ReactiveResourceUtil.readFileFromClasspath(path).flatMap(s -> {
				if (hasText(s)) {
					return Mono.just(Document.parse(s));
				} else {
					return Mono.just(Document.create());
				}
			});
		} else {
			LOGGER.info("path in {} has to be defined. Using default instead.", annotation);
		}

		return Mono.just(Document.create());
	}

	private Class<?> checkForBoundClass() {
		if (boundClass == null) {
			throw new InvalidDataAccessApiUsageException("IndexOperations are not bound");
		}
		return boundClass;
	}

	// endregion

}
