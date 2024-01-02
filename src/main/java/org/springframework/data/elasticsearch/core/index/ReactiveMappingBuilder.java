/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import static org.springframework.util.StringUtils.*;

import reactor.core.publisher.Mono;

import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.ReactiveResourceUtil;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;

/**
 * Subclass of {@link MappingBuilder} with specialized methods To inhibit blocking calls
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public class ReactiveMappingBuilder extends MappingBuilder {

	public ReactiveMappingBuilder(ElasticsearchConverter elasticsearchConverter) {
		super(elasticsearchConverter);
	}

	@Override
	public String buildPropertyMapping(Class<?> clazz) throws MappingException {
		throw new UnsupportedOperationException(
				"Use ReactiveMappingBuilder.buildReactivePropertyMapping() instead of buildPropertyMapping()");
	}

	public Mono<String> buildReactivePropertyMapping(Class<?> clazz) throws MappingException {
		ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.getMappingContext()
				.getRequiredPersistentEntity(clazz);

		return getRuntimeFields(entity) //
				.switchIfEmpty(Mono.just(Document.create())) //
				.map(document -> {
					if (document.isEmpty()) {
						return buildPropertyMapping(entity, null);
					} else {
						return buildPropertyMapping(entity, document);
					}
				});
	}

	private Mono<Document> getRuntimeFields(@Nullable ElasticsearchPersistentEntity<?> entity) {

		if (entity != null) {
			Mapping mappingAnnotation = entity.findAnnotation(Mapping.class);
			if (mappingAnnotation != null) {
				String runtimeFieldsPath = mappingAnnotation.runtimeFieldsPath();

				if (hasText(runtimeFieldsPath)) {
					return ReactiveResourceUtil.readFileFromClasspath(runtimeFieldsPath).map(Document::parse);
				}
			}
		}

		return Mono.empty();
	}
}
