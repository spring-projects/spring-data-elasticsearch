/*
 * Copyright 2019-2025 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import java.util.Map;
import java.util.Set;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Utility to define simple types understood by Spring Data Elasticsearch and the Elasticsearch client.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.2
 */
public class ElasticsearchSimpleTypes {

	static final Set<Class<?>> AUTOGENERATED_ID_TYPES;

	static {
		AUTOGENERATED_ID_TYPES = Set.of(String.class);

		ELASTICSEARCH_SIMPLE_TYPES = Set.of(Document.class, Map.class);
	}

	private static final Set<Class<?>> ELASTICSEARCH_SIMPLE_TYPES;
	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(ELASTICSEARCH_SIMPLE_TYPES, true);

	private ElasticsearchSimpleTypes() {}

}
