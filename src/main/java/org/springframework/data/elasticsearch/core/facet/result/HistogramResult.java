/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.facet.result;

import java.util.List;

import org.springframework.data.elasticsearch.core.facet.AbstractFacetResult;
import org.springframework.data.elasticsearch.core.facet.FacetType;

/**
 * @author Artur Konczak
 */
@Deprecated
public class HistogramResult extends AbstractFacetResult {

	private List<IntervalUnit> terms;

	public HistogramResult(String name, List<IntervalUnit> terms) {
		super(name, FacetType.term);
		this.terms = terms;
	}

	public List<IntervalUnit> getIntervalUnit() {
		return terms;
	}
}
