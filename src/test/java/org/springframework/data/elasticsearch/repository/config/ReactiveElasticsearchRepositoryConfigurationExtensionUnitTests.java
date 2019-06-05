/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.config;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 */
public class ReactiveElasticsearchRepositoryConfigurationExtensionUnitTests {

	StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(Config.class, true);
	ResourceLoader loader = new PathMatchingResourcePatternResolver();
	Environment environment = new StandardEnvironment();
	BeanDefinitionRegistry registry = new DefaultListableBeanFactory();

	RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
			EnableReactiveElasticsearchRepositories.class, loader, environment, registry);

	@Test // DATAES-519
	public void isStrictMatchIfDomainTypeIsAnnotatedWithDocument() {

		ReactiveElasticsearchRepositoryConfigurationExtension extension = new ReactiveElasticsearchRepositoryConfigurationExtension();
		assertHasRepo(CrudRepositoryForAnnotatedType.class,
				extension.getRepositoryConfigurations(configurationSource, loader, true));
	}

	@Test // DATAES-519
	public void isStrictMatchIfRepositoryExtendsStoreSpecificBase() {

		ReactiveElasticsearchRepositoryConfigurationExtension extension = new ReactiveElasticsearchRepositoryConfigurationExtension();
		assertHasRepo(EsRepositoryForUnAnnotatedType.class,
				extension.getRepositoryConfigurations(configurationSource, loader, true));
	}

	@Test // DATAES-519
	public void isNotStrictMatchIfDomainTypeIsNotAnnotatedWithDocument() {

		ReactiveElasticsearchRepositoryConfigurationExtension extension = new ReactiveElasticsearchRepositoryConfigurationExtension();
		assertDoesNotHaveRepo(CrudRepositoryForUnAnnotatedType.class,
				extension.getRepositoryConfigurations(configurationSource, loader, true));
	}

	private static void assertHasRepo(Class<?> repositoryInterface,
			Collection<RepositoryConfiguration<RepositoryConfigurationSource>> configs) {

		for (RepositoryConfiguration<?> config : configs) {
			if (config.getRepositoryInterface().equals(repositoryInterface.getName())) {
				return;
			}
		}

		fail("Expected to find config for repository interface ".concat(repositoryInterface.getName()).concat(" but got ")
				.concat(configs.toString()));
	}

	private static void assertDoesNotHaveRepo(Class<?> repositoryInterface,
			Collection<RepositoryConfiguration<RepositoryConfigurationSource>> configs) {

		for (RepositoryConfiguration<?> config : configs) {
			if (config.getRepositoryInterface().equals(repositoryInterface.getName())) {
				fail("Expected not to find config for repository interface ".concat(repositoryInterface.getName()));
			}
		}
	}

	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {

	}

	@Document(indexName = "star-wars", type = "character")
	static class SwCharacter {}

	static class Store {}

	interface CrudRepositoryForAnnotatedType extends ReactiveCrudRepository<SwCharacter, String> {}

	interface CrudRepositoryForUnAnnotatedType extends ReactiveCrudRepository<Store, String> {}

	interface EsRepositoryForUnAnnotatedType extends ReactiveElasticsearchRepository<Store, String> {}
}
