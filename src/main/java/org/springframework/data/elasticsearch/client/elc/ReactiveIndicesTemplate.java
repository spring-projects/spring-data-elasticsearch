/*
 * Copyright 2021-2024 the original author or authors.
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

import co.elastic.clients.elasticsearch._types.AcknowledgedResponseBase;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.ReactiveResourceUtil;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.index.DeleteIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.mapping.Alias;
import org.springframework.data.elasticsearch.core.mapping.CreateIndexSettings;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 */
public class ReactiveIndicesTemplate
		extends ReactiveChildTemplate<ElasticsearchTransport, ReactiveElasticsearchIndicesClient>
		implements ReactiveIndexOperations {

	// we need a cluster client as well because ES has put some methods from the indices API into the cluster client
	// (component templates)
	private final ReactiveClusterTemplate clusterTemplate;

	@Nullable private final Class<?> boundClass;
	private final IndexCoordinates boundIndexCoordinates;

	public ReactiveIndicesTemplate(ReactiveElasticsearchIndicesClient client, ReactiveClusterTemplate clusterTemplate,
			ElasticsearchConverter elasticsearchConverter, IndexCoordinates index) {

		super(client, elasticsearchConverter);

		Assert.notNull(index, "index must not be null");
		Assert.notNull(clusterTemplate, "clusterTemplate must not be null");

		this.clusterTemplate = clusterTemplate;
		this.boundClass = null;
		this.boundIndexCoordinates = index;
	}

	public ReactiveIndicesTemplate(ReactiveElasticsearchIndicesClient client, ReactiveClusterTemplate clusterTemplate,
			ElasticsearchConverter elasticsearchConverter, Class<?> clazz) {

		super(client, elasticsearchConverter);

		Assert.notNull(clazz, "clazz must not be null");
		Assert.notNull(clusterTemplate, "clusterTemplate must not be null");

		this.clusterTemplate = clusterTemplate;
		this.boundClass = clazz;
		this.boundIndexCoordinates = getIndexCoordinatesFor(clazz);
	}

	@Override
	public Mono<Boolean> create() {

		IndexCoordinates indexCoordinates = getIndexCoordinates();

		if (boundClass != null) {
			return createSettings(boundClass).flatMap(settings -> doCreate(indexCoordinates, settings, null));
		} else {
			return doCreate(indexCoordinates, new Settings(), null);
		}
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

	@Override
	public Mono<Boolean> createWithMapping() {
		return createSettings() //
				.flatMap(settings -> //
				createMapping().flatMap(mapping -> //
				doCreate(getIndexCoordinates(), settings, mapping))); //
	}

	private Mono<Boolean> doCreate(IndexCoordinates indexCoordinates, Map<String, Object> settings,
			@Nullable Document mapping) {
		Set<Alias> aliases = (boundClass != null) ? getAliasesFor(boundClass) : new HashSet<>();
		CreateIndexSettings indexSettings = CreateIndexSettings.builder(indexCoordinates)
				.withAliases(aliases)
				.withSettings(settings)
				.withMapping(mapping)
				.build();

		CreateIndexRequest createIndexRequest = requestConverter.indicesCreateRequest(indexSettings);
		Mono<CreateIndexResponse> createIndexResponse = Mono.from(execute(client -> client.create(createIndexRequest)));
		return createIndexResponse.map(CreateIndexResponse::acknowledged);
	}

	@Override
	public Mono<Boolean> delete() {
		return exists().flatMap(exists -> {

			if (exists) {
				DeleteIndexRequest deleteIndexRequest = requestConverter.indicesDeleteRequest(getIndexCoordinates());
				return Mono.from(execute(client -> client.delete(deleteIndexRequest))) //
						.map(DeleteIndexResponse::acknowledged) //
						.onErrorResume(NoSuchIndexException.class, e -> Mono.just(false));
			} else {
				return Mono.just(false);
			}
		});

	}

	@Override
	public Mono<Boolean> exists() {

		ExistsRequest existsRequest = requestConverter.indicesExistsRequest(getIndexCoordinates());
		Mono<BooleanResponse> existsResponse = Mono.from(execute(client -> client.exists(existsRequest)));
		return existsResponse.map(BooleanResponse::value);
	}

	@Override
	public Mono<Void> refresh() {
		RefreshRequest refreshRequest = requestConverter.indicesRefreshRequest(getIndexCoordinates());
		return Mono.from(execute(client -> client.refresh(refreshRequest))).then();
	}

	@Override
	public Mono<Document> createMapping() {
		return createMapping(checkForBoundClass());
	}

	@Override
	public Mono<Document> createMapping(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		Mapping mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Mapping.class);

		if (mappingAnnotation != null) {
			String mappingPath = mappingAnnotation.mappingPath();

			if (hasText(mappingPath)) {
				return ReactiveResourceUtil.loadDocument(mappingAnnotation.mappingPath(), "@Mapping");
			}
		}

		return new ReactiveMappingBuilder(elasticsearchConverter).buildReactivePropertyMapping(clazz).map(Document::parse);
	}

	@Override
	public Mono<Boolean> putMapping(Mono<Document> mapping) {

		Assert.notNull(mapping, "mapping must not be null");

		Mono<PutMappingResponse> putMappingResponse = mapping
				.map(document -> requestConverter.indicesPutMappingRequest(getIndexCoordinates(), document)) //
				.flatMap(putMappingRequest -> Mono.from(client.putMapping(putMappingRequest)));
		return putMappingResponse.map(PutMappingResponse::acknowledged);
	}

	@Override
	public Mono<Document> getMapping() {

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		GetMappingRequest getMappingRequest = requestConverter.indicesGetMappingRequest(indexCoordinates);
		Mono<GetMappingResponse> getMappingResponse = Mono.from(execute(client -> client.getMapping(getMappingRequest)));
		return getMappingResponse.map(response -> responseConverter.indicesGetMapping(response, indexCoordinates));
	}

	@Override
	public Mono<Settings> createSettings() {
		return createSettings(checkForBoundClass());
	}

	@Override
	public Mono<Settings> createSettings(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		ElasticsearchPersistentEntity<?> persistentEntity = elasticsearchConverter.getMappingContext()
				.getRequiredPersistentEntity(clazz);
		String settingPath = persistentEntity.settingPath();
		return hasText(settingPath) //
				? ReactiveResourceUtil.loadDocument(settingPath, "@Setting") //
						.map(Settings::new) //
				: Mono.just(persistentEntity.getDefaultSettings());
	}

	@Override
	public Mono<Settings> getSettings(boolean includeDefaults) {

		GetIndicesSettingsRequest getSettingsRequest = requestConverter.indicesGetSettingsRequest(getIndexCoordinates(),
				includeDefaults);
		Mono<GetIndicesSettingsResponse> getSettingsResponse = Mono
				.from(execute(client -> client.getSettings(getSettingsRequest)));
		return getSettingsResponse
				.map(response -> responseConverter.indicesGetSettings(response, getIndexCoordinates().getIndexName()));
	}

	@Override
	public Mono<Boolean> alias(AliasActions aliasActions) {

		Assert.notNull(aliasActions, "aliasActions must not be null");

		UpdateAliasesRequest updateAliasesRequest = requestConverter.indicesUpdateAliasesRequest(aliasActions);
		Mono<UpdateAliasesResponse> updateAliasesResponse = Mono
				.from(execute(client -> client.updateAliases(updateAliasesRequest)));
		return updateAliasesResponse.map(AcknowledgedResponseBase::acknowledged);
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

		GetAliasRequest getAliasRequest = requestConverter.indicesGetAliasRequest(aliasNames, indexNames);
		Mono<GetAliasResponse> getAliasResponse = Mono.from(execute(client -> client.getAlias(getAliasRequest)));
		return getAliasResponse.map(responseConverter::indicesGetAliasData);
	}

	@Override
	public Mono<Boolean> putTemplate(PutTemplateRequest putTemplateRequest) {

		Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.PutTemplateRequest putTemplateRequestES = requestConverter
				.indicesPutTemplateRequest(putTemplateRequest);
		Mono<PutTemplateResponse> putTemplateResponse = Mono
				.from(execute(client -> client.putTemplate(putTemplateRequestES)));
		return putTemplateResponse.map(PutTemplateResponse::acknowledged);
	}

	@Override
	public Mono<Boolean> putComponentTemplate(PutComponentTemplateRequest putComponentTemplateRequest) {

		Assert.notNull(putComponentTemplateRequest, "putComponentTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.cluster.PutComponentTemplateRequest putComponentTemplateRequestES = requestConverter
				.clusterPutComponentTemplateRequest(putComponentTemplateRequest);
		// the new Elasticsearch client has this call in the cluster index
		return Mono.from(clusterTemplate.execute(client -> client.putComponentTemplate(putComponentTemplateRequestES)))
				.map(AcknowledgedResponseBase::acknowledged);
	}

	@Override
	public Flux<TemplateResponse> getComponentTemplate(GetComponentTemplateRequest getComponentTemplateRequest) {

		Assert.notNull(getComponentTemplateRequest, "getComponentTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.cluster.GetComponentTemplateRequest getComponentTemplateRequestES = requestConverter
				.clusterGetComponentTemplateRequest(getComponentTemplateRequest);
		return Flux.from(clusterTemplate.execute(client -> client.getComponentTemplate(getComponentTemplateRequestES)))
				.flatMapIterable(responseConverter::clusterGetComponentTemplates);
	}

	@Override
	public Mono<Boolean> existsComponentTemplate(ExistsComponentTemplateRequest existsComponentTemplateRequest) {

		Assert.notNull(existsComponentTemplateRequest, "existsComponentTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.cluster.ExistsComponentTemplateRequest existsComponentTemplateRequestES = requestConverter
				.clusterExistsComponentTemplateRequest(existsComponentTemplateRequest);

		return Mono
				.from(clusterTemplate.execute(client -> client.existsComponentTemplate(existsComponentTemplateRequestES)))
				.map(BooleanResponse::value);
	}

	@Override
	public Mono<Boolean> deleteComponentTemplate(DeleteComponentTemplateRequest deleteComponentTemplateRequest) {

		Assert.notNull(deleteComponentTemplateRequest, "deleteComponentTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.cluster.DeleteComponentTemplateRequest deleteComponentTemplateRequestES = requestConverter
				.clusterDeleteComponentTemplateRequest(deleteComponentTemplateRequest);
		return Mono
				.from(clusterTemplate.execute(client -> client.deleteComponentTemplate(deleteComponentTemplateRequestES)))
				.map(AcknowledgedResponseBase::acknowledged);
	}

	@Override
	public Mono<Boolean> putIndexTemplate(PutIndexTemplateRequest putIndexTemplateRequest) {

		Assert.notNull(putIndexTemplateRequest, "putIndexTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest putIndexTemplateRequestES = requestConverter
				.indicesPutIndexTemplateRequest(putIndexTemplateRequest);

		return Mono.from(execute(client -> client.putIndexTemplate(putIndexTemplateRequestES)))
				.map(PutIndexTemplateResponse::acknowledged);
	}

	@Override
	public Mono<Boolean> existsIndexTemplate(ExistsIndexTemplateRequest existsIndexTemplateRequest) {

		Assert.notNull(existsIndexTemplateRequest, "existsIndexTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.ExistsIndexTemplateRequest existsIndexTemplateRequestES = requestConverter
				.indicesExistsIndexTemplateRequest(existsIndexTemplateRequest);
		return Mono.from(execute(client -> client.existsIndexTemplate(existsIndexTemplateRequestES)))
				.map(BooleanResponse::value);
	}

	@Override
	public Flux<TemplateResponse> getIndexTemplate(GetIndexTemplateRequest getIndexTemplateRequest) {

		Assert.notNull(getIndexTemplateRequest, "getIndexTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.GetIndexTemplateRequest getIndexTemplateRequestES = requestConverter
				.indicesGetIndexTemplateRequest(getIndexTemplateRequest);
		return Mono.from(execute(client -> client.getIndexTemplate(getIndexTemplateRequestES)))
				.flatMapIterable(responseConverter::getIndexTemplates);
	}

	@Override
	public Mono<Boolean> deleteIndexTemplate(DeleteIndexTemplateRequest deleteIndexTemplateRequest) {

		Assert.notNull(deleteIndexTemplateRequest, "deleteIndexTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.DeleteIndexTemplateRequest deleteIndexTemplateRequestES = requestConverter
				.indicesDeleteIndexTemplateRequest(deleteIndexTemplateRequest);
		return Mono.from(execute(client -> client.deleteIndexTemplate(deleteIndexTemplateRequestES)))
				.map(AcknowledgedResponseBase::acknowledged);
	}

	@Override
	public Mono<TemplateData> getTemplate(GetTemplateRequest getTemplateRequest) {

		Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.GetTemplateRequest getTemplateRequestES = requestConverter
				.indicesGetTemplateRequest(getTemplateRequest);
		Mono<GetTemplateResponse> getTemplateResponse = Mono
				.from(execute(client -> client.getTemplate(getTemplateRequestES)));

		return getTemplateResponse.flatMap(response -> {
			if (response != null) {
				TemplateData templateData = responseConverter.indicesGetTemplateData(response,
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

		co.elastic.clients.elasticsearch.indices.ExistsTemplateRequest existsTemplateRequestES = requestConverter
				.indicesExistsTemplateRequest(existsTemplateRequest);
		return Mono.from(execute(client -> client.existsTemplate(existsTemplateRequestES))).map(BooleanResponse::value);
	}

	@Override
	public Mono<Boolean> deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {

		Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

		co.elastic.clients.elasticsearch.indices.DeleteTemplateRequest deleteTemplateRequestES = requestConverter
				.indicesDeleteTemplateRequest(deleteTemplateRequest);
		return Mono.from(execute(client -> client.deleteTemplate(deleteTemplateRequestES)))
				.map(DeleteTemplateResponse::acknowledged);
	}

	@Override
	public Flux<IndexInformation> getInformation(IndexCoordinates index) {

		GetIndexRequest request = requestConverter.indicesGetIndexRequest(index);

		return Mono.from(execute(client -> client.get(request))) //
				.map(responseConverter::indicesGetIndexInformations) //
				.flatMapMany(Flux::fromIterable);
	}

	@Override
	public IndexCoordinates getIndexCoordinates() {
		return (boundClass != null) ? getIndexCoordinatesFor(boundClass) : Objects.requireNonNull(boundIndexCoordinates);
	}

	// region helper functions
	private IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz).getIndexCoordinates();
	}

	/**
	 * Get the {@link Alias} of the provided class.
	 *
	 * @param clazz provided class that can be used to extract aliases.
	 */
	private Set<Alias> getAliasesFor(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz).getAliases();
	}

	private Class<?> checkForBoundClass() {
		if (boundClass == null) {
			throw new InvalidDataAccessApiUsageException("IndexOperations are not bound");
		}
		return boundClass;
	}
	// endregion

}
