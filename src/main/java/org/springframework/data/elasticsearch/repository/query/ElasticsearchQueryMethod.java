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
package org.springframework.data.elasticsearch.repository.query;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.annotations.SourceFilters;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.RuntimeField;
import org.springframework.data.elasticsearch.core.query.ScriptedField;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.data.elasticsearch.repository.support.QueryStringProcessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * ElasticsearchQueryMethod
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Alexander Torres
 * @author Haibo Liu
 */
public class ElasticsearchQueryMethod extends QueryMethod {

	// the following 2 variables exist in the base class, but are private. We need them for
	// correct handling of return types (SearchHits), so we have our own values here.
	// This means that we have to copy code that initializes these variables and in the
	// base class uses them in order to use our variables
	protected final Method method;
	protected final Class<?> unwrappedReturnType;
	@Nullable private Boolean unwrappedReturnTypeFromSearchHit = null;

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
	@Nullable private ElasticsearchEntityMetadata<?> metadata;
	@Nullable private final Query queryAnnotation;
	@Nullable private final Highlight highlightAnnotation;
	@Nullable private final SourceFilters sourceFilters;

	public ElasticsearchQueryMethod(Method method, RepositoryMetadata repositoryMetadata, ProjectionFactory factory,
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {

		super(method, repositoryMetadata, factory);

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.method = method;
		this.mappingContext = mappingContext;
		this.queryAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
		this.highlightAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Highlight.class);
		this.sourceFilters = AnnotatedElementUtils.findMergedAnnotation(method, SourceFilters.class);
		this.unwrappedReturnType = potentiallyUnwrapReturnTypeFor(repositoryMetadata, method);

		verifyCountQueryTypes();
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated
	protected Parameters<?, ?> createParameters(Method method, TypeInformation<?> domainType) {
		return new ElasticsearchParameters(ParametersSource.of(method));
	}

	@Override
	protected Parameters<?, ?> createParameters(ParametersSource parametersSource) {
		return new ElasticsearchParameters(parametersSource);
	}

	protected void verifyCountQueryTypes() {

		if (hasCountQueryAnnotation()) {
			TypeInformation<?> returnType = TypeInformation.fromReturnTypeOf(method);

			if (returnType.getType() != long.class && !Long.class.isAssignableFrom(returnType.getType())) {
				throw new InvalidDataAccessApiUsageException("count query methods must return a Long");
			}
		}
	}

	public boolean hasAnnotatedQuery() {
		return this.queryAnnotation != null;
	}

	/**
	 * @return the query String. Must not be {@literal null} when {@link #hasAnnotatedQuery()} returns true
	 */
	@Nullable
	public String getAnnotatedQuery() {
		return queryAnnotation != null ? queryAnnotation.value() : null;
	}

	/**
	 * @return true if there is a {@link Highlight} annotation present.
	 * @since 4.0
	 */
	public boolean hasAnnotatedHighlight() {
		return highlightAnnotation != null;
	}

	/**
	 * @return a {@link HighlightQuery} built from the {@link Highlight} annotation.
	 * @throws IllegalArgumentException if no {@link Highlight} annotation is present on the method
	 * @see #hasAnnotatedHighlight()
	 */
	public HighlightQuery getAnnotatedHighlightQuery(HighlightConverter highlightConverter) {

		Assert.isTrue(hasAnnotatedHighlight(), "no Highlight annotation present on " + getName());
		Assert.notNull(highlightAnnotation, "highlightAnnotation must not be null");

		return new HighlightQuery(highlightConverter.convert(highlightAnnotation), getDomainClass());
	}

	/**
	 * @return the {@link ElasticsearchEntityMetadata} for the query methods {@link #getReturnedObjectType() return type}.
	 * @since 3.2
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ElasticsearchEntityMetadata<?> getEntityInformation() {

		if (metadata == null) {

			Class<?> returnedObjectType = getReturnedObjectType();
			Class<?> domainClass = getDomainClass();

			if (ClassUtils.isPrimitiveOrWrapper(returnedObjectType)) {

				this.metadata = new SimpleElasticsearchEntityMetadata<>((Class<Object>) domainClass,
						mappingContext.getRequiredPersistentEntity(domainClass));

			} else {

				ElasticsearchPersistentEntity<?> returnedEntity = mappingContext.getPersistentEntity(returnedObjectType);
				ElasticsearchPersistentEntity<?> managedEntity = mappingContext.getRequiredPersistentEntity(domainClass);
				returnedEntity = returnedEntity == null || returnedEntity.getType().isInterface() ? managedEntity
						: returnedEntity;
				ElasticsearchPersistentEntity<?> collectionEntity = domainClass.isAssignableFrom(returnedObjectType)
						? returnedEntity
						: managedEntity;

				this.metadata = new SimpleElasticsearchEntityMetadata<>((Class<Object>) returnedEntity.getType(),
						collectionEntity);
			}
		}

		return this.metadata;
	}

	protected MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	/**
	 * checks whether the return type of the underlying method is a
	 * {@link org.springframework.data.elasticsearch.core.SearchHits} or a collection of
	 * {@link org.springframework.data.elasticsearch.core.SearchHit}.
	 *
	 * @return true if the method has a {@link org.springframework.data.elasticsearch.core.SearchHit} related return type
	 * @since 4.0
	 */
	public boolean isSearchHitMethod() {

		if (unwrappedReturnTypeFromSearchHit != null && unwrappedReturnTypeFromSearchHit) {
			return true;
		}

		Class<?> methodReturnType = method.getReturnType();

		if (SearchHits.class.isAssignableFrom(methodReturnType)) {
			return true;
		}

		try {
			// dealing with Collection<SearchHit<T>>, getting to T
			ParameterizedType methodGenericReturnType = ((ParameterizedType) method.getGenericReturnType());
			if (isAllowedGenericType(methodGenericReturnType)) {
				ParameterizedType collectionTypeArgument = (ParameterizedType) methodGenericReturnType
						.getActualTypeArguments()[0];
				if (SearchHit.class.isAssignableFrom((Class<?>) collectionTypeArgument.getRawType())) {
					return true;
				}
			}
		} catch (Exception ignored) {}

		return false;
	}

	/**
	 * checks if the return type is {@link SearchPage}.
	 *
	 * @since 4.0
	 */
	public boolean isSearchPageMethod() {
		return SearchPage.class.isAssignableFrom(methodReturnType());
	}

	/**
	 * returns the declared return type for this method.
	 *
	 * @return the return type
	 * @since 4.0
	 */
	public Class<?> methodReturnType() {
		return method.getReturnType();
	}

	protected boolean isAllowedGenericType(ParameterizedType methodGenericReturnType) {
		return Collection.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType())
				|| Stream.class.isAssignableFrom((Class<?>) methodGenericReturnType.getRawType());
	}

	/**
	 * checks whether the return type of the underlying method nether a
	 * {@link org.springframework.data.elasticsearch.core.SearchHits} nor a collection of
	 * {@link org.springframework.data.elasticsearch.core.SearchHit}.
	 *
	 * @return true if the method has not a {@link org.springframework.data.elasticsearch.core.SearchHit}t related return
	 *         type
	 * @since 4.0
	 */
	public boolean isNotSearchHitMethod() {
		return !isSearchHitMethod();
	}

	/**
	 * checks if the return type is not {@link SearchPage}.
	 *
	 * @since 4.2
	 */
	public boolean isNotSearchPageMethod() {
		return !isSearchPageMethod();
	}

	/**
	 * @return {@literal true} if the method is annotated with
	 *         {@link org.springframework.data.elasticsearch.annotations.CountQuery} or with {@link Query}(count =true)
	 * @since 4.2
	 */
	public boolean hasCountQueryAnnotation() {
		return queryAnnotation != null && queryAnnotation.count();
	}

	/**
	 * Uses the sourceFilters property to create a {@link SourceFilter} to be added to a
	 * {@link org.springframework.data.elasticsearch.core.query.Query}
	 *
	 * @param parameterAccessor the accessor with the query method parameter details
	 * @param converter {@link ElasticsearchConverter} needed to convert entity property names to the Elasticsearch field
	 *          names and for parameter conversion when the includes or excludes are defined as parameters
	 * @param evaluationContextProvider to provide an evaluation context for SpEL evaluation
	 * @return source filter with includes and excludes for a query, {@literal null} when no {@link SourceFilters}
	 *         annotation was set on the method.
	 * @since 5.0
	 */
	@Nullable
	SourceFilter getSourceFilter(ElasticsearchParametersParameterAccessor parameterAccessor,
			ElasticsearchConverter converter,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		if (sourceFilters == null || (sourceFilters.includes().length == 0 && sourceFilters.excludes().length == 0)) {
			return null;
		}

		ConversionService conversionService = converter.getConversionService();
		FetchSourceFilterBuilder fetchSourceFilterBuilder = new FetchSourceFilterBuilder();

		if (sourceFilters.includes().length > 0) {
			fetchSourceFilterBuilder.withIncludes(mapParameters(sourceFilters.includes(), parameterAccessor,
					conversionService, evaluationContextProvider));
		}

		if (sourceFilters.excludes().length > 0) {
			fetchSourceFilterBuilder.withExcludes(mapParameters(sourceFilters.excludes(), parameterAccessor,
					conversionService, evaluationContextProvider));
		}

		return fetchSourceFilterBuilder.build();
	}

	private String[] mapParameters(String[] source, ElasticsearchParametersParameterAccessor parameterAccessor,
			ConversionService conversionService, QueryMethodEvaluationContextProvider evaluationContextProvider) {

		List<String> fieldNames = new ArrayList<>();

		for (String s : source) {

			if (!s.isBlank()) {
				String fieldName = new QueryStringProcessor(s, this, conversionService, evaluationContextProvider)
						.createQuery(parameterAccessor);
				// this could be "[\"foo\",\"bar\"]", must be split
				if (fieldName.startsWith("[") && fieldName.endsWith("]")) {
					// noinspection RegExpRedundantEscape
					fieldNames.addAll( //
							Arrays.asList(fieldName.substring(1, fieldName.length() - 2) //
									.replaceAll("\\\"", "") //
									.split(","))); //
				} else {
					fieldNames.add(fieldName);
				}
			}
		}

		return fieldNames.toArray(new String[0]);
	}

	// region Copied from QueryMethod base class
	/*
	 * Copied from the QueryMethod class adding support for collections of SearchHit instances. No static method here.
	 */
	private Class<?> potentiallyUnwrapReturnTypeFor(RepositoryMetadata metadata, Method method) {
		TypeInformation<?> returnType = metadata.getReturnType(method);
		if (!QueryExecutionConverters.supports(returnType.getType())
				&& !ReactiveWrapperConverters.supports(returnType.getType())) {
			return returnType.getType();
		} else {
			TypeInformation<?> componentType = returnType.getComponentType();
			if (componentType == null) {
				throw new IllegalStateException(
						String.format("Couldn't find component type for return value of method %s", method));
			} else {

				if (SearchHit.class.isAssignableFrom(componentType.getType())) {
					unwrappedReturnTypeFromSearchHit = true;
					return componentType.getComponentType().getType();
				} else {
					return componentType.getType();
				}
			}
		}
	}

	void addMethodParameter(BaseQuery query, ElasticsearchParametersParameterAccessor parameterAccessor,
			ElasticsearchConverter elasticsearchConverter,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		if (hasAnnotatedHighlight()) {
			var highlightQuery = getAnnotatedHighlightQuery(new HighlightConverter(parameterAccessor,
					elasticsearchConverter.getConversionService(), evaluationContextProvider, this));
			query.setHighlightQuery(highlightQuery);
		}

		var sourceFilter = getSourceFilter(parameterAccessor, elasticsearchConverter, evaluationContextProvider);
		if (sourceFilter != null) {
			query.addSourceFilter(sourceFilter);
		}

		if (parameterAccessor.getParameters() instanceof ElasticsearchParameters methodParameters) {
			var values = parameterAccessor.getValues();

			methodParameters.getScriptedFields().forEach(elasticsearchParameter -> {
				var index = elasticsearchParameter.getIndex();

				if (index >= 0 && index < values.length) {
					query.addScriptedField((ScriptedField) values[index]);
				}
			});

			methodParameters.getRuntimeFields().forEach(elasticsearchParameter -> {
				var index = elasticsearchParameter.getIndex();

				if (index >= 0 && index < values.length) {
					var runtimeField = (RuntimeField) values[index];
					query.addRuntimeField(runtimeField);
					query.addFields(runtimeField.getName());
				}

			});

			var needToAddSourceFilter = sourceFilter == null
					&& !(methodParameters.getRuntimeFields().isEmpty()
							&& methodParameters.getScriptedFields().isEmpty());
			if (needToAddSourceFilter) {
				query.addSourceFilter(FetchSourceFilter.of(b -> b.withIncludes("*")));
			}
		}
	}
	// endregion

}
