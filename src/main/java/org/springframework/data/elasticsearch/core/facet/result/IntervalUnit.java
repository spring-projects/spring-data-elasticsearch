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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Single term
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
@Deprecated
public class IntervalUnit {

	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	long key;
	long count;
	long totalCount;
	double total;
	double mean;
	double min;
	double max;

	public IntervalUnit(long key, long count, long totalCount, double total, double mean, double min, double max) {
		this.key = key;
		this.count = count;
		this.totalCount = totalCount;
		this.total = total;
		this.mean = mean;
		this.min = min;
		this.max = max;
	}

	public long getKey() {
		return key;
	}

	public long getCount() {
		return count;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public double getTotal() {
		return total;
	}

	public double getMean() {
		return mean;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	@Override
	public String toString() {
		return "IntervalUnit{" +
				"key=" + format.format(new Date(key)) +
				", count=" + count +
				", totalCount=" + totalCount +
				", total=" + total +
				", mean=" + mean +
				", min=" + min +
				", max=" + max +
				'}';
	}
}
