/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.orhlc;

import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @author Andriy Redko
 * @since 5.0
 */
public class OpensearchClusterOperations {
	/**
	 * Creates a ClusterOperations for a {@link OpensearchRestTemplate}.
	 *
	 * @param template the template, must not be {@literal null}
	 * @return ClusterOperations
	 */
	public static ClusterOperations forTemplate(OpensearchRestTemplate template) {

		Assert.notNull(template, "template must not be null");

		return new DefaultClusterOperations(template);
	}

}
