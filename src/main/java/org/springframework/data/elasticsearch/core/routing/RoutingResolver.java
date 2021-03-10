/*
 * Copyright2020-2021 the original author or authors.
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
package org.springframework.data.elasticsearch.core.routing;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public interface RoutingResolver {

	/**
	 * returns the routing when no entity is available.
	 *
	 * @return the routing value
	 */
	@Nullable
	String getRouting();

	/**
	 * Returns the routing for a bean.
	 *
	 * @param bean the bean to get the routing for
	 * @return the routing value
	 */
	@Nullable
	<T> String getRouting(T bean);

	/**
	 * Returns a {@link RoutingResolver that always retuns a fixed value}
	 *
	 * @param value the value to return
	 * @return the fixed-value {@link RoutingResolver}
	 */
	static RoutingResolver just(String value) {

		Assert.notNull(value, "value must not be null");

		return new RoutingResolver() {
			@Override
			public String getRouting() {
				return value;
			}

			@Override
			public String getRouting(Object bean) {
				return value;
			}
		};
	}
}
