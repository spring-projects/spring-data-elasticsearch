/*
 * Copyright 2018-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.ClassUtils;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.ElasticsearchEntityMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

/**
 * Unit tests for {@link ElasticsearchConfigurationSupport}.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
public class ElasticsearchConfigurationSupportUnitTests {

	@Test // DATAES-504
	public void usesConfigClassPackageAsBaseMappingPackage() throws ClassNotFoundException {

		ElasticsearchConfigurationSupport configuration = new StubConfig();
		assertThat(configuration.getMappingBasePackages()).contains(ClassUtils.getPackageName(StubConfig.class));
		assertThat(configuration.getInitialEntitySet()).contains(Entity.class);
	}

	@Test // DATAES-504
	public void doesNotScanOnEmptyBasePackage() throws ClassNotFoundException {

		ElasticsearchConfigurationSupport configuration = new StubConfig() {
			@Override
			protected Collection<String> getMappingBasePackages() {
				return Collections.emptySet();
			}
		};

		assertThat(configuration.getInitialEntitySet()).isEmpty();
	}

	@Test // DATAES-504
	public void containsMappingContext() {

		AbstractApplicationContext context = new AnnotationConfigApplicationContext(StubConfig.class);
		assertThat(context.getBean(SimpleElasticsearchMappingContext.class)).isNotNull();
	}

	@Test // DATAES-504
	public void containsElasticsearchConverter() {

		AbstractApplicationContext context = new AnnotationConfigApplicationContext(StubConfig.class);
		assertThat(context.getBean(ElasticsearchConverter.class)).isNotNull();
	}

	@Test // DATAES-504
	public void restConfigContainsElasticsearchTemplate() {

		AbstractApplicationContext context = new AnnotationConfigApplicationContext(RestConfig.class);
		assertThat(context.getBean(ElasticsearchRestTemplate.class)).isNotNull();
	}

	@Test // DATAES-563
	public void restConfigContainsElasticsearchOperationsByNameAndAlias() {

		AbstractApplicationContext context = new AnnotationConfigApplicationContext(RestConfig.class);

		assertThat(context.getBean("elasticsearchOperations")).isNotNull();
		assertThat(context.getBean("elasticsearchTemplate")).isNotNull();
	}

	@Test // DATAES-504
	public void reactiveConfigContainsReactiveElasticsearchTemplate() {

		AbstractApplicationContext context = new AnnotationConfigApplicationContext(ReactiveRestConfig.class);
		assertThat(context.getBean(ReactiveElasticsearchTemplate.class)).isNotNull();
	}

	@Test // DATAES-530
	public void usesConfiguredEntityMapper() {

		AbstractApplicationContext context = new AnnotationConfigApplicationContext(EntityMapperConfig.class);
		assertThat(context.getBean(EntityMapper.class)).isInstanceOf(ElasticsearchEntityMapper.class);
	}

	@Configuration
	static class StubConfig extends ElasticsearchConfigurationSupport {

	}

	@Configuration
	static class ReactiveRestConfig extends AbstractReactiveElasticsearchConfiguration {

		@Override
		public ReactiveElasticsearchClient reactiveElasticsearchClient() {
			return mock(ReactiveElasticsearchClient.class);
		}
	}

	@Configuration
	static class RestConfig extends AbstractElasticsearchConfiguration {

		@Override
		public RestHighLevelClient elasticsearchClient() {
			return mock(RestHighLevelClient.class);
		}
	}

	@Configuration
	static class EntityMapperConfig extends ElasticsearchConfigurationSupport {

		@Bean
		@Override
		public EntityMapper entityMapper() {

			ElasticsearchEntityMapper entityMapper = new ElasticsearchEntityMapper(elasticsearchMappingContext(),
					new DefaultConversionService());
			entityMapper.setConversions(elasticsearchCustomConversions());

			return entityMapper;
		}
	}

	@Document(indexName = "config-support-tests")
	static class Entity {}
}
