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

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
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
		Document settings = null;

		if (boundClass != null) {
			Class<?> clazz = boundClass;

			if (clazz.isAnnotationPresent(Setting.class)) {
				String settingPath = clazz.getAnnotation(Setting.class).settingPath();

				return loadDocument(settingPath, "@Setting").flatMap(document -> doCreate(indexName, document));
			}

			settings = getRequiredPersistentEntity(clazz).getDefaultSettings();
		}

		return doCreate(indexName, settings);
	}

	@Override
	public Mono<Boolean> create(Document settings) {
		return doCreate(getIndexCoordinates().getIndexName(), settings);
	}

	private Mono<Boolean> doCreate(String indexName, @Nullable Document settings) {
		CreateIndexRequest request = requestFactory.createIndexRequestReactive(getIndexCoordinates().getIndexName(),
				settings);
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

	@Override
	public Mono<Document> createMapping() {
		return createMapping(checkForBoundClass());
	}

	@Override
	public Mono<Document> createMapping(Class<?> clazz) {

		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();
			return loadDocument(mappingPath, "@Mapping");
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

	@Override
	public Mono<Document> getSettings(boolean includeDefaults) {

		String indexName = getIndexCoordinates().getIndexName();
		GetSettingsRequest request = requestFactory.getSettingsRequest(indexName, includeDefaults);

		return Mono.from(operations.executeWithIndicesClient(client -> client.getSettings(request)))
				.map(getSettingsResponse -> requestFactory.fromSettingsResponse(getSettingsResponse, indexName));
	}

	// endregion

	// region helper functions
	/**
	 * get the current {@link IndexCoordinates}. These may change over time when the entity class has a SpEL constructed
	 * index name. When this IndexOperations is not bound to a class, the bound IndexCoordinates are returned.
	 *
	 * @return IndexCoordinates
	 */
	private IndexCoordinates getIndexCoordinates() {
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
