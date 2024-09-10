/*
 * Copyright 2023-2024 the original author or authors.
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

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Adapter for creating synchronous calls for a reactive {@link ReactiveIndexOperations}.
 *
 * @author Peter-Josef Meisch
 * @since 5.2
 */
public interface IndexOperationsAdapter extends IndexOperations {
	static IndexOperationsAdapter blocking(ReactiveIndexOperations reactiveIndexOperations) {

		Assert.notNull(reactiveIndexOperations, "reactiveIndexOperations must not be null");

		return new IndexOperationsAdapter() {
			@Override
			public boolean create() {
				return Boolean.TRUE.equals(reactiveIndexOperations.create().block());
			}

			@Override
			public boolean create(Map<String, Object> settings) {
				return Boolean.TRUE.equals(reactiveIndexOperations.create(settings).block());
			}

			@Override
			public boolean create(Map<String, Object> settings, Document mapping) {
				return Boolean.TRUE.equals(reactiveIndexOperations.create(settings, mapping).block());
			}

			@Override
			public boolean createWithMapping() {
				return Boolean.TRUE.equals(reactiveIndexOperations.createWithMapping().block());
			}

			@Override
			public boolean delete() {
				return Boolean.TRUE.equals(reactiveIndexOperations.delete().block());
			}

			@Override
			public boolean exists() {
				return Boolean.TRUE.equals(reactiveIndexOperations.exists().block());
			}

			@Override
			public void refresh() {
				reactiveIndexOperations.refresh().block();
			}

			@Override
			public Document createMapping() {
				return Objects.requireNonNull(reactiveIndexOperations.createMapping().block());
			}

			@Override
			public Document createMapping(Class<?> clazz) {
				return Objects.requireNonNull(reactiveIndexOperations.createMapping(clazz).block());

			}

			@Override
			public boolean putMapping(Document mapping) {
				return Boolean.TRUE.equals(reactiveIndexOperations.putMapping(Mono.just(mapping)).block());
			}

			@Override
			public Map<String, Object> getMapping() {
				return Objects.requireNonNull(reactiveIndexOperations.getMapping().block());
			}

			@Override
			public Settings createSettings() {
				return Objects.requireNonNull(reactiveIndexOperations.createSettings().block());
			}

			@Override
			public Settings createSettings(Class<?> clazz) {
				return Objects.requireNonNull(reactiveIndexOperations.createSettings(clazz).block());
			}

			@Override
			public Settings getSettings() {
				return Objects.requireNonNull(reactiveIndexOperations.getSettings().block());
			}

			@Override
			public Settings getSettings(boolean includeDefaults) {
				return Objects.requireNonNull(reactiveIndexOperations.getSettings(includeDefaults).block());
			}

			@Override
			public boolean alias(AliasActions aliasActions) {
				return Boolean.TRUE.equals(reactiveIndexOperations.alias(aliasActions).block());
			}

			@Override
			public Map<String, Set<AliasData>> getAliases(String... aliasNames) {
				return Objects.requireNonNull(reactiveIndexOperations.getAliases(aliasNames).block());
			}

			@Override
			public Map<String, Set<AliasData>> getAliasesForIndex(String... indexNames) {
				return Objects.requireNonNull(reactiveIndexOperations.getAliasesForIndex(indexNames).block());
			}

			@Deprecated
			@Override
			public boolean putTemplate(PutTemplateRequest putTemplateRequest) {
				return Boolean.TRUE.equals(reactiveIndexOperations.putTemplate(putTemplateRequest).block());
			}

			@Override
			public boolean putIndexTemplate(PutIndexTemplateRequest putIndexTemplateRequest) {
				return Boolean.TRUE.equals(reactiveIndexOperations.putIndexTemplate(putIndexTemplateRequest).block());
			}

			@Override
			public boolean putComponentTemplate(PutComponentTemplateRequest putComponentTemplateRequest) {
				return Boolean.TRUE.equals(reactiveIndexOperations.putComponentTemplate(putComponentTemplateRequest).block());
			}

			@Override
			public boolean existsComponentTemplate(ExistsComponentTemplateRequest existsComponentTemplateRequest) {
				return Boolean.TRUE
						.equals(reactiveIndexOperations.existsComponentTemplate(existsComponentTemplateRequest).block());
			}

			@Override
			public List<TemplateResponse> getComponentTemplate(GetComponentTemplateRequest getComponentTemplateRequest) {
				return Objects.requireNonNull(
						reactiveIndexOperations.getComponentTemplate(getComponentTemplateRequest).collectList().block());
			}

			@Override
			public boolean deleteComponentTemplate(DeleteComponentTemplateRequest deleteComponentTemplateRequest) {
				return Boolean.TRUE
						.equals(reactiveIndexOperations.deleteComponentTemplate(deleteComponentTemplateRequest).block());
			}

			@Deprecated
			@Nullable
			@Override
			public TemplateData getTemplate(GetTemplateRequest getTemplateRequest) {
				return Objects.requireNonNull(reactiveIndexOperations).getTemplate(getTemplateRequest).block();
			}

			@Deprecated
			@Override
			public boolean existsTemplate(ExistsTemplateRequest existsTemplateRequest) {
				return Boolean.TRUE.equals(reactiveIndexOperations.existsTemplate(existsTemplateRequest).block());
			}

			@Override
			public boolean existsIndexTemplate(ExistsIndexTemplateRequest existsTemplateRequest) {
				return Boolean.TRUE.equals(reactiveIndexOperations.existsIndexTemplate(existsTemplateRequest).block());
			}

			@Override
			public List<TemplateResponse> getIndexTemplate(GetIndexTemplateRequest getIndexTemplateRequest) {
				return Objects
						.requireNonNull(reactiveIndexOperations.getIndexTemplate(getIndexTemplateRequest).collectList().block());
			}

			@Override
			public boolean deleteIndexTemplate(DeleteIndexTemplateRequest deleteIndexTemplateRequest) {
				return Boolean.TRUE.equals(reactiveIndexOperations.deleteIndexTemplate(deleteIndexTemplateRequest).block());
			}

			@Deprecated
			@Override
			public boolean deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {
				return Boolean.TRUE.equals(reactiveIndexOperations.deleteTemplate(deleteTemplateRequest).block());
			}

			@Override
			public List<IndexInformation> getInformation(IndexCoordinates index) {
				return Objects.requireNonNull(reactiveIndexOperations.getInformation(index).collectList().block());
			}

			@Override
			public IndexCoordinates getIndexCoordinates() {
				return reactiveIndexOperations.getIndexCoordinates();
			}
		};
	}
}
