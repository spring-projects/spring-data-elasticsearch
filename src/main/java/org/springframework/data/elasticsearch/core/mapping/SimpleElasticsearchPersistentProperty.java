/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Parent;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch specific {@link org.springframework.data.mapping.PersistentProperty} implementation processing
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Sascha Woo
 * @author Oliver Gierke
 * @author Peter-Josef Meisch
 */
public class SimpleElasticsearchPersistentProperty extends
		AnnotationBasedPersistentProperty<ElasticsearchPersistentProperty> implements ElasticsearchPersistentProperty {

	private static final List<String> SUPPORTED_ID_PROPERTY_NAMES = Arrays.asList("id", "document");

	private final boolean isScore;
	private final boolean isParent;
	private final boolean isId;
	private final @Nullable String annotatedFieldName;

	public SimpleElasticsearchPersistentProperty(Property property,
			PersistentEntity<?, ElasticsearchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.annotatedFieldName = getAnnotatedFieldName();
		this.isId = super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
		this.isScore = isAnnotationPresent(Score.class);
		this.isParent = isAnnotationPresent(Parent.class);

		if (isVersionProperty() && !getType().equals(Long.class)) {
			throw new MappingException(String.format("Version property %s must be of type Long!", property.getName()));
		}

		if (isScore && !getType().equals(Float.TYPE) && !getType().equals(Float.class)) {
			throw new MappingException(
					String.format("Score property %s must be either of type float or Float!", property.getName()));
		}

		if (isParent && !getType().equals(String.class)) {
			throw new MappingException(String.format("Parent property %s must be of type String!", property.getName()));
		}
	}

	@Nullable
	private String getAnnotatedFieldName() {

		if (isAnnotationPresent(Field.class)) {

			String name = findAnnotation(Field.class).name();
			return StringUtils.hasText(name) ? name : null;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty#getFieldName()
	 */
	@Override
	public String getFieldName() {
		return annotatedFieldName == null ? getProperty().getName() : annotatedFieldName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isIdProperty()
	 */
	@Override
	public boolean isIdProperty() {
		return isId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#createAssociation()
	 */
	@Override
	protected Association<ElasticsearchPersistentProperty> createAssociation() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty#isScoreProperty()
	 */
	@Override
	public boolean isScoreProperty() {
		return isScore;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#isImmutable()
	 */
	@Override
	public boolean isImmutable() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty#isParentProperty()
	 */
	@Override
	public boolean isParentProperty() {
		return isParent;
	}
}
