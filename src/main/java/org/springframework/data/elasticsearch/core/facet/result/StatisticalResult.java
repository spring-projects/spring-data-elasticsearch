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

import org.springframework.data.elasticsearch.core.facet.AbstractFacetResult;
import org.springframework.data.elasticsearch.core.facet.FacetType;

/**
 * @author Petar Tahchiev
 */
@Deprecated
public class StatisticalResult extends AbstractFacetResult {

	private long count;

	private double max;

	private double min;

	private double mean;

	private double stdDeviation;

	private double sumOfSquares;

	private double total;

	private double variance;

	public StatisticalResult(String name, long count, double max, double min, double mean, double stdDeviation, double sumOfSquares, double total, double variance) {
		super(name, FacetType.statistical);
		this.count = count;
		this.max = max;
		this.min = min;
		this.mean = mean;
		this.stdDeviation = stdDeviation;
		this.sumOfSquares = sumOfSquares;
		this.total = total;
		this.variance = variance;
	}

	public long getCount() {
		return count;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public double getMean() {
		return mean;
	}

	public double getStdDeviation() {
		return stdDeviation;
	}

	public double getSumOfSquares() {
		return sumOfSquares;
	}

	public double getTotal() {
		return total;
	}

	public double getVariance() {
		return variance;
	}
}
