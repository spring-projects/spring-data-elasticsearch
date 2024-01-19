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
package org.springframework.data.elasticsearch.repository.support.spel;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.util.Lazy;

/**
 * To supply a {@link ConversionService} with custom converters to handle SpEL values in elasticsearch query.
 *
 * @since 5.3
 * @author Haibo Liu
 */
public class ElasticsearchValueSpELConversionService {

	public static final Lazy<ConversionService> CONVERSION_SERVICE_LAZY = Lazy.of(
			ElasticsearchValueSpELConversionService::buildSpELConversionService);

	private static ConversionService buildSpELConversionService() {
		// register elasticsearch custom type converter for conversion service
		ConversionService conversionService = new DefaultConversionService();
		ConverterRegistry converterRegistry = (ConverterRegistry) conversionService;
		converterRegistry.addConverter(new ElasticsearchCollectionValueToStringConverter(conversionService));
		converterRegistry.addConverter(new ElasticsearchStringValueToStringConverter());
		return conversionService;
	}
}
