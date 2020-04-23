/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @since 3.2
 */
@Configuration
public class ElasticsearchConfigurationSupport {

	@Bean
	public ElasticsearchConverter elasticsearchEntityMapper(
			SimpleElasticsearchMappingContext elasticsearchMappingContext) {
		MappingElasticsearchConverter elasticsearchConverter = new MappingElasticsearchConverter(
				elasticsearchMappingContext);
		elasticsearchConverter.setConversions(elasticsearchCustomConversions());
		return elasticsearchConverter;
	}

	/**
	 * Creates a {@link SimpleElasticsearchMappingContext} equipped with entity classes scanned from the mapping base
	 * package.
	 *
	 * @see #getMappingBasePackages()
	 * @return never {@literal null}.
	 */
	@Bean
	public SimpleElasticsearchMappingContext elasticsearchMappingContext() {

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(getInitialEntitySet());
		mappingContext.setSimpleTypeHolder(elasticsearchCustomConversions().getSimpleTypeHolder());

		return mappingContext;
	}

	/**
	 * Register custom {@link Converter}s in a {@link ElasticsearchCustomConversions} object if required.
	 *
	 * @return never {@literal null}.
	 */
	@Bean
	public ElasticsearchCustomConversions elasticsearchCustomConversions() {
		return new ElasticsearchCustomConversions(Collections.emptyList());
	}

	/**
	 * Returns the base packages to scan for Elasticsearch mapped entities at startup. Will return the package name of the
	 * configuration class' (the concrete class, not this one here) by default. So if you have a
	 * {@code com.acme.AppConfig} extending {@link ElasticsearchConfigurationSupport} the base package will be considered
	 * {@code com.acme} unless the method is overridden to implement alternate behavior.
	 *
	 * @return the base packages to scan for mapped {@link Document} classes or an empty collection to not enable scanning
	 *         for entities.
	 */
	protected Collection<String> getMappingBasePackages() {

		Package mappingBasePackage = getClass().getPackage();
		return Collections.singleton(mappingBasePackage == null ? null : mappingBasePackage.getName());
	}

	/**
	 * Scans the mapping base package for classes annotated with {@link Document}. By default, it scans for entities in
	 * all packages returned by {@link #getMappingBasePackages()}.
	 *
	 * @see #getMappingBasePackages()
	 * @return never {@literal null}.
	 */
	protected Set<Class<?>> getInitialEntitySet() {

		Set<Class<?>> initialEntitySet = new HashSet<>();

		for (String basePackage : getMappingBasePackages()) {
			initialEntitySet.addAll(scanForEntities(basePackage));
		}

		return initialEntitySet;
	}

	/**
	 * Scans the given base package for entities, i.e. Elasticsearch specific types annotated with {@link Document} and
	 * {@link Persistent}.
	 *
	 * @param basePackage must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	protected Set<Class<?>> scanForEntities(String basePackage) {

		if (!StringUtils.hasText(basePackage)) {
			return Collections.emptySet();
		}

		Set<Class<?>> initialEntitySet = new HashSet<>();

		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
		componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

		for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {

			String beanClassName = candidate.getBeanClassName();

			if (beanClassName != null) {
				try {
					initialEntitySet.add(
							ClassUtils.forName(beanClassName, AbstractReactiveElasticsearchConfiguration.class.getClassLoader()));
				} catch (ClassNotFoundException | LinkageError ignored) {}
			}
		}

		return initialEntitySet;
	}
}
