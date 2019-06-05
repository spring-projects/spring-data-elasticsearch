/*
* Copyright 2014-2019 the original author or authors.
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

import static org.apache.commons.lang.RandomStringUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.Long;
import java.lang.Object;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.query.AliasBuilder;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mohsin Husen
 * @author Ilkang Na
 * @author Peter-Josef Meisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class AliasTests {

	private static final String INDEX_NAME_1 = "test-alias-index-1";
	private static final String INDEX_NAME_2 = "test-alias-index-2";
	private static final String TYPE_NAME = "test-alias-type";

	private static Map<String, Object> settings = new HashMap<>();
	static {
		settings.put("index.refresh_interval", "-1");
		settings.put("index.number_of_replicas", "0");
		settings.put("index.number_of_shards", "2");
		settings.put("index.store.type", "fs");
	}

	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {

		elasticsearchTemplate.deleteIndex(INDEX_NAME_1);
		elasticsearchTemplate.createIndex(INDEX_NAME_1, settings);
		elasticsearchTemplate.refresh(INDEX_NAME_1);

		elasticsearchTemplate.deleteIndex(INDEX_NAME_2);
		elasticsearchTemplate.createIndex(INDEX_NAME_2, settings);
		elasticsearchTemplate.refresh(INDEX_NAME_2);

		IndexInitializer.init(elasticsearchTemplate, AliasedEntity.class);
	}

	@Test
	public void shouldAddAlias() {

		// given
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder().withIndexName(INDEX_NAME_1).withAliasName(aliasName).build();

		// when
		elasticsearchTemplate.addAlias(aliasQuery);

		// then
		List<AliasMetaData> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME_1);
		assertThat(aliases).isNotNull();
		assertThat(aliases.get(0).alias()).isEqualTo(aliasName);
	}

	@Test
	public void shouldRemoveAlias() {

		// given
		String indexName = INDEX_NAME_1;
		String aliasName = "test-alias";
		AliasQuery aliasQuery = new AliasBuilder().withIndexName(indexName).withAliasName(aliasName).build();

		// when
		elasticsearchTemplate.addAlias(aliasQuery);
		List<AliasMetaData> aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases).isNotNull();
		assertThat(aliases.get(0).alias()).isEqualTo(aliasName);

		// then
		elasticsearchTemplate.removeAlias(aliasQuery);
		aliases = elasticsearchTemplate.queryForAlias(indexName);
		assertThat(aliases).isEmpty();
	}

	@Test // DATAES-70
	public void shouldAddAliasWithGivenRoutingValue() {

		// given
		String alias = "test-alias";

		AliasQuery aliasQuery = new AliasBuilder().withIndexName(INDEX_NAME_1).withAliasName(alias).withRouting("0")
				.build();

		// when
		elasticsearchTemplate.addAlias(aliasQuery);

		String documentId = randomNumeric(5);
		AliasedEntity aliasedEntity = AliasedEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder().withIndexName(alias).withId(aliasedEntity.getId())
				.withType(TYPE_NAME).withObject(aliasedEntity).build();

		elasticsearchTemplate.index(indexQuery);
		elasticsearchTemplate.refresh(INDEX_NAME_1);

		SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withIndices(alias)
				.withTypes(TYPE_NAME).build();
		long count = elasticsearchTemplate.count(query);

		// then
		List<AliasMetaData> aliases = elasticsearchTemplate.queryForAlias(INDEX_NAME_1);
		assertThat(aliases).isNotNull();
		AliasMetaData aliasMetaData = aliases.get(0);
		assertThat(aliasMetaData.alias()).isEqualTo(alias);
		assertThat(aliasMetaData.searchRouting()).isEqualTo("0");
		assertThat(aliasMetaData.indexRouting()).isEqualTo("0");
		assertThat(count).isEqualTo(1);

		// cleanup
		elasticsearchTemplate.removeAlias(aliasQuery);
	}

	@Test // DATAES-70
	public void shouldAddAliasForVariousRoutingValues() {

		// given
		String alias1 = "test-alias-1";
		String alias2 = "test-alias-2";

		AliasQuery aliasQuery1 = new AliasBuilder().withIndexName(INDEX_NAME_1).withAliasName(alias1).withIndexRouting("0")
				.build();

		AliasQuery aliasQuery2 = new AliasBuilder().withIndexName(INDEX_NAME_2).withAliasName(alias2).withSearchRouting("1")
				.build();

		// when
		elasticsearchTemplate.addAlias(aliasQuery1);
		elasticsearchTemplate.addAlias(aliasQuery2);

		String documentId = randomNumeric(5);
		AliasedEntity aliasedEntity = AliasedEntity.builder().id(documentId).message("some message")
				.version(System.currentTimeMillis()).build();

		IndexQuery indexQuery = new IndexQueryBuilder().withIndexName(alias1).withType(TYPE_NAME)
				.withId(aliasedEntity.getId()).withObject(aliasedEntity).build();

		elasticsearchTemplate.index(indexQuery);

		// then
		List<AliasMetaData> responseAlias1 = elasticsearchTemplate.queryForAlias(INDEX_NAME_1);
		assertThat(responseAlias1).isNotNull();
		AliasMetaData aliasMetaData1 = responseAlias1.get(0);
		assertThat(aliasMetaData1.alias()).isEqualTo(alias1);
		assertThat(aliasMetaData1.indexRouting()).isEqualTo("0");

		List<AliasMetaData> responseAlias2 = elasticsearchTemplate.queryForAlias(INDEX_NAME_2);
		assertThat(responseAlias2).isNotNull();
		AliasMetaData aliasMetaData2 = responseAlias2.get(0);
		assertThat(aliasMetaData2.alias()).isEqualTo(alias2);
		assertThat(aliasMetaData2.searchRouting()).isEqualTo("1");

		// cleanup
		elasticsearchTemplate.removeAlias(aliasQuery1);
		elasticsearchTemplate.removeAlias(aliasQuery2);
	}

	@Builder
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Document(indexName = "test-index-sample-core-alias", type = "test-type", shards = 1, replicas = 0,
			refreshInterval = "-1")
	static class AliasedEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String message;
		@Version private Long version;
	}
}
