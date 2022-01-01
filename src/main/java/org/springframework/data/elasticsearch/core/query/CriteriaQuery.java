/*
 * Copyright 2013-2022 the original author or authors.
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

import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * CriteriaQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 */
public class CriteriaQuery extends BaseQuery {

	private Criteria criteria;

	public CriteriaQuery(Criteria criteria) {
		this(criteria, Pageable.unpaged());
	}

	public CriteriaQuery(Criteria criteria, Pageable pageable) {

		Assert.notNull(criteria, "Criteria must not be null!");
		Assert.notNull(pageable, "Pageable must not be null!");

		this.criteria = criteria;
		this.pageable = pageable;
		this.addSort(pageable.getSort());
	}

	public static Query fromQuery(CriteriaQuery source) {
		return fromQuery(source, new CriteriaQuery(source.criteria));
	}

	public static <T extends CriteriaQuery> T fromQuery(CriteriaQuery source, T destination) {

		Assert.notNull(source, "source must not be null");
		Assert.notNull(destination, "destination must not be null");

		destination.addCriteria(source.getCriteria());

		if (source.getSort() != null) {
			destination.addSort(source.getSort());
		}

		return destination;
	}

	@SuppressWarnings("unchecked")
	public final <T extends CriteriaQuery> T addCriteria(Criteria criteria) {

		Assert.notNull(criteria, "Cannot add null criteria.");

		this.criteria.and(criteria);
		return (T) this;
	}

	public Criteria getCriteria() {
		return this.criteria;
	}
}
