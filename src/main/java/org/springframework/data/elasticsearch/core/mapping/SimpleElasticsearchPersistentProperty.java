/*
 * Copyright 2013-2021 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.GeoShapeField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.ValueConverter;
import org.springframework.data.elasticsearch.core.Range;
import org.springframework.data.elasticsearch.core.convert.DatePropertyValueConverter;
import org.springframework.data.elasticsearch.core.convert.DateRangePropertyValueConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchDateConverter;
import org.springframework.data.elasticsearch.core.convert.NumberRangePropertyValueConverter;
import org.springframework.data.elasticsearch.core.convert.TemporalPropertyValueConverter;
import org.springframework.data.elasticsearch.core.convert.TemporalRangePropertyValueConverter;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
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
	private static final PropertyNameFieldNamingStrategy DEFAULT_FIELD_NAMING_STRATEGY = PropertyNameFieldNamingStrategy.INSTANCE;

	private final boolean isId;
	private final boolean isSeqNoPrimaryTerm;
	private final @Nullable String annotatedFieldName;
	@Nullable private PropertyValueConverter propertyValueConverter;
	private final boolean storeNullValue;

	public SimpleElasticsearchPersistentProperty(Property property,
			PersistentEntity<?, ElasticsearchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.annotatedFieldName = getAnnotatedFieldName();
		this.isId = super.isIdProperty()
				|| (SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName()) && !hasExplicitFieldName());
		this.isSeqNoPrimaryTerm = SeqNoPrimaryTerm.class.isAssignableFrom(getRawType());

		boolean isField = isAnnotationPresent(Field.class);

		if (isVersionProperty() && !getType().equals(Long.class)) {
			throw new MappingException(String.format("Version property %s must be of type Long!", property.getName()));
		}

		if (isField && isAnnotationPresent(MultiField.class)) {
			throw new MappingException("@Field annotation must not be used on a @MultiField property.");
		}

		initPropertyValueConverter();

		storeNullValue = isField && getRequiredAnnotation(Field.class).storeNullValue();
	}

	@Override
	public boolean hasPropertyValueConverter() {
		return propertyValueConverter != null;
	}

	@Nullable
	@Override
	public PropertyValueConverter getPropertyValueConverter() {
		return propertyValueConverter;
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

	protected boolean hasExplicitFieldName() {
		return StringUtils.hasText(getAnnotatedFieldName());
	}

	/**
	 * Initializes the property converter for this {@link PersistentProperty}, if any.
	 */
	private void initPropertyValueConverter() {

		initPropertyValueConverterFromAnnotation();

		if (hasPropertyValueConverter()) {
			return;
		}

		Class<?> actualType = getActualTypeOrNull();
		if (actualType == null) {
			return;
		}

		Field field = findAnnotation(Field.class);
		if (field == null) {
			return;
		}

		switch (field.type()) {
			case Date:
			case Date_Nanos: {
				List<ElasticsearchDateConverter> dateConverters = getDateConverters(field, actualType);
				if (dateConverters.isEmpty()) {
					LOGGER.warn("No date formatters configured for property '{}'.", getName());
					return;
				}

				if (TemporalAccessor.class.isAssignableFrom(actualType)) {
					propertyValueConverter = new TemporalPropertyValueConverter(this, dateConverters);
				} else if (Date.class.isAssignableFrom(actualType)) {
					propertyValueConverter = new DatePropertyValueConverter(this, dateConverters);
				} else {
					LOGGER.warn("Unsupported type '{}' for date property '{}'.", actualType, getName());
				}
				break;
			}
			case Date_Range: {
				if (!Range.class.isAssignableFrom(actualType)) {
					return;
				}

				List<ElasticsearchDateConverter> dateConverters = getDateConverters(field, actualType);
				if (dateConverters.isEmpty()) {
					LOGGER.warn("No date formatters configured for property '{}'.", getName());
					return;
				}

				Class<?> genericType = getTypeInformation().getTypeArguments().get(0).getType();
				if (TemporalAccessor.class.isAssignableFrom(genericType)) {
					propertyValueConverter = new TemporalRangePropertyValueConverter(this, dateConverters);
				} else if (Date.class.isAssignableFrom(genericType)) {
					propertyValueConverter = new DateRangePropertyValueConverter(this, dateConverters);
				} else {
					LOGGER.warn("Unsupported generic type '{}' for date range property '{}'.", genericType, getName());
				}
				break;
			}
			case Integer_Range:
			case Float_Range:
			case Long_Range:
			case Double_Range: {
				if (!Range.class.isAssignableFrom(actualType)) {
					return;
				}

				Class<?> genericType = getTypeInformation().getTypeArguments().get(0).getType();
				if ((field.type() == FieldType.Integer_Range && !Integer.class.isAssignableFrom(genericType))
						|| (field.type() == FieldType.Float_Range && !Float.class.isAssignableFrom(genericType))
						|| (field.type() == FieldType.Long_Range && !Long.class.isAssignableFrom(genericType))
						|| (field.type() == FieldType.Double_Range && !Double.class.isAssignableFrom(genericType))) {
					LOGGER.warn("Unsupported generic type '{}' for range field type '{}' of property '{}'.", genericType,
							field.type(), getName());
					return;
				}

				propertyValueConverter = new NumberRangePropertyValueConverter(this);
				break;
			}
			case Ip_Range: {
				// TODO currently unsupported, needs a library like https://seancfoley.github.io/IPAddress/
			}
			default:
				break;
		}
	}

	private void initPropertyValueConverterFromAnnotation() {

		ValueConverter annotation = findAnnotation(ValueConverter.class);

		if (annotation != null) {
			Class<? extends PropertyValueConverter> clazz = annotation.value();

			if (Enum.class.isAssignableFrom(clazz)) {
				PropertyValueConverter[] enumConstants = clazz.getEnumConstants();

				if (enumConstants == null || enumConstants.length != 1) {
					throw new IllegalArgumentException(clazz + " is an enum with more than 1 constant and cannot be used here");
				}
				propertyValueConverter = enumConstants[0];
			} else {
				propertyValueConverter = BeanUtils.instantiateClass(clazz);
			}
		}
	}

	private List<ElasticsearchDateConverter> getDateConverters(Field field, Class<?> actualType) {

		DateFormat[] dateFormats = field.format();
		String[] dateFormatPatterns = field.pattern();
		List<ElasticsearchDateConverter> converters = new ArrayList<>();

		if (dateFormats.length == 0 && dateFormatPatterns.length == 0) {
			LOGGER.warn(
					"Property '{}' has @Field type '{}' but has no built-in format or custom date pattern defined. Make sure you have a converter registered for type {}.",
					getName(), field.type().name(), actualType.getSimpleName());
			return converters;
		}

		// register converters for built-in formats
		for (DateFormat dateFormat : dateFormats) {
			switch (dateFormat) {
				case none:
				case custom:
					break;
				case weekyear:
				case weekyear_week:
				case weekyear_week_day:
					LOGGER.warn("No default converter available for '{}' and date format '{}'. Use a custom converter instead.",
							actualType.getName(), dateFormat.name());
					break;
				default:
					converters.add(ElasticsearchDateConverter.of(dateFormat));
					break;
			}
		}

		for (String dateFormatPattern : dateFormatPatterns) {
			if (!StringUtils.hasText(dateFormatPattern)) {
				throw new MappingException(String.format("Date pattern of property '%s' must not be empty", getName()));
			}
			converters.add(ElasticsearchDateConverter.of(dateFormatPattern));
		}

		return converters;
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

		if (annotatedFieldName == null) {
			FieldNamingStrategy fieldNamingStrategy = getFieldNamingStrategy();
			String fieldName = fieldNamingStrategy.getFieldName(this);

			if (!StringUtils.hasText(fieldName)) {
				throw new MappingException(String.format("Invalid (null or empty) field name returned for property %s by %s!",
						this, fieldNamingStrategy.getClass()));
			}

			return fieldName;
		}

		return annotatedFieldName;
	}

	private FieldNamingStrategy getFieldNamingStrategy() {
		PersistentEntity<?, ElasticsearchPersistentProperty> owner = getOwner();

		if (owner instanceof ElasticsearchPersistentEntity) {
			return ((ElasticsearchPersistentEntity<?>) owner).getFieldNamingStrategy();
		}

		return DEFAULT_FIELD_NAMING_STRATEGY;
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
