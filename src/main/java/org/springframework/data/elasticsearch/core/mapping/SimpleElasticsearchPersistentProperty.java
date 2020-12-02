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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.GeoShapeField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Parent;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchDateConverter;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.join.JoinField;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleElasticsearchPersistentProperty.class);

	private static final List<String> SUPPORTED_ID_PROPERTY_NAMES = Arrays.asList("id", "document");

	private final boolean isScore;
	private final boolean isParent;
	private final boolean isId;
	private final boolean isSeqNoPrimaryTerm;
	private final @Nullable String annotatedFieldName;
	@Nullable private ElasticsearchPersistentPropertyConverter propertyConverter;
	private final boolean storeNullValue;

	public SimpleElasticsearchPersistentProperty(Property property,
			PersistentEntity<?, ElasticsearchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.annotatedFieldName = getAnnotatedFieldName();
		this.isId = super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());

		// deprecated since 4.1
		@Deprecated
		boolean isIdWithoutAnnotation = isId && !isAnnotationPresent(Id.class);
		if (isIdWithoutAnnotation && owner.isAnnotationPresent(Document.class)) {
			LOGGER.warn("Using the property name of '{}' to identify the id property is deprecated."
					+ " Please annotate the id property with '@Id'", owner.getName() + "." + getName());
		}

		this.isScore = isAnnotationPresent(Score.class);
		this.isParent = isAnnotationPresent(Parent.class);
		this.isSeqNoPrimaryTerm = SeqNoPrimaryTerm.class.isAssignableFrom(getRawType());

		boolean isField = isAnnotationPresent(Field.class);

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

		if (isField && isAnnotationPresent(MultiField.class)) {
			throw new MappingException("@Field annotation must not be used on a @MultiField property.");
		}

		initDateConverter();

		storeNullValue = isField && getRequiredAnnotation(Field.class).storeNullValue();
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

	@Override
	public boolean storeNullValue() {
		return storeNullValue;
	}

	/**
	 * Initializes an {@link ElasticsearchPersistentPropertyConverter} if this property is annotated as a Field with type
	 * {@link FieldType#Date}, has a {@link DateFormat} set and if the type of the property is one of the Java8 temporal
	 * classes or java.util.Date.
	 */
	private void initDateConverter() {
		Field field = findAnnotation(Field.class);

		Class<?> actualType = getActualTypeOrNull();

		if (actualType == null) {
			return;
		}

		boolean isTemporalAccessor = TemporalAccessor.class.isAssignableFrom(actualType);
		boolean isDate = Date.class.isAssignableFrom(actualType);

		if (field != null && (field.type() == FieldType.Date || field.type() == FieldType.Date_Nanos)
				&& (isTemporalAccessor || isDate)) {
			DateFormat dateFormat = field.format();

			String property = getOwner().getType().getSimpleName() + "." + getName();

			if (dateFormat == DateFormat.none) {
				LOGGER.warn(
						String.format("No DateFormat defined for property %s. Make sure you have a Converter registered for %s",
								property, actualType.getSimpleName()));
				return;
			}

			ElasticsearchDateConverter converter;

			if (dateFormat == DateFormat.custom) {
				String pattern = field.pattern();

				if (!StringUtils.hasLength(pattern)) {
					throw new MappingException(
							String.format("Property %s is annotated with FieldType.%s and a custom format but has no pattern defined",
									property, field.type().name()));
				}

				converter = ElasticsearchDateConverter.of(pattern);
			} else {
				converter = ElasticsearchDateConverter.of(dateFormat);
			}

			propertyConverter = new ElasticsearchPersistentPropertyConverter() {
				final ElasticsearchDateConverter dateConverter = converter;

				@Override
				public String write(Object property) {
					if (isTemporalAccessor && TemporalAccessor.class.isAssignableFrom(property.getClass())) {
						return dateConverter.format((TemporalAccessor) property);
					} else if (isDate && Date.class.isAssignableFrom(property.getClass())) {
						return dateConverter.format((Date) property);
					} else {
						return property.toString();
					}
				}

				@SuppressWarnings("unchecked")
				@Override
				public Object read(String s) {
					if (isTemporalAccessor) {
						return dateConverter.parse(s, (Class<? extends TemporalAccessor>) actualType);
					} else { // must be date
						return dateConverter.parse(s);
					}
				}
			};
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Nullable
	private String getAnnotatedFieldName() {

		String name = null;

		if (isAnnotationPresent(Field.class)) {
			name = findAnnotation(Field.class).name();
		} else if (isAnnotationPresent(MultiField.class)) {
			name = findAnnotation(MultiField.class).mainField().name();
		}

		return StringUtils.hasText(name) ? name : null;
	}

	@Override
	public String getFieldName() {
		return annotatedFieldName == null ? getProperty().getName() : annotatedFieldName;
	}

	@Override
	public boolean isIdProperty() {
		return isId;
	}

	@Override
	protected Association<ElasticsearchPersistentProperty> createAssociation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isScoreProperty() {
		return isScore;
	}

	@Override
	public boolean isImmutable() {
		return false;
	}

	@Override
	public boolean isParentProperty() {
		return isParent;
	}

	@Override
	public boolean isSeqNoPrimaryTermProperty() {
		return isSeqNoPrimaryTerm;
	}

	@Override
	public boolean isGeoPointProperty() {
		return getActualType() == GeoPoint.class || isAnnotationPresent(GeoPointField.class);
	}

	@Override
	public boolean isGeoShapeProperty() {
		return GeoJson.class.isAssignableFrom(getActualType()) || isAnnotationPresent(GeoShapeField.class);
	}

	@Override
	public boolean isJoinFieldProperty() {
		return getActualType() == JoinField.class;
	}

	@Override
	public boolean isCompletionProperty() {
		return getActualType() == Completion.class;
	}
}
