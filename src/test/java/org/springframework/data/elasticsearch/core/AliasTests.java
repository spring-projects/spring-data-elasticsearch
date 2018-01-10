/*
* Copyright 2014-2017 the original author or authors.
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
import java.util.List;
import java.util.Map;

import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.AliasBuilder;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.SampleEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mohsin Husen
 * @author Ilkang Na
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class AliasTests {

	private static final String INDEX_NAME_1 = "test-alias-index-1";
	private static final String INDEX_NAME_2 = "test-alias-index-2";
	private static final String TYPE_NAME = "test-alias-type";

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		Map<String, Object> settings = new HashMap<>();
		settings.put("index.refresh_interval", "-1");
		settings.put("index.number_of_replicas", "0");
		settings.put("index.number_of_shards", "2");
		settings.put("index.store.type", "fs");

		elasticsearchTemplate.deleteIndex(INDEX_NAME_1);
		elasticsearchTemplate.createIndex(INDEX_NAME_1, settings);
		elasticsearchTemplate.refresh(INDEX_NAME_1);

		elasticsearchTemplate.deleteIndex(INDEX_NAME_2);
		elasticsearchTemplate.createIndex(INDEX_NAME_2, settings);
		elasticsearchTemplate.refresh(INDEX_NAME_2);
	}

	@Test
	public void shouldAddAlias() {
		// given
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(INDEX_NAME_1)
				.withAliasName(aliasName).build();
		// when
		elasticsearchTemplate.addAlias(aliasQuery);
		// then
		List<AliasMetaData> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME_1);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.get(0).alias(), is(aliasName));
	}

	@Test
	public void shouldRemoveAlias() {
		// given
		String indexName = INDEX_NAME_1;
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(indexName)
				.withAliasName(aliasName).build();
		// when
		elasticsearchTemplate.addAlias(aliasQuery);
		List<AliasMetaData> aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.get(0).alias(), is(aliasName));
		// then
		elasticsearchTemplate.removeAlias(aliasQuery);
		aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases, anyOf(is(nullValue()), hasSize(0)));
	}

	/*
	DATAES-70
	*/
	@Test
	public void shouldAddAliasWithGivenRoutingValue() {
		//given
		String indexName = INDEX_NAME_1;
		String alias = "test-alias";

		AliasQuery aliasQuery = new AliasBuilder()
				.withIndexName(indexName)
				.withAliasName(alias)
				.withRouting("0").build();

		//when
		elasticsearchTemplate.addAlias(aliasQuery);

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId)
				.message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder()
				.withIndexName(alias)
				.withId(sampleEntity.getId())
				.withType(TYPE_NAME)
				.withObject(sampleEntity)
				.build();

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(INDEX_NAME_1);

		SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withIndices(alias).withTypes(TYPE_NAME).build();
		long count = elasticsearchTemplate.count(query);
		//then
		List<AliasMetaData> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME_1);
		assertThat(aliases, is(notNullValue()));
		assertThat(aliases.get(0).alias(), is(alias));
		assertThat(aliases.get(0).searchRouting(), is("0"));
		assertThat(aliases.get(0).indexRouting(), is("0"));
		assertThat(count, is(1L));

		//cleanup
		elasticsearchTemplate.removeAlias(aliasQuery);
		elasticsearchTemplate.deleteIndex(SampleEntity.class);
		elasticsearchTemplate.createIndex(SampleEntity.class);
		elasticsearchTemplate.putMapping(SampleEntity.class);
		elasticsearchTemplate.refresh(SampleEntity.class);
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
				.withIndexName(INDEX_NAME_1)
				.withAliasName(alias1)
				.withIndexRouting("0").build();

		AliasQuery aliasQuery2 = new AliasBuilder()
				.withIndexName(INDEX_NAME_2)
				.withAliasName(alias2)
				.withSearchRouting("1").build();

		//when
		elasticsearchTemplate.addAlias(aliasQuery1);
		elasticsearchTemplate.addAlias(aliasQuery2);

		String documentId = randomNumeric(5);
		SampleEntity sampleEntity = SampleEntity.builder().id(documentId)
				.message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder()
				.withIndexName(alias1)
				.withType(TYPE_NAME)
				.withId(sampleEntity.getId())
				.withObject(sampleEntity).build();

		elasticsearchTemplate.index(indexQuery);


		// then
		List<AliasMetaData> responseAlias1 = elasticsearchTemplate.queryForAlias(INDEX_NAME_1);
		assertThat(responseAlias1, is(notNullValue()));
		assertThat(responseAlias1.get(0).alias(), is(alias1));
		assertThat(responseAlias1.get(0).indexRouting(), is("0"));

		List<AliasMetaData> responseAlias2 = elasticsearchTemplate.queryForAlias(INDEX_NAME_2);
		assertThat(responseAlias2, is(notNullValue()));
		assertThat(responseAlias2.get(0).alias(), is(alias2));
		assertThat(responseAlias2.get(0).searchRouting(), is("1"));

		//cleanup
		elasticsearchTemplate.removeAlias(aliasQuery1);
		elasticsearchTemplate.removeAlias(aliasQuery2);
	}
}
