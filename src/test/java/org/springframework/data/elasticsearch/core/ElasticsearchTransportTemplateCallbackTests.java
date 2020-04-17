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

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.unit.TimeValue;
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
class ElasticsearchTransportTemplateCallbackTests extends AbstractElasticsearchTemplateCallbackTests {

	@Mock private Client client;

	@Mock private IndexRequestBuilder indexRequestBuilder;
	@Mock private ActionFuture<IndexResponse> indexResponseActionFuture;
	@Mock private IndexResponse indexResponse;
	@Mock private BulkRequestBuilder bulkRequestBuilder;
	@Mock private ActionFuture<BulkResponse> bulkResponseActionFuture;
	@Mock private BulkResponse bulkResponse;
	@Mock private BulkItemResponse bulkItemResponse;
	@Mock private GetRequestBuilder getRequestBuilder;
	@Mock private ActionFuture<GetResponse> getResponseActionFuture;
	@Mock private GetResponse getResponse;
	@Mock private MultiGetRequestBuilder multiGetRequestBuilder;
	@Mock private ActionFuture<MultiGetResponse> multiGetResponseActionFuture;
	@Mock private MultiGetResponse multiGetResponse;
	@Mock private MultiGetItemResponse multiGetItemResponse;
	@Mock private SearchRequestBuilder searchRequestBuilder;
	@Mock private ActionFuture<SearchResponse> searchResponseActionFuture;
	@Mock private ActionFuture<MultiSearchResponse> multiSearchResponseActionFuture;
	@Mock private MultiSearchResponse.Item multiSearchResponseItem;
	@Mock private SearchScrollRequestBuilder searchScrollRequestBuilder;

	@BeforeEach
	@SuppressWarnings("deprecation") // we know what we are testing
	public void setUp() {
		initTemplate(new ElasticsearchTemplate(client));

		when(client.prepareIndex(anyString(), anyString(), anyString())).thenReturn(indexRequestBuilder);
		doReturn(indexResponseActionFuture).when(indexRequestBuilder).execute();
		when(indexResponseActionFuture.actionGet()).thenReturn(indexResponse);
		doReturn("response-id").when(indexResponse).getId();

		when(client.prepareBulk()).thenReturn(bulkRequestBuilder);
		doReturn(bulkResponseActionFuture).when(bulkRequestBuilder).execute();
		when(bulkResponseActionFuture.actionGet()).thenReturn(bulkResponse);
		doReturn(new BulkItemResponse[] { bulkItemResponse, bulkItemResponse }).when(bulkResponse).getItems();
		doReturn("response-id").when(bulkItemResponse).getId();

		when(client.prepareGet(anyString(), any(), any())).thenReturn(getRequestBuilder);
		doReturn(getResponseActionFuture).when(getRequestBuilder).execute();
		when(getResponseActionFuture.actionGet()).thenReturn(getResponse);

		doReturn(true).when(getResponse).isExists();
		doReturn(false).when(getResponse).isSourceEmpty();
		doReturn(new HashMap<String, Object>() {
			{
				put("id", "init");
				put("firstname", "luke");
			}
		}).when(getResponse).getSourceAsMap();

		when(client.prepareMultiGet()).thenReturn(multiGetRequestBuilder);
		doReturn(multiGetResponseActionFuture).when(multiGetRequestBuilder).execute();
		when(multiGetResponseActionFuture.actionGet()).thenReturn(multiGetResponse);
		doReturn(new MultiGetItemResponse[] { multiGetItemResponse, multiGetItemResponse }).when(multiGetResponse)
				.getResponses();
		doReturn(getResponse).when(multiGetItemResponse).getResponse();

		when(client.prepareSearch(anyVararg())).thenReturn(searchRequestBuilder);
		doReturn(searchRequestBuilder).when(searchRequestBuilder).setSearchType(any(SearchType.class));
		doReturn(searchRequestBuilder).when(searchRequestBuilder).setVersion(anyBoolean());
		doReturn(searchRequestBuilder).when(searchRequestBuilder).setTrackScores(anyBoolean());
		doReturn(searchRequestBuilder).when(searchRequestBuilder).setScroll(any(TimeValue.class));
		doReturn(searchResponseActionFuture).when(searchRequestBuilder).execute();
		when(searchResponseActionFuture.actionGet()).thenReturn(searchResponse);
		when(searchResponseActionFuture.actionGet(anyString())).thenReturn(searchResponse);
		doReturn(nSearchHits(2)).when(searchResponse).getHits();
		doReturn("scroll-id").when(searchResponse).getScrollId();
		doReturn(new BytesArray(new byte[8])).when(searchHit).getSourceRef();
		doReturn(new HashMap<String, Object>() {
			{
				put("id", "init");
				put("firstname", "luke");
			}
		}).when(searchHit).getSourceAsMap();

		when(client.multiSearch(any(MultiSearchRequest.class))).thenReturn(multiSearchResponseActionFuture);
		MultiSearchResponse multiSearchResponse = new MultiSearchResponse(
				new MultiSearchResponse.Item[] { multiSearchResponseItem }, 1L);
		when(multiSearchResponseActionFuture.actionGet()).thenReturn(multiSearchResponse);
		doReturn(searchResponse).when(multiSearchResponseItem).getResponse();

		when(client.prepareSearchScroll(anyString())).thenReturn(searchScrollRequestBuilder);
		doReturn(searchScrollRequestBuilder).when(searchScrollRequestBuilder).setScroll(any(TimeValue.class));
		doReturn(searchResponseActionFuture).when(searchScrollRequestBuilder).execute();
	}
}
