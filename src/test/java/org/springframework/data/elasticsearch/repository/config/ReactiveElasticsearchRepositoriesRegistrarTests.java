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

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.lang.Double;
import java.lang.Long;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.TestUtils;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ReactiveElasticsearchRepositoriesRegistrarTests {

	@Configuration
	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {

		@Bean
		public ReactiveElasticsearchTemplate reactiveElasticsearchTemplate() {
			return new ReactiveElasticsearchTemplate(TestUtils.reactiveClient());
		}
	}

	@Autowired ReactiveSampleEntityRepository repository;
	@Autowired ApplicationContext context;

	@Test // DATAES-519
	public void testConfiguration() {

		Assertions.assertThat(context).isNotNull();
		Assertions.assertThat(repository).isNotNull();

	}

	interface ReactiveSampleEntityRepository extends ReactiveElasticsearchRepository<SampleEntity, String> {}

	/**
	 * @author Rizwan Idrees
	 * @author Mohsin Husen
	 * @author Chris White
	 * @author Sascha Woo
	 */
	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	@Builder
	@Document(indexName = "test-index-sample-reactive-repositories-registrar", type = "test-type", shards = 1, replicas = 0, refreshInterval = "-1")
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
}
