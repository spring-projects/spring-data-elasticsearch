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
package org.springframework.data.elasticsearch.core;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.ParentEntity;
import org.springframework.data.elasticsearch.ParentEntity.ChildEntity;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Philipp Jardas
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateParentChildTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		clean();
		elasticsearchTemplate.createIndex(ParentEntity.class);
		elasticsearchTemplate.createIndex(ChildEntity.class);
		elasticsearchTemplate.putMapping(ParentEntity.class);
		elasticsearchTemplate.putMapping(ChildEntity.class);
	}

	@After
	public void clean() {
		elasticsearchTemplate.deleteIndex(ChildEntity.class);
		elasticsearchTemplate.deleteIndex(ParentEntity.class);
	}

	@Test
	public void shouldIndexParentChildEntity() {
		// index two parents
		ParentEntity parent1 = index("parent1", "First Parent");
		ParentEntity parent2 = index("parent2", "Second Parent");

		// index a child for each parent
		String child1name = "First";
		index("child1", parent1.getId(), child1name);
		index("child2", parent2.getId(), "Second");

		elasticsearchTemplate.refresh(ParentEntity.class, true);
		elasticsearchTemplate.refresh(ChildEntity.class, true);

		// find all parents that have the first child
		QueryBuilder query = hasChildQuery(ParentEntity.CHILD_TYPE, QueryBuilders.fieldQuery("name", child1name));
		List<ParentEntity> parents = elasticsearchTemplate.queryForList(new NativeSearchQuery(query), ParentEntity.class);

		// we're expecting only the first parent as result
		assertThat("parents", parents, contains(hasProperty("id", is(parent1.getId()))));
	}

	@Test
	public void shouldSearchTopChildrenForGivenParent() {
		// index two parents
		ParentEntity parent1 = index("parent1", "First Parent");
		ParentEntity parent2 = index("parent2", "Second Parent");

		// index a child for each parent
		String child1name = "First";
		index("child1", parent1.getId(), child1name);
		index("child2", parent2.getId(), "Second");

		elasticsearchTemplate.refresh(ParentEntity.class, true);
		elasticsearchTemplate.refresh(ChildEntity.class, true);

		// find all parents that have the first child using topChildren Query
		QueryBuilder query = topChildrenQuery(ParentEntity.CHILD_TYPE, QueryBuilders.fieldQuery("name", child1name));
		List<ParentEntity> parents = elasticsearchTemplate.queryForList(new NativeSearchQuery(query), ParentEntity.class);

		// we're expecting only the first parent as result
		assertThat("parents", parents, contains(hasProperty("id", is(parent1.getId()))));
	}

	private ParentEntity index(String parentId, String name) {
		ParentEntity parent = new ParentEntity(parentId, name);
		IndexQuery index = new IndexQuery();
		index.setId(parent.getId());
		index.setObject(parent);
		elasticsearchTemplate.index(index);

		return parent;
	}

	private ChildEntity index(String childId, String parentId, String name) {
		ChildEntity child = new ChildEntity(childId, parentId, name);
		IndexQuery index = new IndexQuery();
		index.setId(child.getId());
		index.setObject(child);
		index.setParentId(child.getParentId());
		elasticsearchTemplate.index(index);

		return child;
	}
}
