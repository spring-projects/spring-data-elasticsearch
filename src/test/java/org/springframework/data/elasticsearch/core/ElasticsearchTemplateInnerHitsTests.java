/*
 * Copyright 2014-2016 the original author or authors.
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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.entities.Book;
import org.springframework.data.elasticsearch.entities.ParentEntity;
import org.springframework.data.elasticsearch.entities.ParentEntity.ChildEntity;
import org.springframework.data.elasticsearch.entities.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Franck Lefebure
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class ElasticsearchTemplateInnerHitsTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		clean();
		elasticsearchTemplate.createIndex(ParentEntity.class);
		elasticsearchTemplate.createIndex(ChildEntity.class);
		elasticsearchTemplate.putMapping(ChildEntity.class);
		elasticsearchTemplate.createIndex(Book.class);
		elasticsearchTemplate.putMapping(Book.class);
		elasticsearchTemplate.createIndex(Person.class);
		elasticsearchTemplate.putMapping(Person.class);
	}

	@After
	public void clean() {
		elasticsearchTemplate.deleteIndex(ChildEntity.class);
		elasticsearchTemplate.deleteIndex(ParentEntity.class);
		elasticsearchTemplate.deleteIndex(Book.class);
		elasticsearchTemplate.deleteIndex(Person.class);
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

	@Test
	public void shouldHaveInnerHitsChildren() {
		// index two parents
		ParentEntity parent1 = index("parent1", "First Parent");
		ParentEntity parent2 = index("parent2", "Second Parent");

		// index a child for each parent
		String child1name = "First";
		index("child1", parent1.getId(), child1name);
		index("child2", parent2.getId(), "Second");

		elasticsearchTemplate.refresh(ParentEntity.class);
		elasticsearchTemplate.refresh(ChildEntity.class);

		// find all parents that have the first child
		QueryBuilder query = hasChildQuery(ParentEntity.CHILD_TYPE, QueryBuilders.termQuery("name", child1name.toLowerCase())).innerHit(new QueryInnerHitBuilder());
		List<ParentEntity> parents = elasticsearchTemplate.queryForList(new NativeSearchQuery(query), ParentEntity.class);

		assertEquals(1, parents.size());
		assertNotNull(parents.get(0).getChildren());
		assertEquals("First", parents.get(0).getChildren().get(0).getName());

	}

	@Test
	public void shouldHaveInnerHitsParent() {
		// index two parents
		ParentEntity parent1 = index("parent1", "firstparent");
		ParentEntity parent2 = index("parent2", "secondparent");

		// index a child for each parent
		String child1name = "First";
		index("child1", parent1.getId(), child1name);
		index("child2", parent2.getId(), "Second");

		elasticsearchTemplate.refresh(ParentEntity.class);
		elasticsearchTemplate.refresh(ChildEntity.class);

		// find all parents that have the first child
		QueryBuilder query = hasParentQuery(ParentEntity.PARENT_TYPE, QueryBuilders.termQuery("name", "firstparent")).innerHit(new QueryInnerHitBuilder());
		List<ChildEntity> children = elasticsearchTemplate.queryForList(new NativeSearchQuery(query), ChildEntity.class);

		assertEquals(1, children.size());
		assertNotNull(children.get(0).getParent());
		assertEquals("firstparent", children.get(0).getParent().getName());

	}

	@Test
	public void shouldHaveInnerHitNested() {

		final Book book1 = new Book();
		final Book book2 = new Book();

		book1.setId(randomNumeric(5));
		book1.setName("testbook1");

		book2.setId(randomNumeric(5));
		book2.setName("testbook2");

		final Person person1 = new Person();
		person1.setName("doe");
		person1.setId(randomNumeric(5));
		person1.setBooks(new ArrayList<Book>());
		person1.getBooks().add(book1);
		person1.getBooks().add(book2);
		IndexQuery index = new IndexQuery();
		index.setId(person1.getId());
		index.setObject(person1);
		elasticsearchTemplate.index(index);
		elasticsearchTemplate.refresh(Person.class);
		QueryBuilder query = nestedQuery("books", QueryBuilders.termQuery("books.name", "testbook1")).innerHit(new QueryInnerHitBuilder());
		List<Person> persons = elasticsearchTemplate.queryForList(new NativeSearchQuery(query), Person.class);

		assertEquals(1, persons.size());
		assertNotNull(persons.get(0).getTargetedBooks());
		assertEquals("testbook1", persons.get(0).getTargetedBooks().get(0).getName());

	}
}
