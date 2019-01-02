/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.setting;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.FieldDynamicMappingEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * FieldDynamicMappingEntityRepositoryTests
 *
 * @author Ted Liang
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:dynamic-settings-test.xml")
public class FieldDynamicMappingEntityRepositoryTests {

	@Autowired
	private FieldDynamicMappingEntityRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(FieldDynamicMappingEntity.class);
		elasticsearchTemplate.createIndex(FieldDynamicMappingEntity.class);
		elasticsearchTemplate.putMapping(FieldDynamicMappingEntity.class);
		elasticsearchTemplate.refresh(FieldDynamicMappingEntity.class);
	}

	/*
	DATAES-209
	*/
	@Test
	public void shouldCreateMappingWithMappingAnnotationAtFieldLevel() {
		//given

		//then
		Map mapping = elasticsearchTemplate.getMapping(FieldDynamicMappingEntity.class);
		assertThat(mapping, is(notNullValue()));

		Map properties = (Map) mapping.get("properties");
		assertThat(properties, is(notNullValue()));

		assertThat(properties.containsKey("file"), is(true));
		Map file = (Map) properties.get("file");
		assertThat(file, is(notNullValue()));
		assertThat(((String) file.get("type")), is("text"));

		assertThat(file.containsKey("fields"), is(true));
		Map fields = (Map) file.get("fields");
		assertThat(fields, is(notNullValue()));

		assertThat(fields.containsKey("content"), is(true));
		Map content = (Map) fields.get("content");
		assertThat(content, is(notNullValue()));

		assertThat((String)content.get("type"), is("text"));
		assertThat((String)content.get("term_vector"), is("with_positions_offsets"));
		assertThat((Boolean)content.get("store"), is(Boolean.TRUE));
	}
}
