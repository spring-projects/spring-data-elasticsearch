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

import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.elasticsearch.join.query.JoinQueryBuilders.hasChildQuery;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.RoutingMissingException;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.entities.ParentEntity;
import org.springframework.data.elasticsearch.entities.ParentEntity.ChildEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Philipp Jardas
 */
@Ignore(value = "DATAES-421")
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
		elasticsearchTemplate.putMapping(ChildEntity.class);
	}

	@After
	public void clean() {
		elasticsearchTemplate.deleteIndex(ChildEntity.class);
		elasticsearchTemplate.deleteIndex(ParentEntity.class);
	}

	@Ignore(value = "DATAES-421")
	@Test
	public void shouldIndexParentChildEntity() {
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
		QueryBuilder query = hasChildQuery(ParentEntity.CHILD_TYPE, QueryBuilders.termQuery("name", child1name.toLowerCase()), ScoreMode.None);
		List<ParentEntity> parents = elasticsearchTemplate.queryForList(new NativeSearchQuery(query), ParentEntity.class);

		// we're expecting only the first parent as result
		assertThat("parents", parents, contains(hasProperty("id", is(parent1.getId()))));
	}

	@Ignore(value = "DATAES-421")
	@Test
	public void shouldUpdateChild() throws Exception {
		// index parent and child
		ParentEntity parent = index("parent", "Parent");
		ChildEntity child = index("child", parent.getId(), "Child");
		String newChildName = "New Child Name";

		// update the child, not forgetting to set the parent id as routing parameter
		UpdateRequest updateRequest = new UpdateRequest(ParentEntity.INDEX, ParentEntity.CHILD_TYPE, child.getId());
		updateRequest.routing(parent.getId());
		XContentBuilder builder;
			builder = jsonBuilder().startObject().field("name", newChildName).endObject();
		updateRequest.doc(builder);
		final UpdateResponse response = update(updateRequest);

		assertThat(response.getShardInfo().getSuccessful(), is(1));
	}

	@Ignore(value = "DATAES-421")
	@Test(expected = RoutingMissingException.class)
	public void shouldFailWithRoutingMissingExceptionOnUpdateChildIfNotRoutingSetOnUpdateRequest() throws Exception {
		// index parent and child
		ParentEntity parent = index("parent", "Parent");
		ChildEntity child = index("child", parent.getId(), "Child");
		String newChildName = "New Child Name";

		// update the child, forget routing parameter
		UpdateRequest updateRequest = new UpdateRequest(ParentEntity.INDEX, ParentEntity.CHILD_TYPE, child.getId());
		XContentBuilder builder;
		builder = jsonBuilder().startObject().field("name", newChildName).endObject();
		updateRequest.doc(builder);
		update(updateRequest);
	}

	@Ignore(value = "DATAES-421")
	@Test(expected = RoutingMissingException.class)
	public void shouldFailWithRoutingMissingExceptionOnUpdateChildIfRoutingOnlySetOnRequestDoc() throws Exception {
		// index parent and child
		ParentEntity parent = index("parent", "Parent");
		ChildEntity child = index("child", parent.getId(), "Child");
		String newChildName = "New Child Name";

		// update the child
		UpdateRequest updateRequest = new UpdateRequest(ParentEntity.INDEX, ParentEntity.CHILD_TYPE, child.getId());
		XContentBuilder builder;
		builder = jsonBuilder().startObject().field("name", newChildName).endObject();
		updateRequest.doc(builder);
		updateRequest.doc().routing(parent.getId());
		update(updateRequest);
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

	private UpdateResponse update(UpdateRequest updateRequest) {
		final UpdateQuery update = new UpdateQuery();
		update.setId(updateRequest.id());
		update.setType(updateRequest.type());
		update.setIndexName(updateRequest.index());
		update.setUpdateRequest(updateRequest);
		return elasticsearchTemplate.update(update);
	}
}
