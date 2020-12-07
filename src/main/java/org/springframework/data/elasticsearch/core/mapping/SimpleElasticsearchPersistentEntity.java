/*
 * Copyright 2013-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.index.VersionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.elasticsearch.annotations.Parent;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.PersistentPropertyAccessorFactory;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Elasticsearch specific {@link org.springframework.data.mapping.PersistentEntity} implementation holding
 *
 * @param <T>
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Sascha Woo
 * @author Ivan Greene
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @author Subhobrata Dey
 */
public class SimpleElasticsearchPersistentEntity<T> extends BasicPersistentEntity<T, ElasticsearchPersistentProperty>
		implements ElasticsearchPersistentEntity<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleElasticsearchPersistentEntity.class);
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private @Nullable String indexName;
	private boolean useServerConfiguration;
	private short shards;
	private short replicas;
	private @Nullable String refreshInterval;
	private @Nullable String indexStoreType;
	@Deprecated private @Nullable String parentType;
	@Deprecated private @Nullable ElasticsearchPersistentProperty parentIdProperty;
	private @Nullable ElasticsearchPersistentProperty scoreProperty;
	private @Nullable ElasticsearchPersistentProperty seqNoPrimaryTermProperty;
	private @Nullable ElasticsearchPersistentProperty joinFieldProperty;
	private @Nullable String settingPath;
	private @Nullable VersionType versionType;
	private boolean createIndexAndMapping;
	private final Map<String, ElasticsearchPersistentProperty> fieldNamePropertyCache = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, Expression> indexNameExpressions = new ConcurrentHashMap<>();
	private final Lazy<EvaluationContext> indexNameEvaluationContext = Lazy.of(this::getIndexNameEvaluationContext);

	public SimpleElasticsearchPersistentEntity(TypeInformation<T> typeInformation) {

		super(typeInformation);

		Class<T> clazz = typeInformation.getType();
		org.springframework.data.elasticsearch.annotations.Document document = AnnotatedElementUtils
				.findMergedAnnotation(clazz, org.springframework.data.elasticsearch.annotations.Document.class);

		if (document != null) {

			Assert.hasText(document.indexName(),
					" Unknown indexName. Make sure the indexName is defined. e.g @Document(indexName=\"foo\")");
			this.indexName = document.indexName();
			this.useServerConfiguration = document.useServerConfiguration();
			this.shards = document.shards();
			this.replicas = document.replicas();
			this.refreshInterval = document.refreshInterval();
			this.indexStoreType = document.indexStoreType();
			this.versionType = document.versionType();
			this.createIndexAndMapping = document.createIndex();
		}

		Setting setting = AnnotatedElementUtils.getMergedAnnotation(clazz, Setting.class);

		if (setting != null) {
			this.settingPath = setting.settingPath();
		}
	}

	private String getIndexName() {
		return indexName != null ? indexName : getTypeInformation().getType().getSimpleName();
	}

	@Override
	public IndexCoordinates getIndexCoordinates() {
		return resolve(IndexCoordinates.of(getIndexName()));
	}

	@Nullable
	@Override
	public String getIndexStoreType() {
		return indexStoreType;
	}

	@Override
	public short getShards() {
		return shards;
	}

	@Override
	public short getReplicas() {
		return replicas;
	}

	@Override
	public boolean isUseServerConfiguration() {
		return useServerConfiguration;
	}

	@Nullable
	@Override
	public String getRefreshInterval() {
		return refreshInterval;
	}

	@Nullable
	@Override
	@Deprecated
	public String getParentType() {
		return parentType;
	}

	@Nullable
	@Override
	@Deprecated
	public ElasticsearchPersistentProperty getParentIdProperty() {
		return parentIdProperty;
	}

	@Nullable
	@Override
	public VersionType getVersionType() {
		return versionType;
	}

	@Override
	public String settingPath() {
		return settingPath;
	}

	@Override
	public boolean isCreateIndexAndMapping() {
		return createIndexAndMapping;
	}

	@Override
	public boolean hasScoreProperty() {
		return scoreProperty != null;
	}

	@Nullable
	@Override
	public ElasticsearchPersistentProperty getScoreProperty() {
		return scoreProperty;
	}

	@Override
	public void addPersistentProperty(ElasticsearchPersistentProperty property) {
		super.addPersistentProperty(property);

		if (property.isParentProperty()) {
			ElasticsearchPersistentProperty parentProperty = this.parentIdProperty;

			if (parentProperty != null) {
				throw new MappingException(String.format(
						"Attempt to add parent property %s but already have property %s registered "
								+ "as parent property. Check your mapping configuration!",
						property.getField(), parentProperty.getField()));
			}

			Parent parentAnnotation = property.findAnnotation(Parent.class);
			this.parentIdProperty = property;
			this.parentType = parentAnnotation.type();
		}

		if (property.isScoreProperty()) {

			ElasticsearchPersistentProperty scoreProperty = this.scoreProperty;

			if (scoreProperty != null) {
				throw new MappingException(String.format(
						"Attempt to add score property %s but already have property %s registered "
								+ "as score property. Check your mapping configuration!",
						property.getField(), scoreProperty.getField()));
			}

			this.scoreProperty = property;
		}

		if (property.isSeqNoPrimaryTermProperty()) {

			ElasticsearchPersistentProperty seqNoPrimaryTermProperty = this.seqNoPrimaryTermProperty;

			if (seqNoPrimaryTermProperty != null) {
				throw new MappingException(String.format(
						"Attempt to add SeqNoPrimaryTerm property %s but already have property %s registered "
								+ "as SeqNoPrimaryTerm property. Check your entity configuration!",
						property.getField(), seqNoPrimaryTermProperty.getField()));
			}

			this.seqNoPrimaryTermProperty = property;

			if (hasVersionProperty()) {
				warnAboutBothSeqNoPrimaryTermAndVersionProperties();
			}
		}

		if (property.isVersionProperty()) {
			if (hasSeqNoPrimaryTermProperty()) {
				warnAboutBothSeqNoPrimaryTermAndVersionProperties();
			}
		}

		Class<?> actualType = property.getActualTypeOrNull();
		if (actualType == JoinField.class) {
			ElasticsearchPersistentProperty joinProperty = this.joinFieldProperty;

			if (joinProperty != null) {
				throw new MappingException(
						String.format(
								"Attempt to add Join property %s but already have property %s registered "
										+ "as Join property. Check your entity configuration!",
								property.getField(), joinProperty.getField()));
			}

			this.joinFieldProperty = property;
		}
	}

	private void warnAboutBothSeqNoPrimaryTermAndVersionProperties() {
		LOGGER.warn(
				"Both SeqNoPrimaryTerm and @Version properties are defined on {}. Version will not be sent in index requests when seq_no is sent!",
				getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#setPersistentPropertyAccessorFactory(org.springframework.data.mapping.model.PersistentPropertyAccessorFactory)
	 */
	@Override
	public void setPersistentPropertyAccessorFactory(PersistentPropertyAccessorFactory factory) {

		// Do nothing to avoid the usage of ClassGeneratingPropertyAccessorFactory for now
		// DATACMNS-1322 switches to proper immutability behavior which Spring Data Elasticsearch
		// cannot yet implement
	}

	@Nullable
	@Override
	public ElasticsearchPersistentProperty getPersistentPropertyWithFieldName(String fieldName) {

		Assert.notNull(fieldName, "fieldName must not be null");

		return fieldNamePropertyCache.computeIfAbsent(fieldName, key -> {
			AtomicReference<ElasticsearchPersistentProperty> propertyRef = new AtomicReference<>();
			doWithProperties((PropertyHandler<ElasticsearchPersistentProperty>) property -> {
				if (key.equals(property.getFieldName())) {
					propertyRef.set(property);
				}
			});

			return propertyRef.get();
		});
	}

	@Override
	public boolean hasSeqNoPrimaryTermProperty() {
		return seqNoPrimaryTermProperty != null;
	}

	@Override
	public boolean hasJoinFieldProperty() {
		return joinFieldProperty != null;
	}

	@Override
	@Nullable
	public ElasticsearchPersistentProperty getSeqNoPrimaryTermProperty() {
		return seqNoPrimaryTermProperty;
	}

	@Nullable
	@Override
	public ElasticsearchPersistentProperty getJoinFieldProperty() {
		return joinFieldProperty;
	}

	// region SpEL handling
	/**
	 * resolves all the names in the IndexCoordinates object. If a name cannot be resolved, the original name is returned.
	 *
	 * @param indexCoordinates IndexCoordinates with names to resolve
	 * @return IndexCoordinates with resolved names
	 */
	private IndexCoordinates resolve(IndexCoordinates indexCoordinates) {

		String[] indexNames = indexCoordinates.getIndexNames();
		String[] resolvedNames = new String[indexNames.length];

		for (int i = 0; i < indexNames.length; i++) {
			String indexName = indexNames[i];
			resolvedNames[i] = resolve(indexName);
		}

		return IndexCoordinates.of(resolvedNames);
	}

	/**
	 * tries to resolve the given name. If this is not successful, the original value is returned
	 *
	 * @param name name to resolve
	 * @return the resolved name or the input name if it cannot be resolved
	 */
	private String resolve(String name) {

		Assert.notNull(name, "name must not be null");

		Expression expression = getExpressionForIndexName(name);

		String resolvedName = expression != null ? expression.getValue(indexNameEvaluationContext.get(), String.class) : null;
		return resolvedName != null ? resolvedName : name;
	}

	/**
	 * returns an {@link Expression} for #name if name contains a {@link ParserContext#TEMPLATE_EXPRESSION} otherwise
	 * returns {@literal null}.
	 * 
	 * @param name the name to get the expression for
	 * @return Expression may be null
	 */
	@Nullable
	private Expression getExpressionForIndexName(String name) {
		return indexNameExpressions.computeIfAbsent(name, s -> {
			Expression expr = PARSER.parseExpression(s, ParserContext.TEMPLATE_EXPRESSION);
			return expr instanceof LiteralExpression ? null : expr;
		});
	}

	/**
	 * build the {@link EvaluationContext} considering {@link ExpressionDependencies} from the name returned by
	 * {@link #getIndexName()}.
	 * 
	 * @return EvaluationContext
	 */
	private EvaluationContext getIndexNameEvaluationContext() {

		Expression expression = getExpressionForIndexName(getIndexName());
		ExpressionDependencies expressionDependencies = expression != null ? ExpressionDependencies.discover(expression)
				: ExpressionDependencies.none();

		return getEvaluationContext(null, expressionDependencies);
	}

	// endregion

	@Override
	public Document getDefaultSettings() {

		if (isUseServerConfiguration()) {
			return Document.create();
		}

		Map<String, String> map = new MapBuilder<String, String>()
				.put("index.number_of_shards", String.valueOf(getShards()))
				.put("index.number_of_replicas", String.valueOf(getReplicas()))
				.put("index.refresh_interval", getRefreshInterval()).put("index.store.type", getIndexStoreType()).map();
		return Document.from(map);
	}
}
