/*
 * Copyright 2013-2024 the original author or authors.
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Routing;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
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

	private static final Log LOGGER = LogFactory.getLog(SimpleElasticsearchPersistentEntity.class);
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private @Nullable final Document document;
	private @Nullable String indexName;
	private final Lazy<SettingsParameter> settingsParameter;
	private @Nullable ElasticsearchPersistentProperty seqNoPrimaryTermProperty;
	private @Nullable ElasticsearchPersistentProperty joinFieldProperty;
	private @Nullable ElasticsearchPersistentProperty indexedIndexNameProperty;
	private @Nullable Document.VersionType versionType;
	private final boolean createIndexAndMapping;
	private final boolean alwaysWriteMapping;
	private final Dynamic dynamic;
	private final Map<String, ElasticsearchPersistentProperty> fieldNamePropertyCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Expression> routingExpressions = new ConcurrentHashMap<>();
	private @Nullable String routing;
	private final ContextConfiguration contextConfiguration;
	private final Set<Alias> aliases = new HashSet<>();

	private final ConcurrentHashMap<String, Expression> indexNameExpressions = new ConcurrentHashMap<>();
	private final Lazy<EvaluationContext> indexNameEvaluationContext = Lazy.of(this::getIndexNameEvaluationContext);

	private final boolean storeIdInSource;
	private final boolean storeVersionInSource;

	public SimpleElasticsearchPersistentEntity(TypeInformation<T> typeInformation,
			ContextConfiguration contextConfiguration) {

		super(typeInformation);
		this.contextConfiguration = contextConfiguration;

		Class<T> clazz = typeInformation.getType();

		document = AnnotatedElementUtils.findMergedAnnotation(clazz,
				org.springframework.data.elasticsearch.annotations.Document.class);

		// need a Lazy here, because we need the persistent properties available
		this.settingsParameter = Lazy.of(() -> buildSettingsParameter(clazz));

		if (document != null) {

			Assert.hasText(document.indexName(),
					" Unknown indexName. Make sure the indexName is defined. e.g @Document(indexName=\"foo\")");
			this.indexName = document.indexName();
			this.versionType = document.versionType();
			this.createIndexAndMapping = document.createIndex();
			this.alwaysWriteMapping = document.alwaysWriteMapping();
			this.dynamic = document.dynamic();
			this.storeIdInSource = document.storeIdInSource();
			this.storeVersionInSource = document.storeVersionInSource();
			buildAliases();
		} else {
			this.dynamic = Dynamic.INHERIT;
			this.storeIdInSource = true;
			this.storeVersionInSource = true;
			this.createIndexAndMapping = false;
			this.alwaysWriteMapping = false;
		}
		Routing routingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Routing.class);

		if (routingAnnotation != null) {

			Assert.hasText(routingAnnotation.value(), "@Routing annotation must contain a non-empty value");

			this.routing = routingAnnotation.value();
		}
	}

	private String getIndexName() {
		return indexName != null ? indexName : getTypeInformation().getType().getSimpleName();
	}

	@Override
	public IndexCoordinates getIndexCoordinates() {
		return resolve(IndexCoordinates.of(getIndexName()));
	}

	@Override
	public Set<Alias> getAliases() {
		return aliases;
	}

	@Nullable
	@Override
	public String getIndexStoreType() {
		return settingsParameter.get().indexStoreType;
	}

	@Override
	public short getShards() {
		return settingsParameter.get().shards;
	}

	@Override
	public short getReplicas() {
		return settingsParameter.get().replicas;
	}

	@Override
	public boolean isUseServerConfiguration() {
		return settingsParameter.get().useServerConfiguration;
	}

	@Nullable
	@Override
	public String getRefreshInterval() {
		return settingsParameter.get().refreshIntervall;
	}

	@Nullable
	@Override
	public Document.VersionType getVersionType() {
		return versionType;
	}

	@Override
	public boolean isCreateIndexAndMapping() {
		return createIndexAndMapping;
	}

	public boolean isAlwaysWriteMapping() {
		return alwaysWriteMapping;
	}

	@Override
	public FieldNamingStrategy getFieldNamingStrategy() {
		return contextConfiguration.getFieldNamingStrategy();
	}

	@Override
	public boolean writeTypeHints() {

		boolean writeTypeHints = contextConfiguration.getWriteTypeHints();

		if (document != null) {
			switch (document.writeTypeHint()) {
				case TRUE:
					writeTypeHints = true;
					break;
				case FALSE:
					writeTypeHints = false;
					break;
				case DEFAULT:
					break;
			}
		}

		return writeTypeHints;
	}

	@Override
	public boolean storeIdInSource() {
		return storeIdInSource;
	}

	@Override
	public boolean storeVersionInSource() {
		return storeVersionInSource;
	}

	@Override
	public void addPersistentProperty(ElasticsearchPersistentProperty property) {
		super.addPersistentProperty(property);

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

		if (property.isIndexedIndexNameProperty()) {

			if (!property.getActualType().isAssignableFrom(String.class)) {
				throw new MappingException("@IndexedIndexName annotation must be put on String property");
			}

			if (indexedIndexNameProperty != null) {
				throw new MappingException(
						"@IndexedIndexName annotation can only be put on one property in an entity");
			}

			this.indexedIndexNameProperty = property;
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
		LOGGER.warn(String.format(
				"Both SeqNoPrimaryTerm and @Version properties are defined on %s. Version will not be sent in index requests when seq_no is sent!",
				getType()));
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

	@Nullable
	@Override
	public ElasticsearchPersistentProperty getIndexedIndexNameProperty() {
		return indexedIndexNameProperty;
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

		String resolvedName = expression != null ? expression.getValue(indexNameEvaluationContext.get(), String.class)
				: null;
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

		// noinspection ConstantConditions
		return getEvaluationContext(null, expressionDependencies);
	}

	@Override
	@Nullable
	public String resolveRouting(T bean) {

		if (routing == null) {
			return null;
		}

		ElasticsearchPersistentProperty persistentProperty = getPersistentProperty(routing);

		if (persistentProperty != null) {
			Object propertyValue = getPropertyAccessor(bean).getProperty(persistentProperty);

			return propertyValue != null ? propertyValue.toString() : null;
		}

		try {
			Expression expression = routingExpressions.computeIfAbsent(routing, PARSER::parseExpression);
			ExpressionDependencies expressionDependencies = ExpressionDependencies.discover(expression);

			// noinspection ConstantConditions
			EvaluationContext context = getEvaluationContext(null, expressionDependencies);
			context.setVariable("entity", bean);

			return expression.getValue(context, String.class);
		} catch (EvaluationException e) {
			throw new InvalidDataAccessApiUsageException(
					"Could not resolve expression: " + routing + " for object of class " + bean.getClass().getCanonicalName(), e);
		}
	}

	// endregion

	// region index settings
	@Override
	public String settingPath() {
		return settingsParameter.get().settingPath;
	}

	@Override
	public Settings getDefaultSettings() {
		return settingsParameter.get().toSettings(); //
	}

	private SettingsParameter buildSettingsParameter(Class<?> clazz) {

		SettingsParameter settingsParameter = new SettingsParameter();
		Setting settingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);

		// default values
		settingsParameter.useServerConfiguration = false;
		settingsParameter.shards = 1;
		settingsParameter.replicas = 1;
		settingsParameter.refreshIntervall = "1s";

		if (settingAnnotation != null) {
			processSettingAnnotation(settingAnnotation, settingsParameter);
		}

		return settingsParameter;
	}

	private void processSettingAnnotation(Setting settingAnnotation, SettingsParameter settingsParameter) {
		settingsParameter.useServerConfiguration = settingAnnotation.useServerConfiguration();
		settingsParameter.settingPath = settingAnnotation.settingPath();
		settingsParameter.shards = settingAnnotation.shards();
		settingsParameter.replicas = settingAnnotation.replicas();
		settingsParameter.refreshIntervall = settingAnnotation.refreshInterval();
		settingsParameter.indexStoreType = settingAnnotation.indexStoreType();

		String[] sortFields = settingAnnotation.sortFields();

		if (sortFields.length > 0) {
			String[] fieldNames = new String[sortFields.length];
			int index = 0;
			for (String propertyName : sortFields) {
				ElasticsearchPersistentProperty property = getPersistentProperty(propertyName);

				if (property == null) {
					throw new IllegalArgumentException("sortField property " + propertyName + " not found");
				}
				Field fieldAnnotation = property.getRequiredAnnotation(Field.class);

				FieldType fieldType = fieldAnnotation.type();
				switch (fieldType) {
					case Boolean:
					case Long:
					case Integer:
					case Short:
					case Byte:
					case Float:
					case Half_Float:
					case Scaled_Float:
					case Date:
					case Date_Nanos:
					case Keyword:
						break;
					default:
						throw new IllegalArgumentException("field type " + fieldType + " not allowed for sortField");
				}

				if (!fieldAnnotation.docValues()) {
					throw new IllegalArgumentException("doc_values must be set to true for sortField");
				}
				fieldNames[index++] = property.getFieldName();
			}
			settingsParameter.sortFields = fieldNames;

			Setting.SortOrder[] sortOrders = settingAnnotation.sortOrders();
			if (sortOrders.length > 0) {

				if (sortOrders.length != sortFields.length) {
					throw new IllegalArgumentException("@Settings parameter sortFields and sortOrders must have the same size");
				}
				settingsParameter.sortOrders = sortOrders;
			}

			Setting.SortMode[] sortModes = settingAnnotation.sortModes();
			if (sortModes.length > 0) {

				if (sortModes.length != sortFields.length) {
					throw new IllegalArgumentException("@Settings parameter sortFields and sortModes must have the same size");
				}
				settingsParameter.sortModes = sortModes;
			}

			Setting.SortMissing[] sortMissingValues = settingAnnotation.sortMissingValues();
			if (sortMissingValues.length > 0) {

				if (sortMissingValues.length != sortFields.length) {
					throw new IllegalArgumentException(
							"@Settings parameter sortFields and sortMissingValues must have the same size");
				}
				settingsParameter.sortMissingValues = sortMissingValues;
			}
		}
	}

	/**
	 * internal class to collect settings values from the {@link Document} and {@link Setting} annotations-
	 */
	private static class SettingsParameter {
		boolean useServerConfiguration = false;
		@Nullable String settingPath;
		short shards;
		short replicas;
		@Nullable String refreshIntervall;
		@Nullable String indexStoreType;
		@Nullable private String[] sortFields;
		@Nullable private Setting.SortOrder[] sortOrders;
		@Nullable private Setting.SortMode[] sortModes;
		@Nullable private Setting.SortMissing[] sortMissingValues;

		Settings toSettings() {

			if (useServerConfiguration) {
				return new Settings();
			}

			var index = new Settings() //
					.append("number_of_shards", String.valueOf(shards)) //
					.append("number_of_replicas", String.valueOf(replicas));

			if (refreshIntervall != null) {
				index.append("refresh_interval", refreshIntervall);
			}

			if (indexStoreType != null && !"fs".equals(indexStoreType)) {
				index.append("store", new Settings().append("type", indexStoreType));
			}

			if (sortFields != null && sortFields.length > 0) {
				var sort = new Settings().append("field", sortFields);

				if (sortOrders != null && sortOrders.length > 0) {
					sort.append("order", sortOrders);
				}

				if (sortModes != null && sortModes.length > 0) {
					sort.append("mode", sortModes);
				}

				if (sortMissingValues != null && sortMissingValues.length > 0) {
					sort.append("missing", sortMissingValues);
				}

				index.append("sort", sort);
			}

			return new Settings().append("index", index); //
		}
	}

	// endregion

	/**
	 * Configuration settings passed in from the creating {@link SimpleElasticsearchMappingContext}.
	 */
	public static class ContextConfiguration {

		private final FieldNamingStrategy fieldNamingStrategy;
		private final boolean writeTypeHints;

		ContextConfiguration(FieldNamingStrategy fieldNamingStrategy, boolean writeTypeHints) {
			this.fieldNamingStrategy = fieldNamingStrategy;
			this.writeTypeHints = writeTypeHints;
		}

		public FieldNamingStrategy getFieldNamingStrategy() {
			return fieldNamingStrategy;
		}

		public boolean getWriteTypeHints() {
			return writeTypeHints;
		}
	}

	@Override
	public Dynamic dynamic() {
		return dynamic;
	}

	/**
	 * Building once the aliases for the current document.
	 */
	private void buildAliases() {
		// Clear the existing aliases.
		aliases.clear();

		if (document != null) {
			for (org.springframework.data.elasticsearch.annotations.Alias alias : document.aliases()) {
				if (alias.value().isEmpty()) {
					continue;
				}

				Query query = null;
				if (!alias.filter().value().isEmpty()) {
					query = new StringQuery(alias.filter().value());
				}

				aliases.add(
						Alias.builder(alias.value())
								.withFilter(query)
								.withIndexRouting(alias.indexRouting())
								.withSearchRouting(alias.searchRouting())
								.withRouting(alias.routing())
								.withHidden(alias.isHidden())
								.withWriteIndex(alias.isWriteIndex())
								.build());
			}
		}
	}
}
