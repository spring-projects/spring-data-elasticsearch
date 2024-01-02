/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support.querybyexample;

import java.util.Map;
import java.util.Optional;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Maps a {@link Example} to a {@link org.springframework.data.elasticsearch.core.query.Criteria}
 *
 * @author Ezequiel Antúnez Camacho
 * @since 5.1
 */
class ExampleCriteriaMapper {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	/**
	 * Builds a {@link ExampleCriteriaMapper}
	 *
	 * @param mappingContext mappingContext to use
	 */
	ExampleCriteriaMapper(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		this.mappingContext = mappingContext;
	}

	<S> Criteria criteria(Example<S> example) {
		return buildCriteria(example);
	}

	private <S> Criteria buildCriteria(Example<S> example) {
		final ExampleMatcherAccessor matcherAccessor = new ExampleMatcherAccessor(example.getMatcher());

		return applyPropertySpecs(new Criteria(), "", example.getProbe(),
				mappingContext.getRequiredPersistentEntity(example.getProbeType()), matcherAccessor,
				example.getMatcher().getMatchMode());
	}

	private Criteria applyPropertySpecs(Criteria criteria, String path, @Nullable Object probe,
			ElasticsearchPersistentEntity<?> persistentEntity, ExampleMatcherAccessor exampleSpecAccessor,
			ExampleMatcher.MatchMode matchMode) {

		if (probe == null) {
			return criteria;
		}

		PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(probe);

		for (ElasticsearchPersistentProperty property : persistentEntity) {
			final String propertyName = property.getName();
			String propertyPath = StringUtils.hasText(path) ? (path + "." + propertyName) : propertyName;
			if (exampleSpecAccessor.isIgnoredPath(propertyPath) || property.isCollectionLike()
					|| property.isVersionProperty()) {
				continue;
			}

			Object propertyValue = propertyAccessor.getProperty(property);
			if (property.isMap() && propertyValue != null) {
				for (Map.Entry<String, Object> entry : ((Map<String, Object>) propertyValue).entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();
					criteria = applyPropertySpec(propertyPath + "." + key, value, exampleSpecAccessor, property, matchMode,
							criteria);
				}
				continue;
			}

			criteria = applyPropertySpec(propertyPath, propertyValue, exampleSpecAccessor, property, matchMode, criteria);
		}
		return criteria;
	}

	private Criteria applyPropertySpec(String path, Object propertyValue, ExampleMatcherAccessor exampleSpecAccessor,
			ElasticsearchPersistentProperty property, ExampleMatcher.MatchMode matchMode, Criteria criteria) {

		if (exampleSpecAccessor.isIgnoreCaseForPath(path)) {
			throw new InvalidDataAccessApiUsageException(
					"Current implementation of Query-by-Example supports only case-sensitive matching.");
		}

		final Object transformedValue = exampleSpecAccessor.getValueTransformerForPath(path)
				.apply(Optional.ofNullable(propertyValue)).orElse(null);

		if (transformedValue == null) {
			criteria = tryToAppendMustNotSentence(criteria, path, exampleSpecAccessor);
		} else {
			if (property.isEntity()) {
				return applyPropertySpecs(criteria, path, transformedValue,
						mappingContext.getRequiredPersistentEntity(property), exampleSpecAccessor, matchMode);
			} else {
				return applyStringMatcher(applyMatchMode(criteria, path, matchMode), transformedValue,
						exampleSpecAccessor.getStringMatcherForPath(path));
			}
		}
		return criteria;
	}

	private Criteria tryToAppendMustNotSentence(Criteria criteria, String path,
			ExampleMatcherAccessor exampleSpecAccessor) {
		if (ExampleMatcher.NullHandler.INCLUDE.equals(exampleSpecAccessor.getNullHandler())
				|| exampleSpecAccessor.hasPropertySpecifier(path)) {
			return criteria.and(path).not().exists();
		}
		return criteria;
	}

	private Criteria applyMatchMode(Criteria criteria, String path, ExampleMatcher.MatchMode matchMode) {
		if (matchMode == ExampleMatcher.MatchMode.ALL) {
			return criteria.and(path);
		} else {
			return criteria.or(path);
		}
	}

	private Criteria applyStringMatcher(Criteria criteria, Object value, ExampleMatcher.StringMatcher stringMatcher) {
		return switch (stringMatcher) {
			case DEFAULT -> criteria.is(value);
			case EXACT -> criteria.matchesAll(value);
			case STARTING -> criteria.startsWith(validateString(value));
			case ENDING -> criteria.endsWith(validateString(value));
			case CONTAINING -> criteria.contains(validateString(value));
			case REGEX -> criteria.regexp(validateString(value));
		};
	}

	private String validateString(Object value) {
		if (value instanceof String) {
			return value.toString();
		}
		throw new IllegalArgumentException("This operation requires a String but got " + value.getClass());
	}

}
