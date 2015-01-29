/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.SampleExplanableEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-explanation-test.xml")
public class ExplanationQueryTests {


	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate ;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(SampleExplanableEntity.class);
		elasticsearchTemplate.createIndex(SampleExplanableEntity.class);
		elasticsearchTemplate.refresh(SampleExplanableEntity.class, true);
	}

	@Test
	public void shouldPerformAndOperation() {

		// given
		String documentId = randomNumeric(5);
        SampleExplanableEntity sampleEntity = new SampleExplanableEntity();
		sampleEntity.setId(documentId);
		sampleEntity.setMessage("some test message");
		sampleEntity.setVersion(System.currentTimeMillis());

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(documentId);
		indexQuery.setObject(sampleEntity);
		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(SampleExplanableEntity.class, true);

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("message").contains("test").and("message")
				.contains("some"));
        criteriaQuery.setExplain(true);

		// when
        SampleExplanableEntity sampleEntity1 = elasticsearchTemplate.queryForObject(criteriaQuery, SampleExplanableEntity.class);

		// then
		assertThat(sampleEntity1.getExplanation(), is(notNullValue()));
	}

}
