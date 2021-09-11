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
package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.Version;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.util.Assert;

/**
 * This class contains methods that are common the implementations derived from {@link AbstractElasticsearchTemplate}
 * using either the {@link org.elasticsearch.client.transport.TransportClient} or the
 * {@link org.elasticsearch.client.RestHighLevelClient} and that use Elasticsearch specific libraries.
 * <p>
 * <strong>Note:</strong> Although this class is public, it is not considered to be part of the official Spring Data
 * Elasticsearch API and so might change at any time.
 *
 * @author Peter-Josef Meisch
 */
public abstract class AbstractElasticsearchRestTransportTemplate extends AbstractElasticsearchTemplate {

	// region DocumentOperations
	/**
	 * @param bulkResponse
	 * @return the list of the item id's
	 */
	protected List<IndexedObjectInformation> checkForBulkOperationFailure(BulkResponse bulkResponse) {

		if (bulkResponse.hasFailures()) {
			Map<String, String> failedDocuments = new HashMap<>();
			for (BulkItemResponse item : bulkResponse.getItems()) {

				if (item.isFailed())
					failedDocuments.put(item.getId(), item.getFailureMessage());
			}
			throw new BulkFailureException(
					"Bulk operation has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + ']',
					failedDocuments);
		}

		return Stream.of(bulkResponse.getItems()).map(bulkItemResponse -> {
			DocWriteResponse response = bulkItemResponse.getResponse();
			if (response != null) {
				return IndexedObjectInformation.of(response.getId(), response.getSeqNo(), response.getPrimaryTerm(),
						response.getVersion());
			} else {
				return IndexedObjectInformation.of(bulkItemResponse.getId(), null, null, null);
			}

		}).collect(Collectors.toList());
	}

	// endregion

	// region SearchOperations
	protected <T> SearchHits<T> doSearch(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index) {
		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = requestFactory.moreLikeThisQueryBuilder(query, index);
		return search(
				new NativeSearchQueryBuilder().withQuery(moreLikeThisQueryBuilder).withPageable(query.getPageable()).build(),
				clazz, index);
	}

	@Override
	public <T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index) {
		MultiSearchRequest request = new MultiSearchRequest();
		for (Query query : queries) {
			request.add(requestFactory.searchRequest(query, clazz, index));
		}

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);

		ReadDocumentCallback<T> documentCallback = new ReadDocumentCallback<T>(elasticsearchConverter, clazz, index);
		SearchDocumentResponseCallback<SearchHits<T>> callback = new ReadSearchDocumentResponseCallback<>(clazz, index);
		List<SearchHits<T>> res = new ArrayList<>(queries.size());
		int c = 0;
		for (Query query : queries) {
			res.add(callback.doWith(SearchDocumentResponse.from(items[c++].getResponse(), documentCallback::doWith)));
		}
		return res;
	}

	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(classes, "classes must not be null");
		Assert.isTrue(queries.size() == classes.size(), "queries and classes must have the same size");

		MultiSearchRequest request = new MultiSearchRequest();
		Iterator<Class<?>> it = classes.iterator();
		for (Query query : queries) {
			Class<?> clazz = it.next();
			request.add(requestFactory.searchRequest(query, clazz, getIndexCoordinatesFor(clazz)));
		}

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);

		List<SearchHits<?>> res = new ArrayList<>(queries.size());
		int c = 0;
		Iterator<Class<?>> it1 = classes.iterator();
		for (Query query : queries) {
			Class entityClass = it1.next();

			IndexCoordinates index = getIndexCoordinatesFor(entityClass);
			ReadDocumentCallback<?> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, entityClass, index);
			SearchDocumentResponseCallback<SearchHits<?>> callback = new ReadSearchDocumentResponseCallback<>(entityClass,
					index);

			SearchResponse response = items[c++].getResponse();
			res.add(callback.doWith(SearchDocumentResponse.from(response, documentCallback::doWith)));
		}
		return res;
	}

	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes,
			IndexCoordinates index) {

		Assert.notNull(queries, "queries must not be null");
		Assert.notNull(classes, "classes must not be null");
		Assert.notNull(index, "index must not be null");
		Assert.isTrue(queries.size() == classes.size(), "queries and classes must have the same size");

		MultiSearchRequest request = new MultiSearchRequest();
		Iterator<Class<?>> it = classes.iterator();
		for (Query query : queries) {
			request.add(requestFactory.searchRequest(query, it.next(), index));
		}

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);

		List<SearchHits<?>> res = new ArrayList<>(queries.size());
		int c = 0;
		Iterator<Class<?>> it1 = classes.iterator();
		for (Query query : queries) {
			Class entityClass = it1.next();

			ReadDocumentCallback<?> documentCallback = new ReadDocumentCallback<>(elasticsearchConverter, entityClass, index);
			SearchDocumentResponseCallback<SearchHits<?>> callback = new ReadSearchDocumentResponseCallback<>(entityClass,
					index);

			SearchResponse response = items[c++].getResponse();
			res.add(callback.doWith(SearchDocumentResponse.from(response, documentCallback::doWith)));
		}
		return res;
	}

	abstract protected MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request);

	// endregion

	// region helper
	@Override
	public Query matchAllQuery() {
		return new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).build();
	}

	@Override
	public Query idsQuery(List<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		return new NativeSearchQueryBuilder().withQuery(QueryBuilders.idsQuery().addIds(ids.toArray(new String[] {})))
				.build();
	}

	@Override
	protected String getVendor() {
		return "Elasticsearch";
	}

	@Override
	protected String getRuntimeLibraryVersion() {
		return Version.CURRENT.toString();
	}

	@Override
	@Deprecated
	public SearchResponse suggest(SuggestBuilder suggestion, Class<?> clazz) {
		return suggest(suggestion, getIndexCoordinatesFor(clazz));
	}

	// endregion
}
