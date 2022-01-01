/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

/**
 * Enum to represent the relation that Elasticsearch returns for the totalHits value {@see <a href=
 * "https://www.elastic.co/guide/en/elasticsearch/reference/7.5/search-request-body.html#request-body-search-track-total-hits">Elasticsearch
 * docs</a>}
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 */
public enum TotalHitsRelation {
	EQUAL_TO, //
	GREATER_THAN_OR_EQUAL_TO, //
	/**
	 * @since 4.1
	 */
	OFF
}
