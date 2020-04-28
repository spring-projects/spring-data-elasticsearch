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

import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Parent;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchDateConverter;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
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
 * @author Roman Puchkovskiy
 */
public class SimpleElasticsearchPersistentProperty extends
		AnnotationBasedPersistentProperty<ElasticsearchPersistentProperty> implements ElasticsearchPersistentProperty {

	private static final List<String> SUPPORTED_ID_PROPERTY_NAMES = Arrays.asList("id", "document");

	private final boolean isScore;
	private final boolean isParent;
	private final boolean isId;
	private final boolean isSeqNoPrimaryTerm;
	private final @Nullable String annotatedFieldName;
	@Nullable private ElasticsearchPersistentPropertyConverter propertyConverter;

	public SimpleElasticsearchPersistentProperty(Property property,
			PersistentEntity<?, ElasticsearchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.annotatedFieldName = getAnnotatedFieldName();
		this.isId = super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
		this.isScore = isAnnotationPresent(Score.class);
		this.isParent = isAnnotationPresent(Parent.class);
		this.isSeqNoPrimaryTerm = SeqNoPrimaryTerm.class.isAssignableFrom(getRawType());

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

		initDateConverter();
	}

	@Override
	public boolean hasPropertyConverter() {
		return propertyConverter != null;
	}

	@Nullable
	@Override
	public ElasticsearchPersistentPropertyConverter getPropertyConverter() {
		return propertyConverter;
	}

	@Override
	public boolean isWritable() {
		return super.isWritable() && !isSeqNoPrimaryTermProperty();
	}

	@Override
	public boolean isReadable() {
		return !isTransient() && !isSeqNoPrimaryTermProperty();
	}

	/**
	 * Initializes an {@link ElasticsearchPersistentPropertyConverter} if this property is annotated as a Field with type
	 * {@link FieldType#Date}, has a {@link DateFormat} set and if the type of the property is one of the Java8 temporal
	 * classes or java.util.Date.
	 */
	private void initDateConverter() {
		Field field = findAnnotation(Field.class);
		boolean isTemporalAccessor = TemporalAccessor.class.isAssignableFrom(getType());
		boolean isDate = Date.class.isAssignableFrom(getType());

		if (field != null && field.type() == FieldType.Date && (isTemporalAccessor || isDate)) {
			DateFormat dateFormat = field.format();

			ElasticsearchDateConverter converter = null;

			if (dateFormat == DateFormat.custom) {
				String pattern = field.pattern();

				if (StringUtils.hasLength(pattern)) {
					converter = ElasticsearchDateConverter.of(pattern);
				}
			} else if (dateFormat != DateFormat.none) {
				converter = ElasticsearchDateConverter.of(dateFormat);
			}

			if (converter != null) {
				ElasticsearchDateConverter dateConverter = converter;
				propertyConverter = new ElasticsearchPersistentPropertyConverter() {
					@Override
					public String write(Object property) {
						if (isTemporalAccessor) {
							return dateConverter.format((TemporalAccessor) property);
						} else { // must be Date
							return dateConverter.format((Date) property);
						}
					}

					@SuppressWarnings("unchecked")
					@Override
					public Object read(String s) {
						if (isTemporalAccessor) {
							return dateConverter.parse(s, (Class<? extends TemporalAccessor>) getType());
						} else { // must be date
							return dateConverter.parse(s);
						}
					}
				};
			}
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
		throw new UnsupportedOperationException();
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty#isSeqNoPrimaryTermProperty()
	 */
	@Override
	public boolean isSeqNoPrimaryTermProperty() {
		return isSeqNoPrimaryTerm;
	}
}
