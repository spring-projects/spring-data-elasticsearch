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

import static org.assertj.core.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Routing;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Peter-Josef Meisch
 */
@SpringJUnitConfig({ DefaultRoutingResolverUnitTest.Config.class })
class DefaultRoutingResolverUnitTest {

	@Autowired private ApplicationContext applicationContext;
	private SimpleElasticsearchMappingContext mappingContext;

	@Nullable private RoutingResolver routingResolver;

	@Configuration
	static class Config {
		@Bean
		SpelRouting spelRouting() {
			return new SpelRouting();
		}
	}

	@BeforeEach
	void setUp() {
		mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setApplicationContext(applicationContext);

		routingResolver = new DefaultRoutingResolver(mappingContext);
	}

	@Test // #1218
	@DisplayName("should throw an exception on unknown property")
	void shouldThrowAnExceptionOnUnknownProperty() {

		InvalidRoutingEntity entity = new InvalidRoutingEntity("42", "route 66");

		assertThatThrownBy(() -> routingResolver.getRouting(entity)).isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // #1218
	@DisplayName("should return the routing from the entity")
	void shouldReturnTheRoutingFromTheEntity() {

		ValidRoutingEntity entity = new ValidRoutingEntity("42", "route 66");

		String routing = routingResolver.getRouting(entity);

		assertThat(routing).isEqualTo("route 66");
	}

	@Test // #1218
	@DisplayName("should return routing from SpEL expression")
	void shouldReturnRoutingFromSpElExpression() {

		ValidSpelRoutingEntity entity = new ValidSpelRoutingEntity("42", "route 42");

		String routing = routingResolver.getRouting(entity);

		assertThat(routing).isEqualTo("route 42");
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "routing-resolver-test")
	@Routing("theRouting")
	static class ValidRoutingEntity {
		@Id private String id;
		private String theRouting;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "routing-resolver-test")
	@Routing(value = "@spelRouting.getRouting(#entity)")
	static class ValidSpelRoutingEntity {
		@Id private String id;
		private String theRouting;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "routing-resolver-test")
	@Routing("unknownProperty")
	static class InvalidRoutingEntity {
		@Id private String id;
		private String theRouting;
	}

	static class SpelRouting {

		@Nullable
		public String getRouting(Object o) {

			if (o instanceof ValidSpelRoutingEntity) {
				return ((ValidSpelRoutingEntity) o).getTheRouting();
			}

			return null;
		}
	}
}
