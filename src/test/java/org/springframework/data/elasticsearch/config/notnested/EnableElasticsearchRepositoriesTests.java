/*
 * Copyright 2013-15 the original author or authors.
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
package org.springframework.data.elasticsearch.config.notnested;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.Data;

import java.lang.Double;
import java.lang.Long;
import java.util.UUID;

import org.elasticsearch.node.NodeValidationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.Utils;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.data.repository.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 * @author Gad Akuka
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableElasticsearchRepositoriesTests implements ApplicationContextAware {

	ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.context = applicationContext;
	}

	@Configuration
	@EnableElasticsearchRepositories(basePackages = { "org.springframework.data.elasticsearch.config.notnested" })
	static class Config {

		@Bean
		public ElasticsearchOperations elasticsearchTemplate() throws NodeValidationException {

			return new ElasticsearchTemplate(Utils.getNodeClient());
		}
	}

	@Autowired ElasticsearchTemplate elasticsearchTemplate;

	@Autowired private SampleElasticsearchRepository repository;

	@Autowired(required = false) private SampleRepository nestedRepository;

	interface SampleRepository extends Repository<EnableElasticsearchRepositoriesTests.SampleEntity, Long> {}

	@Before
	public void before() {
		IndexInitializer.init(elasticsearchTemplate, SampleEntity.class);
	}

	@Test
	public void bootstrapsRepository() {

		assertThat(repository).isNotNull();
	}

	@Test
	public void shouldScanSelectedPackage() {

		// given

		// when
		String[] beanNamesForType = context.getBeanNamesForType(ElasticsearchRepository.class);

		// then
		assertThat(beanNamesForType).containsExactlyInAnyOrder("sampleElasticsearchRepository",
				"sampleUUIDKeyedElasticsearchRepository");
	}

	@Test
	public void hasNotNestedRepository() {

		assertThat(nestedRepository).isNull();
	}

	@Data
	@Document(indexName = "test-index-sample-config-not-nested", type = "test-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		@ScriptedField private Double scriptedRate;
		private boolean available;
		private String highlightedMessage;
		private GeoPoint location;
		@Version private Long version;
		@Score private float score;
	}

	@Data
	@Document(indexName = "test-index-uuid-keyed-config-not-nested", type = "test-type-uuid-keyed", shards = 1,
			replicas = 0, refreshInterval = "-1")
	static class SampleEntityUUIDKeyed {

		@Id private UUID id;
		private String type;
		@Field(type = FieldType.Text, fielddata = true) private String message;
		private int rate;
		@ScriptedField private Long scriptedRate;
		private boolean available;
		private String highlightedMessage;
		private GeoPoint location;
		@Version private Long version;
	}
}
