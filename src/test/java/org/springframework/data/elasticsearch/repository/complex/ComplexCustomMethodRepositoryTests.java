/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.complex;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:complex-custom-method-repository-test.xml")
public class ComplexCustomMethodRepositoryTests {

	@Autowired
	private ComplexElasticsearchRepository complexRepository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);
	}

	@Test
	public void shouldExecuteComplexCustomMethod() {
		//Given

		//When
		String result = complexRepository.doSomethingSpecial();
		//Then
		assertThat(result, is("2+2=4"));
	}
}
