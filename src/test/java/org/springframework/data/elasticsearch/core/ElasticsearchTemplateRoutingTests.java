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

import static org.elasticsearch.index.query.QueryBuilders.hasChildQuery;
import static org.elasticsearch.index.query.QueryBuilders.topChildrenQuery;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.entities.*;
import org.springframework.data.elasticsearch.entities.ParentEntity.ChildEntity;
import org.springframework.data.elasticsearch.entities.ParentEntity.ChildEntity.GrandChildEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Matthias Melitzer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateRoutingTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		clean();
		elasticsearchTemplate.createIndex(ParentEntity.class);
		elasticsearchTemplate.createIndex(ChildEntity.class);
		elasticsearchTemplate.createIndex(GrandChildEntity.class);
		elasticsearchTemplate.putMapping(ParentEntity.class);
		elasticsearchTemplate.putMapping(ChildEntity.class);
		elasticsearchTemplate.putMapping(GrandChildEntity.class);
	}

	@After
	public void clean() {
		elasticsearchTemplate.deleteIndex(GrandChildEntity.class);
		elasticsearchTemplate.deleteIndex(ChildEntity.class);
		elasticsearchTemplate.deleteIndex(ParentEntity.class);
	}

	@Test
	public void shouldIndexParentChildGrandChildEntity() {
		// index two parents
		ParentEntity parent1 = index("parent1", "First Parent");
		ParentEntity parent2 = index("parent2", "Second Parent");

		// index a child for each parent
		String child1name = "First";
		ChildEntity child1 = index("child1", parent1.getId(), child1name);
		ChildEntity child2 = index("child2", parent2.getId(), "Second");

		String grandChildName = "FirstGrandChild";
		indexGrandChild("grandChild1", child1.getId(), parent1.getId(), grandChildName);
		indexGrandChild("grandChild2", child2.getId(), child2.getParentId(), "SecondGrandchild");

		elasticsearchTemplate.refresh(ParentEntity.class, true);
		elasticsearchTemplate.refresh(ChildEntity.class, true);
		elasticsearchTemplate.refresh(GrandChildEntity.class, true);

		// find all parents that have the first grand child
		QueryBuilder query = hasChildQuery(ParentEntity.CHILD_TYPE, hasChildQuery(ParentEntity.GRAND_CHILD_TYPE, QueryBuilders.termQuery("name", grandChildName.toLowerCase())));
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
		ChildEntity child1 = index("child1", parent1.getId(), child1name);
		ChildEntity child2 = index("child2", parent2.getId(), "Second");

		String grandChildName = "FirstGrandChild";
		indexGrandChild("grandChild1", child1.getId(), parent1.getId(), grandChildName);
		indexGrandChild("grandChild2", child2.getId(), child2.getParentId(), "SecondGrandchild");

		elasticsearchTemplate.refresh(ParentEntity.class, true);
		elasticsearchTemplate.refresh(ChildEntity.class, true);
		elasticsearchTemplate.refresh(GrandChildEntity.class, true);

		// find all parents that have the first child using topChildren Query
		QueryBuilder query = topChildrenQuery(ParentEntity.CHILD_TYPE, topChildrenQuery(ParentEntity.GRAND_CHILD_TYPE, QueryBuilders.termQuery("name", grandChildName.toLowerCase())));
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
	
	private GrandChildEntity indexGrandChild(String childId, String parentId, String grandParentId, String name) {
		GrandChildEntity grandChild = new GrandChildEntity(childId, parentId, grandParentId, name);
		IndexQuery index = new IndexQuery();
		index.setId(grandChild.getId());
		index.setObject(grandChild);
		index.setParentId(parentId);
		index.setRoutingId(grandParentId);
		elasticsearchTemplate.index(index);

		return grandChild;
	}
}
