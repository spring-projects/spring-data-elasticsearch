/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.IndexInformation;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author George Popides
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class IndexOperationTests {
	@Autowired
	ElasticsearchOperations operations;


	@Test // #1646
	void shouldReturnInformationList() {
		String indexName = "random-index-name";
		IndexOperations indexOps = operations.indexOps(IndexCoordinates.of(indexName));
		indexOps.create();

		List<IndexInformation> indexInformationList = indexOps.getInformation();
		assertThat(indexInformationList.size()).isEqualTo(1);

		IndexInformation indexInformation = indexInformationList.get(0);

		assertThat(indexInformation).isNotNull();
		assertThat(indexInformation.getName()).isNotNull();
		assertThat(indexInformation.getName()).isEqualTo(indexName);
		assertThat(indexInformation.getMappings()).isInstanceOf(org.springframework.data.elasticsearch.core.document.Document.class);
		assertThat(indexInformation.getSettings()).isInstanceOf(org.springframework.data.elasticsearch.core.document.Document.class);
		assertThat(indexInformation.getAliases()).isInstanceOf(Set.class);

	}
}
