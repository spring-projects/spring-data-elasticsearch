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

import static org.springframework.util.StringUtils.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Base implementation of {@link IndexOperations} common to Transport and Rest based Implementations of IndexOperations.
 * 
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
abstract class AbstractDefaultIndexOperations implements IndexOperations {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDefaultIndexOperations.class);

	protected final ElasticsearchConverter elasticsearchConverter;
	protected final RequestFactory requestFactory;

	@Nullable protected final Class<?> boundClass;
	private final IndexCoordinates boundIndex;

	public AbstractDefaultIndexOperations(ElasticsearchConverter elasticsearchConverter, Class<?> boundClass) {
		this.elasticsearchConverter = elasticsearchConverter;
		requestFactory = new RequestFactory(elasticsearchConverter);

		this.boundClass = boundClass;
		this.boundIndex = getIndexCoordinatesFor(boundClass);
	}

	public AbstractDefaultIndexOperations(ElasticsearchConverter elasticsearchConverter, IndexCoordinates boundIndex) {
		this.elasticsearchConverter = elasticsearchConverter;
		requestFactory = new RequestFactory(elasticsearchConverter);

		this.boundClass = null;
		this.boundIndex = boundIndex;
	}

	protected Class<?> checkForBoundClass() {
		if (boundClass == null) {
			throw new InvalidDataAccessApiUsageException("IndexOperations are not bound");
		}
		return boundClass;
	}

	// region IndexOperations

	@Override
	public boolean create() {

		if (boundClass != null) {
			Class<?> clazz = boundClass;
			String indexName = getIndexCoordinates().getIndexName();

			if (clazz.isAnnotationPresent(Setting.class)) {
				String settingPath = clazz.getAnnotation(Setting.class).settingPath();

				if (hasText(settingPath)) {
					String settings = ResourceUtil.readFileFromClasspath(settingPath);

					if (hasText(settings)) {
						return doCreate(indexName, Document.parse(settings));
					}
				} else {
					LOGGER.info("settingPath in @Setting has to be defined. Using default instead.");
				}
			}
			return doCreate(indexName, getDefaultSettings(getRequiredPersistentEntity(clazz)));
		}
		return doCreate(getIndexCoordinates().getIndexName(), null);
	}

	@Override
	public boolean create(Document settings) {
		return doCreate(getIndexCoordinates().getIndexName(), settings);
	}

	protected abstract boolean doCreate(String indexName, @Nullable Document settings);

	@Override
	public boolean delete() {
		return doDelete(getIndexCoordinates().getIndexName());
	}

	protected abstract boolean doDelete(String indexName);

	@Override
	public boolean exists() {
		return doExists(getIndexCoordinates().getIndexName());
	}

	protected abstract boolean doExists(String indexName);

	@Override
	public boolean putMapping(Document mapping) {
		return doPutMapping(getIndexCoordinates(), mapping);
	}

	protected abstract boolean doPutMapping(IndexCoordinates index, Document mapping);

	@Override
	public Map<String, Object> getMapping() {
		return doGetMapping(getIndexCoordinates());
	}

	abstract protected Map<String, Object> doGetMapping(IndexCoordinates index);

	@Override
	public Map<String, Object> getSettings() {
		return getSettings(false);
	}

	@Override
	public Map<String, Object> getSettings(boolean includeDefaults) {
		return doGetSettings(getIndexCoordinates().getIndexName(), includeDefaults);
	}

	protected abstract Map<String, Object> doGetSettings(String indexName, boolean includeDefaults);

	@Override
	public void refresh() {
		doRefresh(getIndexCoordinates());
	}

	protected abstract void doRefresh(IndexCoordinates indexCoordinates);

	@Override
	public boolean addAlias(AliasQuery query) {
		return doAddAlias(query, getIndexCoordinates());
	}

	protected abstract boolean doAddAlias(AliasQuery query, IndexCoordinates index);

	@Override
	public List<AliasMetaData> queryForAlias() {
		return doQueryForAlias(getIndexCoordinates().getIndexName());
	}

	protected abstract List<AliasMetaData> doQueryForAlias(String indexName);

	@Override
	public boolean removeAlias(AliasQuery query) {
		return doRemoveAlias(query, getIndexCoordinates());
	}

	protected abstract boolean doRemoveAlias(AliasQuery query, IndexCoordinates index);

	@Override
	public Document createMapping() {
		return createMapping(checkForBoundClass());
	}

	@Override
	public Document createMapping(Class<?> clazz) {
		return buildMapping(clazz);
	}

	protected Document buildMapping(Class<?> clazz) {

		// load mapping specified in Mapping annotation if present
		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();

			if (!StringUtils.isEmpty(mappingPath)) {
				String mappings = ResourceUtil.readFileFromClasspath(mappingPath);

				if (!StringUtils.isEmpty(mappings)) {
					return Document.parse(mappings);
				}
			} else {
				LOGGER.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
			}
		}

		// build mapping from field annotations
		try {
			String mapping = new MappingBuilder(elasticsearchConverter).buildPropertyMapping(clazz);
			return Document.parse(mapping);
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}
	// endregion

	// region Helper functions
	private <T> Document getDefaultSettings(ElasticsearchPersistentEntity<T> persistentEntity) {

		if (persistentEntity.isUseServerConfiguration()) {
			return Document.create();
		}

		Map<String, String> map = new MapBuilder<String, String>()
				.put("index.number_of_shards", String.valueOf(persistentEntity.getShards()))
				.put("index.number_of_replicas", String.valueOf(persistentEntity.getReplicas()))
				.put("index.refresh_interval", persistentEntity.getRefreshInterval())
				.put("index.store.type", persistentEntity.getIndexStoreType()).map();
		return Document.from(map);
	}

	ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	/**
	 * get the current {@link IndexCoordinates}. These may change over time when the entity class has a SpEL constructed
	 * index name. When this IndexOperations is not bound to a class, the bound IndexCoordinates are returned.
	 *
	 * @return IndexCoordinates
	 */
	protected IndexCoordinates getIndexCoordinates() {
		return (boundClass != null) ? getIndexCoordinatesFor(boundClass) : boundIndex;
	}

	public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return getRequiredPersistentEntity(clazz).getIndexCoordinates();
	}

	protected Map<String, Object> convertSettingsResponseToMap(GetSettingsResponse response, String indexName) {

		Map<String, Object> settings = new HashMap<>();

		if (!response.getIndexToDefaultSettings().isEmpty()) {
			Settings defaultSettings = response.getIndexToDefaultSettings().get(indexName);
			for (String key : defaultSettings.keySet()) {
				settings.put(key, defaultSettings.get(key));
			}
		}

		if (!response.getIndexToSettings().isEmpty()) {
			Settings customSettings = response.getIndexToSettings().get(indexName);
			for (String key : customSettings.keySet()) {
				settings.put(key, customSettings.get(key));
			}
		}

		return settings;
	}
	// endregion

}
