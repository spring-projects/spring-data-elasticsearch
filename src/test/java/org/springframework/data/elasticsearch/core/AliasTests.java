/*
* Copyright 2014-2015 the original author or authors.
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
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.builder.SampleEntityBuilder;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mohsin Husen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class AliasTests {

	private static final String INDEX_NAME = "test-alias-index";
	private static final String TYPE_NAME = "test-alias-type";

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put("index.refresh_interval", "-1");
		settings.put("index.number_of_replicas", "0");
		settings.put("index.number_of_shards", "2");
		settings.put("index.store.type", "memory");

		elasticsearchTemplate.deleteIndex(INDEX_NAME);
		elasticsearchTemplate.createIndex(INDEX_NAME, settings);
		elasticsearchTemplate.refresh(INDEX_NAME, true);
	}

	@Test
	public void shouldAddAlias() {
		// given
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(INDEX_NAME)
				.withAliasName(aliasName).build();
		// when
		elasticsearchTemplate.addAlias(aliasQuery);
		// then
		Set<String> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.contains(aliasName), is(true));
	}

	@Test
	public void shouldRemoveAlias() {
		// given
		String indexName = INDEX_NAME;
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(indexName)
				.withAliasName(aliasName).build();
		// when
		elasticsearchTemplate.addAlias(aliasQuery);
		Set<String> aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.contains(aliasName), is(true));
		// then
		elasticsearchTemplate.removeAlias(aliasQuery);
		aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.size(), is(0));
	}

	/*
	DATAES-70
	*/
	@Test
	public void shouldAddAliasWithGivenRoutingValue() {
		//given
		String indexName = INDEX_NAME;
		String alias = "test-alias";

		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(indexName)
				.withAliasName(alias)
				.withRouting("0").build();

		//when
		elasticsearchTemplate.addAlias(aliasQuery);

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder()
				.withIndexName(alias)
				.withId(sampleEntity.getId())
				.withType(TYPE_NAME)
				.withObject(sampleEntity)
				.build();

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(INDEX_NAME, true);

		SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(alias).withTypes(TYPE_NAME).build();
		long count = elasticsearchTemplate.count(query);
		//then
		Set<String> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.contains(alias), is(true));
		assertThat(count, is(1L));

		//cleanup
		elasticsearchTemplate.removeAlias(aliasQuery);
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.putMapping(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class, true);
	}

	/*
	DATAES-70
	*/
	@Test
	public void shouldAddAliasForVariousRoutingValues() {
		//given
		String alias1 = "test-alias-1";
		String alias2 = "test-alias-2";

		AliasQuery aliasQuery1 = new AliasBuilder()
				.withIndexName(INDEX_NAME)
				.withAliasName(alias1)
				.withIndexRouting("0").build();

		AliasQuery aliasQuery2 = new AliasBuilder()
				.withIndexName(INDEX_NAME)
				.withAliasName(alias2)
				.withSearchRouting("1").build();

		//when
		elasticsearchTemplate.addAlias(aliasQuery1);
		elasticsearchTemplate.addAlias(aliasQuery2);

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = new SampleEntityBuilder(documentId)
				.message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder()
				.withIndexName(alias1)
				.withType(TYPE_NAME)
				.withId(sampleEntity.getId())
				.withObject(sampleEntity).build();

		elasticsearchTemplate.index(indexQuery);

		SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(alias2).withTypes(TYPE_NAME).build();
		long count = elasticsearchTemplate.count(query, SampleEntity.class);
		// then
		Set<String> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.contains(alias1), is(true));
		assertThat(aliases.contains(alias2), is(true));
		assertThat(count, is(0L));

		//cleanup
		elasticsearchTemplate.removeAlias(aliasQuery1);
		elasticsearchTemplate.removeAlias(aliasQuery2);
	}
}
