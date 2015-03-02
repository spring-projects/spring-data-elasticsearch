/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.config;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

/**
 * Unit tests for {@link ElasticsearchRepositoryConfigExtension}.
 * 
 * @author Kevin Leturc
 */
public class ElasticsearchRepositoryConfigExtensionUnitTests {

	StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(Config.class, true);
	ResourceLoader loader = new PathMatchingResourcePatternResolver();
	Environment environment = new StandardEnvironment();
	RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
			EnableElasticsearchRepositories.class, loader, environment);

	/**
	 * @see DATAES-135
	 */
	@Test
	public void isStrictMatchIfDomainTypeIsAnnotatedWithDocument() {

		ElasticsearchRepositoryConfigExtension extension = new ElasticsearchRepositoryConfigExtension();
		assertHasRepo(SampleRepository.class, extension.getRepositoryConfigurations(configurationSource, loader, true));
	}

	/**
	 * @see DATAES-135
	 */
	@Test
	public void isStrictMatchIfRepositoryExtendsStoreSpecificBase() {

		ElasticsearchRepositoryConfigExtension extension = new ElasticsearchRepositoryConfigExtension();
		assertHasRepo(StoreRepository.class, extension.getRepositoryConfigurations(configurationSource, loader, true));
	}

	/**
	 * @see DATAES-135
	 */
	@Test
	public void isNotStrictMatchIfDomainTypeIsNotAnnotatedWithDocument() {

		ElasticsearchRepositoryConfigExtension extension = new ElasticsearchRepositoryConfigExtension();
		assertDoesNotHaveRepo(UnannotatedRepository.class,
				extension.getRepositoryConfigurations(configurationSource, loader, true));
	}

	private static void assertHasRepo(Class<?> repositoryInterface,
			Collection<RepositoryConfiguration<RepositoryConfigurationSource>> configs) {

		for (RepositoryConfiguration<?> config : configs) {
			if (config.getRepositoryInterface().equals(repositoryInterface.getName())) {
				return;
			}
		}

		Assert.fail("Expected to find config for repository interface ".concat(repositoryInterface.getName()).concat(" but got ")
				.concat(configs.toString()));
	}

	private static void assertDoesNotHaveRepo(Class<?> repositoryInterface,
			Collection<RepositoryConfiguration<RepositoryConfigurationSource>> configs) {

		for (RepositoryConfiguration<?> config : configs) {
			if (config.getRepositoryInterface().equals(repositoryInterface.getName())) {
				Assert.fail("Expected not to find config for repository interface ".concat(repositoryInterface.getName()));
			}
		}
	}

	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {

	}

	@Document(indexName = "index")
	static class Sample {}

	interface SampleRepository extends Repository<Sample, Long> {}

	interface UnannotatedRepository extends Repository<Object, Long> {}

	interface StoreRepository extends ElasticsearchRepository<Object, Long> {}
}
