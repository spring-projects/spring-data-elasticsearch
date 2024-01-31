/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support.value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link ConversionService} using custom converters to handle query values in elasticsearch query. If the value to be
 * converted beyond the scope of custom converters, it'll delegate to the {@link #delegate delegated conversion
 * service}.
 * <p>
 * This is a better solution for converting query values in elasticsearch query, because it has all the capability the
 * {@link #delegate delegated conversion service} has, especially for user-registered {@link Converter}s.
 *
 * @since 5.3
 * @author Haibo Liu
 */
public class ElasticsearchQueryValueConversionService implements ConversionService {

	private static final Map<ConversionService, ElasticsearchQueryValueConversionService> CACHE = new ConcurrentHashMap<>();

	private final GenericConversionService valueConversionService = new GenericConversionService();

	private final ConversionService delegate;

	private ElasticsearchQueryValueConversionService(ConversionService delegate) {

		Assert.notNull(delegate, "delegated ConversionService must not be null");

		this.delegate = delegate;

		// register elasticsearch custom type converters for conversion service
		valueConversionService.addConverter(new ElasticsearchCollectionValueToStringConverter(this));
		valueConversionService.addConverter(new ElasticsearchStringValueToStringConverter());
	}

	/**
	 * Get a {@link ElasticsearchQueryValueConversionService} with this conversion service as delegated.
	 *
	 * @param conversionService conversion service as delegated
	 * @return a conversion service having the capability to convert query values in elasticsearch query
	 */
	public static ElasticsearchQueryValueConversionService getInstance(ConversionService conversionService) {
		return CACHE.computeIfAbsent(conversionService, ElasticsearchQueryValueConversionService::new);
	}

	@Override
	public boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType) {
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		return canConvert(TypeDescriptor.valueOf(sourceType), TypeDescriptor.valueOf(targetType));
	}

	@Override
	public boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		return valueConversionService.canConvert(sourceType, targetType)
				|| delegate.canConvert(sourceType, targetType);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T convert(@Nullable Object source, Class<T> targetType) {
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (valueConversionService.canConvert(sourceType, targetType)) {
			return valueConversionService.convert(source, sourceType, targetType);
		} else {
			return delegate.convert(source, sourceType, targetType);
		}
	}
}
