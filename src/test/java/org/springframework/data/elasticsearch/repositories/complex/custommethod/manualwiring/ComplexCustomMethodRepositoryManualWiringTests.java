/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.complex.custommethod.manualwiring;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.Data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:complex-custom-method-repository-manual-wiring-test.xml")
public class ComplexCustomMethodRepositoryManualWiringTests {

	@Autowired private ComplexElasticsearchRepositoryManualWiring complexRepository;

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		IndexInitializer.init(elasticsearchTemplate, SampleEntity.class);
	}

	@Test
	public void shouldExecuteComplexCustomMethod() {

		// given

		// when
		String result = complexRepository.doSomethingSpecial();

		// then
		assertThat(result).isEqualTo("3+3=6");
	}

	@Data
	@Document(indexName = "test-index-sample-repository-manual-wiring", type = "test-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class SampleEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
	}
}
