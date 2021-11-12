/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.elasticsearch.backend.elasticsearch7.cluster;

import org.springframework.data.elasticsearch.backend.elasticsearch7.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class ElasticsearchClusterOperations {
	/**
	 * Creates a ClusterOperations for a {@link ElasticsearchRestTemplate}.
	 *
	 * @param template the template, must not be {@literal null}
	 * @return ClusterOperations
	 */
	public static ClusterOperations forTemplate(ElasticsearchRestTemplate template) {

		Assert.notNull(template, "template must not be null");

		return new DefaultClusterOperations(template);
	}

}
