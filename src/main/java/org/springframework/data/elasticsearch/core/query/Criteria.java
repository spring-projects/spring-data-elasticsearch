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
package org.springframework.data.elasticsearch.core.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Criteria is the central class when constructing queries. It follows more or less a fluent API style, which allows to
 * easily chain together multiple criteria. <br/>
 * <br/>
 * A Criteria references a field and has {@link CriteriaEntry} sets for query and filter context. When building the
 * query, the entries from the criteria entries are combined in a bool must query (if more than one.<br/>
 * <br/>
 * A Criteria also has a {@link CriteriaChain} which is used to build a collection of Criteria with the fluent API. The
 * value of {@link #isAnd()} and {@link #isOr()} describes whether the queries built from the criteria chain should be
 * put in a must (and) or a should (or) clause when a query is built from it, it is not used to build some relationship
 * between the elements of the criteria chain.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Peter-Josef Meisch
 * @author Ezequiel Antúnez Camacho
 */
public class Criteria {

	public static final String CRITERIA_VALUE_SEPARATOR = " ";

	private @Nullable Field field;
	private float boost = Float.NaN;
	private boolean negating = false;

	private final CriteriaChain criteriaChain = new CriteriaChain();
	private final Set<CriteriaEntry> queryCriteriaEntries = new LinkedHashSet<>();
	private final Set<CriteriaEntry> filterCriteriaEntries = new LinkedHashSet<>();
	private final Set<Criteria> subCriteria = new LinkedHashSet<>();

	// region criteria creation

	/**
	 * @return factory method to create an and-Criteria that is not bound to a field
	 * @since 4.1
	 */
	public static Criteria and() {
		return new Criteria();
	}

	/**
	 * @return factory method to create an or-Criteria that is not bound to a field
	 * @since 4.1
	 */
	public static Criteria or() {
		return new OrCriteria();
	}

	public Criteria() {}

	/**
	 * Creates a new Criteria with provided field name
	 *
	 * @param fieldName the field name
	 */
	public Criteria(String fieldName) {
		this(new SimpleField(fieldName));
	}

	/**
	 * Creates a new Criteria for the given field
	 *
	 * @param field field to create the Criteria for
	 */
	public Criteria(Field field) {

		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.field = field;
		this.criteriaChain.add(this);
	}

	/**
	 * Creates a Criteria for the given field, sets it's criteriaChain to the given value and adds itself to the end of
	 * the chain.
	 *
	 * @param criteriaChain the chain to add to
	 * @param fieldName field to create the Criteria for
	 */
	protected Criteria(List<Criteria> criteriaChain, String fieldName) {
		this(criteriaChain, new SimpleField(fieldName));
	}

	/**
	 * Creates a Criteria for the given field, sets it's criteriaChain to the given value and adds itself to the end of
	 * the chain.
	 *
	 * @param criteriaChain the chain to add to
	 * @param field field to create the Criteria for
	 */
	protected Criteria(List<Criteria> criteriaChain, Field field) {

		Assert.notNull(criteriaChain, "CriteriaChain must not be null");
		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.field = field;
		this.criteriaChain.addAll(criteriaChain);
		this.criteriaChain.add(this);
	}

	/**
	 * Static factory method to create a new Criteria for field with given name
	 *
	 * @param fieldName field to create the Criteria for
	 */
	public static Criteria where(String fieldName) {
		return new Criteria(fieldName);
	}

	/**
	 * Static factory method to create a new Criteria for provided field
	 *
	 * @param field field to create the Criteria for
	 */
	public static Criteria where(Field field) {
		return new Criteria(field);
	}
	// endregion

	// region criteria attributes
	/**
	 * @return the Field targeted by this Criteria
	 */
	@Nullable
	public Field getField() {
		return this.field;
	}

	public Set<CriteriaEntry> getQueryCriteriaEntries() {
		return Collections.unmodifiableSet(this.queryCriteriaEntries);
	}

	public Set<CriteriaEntry> getFilterCriteriaEntries() {
		return Collections.unmodifiableSet(this.filterCriteriaEntries);
	}

	public Operator getOperator() {
		return Operator.AND;
	}

	public List<Criteria> getCriteriaChain() {
		return Collections.unmodifiableList(this.criteriaChain);
	}

	/**
	 * Sets the negating flag
	 *
	 * @return this object
	 */
	public Criteria not() {
		this.negating = true;
		return this;
	}

	public boolean isNegating() {
		return this.negating;
	}

	/**
	 * Sets the boost factor.
	 *
	 * @param boost boost factor
	 * @return this object
	 */
	public Criteria boost(float boost) {

		Assert.isTrue(boost >= 0, "boost must not be negative");

		this.boost = boost;
		return this;
	}

	public float getBoost() {
		return this.boost;
	}

	public boolean isAnd() {
		return getOperator() == Operator.AND;
	}

	public boolean isOr() {
		return getOperator() == Operator.OR;
	}

	/**
	 * @return the set ob subCriteria
	 * @since 4.1
	 */
	public Set<Criteria> getSubCriteria() {
		return subCriteria;
	}

	// endregion

	// region criteria chaining
	/**
	 * Chain a new and-Criteria
	 *
	 * @param field the field for the new Criteria
	 * @return the new chained Criteria
	 */
	public Criteria and(Field field) {
		return new Criteria(criteriaChain, field);
	}

	/**
	 * Chain a new and- Criteria
	 *
	 * @param fieldName the field for the new Criteria
	 * @return the new chained Criteria
	 */
	public Criteria and(String fieldName) {
		return new Criteria(criteriaChain, fieldName);
	}

	/**
	 * Chain a Criteria to this object.
	 *
	 * @param criteria the Criteria to add
	 * @return this object
	 */
	public Criteria and(Criteria criteria) {

		Assert.notNull(criteria, "Cannot chain 'null' criteria.");

		this.criteriaChain.add(criteria);
		return this;
	}

	/**
	 * Chain an array of Criteria to this object.
	 *
	 * @param criterias the Criteria to add
	 * @return this object
	 */
	public Criteria and(Criteria... criterias) {

		Assert.notNull(criterias, "Cannot chain 'null' criterias.");

		this.criteriaChain.addAll(Arrays.asList(criterias));
		return this;
	}

	/**
	 * Chain a new or-Criteria
	 *
	 * @param field the field for the new Criteria
	 * @return the new chained Criteria
	 */
	public Criteria or(Field field) {
		return new OrCriteria(this.criteriaChain, field);
	}

	/**
	 * Chain a new or-Criteria
	 *
	 * @param fieldName the field for the new Criteria
	 * @return the new chained Criteria
	 */
	public Criteria or(String fieldName) {
		return or(new SimpleField(fieldName));
	}

	/**
	 * Chain a new or-Criteria. The new Criteria uses the {@link #getField()}, {@link #getQueryCriteriaEntries()} and
	 * {@link #getFilterCriteriaEntries()} of the passed in parameter. the new created criteria is added to the criteria
	 * chain.
	 *
	 * @param criteria contains the information for the new Criteria
	 * @return the new chained criteria
	 */
	public Criteria or(Criteria criteria) {

		Assert.notNull(criteria, "Cannot chain 'null' criteria.");
		Assert.notNull(criteria.getField(), "Cannot chain Criteria with no field");

		Criteria orCriteria = new OrCriteria(this.criteriaChain, criteria.getField());
		orCriteria.queryCriteriaEntries.addAll(criteria.queryCriteriaEntries);
		orCriteria.filterCriteriaEntries.addAll(criteria.filterCriteriaEntries);
		orCriteria.subCriteria.addAll(criteria.subCriteria);
		orCriteria.boost = criteria.boost;
		orCriteria.negating = criteria.isNegating();
		return orCriteria;
	}

	/**
	 * adds a Criteria as subCriteria
	 *
	 * @param criteria the criteria to add, must not be {@literal null}
	 * @return this object
	 * @since 4.1
	 */
	public Criteria subCriteria(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		subCriteria.add(criteria);
		return this;
	}

	// endregion

	// region criteria entries - query
	/**
	 * Add a {@link OperationKey#EQUALS} entry to the {@link #queryCriteriaEntries}
	 *
	 * @param o the argument to the operation
	 * @return this object
	 */
	public Criteria is(Object o) {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EQUALS, o));
		return this;
	}

	/**
	 * Add a {@link OperationKey#EXISTS} entry to the {@link #queryCriteriaEntries}
	 *
	 * @return this object
	 * @since 4.0
	 */
	public Criteria exists() {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EXISTS));
		return this;
	}

	/**
	 * Adds a OperationKey.BETWEEN entry to the {@link #queryCriteriaEntries}. Only one of the parameters may be null to
	 * define an unbounded end of the range.
	 *
	 * @param lowerBound the lower bound of the range, null for unbounded
	 * @param upperBound the upper bound of the range, null for unbounded
	 * @return this object
	 */
	public Criteria between(@Nullable Object lowerBound, @Nullable Object upperBound) {

		if (lowerBound == null && upperBound == null) {
			throw new InvalidDataAccessApiUsageException("Range [* TO *] is not allowed");
		}

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.BETWEEN, new Object[] { lowerBound, upperBound }));
		return this;
	}

	/**
	 * Add a {@link OperationKey#STARTS_WITH} entry to the {@link #queryCriteriaEntries}
	 *
	 * @param s the argument to the operation
	 * @return this object
	 */
	public Criteria startsWith(String s) {

		Assert.notNull(s, "s may not be null");

		assertNoBlankInWildcardQuery(s, false, true);
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.STARTS_WITH, s));
		return this;
	}

	/**
	 * Add a {@link OperationKey#CONTAINS} entry to the {@link #queryCriteriaEntries} <br/>
	 * <strong>NOTE: </strong> mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 *
	 * @param s the argument to the operation
	 * @return this object
	 */
	public Criteria contains(String s) {

		Assert.notNull(s, "s may not be null");

		assertNoBlankInWildcardQuery(s, true, true);
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.CONTAINS, s));
		return this;
	}

	/**
	 * Add a {@link OperationKey#ENDS_WITH} entry to the {@link #queryCriteriaEntries} <br/>
	 * <strong>NOTE: </strong> mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 *
	 * @param s the argument to the operation
	 * @return this object
	 */
	public Criteria endsWith(String s) {

		Assert.notNull(s, "s may not be null");

		assertNoBlankInWildcardQuery(s, true, false);
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.ENDS_WITH, s));
		return this;
	}

	/**
	 * Add a {@link OperationKey#IN} entry to the {@link #queryCriteriaEntries}. This will create a terms query, so don't
	 * use it with text fields as these are analyzed and changed by Elasticsearch (converted to lowercase with the default
	 * analyzer). If used for Strings, these should be marked as field type Keyword.
	 *
	 * @param values the argument to the operation
	 * @return this object
	 */
	public Criteria in(Object... values) {
		return in(toCollection(values));
	}

	/**
	 * Add a {@link OperationKey#IN} entry to the {@link #queryCriteriaEntries}. See the comment at
	 * {@link Criteria#in(Object...)}.
	 *
	 * @param values the argument to the operation
	 * @return this object
	 */
	public Criteria in(Iterable<?> values) {

		Assert.notNull(values, "Collection of 'in' values must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.IN, values));
		return this;
	}

	/**
	 * Add a {@link OperationKey#NOT_IN} entry to the {@link #queryCriteriaEntries}. See the comment at
	 * {@link Criteria#in(Object...)}.
	 *
	 * @param values the argument to the operation
	 * @return this object
	 */
	public Criteria notIn(Object... values) {
		return notIn(toCollection(values));
	}

	/**
	 * Add a {@link OperationKey#NOT_IN} entry to the {@link #queryCriteriaEntries}. See the comment at
	 * {@link Criteria#in(Object...)}.
	 *
	 * @param values the argument to the operation
	 * @return this object
	 */
	public Criteria notIn(Iterable<?> values) {

		Assert.notNull(values, "Collection of 'NotIn' values must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.NOT_IN, values));
		return this;
	}

	/**
	 * Add a {@link OperationKey#EXPRESSION} entry to the {@link #queryCriteriaEntries} allowing native elasticsearch
	 * expressions
	 *
	 * @param s the argument to the operation
	 * @return this object
	 */
	public Criteria expression(String s) {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EXPRESSION, s));
		return this;
	}

	/**
	 * Add a {@link OperationKey#FUZZY} entry to the {@link #queryCriteriaEntries}
	 *
	 * @param s the argument to the operation
	 * @return this object
	 */
	public Criteria fuzzy(String s) {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.FUZZY, s));
		return this;
	}

	/**
	 * Add a {@link OperationKey#LESS_EQUAL} entry to the {@link #queryCriteriaEntries}
	 *
	 * @param upperBound the argument to the operation
	 * @return this object
	 */
	public Criteria lessThanEqual(Object upperBound) {

		Assert.notNull(upperBound, "upperBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.LESS_EQUAL, upperBound));
		return this;
	}

	/**
	 * Add a {@link OperationKey#LESS} entry to the {@link #queryCriteriaEntries}
	 *
	 * @param upperBound the argument to the operation
	 * @return this object
	 */
	public Criteria lessThan(Object upperBound) {

		Assert.notNull(upperBound, "upperBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.LESS, upperBound));
		return this;
	}

	/**
	 * Add a {@link OperationKey#GREATER_EQUAL} entry to the {@link #queryCriteriaEntries}
	 *
	 * @param lowerBound the argument to the operation
	 * @return this object
	 */
	public Criteria greaterThanEqual(Object lowerBound) {

		Assert.notNull(lowerBound, "lowerBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.GREATER_EQUAL, lowerBound));
		return this;
	}

	/**
	 * Add a {@link OperationKey#GREATER} entry to the {@link #queryCriteriaEntries}
	 *
	 * @param lowerBound the argument to the operation
	 * @return this object
	 */
	public Criteria greaterThan(Object lowerBound) {

		Assert.notNull(lowerBound, "lowerBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.GREATER, lowerBound));
		return this;
	}

	/**
	 * Add a {@link OperationKey#MATCHES} entry to the {@link #queryCriteriaEntries}. This will build a match query with
	 * the OR operator.
	 *
	 * @param value the value to match
	 * @return this object
	 * @since 4.1
	 */
	public Criteria matches(Object value) {

		Assert.notNull(value, "value must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.MATCHES, value));
		return this;
	}

	/**
	 * Add a {@link OperationKey#MATCHES} entry to the {@link #queryCriteriaEntries}. This will build a match query with
	 * the AND operator.
	 *
	 * @param value the value to match
	 * @return this object
	 * @since 4.1
	 */
	public Criteria matchesAll(Object value) {

		Assert.notNull(value, "value must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.MATCHES_ALL, value));
		return this;
	}

	/**
	 * Add a {@link OperationKey#EMPTY} entry to the {@link #queryCriteriaEntries}.
	 *
	 * @return this object
	 * @since 4.3
	 */
	public Criteria empty() {

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EMPTY));
		return this;
	}

	/**
	 * Add a {@link OperationKey#NOT_EMPTY} entry to the {@link #queryCriteriaEntries}.
	 *
	 * @return this object
	 * @since 4.3
	 */
	public Criteria notEmpty() {

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.NOT_EMPTY));
		return this;
	}

	/**
	 * Add a {@link OperationKey#REGEXP} entry to the {@link #queryCriteriaEntries}.
	 *
	 * @param value the regexp value to match
	 * @return this object
	 * @since 5.1
	 */
	public Criteria regexp(String value) {

		Assert.notNull(value, "value must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.REGEXP, value));
		return this;
	}

	// endregion

	// region criteria entries - filter
	/**
	 * Adds a new filter CriteriaEntry for {@code location GeoBox bounding box}
	 *
	 * @param boundingBox {@link org.springframework.data.elasticsearch.core.geo.GeoBox} bounding box(left top corner +
	 *          right bottom corner)
	 * @return this object
	 */
	public Criteria boundedBy(GeoBox boundingBox) {

		Assert.notNull(boundingBox, "boundingBox value for boundedBy criteria must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.BBOX, new Object[] { boundingBox }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for {@code location Box bounding box}
	 *
	 * @param boundingBox {@link org.springframework.data.elasticsearch.core.geo.GeoBox} bounding box(left top corner +
	 *          right bottom corner)
	 * @return this object
	 */
	public Criteria boundedBy(Box boundingBox) {

		Assert.notNull(boundingBox, "boundingBox value for boundedBy criteria must not be null");

		filterCriteriaEntries
				.add(new CriteriaEntry(OperationKey.BBOX, new Object[] { boundingBox.getFirst(), boundingBox.getSecond() }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for bounding box created from points
	 *
	 * @param topLeftGeohash left top corner of bounding box as geohash
	 * @param bottomRightGeohash right bottom corner of bounding box as geohash
	 * @return this object
	 */
	public Criteria boundedBy(String topLeftGeohash, String bottomRightGeohash) {

		Assert.isTrue(StringUtils.hasLength(topLeftGeohash), "topLeftGeohash must not be empty");
		Assert.isTrue(StringUtils.hasLength(bottomRightGeohash), "bottomRightGeohash must not be empty");

		filterCriteriaEntries
				.add(new CriteriaEntry(OperationKey.BBOX, new Object[] { topLeftGeohash, bottomRightGeohash }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for bounding box created from points
	 *
	 * @param topLeftPoint left top corner of bounding box
	 * @param bottomRightPoint right bottom corner of bounding box
	 * @return this object
	 */
	public Criteria boundedBy(GeoPoint topLeftPoint, GeoPoint bottomRightPoint) {

		Assert.notNull(topLeftPoint, "topLeftPoint must not be null");
		Assert.notNull(bottomRightPoint, "bottomRightPoint must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.BBOX, new Object[] { topLeftPoint, bottomRightPoint }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for bounding box created from points
	 *
	 * @param topLeftPoint left top corner of bounding box
	 * @param bottomRightPoint right bottom corner of bounding box
	 * @return this object
	 */
	public Criteria boundedBy(Point topLeftPoint, Point bottomRightPoint) {

		Assert.notNull(topLeftPoint, "topLeftPoint must not be null");
		Assert.notNull(bottomRightPoint, "bottomRightPoint must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.BBOX,
				new Object[] { GeoPoint.fromPoint(topLeftPoint), GeoPoint.fromPoint(bottomRightPoint) }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for {@code location WITHIN distance}
	 *
	 * @param location {@link org.springframework.data.elasticsearch.core.geo.GeoPoint} center coordinates
	 * @param distance {@link String} radius as a string (e.g. : '100km'). Distance unit : either mi/miles or km can be
	 *          set
	 * @return this object
	 */
	public Criteria within(GeoPoint location, String distance) {

		Assert.notNull(location, "Location value for near criteria must not be null");
		Assert.notNull(location, "Distance value for near criteria must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.WITHIN, new Object[] { location, distance }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for {@code location WITHIN distance}
	 *
	 * @param location {@link org.springframework.data.geo.Point} center coordinates
	 * @param distance {@link org.springframework.data.geo.Distance} radius .
	 * @return this object
	 */
	public Criteria within(Point location, Distance distance) {

		Assert.notNull(location, "Location value for near criteria must not be null");
		Assert.notNull(location, "Distance value for near criteria must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.WITHIN, new Object[] { location, distance }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for {@code geoLocation WITHIN distance}
	 *
	 * @param geoLocation {@link String} center point supported formats: lat on = > "41.2,45.1", geohash = > "asd9as0d"
	 * @param distance {@link String} radius as a string (e.g. : '100km'). Distance unit : either mi/miles or km can be
	 *          set
	 * @return this object
	 */
	public Criteria within(String geoLocation, String distance) {

		Assert.isTrue(StringUtils.hasLength(geoLocation), "geoLocation value must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.WITHIN, new Object[] { geoLocation, distance }));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for GEO_INTERSECTS.
	 *
	 * @param geoShape the GeoJson shape
	 * @return this object
	 */
	public Criteria intersects(GeoJson<?> geoShape) {

		Assert.notNull(geoShape, "geoShape must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.GEO_INTERSECTS, geoShape));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for GEO_IS_DISJOINT.
	 *
	 * @param geoShape the GeoJson shape
	 * @return this object
	 */
	public Criteria isDisjoint(GeoJson<?> geoShape) {

		Assert.notNull(geoShape, "geoShape must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.GEO_IS_DISJOINT, geoShape));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for GEO_WITHIN.
	 *
	 * @param geoShape the GeoJson shape
	 * @return this object
	 */
	public Criteria within(GeoJson<?> geoShape) {
		Assert.notNull(geoShape, "geoShape must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.GEO_WITHIN, geoShape));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for GEO_CONTAINS.
	 *
	 * @param geoShape the GeoJson shape
	 * @return this object
	 */
	public Criteria contains(GeoJson<?> geoShape) {
		Assert.notNull(geoShape, "geoShape must not be null");

		filterCriteriaEntries.add(new CriteriaEntry(OperationKey.GEO_CONTAINS, geoShape));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for HAS_CHILD.
	 *
	 * @param query the has_child query.
	 * @return the current Criteria.
	 */
	public Criteria hasChild(HasChildQuery query) {
		Assert.notNull(query, "has_child query must not be null.");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.HAS_CHILD, query));
		return this;
	}

	/**
	 * Adds a new filter CriteriaEntry for HAS_PARENT.
	 *
	 * @param query the has_parent query.
	 * @return the current Criteria.
	 */
	public Criteria hasParent(HasParentQuery query) {
		Assert.notNull(query, "has_parent query must not be null.");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.HAS_PARENT, query));
		return this;
	}
	// endregion

	// region helper functions
	private void assertNoBlankInWildcardQuery(String searchString, boolean leadingWildcard, boolean trailingWildcard) {

		if (searchString.contains(CRITERIA_VALUE_SEPARATOR)) {
			throw new InvalidDataAccessApiUsageException("Cannot constructQuery '" + (leadingWildcard ? "*" : "") + '"'
					+ searchString + '"' + (trailingWildcard ? "*" : "") + "'. Use expression or multiple clauses instead.");
		}
	}

	private List<Object> toCollection(Object... values) {
		return Arrays.asList(values);
	}

	// endregion

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Criteria criteria = (Criteria) o;

		if (Float.compare(criteria.boost, boost) != 0)
			return false;
		if (negating != criteria.negating)
			return false;
		if (!Objects.equals(field, criteria.field))
			return false;
		if (!queryCriteriaEntries.equals(criteria.queryCriteriaEntries))
			return false;
		if (!filterCriteriaEntries.equals(criteria.filterCriteriaEntries))
			return false;
		return subCriteria.equals(criteria.subCriteria);
	}

	@Override
	public int hashCode() {
		int result = field != null ? field.hashCode() : 0;
		result = 31 * result + (boost != +0.0f ? Float.floatToIntBits(boost) : 0);
		result = 31 * result + (negating ? 1 : 0);
		result = 31 * result + queryCriteriaEntries.hashCode();
		result = 31 * result + filterCriteriaEntries.hashCode();
		result = 31 * result + subCriteria.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Criteria{" + //
				"field=" + field + //
				", boost=" + boost + //
				", negating=" + negating + //
				", queryCriteriaEntries=" + queryCriteriaEntries + //
				", filterCriteriaEntries=" + filterCriteriaEntries + //
				", subCriteria=" + subCriteria + //
				'}'; //
	}

	@SuppressWarnings("unused")
	static class OrCriteria extends Criteria {

		public OrCriteria() {
			super();
		}

		public OrCriteria(String fieldName) {
			super(fieldName);
		}

		public OrCriteria(Field field) {
			super(field);
		}

		public OrCriteria(List<Criteria> criteriaChain, String fieldName) {
			super(criteriaChain, fieldName);
		}

		public OrCriteria(List<Criteria> criteriaChain, Field field) {
			super(criteriaChain, field);
		}

		@Override
		public Operator getOperator() {
			return Operator.OR;
		}
	}

	/**
	 * a list of {@link Criteria} objects that belong to one query.
	 *
	 * @since 4.1
	 */
	public static class CriteriaChain extends LinkedList<Criteria> {}

	/**
	 * Operator to join the entries of the criteria chain
	 */
	public enum Operator {
		AND, //
		OR //
	}

	public enum OperationKey { //
		EQUALS, //
		CONTAINS, //
		STARTS_WITH, //
		ENDS_WITH, //
		EXPRESSION, //
		BETWEEN, //
		FUZZY, //
		/**
		 * @since 4.1
		 */
		MATCHES, //
		/**
		 * @since 4.1
		 */
		MATCHES_ALL, //
		IN, //
		NOT_IN, //
		WITHIN, //
		BBOX, //
		LESS, //
		LESS_EQUAL, //
		GREATER, //
		GREATER_EQUAL, //
		/**
		 * @since 4.0
		 */
		EXISTS, //
		/**
		 * @since 4.1
		 */
		GEO_INTERSECTS, //
		/**
		 * @since 4.1
		 */
		GEO_IS_DISJOINT, //
		/**
		 * @since 4.1
		 */
		GEO_WITHIN, //
		/**
		 * @since 4.1
		 */
		GEO_CONTAINS, //
		/**
		 * @since 4.3
		 */
		EMPTY, //
		/**
		 * @since 4.3
		 */
		NOT_EMPTY, //
		/**
		 * @since 5.1
		 */
		REGEXP,
		/**
		 * @since 5.3
		 */
		HAS_CHILD, HAS_PARENT;

		/**
		 * @return true if this key does not have an associated value
		 * @since 4.4
		 */
		public boolean hasNoValue() {
			return this == OperationKey.EXISTS || this == OperationKey.EMPTY || this == OperationKey.NOT_EMPTY;
		}

		/**
		 * @return true if this key does have an associated value
		 * @since 4.4
		 */
		public boolean hasValue() {
			return !hasNoValue();
		}
	}

	/**
	 * A class defining a single operation and it's argument value for the field of a {@link Criteria}.
	 */
	public static class CriteriaEntry {

		private final OperationKey key;
		@Nullable private Object value;

		protected CriteriaEntry(OperationKey key) {

			Assert.isTrue(key.hasNoValue(),
					"key must be OperationKey.EXISTS, OperationKey.EMPTY or OperationKey.NOT_EMPTY for this call");

			this.key = key;
		}

		CriteriaEntry(OperationKey key, Object value) {

			Assert.notNull(key, "key must not be null");
			Assert.notNull(value, "value must not be null");

			this.key = key;
			this.value = value;
		}

		public OperationKey getKey() {
			return key;
		}

		public void setValue(Object value) {

			Assert.notNull(value, "value must not be null");

			this.value = value;
		}

		public Object getValue() {

			Assert.isTrue(key != OperationKey.EXISTS, key.name() + " has no value");
			Assert.notNull(value, "unexpected null value");

			return value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			CriteriaEntry that = (CriteriaEntry) o;

			if (key != that.key)
				return false;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			int result = key.hashCode();
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "CriteriaEntry{" + "key=" + key + ", value=" + value + '}';
		}
	}
}
