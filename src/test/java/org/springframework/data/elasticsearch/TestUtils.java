/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.data.elasticsearch;

import lombok.SneakyThrows;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.data.elasticsearch.client.ReactiveElasticsearchClient;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @currentRead Fool's Fate - Robin Hobb
 */
public final class TestUtils {

	private TestUtils() {}

	public static RestHighLevelClient restHighLevelClient() {
		return new RestHighLevelClient(RestClient.builder(HttpHost.create("http://localhost:9200")));
	}

	public static ReactiveElasticsearchClient reactiveClient() {
		return ReactiveElasticsearchClient.local();
	}

	@SneakyThrows
	public static void deleteIndex(String... indexes) {

		if (ObjectUtils.isEmpty(indexes)) {
			return;
		}

		try (RestHighLevelClient client = restHighLevelClient()) {
			for (String index : indexes) {

				try {
					client.indices().delete(new DeleteIndexRequest(index));
				} catch (ElasticsearchStatusException ex) {
					// just ignore it
				}
			}
		}
	}
}
