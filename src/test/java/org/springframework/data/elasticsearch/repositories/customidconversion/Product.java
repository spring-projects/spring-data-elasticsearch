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
package org.springframework.data.elasticsearch.repositories.customidconversion;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

/**
 * Entity with an id of a custom type whose registered {@link Converter} to String produces a different value than its
 * {@code toString()} method.
 *
 * @author Minju Kim
 */
@Document(indexName = "#{@indexNameProvider.indexName()}")
public class Product {

	@Id
	@Nullable private ProductId id;
	@Field(type = FieldType.Text)
	@Nullable private String name;

	public Product() {}

	public Product(@Nullable ProductId id, @Nullable String name) {
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

	/**
	 * the record's toString() returns "ProductId[value=...]", the registered converter returns only the value.
	 */
	public record ProductId(String value) {
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

	static ElasticsearchCustomConversions customConversions() {
		return new ElasticsearchCustomConversions(
				List.of(new ProductIdToStringConverter(), new StringToProductIdConverter()));
	}
}
