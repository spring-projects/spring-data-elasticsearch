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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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
	@Nullable private final IndexCoordinates boundIndex;

	public AbstractDefaultIndexOperations(ElasticsearchConverter elasticsearchConverter, Class<?> boundClass) {

		Assert.notNull(boundClass, "boundClass may not be null");

		this.elasticsearchConverter = elasticsearchConverter;
		requestFactory = new RequestFactory(elasticsearchConverter);
		this.boundClass = boundClass;
		this.boundIndex = null;
	}

	public AbstractDefaultIndexOperations(ElasticsearchConverter elasticsearchConverter, IndexCoordinates boundIndex) {

		Assert.notNull(boundIndex, "boundIndex may not be null");

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

		Document settings = null;

		if (boundClass != null) {
			settings = createSettings(boundClass);
		}

		return doCreate(getIndexCoordinates(), settings);
	}

	@Override
	public Document createSettings(Class<?> clazz) {

		Assert.notNull(clazz, "clazz must not be null");

		Document settings = null;

		Setting setting = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);

		if (setting != null) {
			settings = loadSettings(setting.settingPath());
		}

		if (settings == null) {
			settings = getRequiredPersistentEntity(clazz).getDefaultSettings();
		}

		return settings;
	}

	@Override
	public boolean create(Document settings) {
		return doCreate(getIndexCoordinates(), settings);
	}

	protected abstract boolean doCreate(IndexCoordinates index, @Nullable Document settings);

	@Override
	public boolean delete() {
		return doDelete(getIndexCoordinates());
	}

	protected abstract boolean doDelete(IndexCoordinates index);

	@Override
	public boolean exists() {
		return doExists(getIndexCoordinates());
	}

	protected abstract boolean doExists(IndexCoordinates index);

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
		return doGetSettings(getIndexCoordinates(), includeDefaults);
	}

	protected abstract Map<String, Object> doGetSettings(IndexCoordinates index, boolean includeDefaults);

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
	public List<AliasMetadata> queryForAlias() {
		return doQueryForAlias(getIndexCoordinates());
	}

	protected abstract List<AliasMetadata> doQueryForAlias(IndexCoordinates index);

	@Override
	public boolean removeAlias(AliasQuery query) {
		return doRemoveAlias(query, getIndexCoordinates());
	}

	protected abstract boolean doRemoveAlias(AliasQuery query, IndexCoordinates index);

	@Override
	public Map<String, Set<AliasData>> getAliases(String... aliasNames) {

		Assert.notEmpty(aliasNames, "aliasNames must not be empty");

		return doGetAliases(aliasNames, null);
	}

	@Override
	public Map<String, Set<AliasData>> getAliasesForIndex(String... indexNames) {

		Assert.notEmpty(indexNames, "indexNames must not be empty");

		return doGetAliases(null, indexNames);
	}

	protected abstract Map<String, Set<AliasData>> doGetAliases(@Nullable String[] aliasNames,
			@Nullable String[] indexNames);

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
		Mapping mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Mapping.class);
		if (mappingAnnotation != null) {
			String mappingPath = mappingAnnotation.mappingPath();

			if (StringUtils.hasText(mappingPath)) {
				String mappings = ResourceUtil.readFileFromClasspath(mappingPath);

				if (StringUtils.hasText(mappings)) {
					return Document.parse(mappings);
				}
			} else {
				LOGGER.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
			}
		}

		// build mapping from field annotations
		try

		{
			String mapping = new MappingBuilder(elasticsearchConverter).buildPropertyMapping(clazz);
			return Document.parse(mapping);
		} catch (Exception e) {
			throw new UncategorizedElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}

	@Override
	public Document createSettings() {
		return createSettings(checkForBoundClass());
	}

	// endregion

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

	@Nullable
	private Document loadSettings(String settingPath) {
		if (hasText(settingPath)) {
			String settingsFile = ResourceUtil.readFileFromClasspath(settingPath);

			if (hasText(settingsFile)) {
				return Document.parse(settingsFile);
			}
		} else {
			LOGGER.info("settingPath in @Setting has to be defined. Using default instead.");
		}
		return null;
	}
	// endregion
}
