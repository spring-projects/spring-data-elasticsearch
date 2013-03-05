/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;

/**
 * Criteria is the central class when constructing queries. It follows more or less a fluent API style, which allows to
 * easily chain together multiple criteria.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class Criteria {

	public static final String WILDCARD = "*";
	public static final String CRITERIA_VALUE_SEPERATOR = " ";

	private static final String OR_OPERATOR = " OR ";
	private static final String AND_OPERATOR = " AND ";

	private Field field;
	private float boost = Float.NaN;
	private boolean negating = false;

	private List<Criteria> criteriaChain = new ArrayList<Criteria>(1);

	private Set<CriteriaEntry> criteria = new LinkedHashSet<CriteriaEntry>();

	public Criteria() {
	}

	/**
	 * Creates a new CriterSimpleFieldia for the Filed with provided name
	 * 
	 * @param fieldname
	 */
	public Criteria(String fieldname) {
		this(new SimpleField(fieldname));
	}

	/**
	 * Creates a new Criteria for the given field
	 * 
	 * @param field
	 */
	public Criteria(Field field) {
		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.criteriaChain.add(this);
		this.field = field;
	}

	protected Criteria(List<Criteria> criteriaChain, String fieldname) {
		this(criteriaChain, new SimpleField(fieldname));
	}

	protected Criteria(List<Criteria> criteriaChain, Field field) {
		Assert.notNull(criteriaChain, "CriteriaChain must not be null");
		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.criteriaChain.addAll(criteriaChain);
		this.criteriaChain.add(this);
		this.field = field;
	}

	/**
	 * Static factory method to create a new Criteria for field with given name
	 * 
	 * @param field
	 * @return
	 */
	public static Criteria where(String field) {
		return where(new SimpleField(field));
	}

	/**
	 * Static factory method to create a new Criteria for provided field
	 * 
	 * @param field
	 * @return
	 */
	public static Criteria where(Field field) {
		return new Criteria(field);
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(Field field) {
		return new Criteria(this.criteriaChain, field);
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param fieldName
	 * @return
	 */
	public Criteria and(String fieldName) {
		return new Criteria(this.criteriaChain, fieldName);
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param criteria
	 * @return
	 */
	public Criteria and(Criteria criteria) {
		this.criteriaChain.add(criteria);
		return this;
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param criterias
	 * @return
	 */
	public Criteria and(Criteria... criterias) {
		this.criteriaChain.addAll(Arrays.asList(criterias));
		return this;
	}

	/**
	 * Chain using {@code OR}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria or(Field field) {
		return new OrCriteria(this.criteriaChain, field);
	}

	/**
	 * Chain using {@code OR}
	 * 
	 * @param criteria
	 * @return
	 */
	public Criteria or(Criteria criteria) {
		Assert.notNull(criteria, "Cannot chain 'null' criteria.");

		Criteria orConnectedCritiera = new OrCriteria(this.criteriaChain, criteria.getField());
		orConnectedCritiera.criteria.addAll(criteria.criteria);
		return orConnectedCritiera;
	}

	/**
	 * Chain using {@code OR}
	 * 
	 * @param fieldName
	 * @return
	 */
	public Criteria or(String fieldName) {
		return or(new SimpleField(fieldName));
	}

	/**
	 * Crates new CriteriaEntry without any wildcards
	 * 
	 * @param o
	 * @return
	 */
	public Criteria is(Object o) {
		criteria.add(new CriteriaEntry(OperationKey.EQUALS, o));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with leading and trailing wildcards <br/>
	 * <strong>NOTE: </strong> mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 * 
	 * @param s
	 * @return
	 */
	public Criteria contains(String s) {
		assertNoBlankInWildcardedQuery(s, true, true);
		criteria.add(new CriteriaEntry(OperationKey.CONTAINS, s));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with trailing wildcard
	 * 
	 * @param s
	 * @return
	 */
	public Criteria startsWith(String s) {
		assertNoBlankInWildcardedQuery(s, true, false);
		criteria.add(new CriteriaEntry(OperationKey.STARTS_WITH, s));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with leading wildcard <br />
	 * <strong>NOTE: </strong> mind your schema and execution times as leading wildcards may not be supported.
	 * 
	 * @param s
	 * @return
	 */
	public Criteria endsWith(String s) {
		assertNoBlankInWildcardedQuery(s, false, true);
		criteria.add(new CriteriaEntry(OperationKey.ENDS_WITH, s));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with trailing -
	 * 
	 * @return
	 */
	public Criteria not() {
		this.negating = true;
		return this;
	}

	/**
	 * Crates new CriteriaEntry with trailing ~
	 * 
	 * @param s
	 * @return
	 */
	public Criteria fuzzy(String s) {
        criteria.add(new CriteriaEntry(OperationKey.FUZZY, s));
        return this;
	}


	/**
	 * Crates new CriteriaEntry allowing native elasticsearch expressions
	 * 
	 * @param s
	 * @return
	 */
	public Criteria expression(String s) {
		criteria.add(new CriteriaEntry(OperationKey.EXPRESSION, s));
		return this;
	}

	/**
	 * Boost positive hit with given factor. eg. ^2.3
	 * 
	 * @param boost
	 * @return
	 */
	public Criteria boost(float boost) {
		if (boost < 0) {
			throw new InvalidDataAccessApiUsageException("Boost must not be negative.");
		}
		this.boost = boost;
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE [lowerBound TO upperBound]}
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public Criteria between(Object lowerBound, Object upperBound) {
		if (lowerBound == null && upperBound == null) {
			throw new InvalidDataAccessApiUsageException("Range [* TO *] is not allowed");
		}

		criteria.add(new CriteriaEntry(OperationKey.BETWEEN, new Object[] { lowerBound, upperBound }));
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE [* TO upperBound]}
	 * 
	 * @param upperBound
	 * @return
	 */
	public Criteria lessThanEqual(Object upperBound) {
		between(null, upperBound);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE [lowerBound TO *]}
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria greaterThanEqual(Object lowerBound) {
		between(lowerBound, null);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for multiple values {@code (arg0 arg1 arg2 ...)}
	 * 
	 * @param values
	 * @return
	 */
	public Criteria in(Object... values) {
		if (values.length == 0 || (values.length > 1 && values[1] instanceof Collection)) {
			throw new InvalidDataAccessApiUsageException("At least one element "
					+ (values.length > 0 ? ("of argument of type " + values[1].getClass().getName()) : "")
					+ " has to be present.");
		}
		return in(Arrays.asList(values));
	}

	/**
	 * Crates new CriteriaEntry for multiple values {@code (arg0 arg1 arg2 ...)}
	 * 
	 * @param values the collection containing the values to match against
	 * @return
	 */
	public Criteria in(Iterable<?> values) {
		Assert.notNull(values, "Collection of 'in' values must not be null");
        criteria.add(new CriteriaEntry(OperationKey.IN, values));
		return this;
	}


	private void assertNoBlankInWildcardedQuery(String searchString, boolean leadingWildcard, boolean trailingWildcard) {
		if (StringUtils.contains(searchString, CRITERIA_VALUE_SEPERATOR)) {
			throw new InvalidDataAccessApiUsageException("Cannot constructQuery '" + (leadingWildcard ? "*" : "") + "\""
					+ searchString + "\"" + (trailingWildcard ? "*" : "") + "'. Use epxression or mulitple clauses instead.");
		}
	}

	/**
	 * Field targeted by this Criteria
	 * 
	 * @return
	 */
	public Field getField() {
		return this.field;
	}

	public Set<CriteriaEntry> getCriteriaEntries() {
		return Collections.unmodifiableSet(this.criteria);
	}

	/**
	 * Conjunction to be used with this criteria (AND | OR)
	 * 
	 * @return
	 */
	public String getConjunctionOperator() {
		return AND_OPERATOR;
	}

	public List<Criteria> getCriteriaChain() {
		return Collections.unmodifiableList(this.criteriaChain);
	}

	public boolean isNegating() {
		return this.negating;
	}

    public boolean isAnd(){
        return AND_OPERATOR == getConjunctionOperator();
    }

    public boolean isOr(){
        return OR_OPERATOR == getConjunctionOperator();
    }

	public float getBoost() {
		return this.boost;
	}

	static class OrCriteria extends Criteria {

		public OrCriteria() {
			super();
		}

		public OrCriteria(Field field) {
			super(field);
		}

		public OrCriteria(List<Criteria> criteriaChain, Field field) {
			super(criteriaChain, field);
		}

		public OrCriteria(List<Criteria> criteriaChain, String fieldname) {
			super(criteriaChain, fieldname);
		}

		public OrCriteria(String fieldname) {
			super(fieldname);
		}

		@Override
		public String getConjunctionOperator() {
			return OR_OPERATOR;
		}

	}

	public enum OperationKey {
		EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, EXPRESSION, BETWEEN, FUZZY, IN;
	}

	public static class CriteriaEntry {

		private OperationKey  key;
		private Object value;

		CriteriaEntry(OperationKey key, Object value) {
			this.key = key;
			this.value = value;
		}

		public OperationKey getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}

	}

}
