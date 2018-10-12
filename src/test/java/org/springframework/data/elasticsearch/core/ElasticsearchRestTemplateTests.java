/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static org.apache.commons.lang.RandomStringUtils.*;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQueryBuilder;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Franck Marchand
 * @author Abdul Mohammed
 * @author Kevin Leturc
 * @author Mason Chan
 * @author Chris White
 * @author Ilkang Na
 * @author Alen Turkovic
 * @author Sascha Woo
 * @author Don Wellington
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-rest-template-test.xml")
public class ElasticsearchRestTemplateTests extends ElasticsearchTemplateTests {

	@Test(expected = ElasticsearchStatusException.class)
	public void shouldThrowExceptionIfDocumentDoesNotExistWhileDoingPartialUpdate() {
		// when
		IndexRequest indexRequest = new IndexRequest();
		indexRequest.source("{}", XContentType.JSON);
		UpdateQuery updateQuery = new UpdateQueryBuilder().withId(randomNumeric(5))
				.withClass(SampleEntity.class).withIndexRequest(indexRequest).build();
		elasticsearchTemplate.update(updateQuery);
	}
}
