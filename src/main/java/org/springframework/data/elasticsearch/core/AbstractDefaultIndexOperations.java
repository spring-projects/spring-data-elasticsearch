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

import static org.springframework.util.StringUtils.*;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
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

	public AbstractDefaultIndexOperations(ElasticsearchConverter elasticsearchConverter) {
		this.elasticsearchConverter = elasticsearchConverter;
		requestFactory = new RequestFactory(elasticsearchConverter);
	}

	// region IndexOperations
	@Override
	public boolean createIndex(String indexName) {
		return createIndex(indexName, null);
	}

	@Override
	public boolean createIndex(Class<?> clazz) {

		String indexName = getRequiredPersistentEntity(clazz).getIndexCoordinates().getIndexName();
		if (clazz.isAnnotationPresent(Setting.class)) {
			String settingPath = clazz.getAnnotation(Setting.class).settingPath();

			if (hasText(settingPath)) {
				String settings = ResourceUtil.readFileFromClasspath(settingPath);

				if (hasText(settings)) {
					return createIndex(indexName, settings);
				}
			} else {
				LOGGER.info("settingPath in @Setting has to be defined. Using default instead.");
			}
		}
		return createIndex(indexName, getDefaultSettings(getRequiredPersistentEntity(clazz)));
	}

	@Override
	public boolean createIndex(Class<?> clazz, Object settings) {
		return createIndex(getRequiredPersistentEntity(clazz).getIndexCoordinates().getIndexName(), settings);
	}

	@Override
	public boolean deleteIndex(Class<?> clazz) {
		return deleteIndex(getRequiredPersistentEntity(clazz).getIndexCoordinates().getIndexName());
	}

	@Override
	public boolean indexExists(Class<?> clazz) {
		return indexExists(getIndexCoordinatesFor(clazz).getIndexName());
	}

	@Override
	public Map<String, Object> getMapping(Class<?> clazz) {
		return getMapping(getIndexCoordinatesFor(clazz));
	}

	@Override
	public boolean putMapping(Class<?> clazz) {
		return putMapping(clazz, buildMapping(clazz));
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz, Object mapping) {
		return putMapping(getIndexCoordinatesFor(clazz), mapping);
	}

	@Override
	public boolean putMapping(IndexCoordinates index, Class<?> clazz) {
		return putMapping(index, buildMapping(clazz));
	}

	@Override
	public Map<String, Object> getSettings(Class<?> clazz) {
		return getSettings(clazz, false);
	}

	@Override
	public Map<String, Object> getSettings(Class<?> clazz, boolean includeDefaults) {
		return getSettings(getRequiredPersistentEntity(clazz).getIndexCoordinates().getIndexName(), includeDefaults);
	}

	@Override
	public void refresh(Class<?> clazz) {
		refresh(getIndexCoordinatesFor(clazz));
	}

	protected String buildMapping(Class<?> clazz) {

		// load mapping specified in Mapping annotation if present
		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();

			if (!StringUtils.isEmpty(mappingPath)) {
				String mappings = ResourceUtil.readFileFromClasspath(mappingPath);

				if (!StringUtils.isEmpty(mappings)) {
					return mappings;
				}
			} else {
				LOGGER.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
			}
		}

		// build mapping from field annotations
		try {
			return new MappingBuilder(elasticsearchConverter).buildPropertyMapping(clazz);
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}
	// endregion

	// region Helper functions
	private <T> Map getDefaultSettings(ElasticsearchPersistentEntity<T> persistentEntity) {

		if (persistentEntity.isUseServerConfiguration())
			return new HashMap();

		return new MapBuilder<String, String>().put("index.number_of_shards", String.valueOf(persistentEntity.getShards()))
				.put("index.number_of_replicas", String.valueOf(persistentEntity.getReplicas()))
				.put("index.refresh_interval", persistentEntity.getRefreshInterval())
				.put("index.store.type", persistentEntity.getIndexStoreType()).map();
	}

	ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
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
