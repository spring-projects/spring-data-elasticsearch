/*
 * Copyright 2020-2021the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Routing;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.routing.DefaultRoutingResolver;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
class EntityOperationsTest {

	@Nullable private static ConversionService conversionService;
	@Nullable private static EntityOperations entityOperations;
	@Nullable private static SimpleElasticsearchMappingContext mappingContext;

	@BeforeAll
	static void setUpAll() {
		mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(EntityWithRouting.class)));
		mappingContext.afterPropertiesSet();
		entityOperations = new EntityOperations(mappingContext);

		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext,
				new GenericConversionService());
		converter.afterPropertiesSet();

		conversionService = converter.getConversionService();
	}

	@Test // #1218
	@DisplayName("should return routing from DefaultRoutingAccessor")
	void shouldReturnRoutingFromDefaultRoutingAccessor() {

		EntityWithRouting entity = EntityWithRouting.builder().id("42").routing("theRoute").build();
		EntityOperations.AdaptibleEntity<EntityWithRouting> adaptibleEntity = entityOperations.forEntity(entity,
				conversionService, new DefaultRoutingResolver(mappingContext));

		String routing = adaptibleEntity.getRouting();

		assertThat(routing).isEqualTo("theRoute");
	}

	@Test // #1218
	@DisplayName("should return routing from JoinField when routing value is null")
	void shouldReturnRoutingFromJoinFieldWhenRoutingValueIsNull() {

		EntityWithRoutingAndJoinField entity = EntityWithRoutingAndJoinField.builder().id("42")
				.joinField(new JoinField<>("foo", "foo-routing")).build();

		EntityOperations.AdaptibleEntity<EntityWithRoutingAndJoinField> adaptibleEntity = entityOperations.forEntity(entity,
				conversionService, new DefaultRoutingResolver(mappingContext));

		String routing = adaptibleEntity.getRouting();

		assertThat(routing).isEqualTo("foo-routing");
	}

	@Test // #1218
	@DisplayName("should return routing from routing when JoinField is set")
	void shouldReturnRoutingFromRoutingWhenJoinFieldIsSet() {
		EntityWithRoutingAndJoinField entity = EntityWithRoutingAndJoinField.builder().id("42").routing("theRoute")
				.joinField(new JoinField<>("foo", "foo-routing")).build();

		EntityOperations.AdaptibleEntity<EntityWithRoutingAndJoinField> adaptibleEntity = entityOperations.forEntity(entity,
				conversionService, new DefaultRoutingResolver(mappingContext));

		String routing = adaptibleEntity.getRouting();

		assertThat(routing).isEqualTo("theRoute");
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "entity-operations-test")
	@Routing("routing")
	static class EntityWithRouting {
		@Id private String id;
		private String routing;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "entity-operations-test")
	@Routing("routing")
	static class EntityWithRoutingAndJoinField {
		@Id private String id;
		private String routing;
		private JoinField<String> joinField;
	}
}
