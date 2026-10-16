/*
 * Copyright 2026-present the original author or authors.
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

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * Reproduces that the reactive template does not use the registered id conversion when building the
 * {@link IndexQuery} for a save operation, whereas reads and deletes do.
 *
 * @author Minju Kim
 */
class ReactiveIndexQueryIdConversionUnitTests {

	private MappingElasticsearchConverter converter;
	private ReactiveElasticsearchTemplate template;

	@BeforeEach
	void setUp() {

		ElasticsearchCustomConversions conversions = new ElasticsearchCustomConversions(
				List.of(new ProductIdToStringConverter(), new StringToProductIdConverter()));

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		mappingContext.afterPropertiesSet();

		converter = new MappingElasticsearchConverter(mappingContext);
		converter.setConversions(conversions);
		converter.afterPropertiesSet();

		// the client is never used to send a request in this test, no connection is established
		template = new ReactiveElasticsearchTemplate(ElasticsearchClients.createReactive(ClientConfiguration.localhost()),
				converter);
	}

	@Test // #3359
	@DisplayName("should use the converted id when building the IndexQuery for save")
	void shouldUseConvertedIdWhenBuildingIndexQuery() {

		var entity = new Product(new ProductId("abc"), "name");

		IndexQuery indexQuery = template.getIndexQuery(entity);

		// this is the id that is used by the reactive template / repository for get, exists and delete
		assertThat(converter.convertId(entity.getId())).isEqualTo("abc");
		assertThat(indexQuery.getId()).isEqualTo("abc");
	}

	record ProductId(String value) {
	}

	@WritingConverter
	static class ProductIdToStringConverter implements Converter<ProductId, String> {
		@Override
		public String convert(ProductId source) {
			return source.value();
		}
	}

	@ReadingConverter
	static class StringToProductIdConverter implements Converter<String, ProductId> {
		@Override
		public ProductId convert(String source) {
			return new ProductId(source);
		}
	}

	@Document(indexName = "product")
	static class Product {
		@Id
		@Nullable private ProductId id;
		@Nullable private String name;

		Product(@Nullable ProductId id, @Nullable String name) {
			this.id = id;
			this.name = name;
		}

		@Nullable
		public ProductId getId() {
			return id;
		}

		public void setId(@Nullable ProductId id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}
}
