/* Copyright 2019 the original author or authors.
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

import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.util.Lazy;

/**
 * @author Peter-Josef Meisch
 */
abstract class MappingContextBaseTests {

	protected final Lazy<ElasticsearchConverter> elasticsearchConverter = Lazy.of(this::setupElasticsearchConverter);

	private ElasticsearchConverter setupElasticsearchConverter() {
		return new MappingElasticsearchConverter(setupMappingContext());
	}

	private SimpleElasticsearchMappingContext setupMappingContext() {

		SimpleElasticsearchMappingContext mappingContext = new ElasticsearchConfigurationSupport() {}
				.elasticsearchMappingContext();
		mappingContext.initialize();
		return mappingContext;
	}

	final MappingBuilder getMappingBuilder() {
		return new MappingBuilder(elasticsearchConverter.get());
	}
}
