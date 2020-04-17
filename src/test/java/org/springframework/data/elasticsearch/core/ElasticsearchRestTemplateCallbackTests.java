/*
 * Copyright 2020 the original author or authors.
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

import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Roman Puchkovskiy
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ElasticsearchRestTemplateCallbackTests extends AbstractElasticsearchTemplateCallbackTests {

	@Mock private RestHighLevelClient client;

	@Mock private IndexResponse indexResponse;
	@Mock private BulkResponse bulkResponse;
	@Mock private BulkItemResponse bulkItemResponse;
	@Mock private GetResponse getResponse;
	@Mock private MultiGetResponse multiGetResponse;
	@Mock private MultiGetItemResponse multiGetItemResponse;
	@Mock private MultiSearchResponse.Item multiSearchResponseItem;

	@SuppressWarnings("deprecation") // we know what we test
	@BeforeEach
	public void setUp() throws Exception {
		initTemplate(new ElasticsearchRestTemplate(client));

		doReturn(indexResponse).when(client).index(any(IndexRequest.class), any(RequestOptions.class));
		doReturn("response-id").when(indexResponse).getId();

		doReturn(bulkResponse).when(client).bulk(any(BulkRequest.class), any(RequestOptions.class));
		doReturn(new BulkItemResponse[] { bulkItemResponse, bulkItemResponse }).when(bulkResponse).getItems();
		doReturn("response-id").when(bulkItemResponse).getId();

		doReturn(getResponse).when(client).get(any(GetRequest.class), any(RequestOptions.class));

		doReturn(true).when(getResponse).isExists();
		doReturn(false).when(getResponse).isSourceEmpty();
		doReturn(new HashMap<String, Object>() {
			{
				put("id", "init");
				put("firstname", "luke");
			}
		}).when(getResponse).getSourceAsMap();

		doReturn(multiGetResponse).when(client).mget(any(MultiGetRequest.class), any(RequestOptions.class));
		doReturn(new MultiGetItemResponse[] { multiGetItemResponse, multiGetItemResponse }).when(multiGetResponse)
				.getResponses();
		doReturn(getResponse).when(multiGetItemResponse).getResponse();

		doReturn(searchResponse).when(client).search(any(SearchRequest.class), any(RequestOptions.class));
		doReturn(nSearchHits(2)).when(searchResponse).getHits();
		doReturn("scroll-id").when(searchResponse).getScrollId();
		doReturn(new BytesArray(new byte[8])).when(searchHit).getSourceRef();
		doReturn(new HashMap<String, Object>() {
			{
				put("id", "init");
				put("firstname", "luke");
			}
		}).when(searchHit).getSourceAsMap();

		MultiSearchResponse multiSearchResponse = new MultiSearchResponse(
				new MultiSearchResponse.Item[] { multiSearchResponseItem }, 1L);
		doReturn(multiSearchResponse).when(client).multiSearch(any(MultiSearchRequest.class), any());
		doReturn(searchResponse).when(multiSearchResponseItem).getResponse();

		doReturn(searchResponse).when(client).scroll(any(SearchScrollRequest.class), any(RequestOptions.class));
	}
}
