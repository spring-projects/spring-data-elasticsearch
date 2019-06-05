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
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-custom-mapper.xml")
public class ElasticsearchTemplateCustomMapperTests {

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired private EntityMapper entityMapper;

	@Autowired private ResultsMapper resultsMapper;

	@Test
	public void shouldUseCustomMapper() {

		// given

		// when

		// then
		assertThat(elasticsearchTemplate.getResultsMapper()).isSameAs(resultsMapper);
		assertThat(elasticsearchTemplate.getResultsMapper().getEntityMapper()).isSameAs(entityMapper);
	}
}
