package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;

/**
 * AbstractElasticsearchTemplate
 *
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 */
public abstract class AbstractElasticsearchTemplate implements ElasticsearchOperations, ApplicationContextAware {

	protected ElasticsearchConverter elasticsearchConverter;
	protected RequestFactory requestFactory;
	protected IndexOperations indexOperations;

	// region Initialization
	protected void initialize(ElasticsearchConverter elasticsearchConverter, IndexOperations indexOperations) {

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null.");

		this.elasticsearchConverter = elasticsearchConverter;
		requestFactory = new RequestFactory(elasticsearchConverter);
		this.indexOperations = indexOperations;
	}

	protected ElasticsearchConverter createElasticsearchConverter() {
		MappingElasticsearchConverter mappingElasticsearchConverter = new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());
		mappingElasticsearchConverter.afterPropertiesSet();
		return mappingElasticsearchConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {

		if (elasticsearchConverter instanceof ApplicationContextAware) {
			((ApplicationContextAware) elasticsearchConverter).setApplicationContext(context);
		}
	}
	// endregion

	// region getter/setter
	@Override
	public IndexOperations getIndexOperations() {
		return indexOperations;
	}
	// endregion

	// region DocumentOperations
	public void delete(Query query, Class<?> clazz, IndexCoordinates index) {

		Assert.notNull(query, "Query must not be null.");

		SearchRequest searchRequest = requestFactory.searchRequest(query, clazz, index);
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(searchRequest.source().query());

		delete(deleteQuery, index);
	}
	// endregion

	// region SearchOperations
	@Override
	public <T> CloseableIterator<T> stream(Query query, Class<T> clazz, IndexCoordinates index) {
		long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		return StreamQueries.streamResults(startScroll(scrollTimeInMillis, query, clazz, index),
				scrollId -> continueScroll(scrollId, scrollTimeInMillis, clazz), this::searchScrollClear);
	}

	@Override
	public <T> CloseableIterator<SearchHit<T>> searchForStream(Query query, Class<T> clazz, IndexCoordinates index) {
		long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		return StreamQueries.streamResults(searchScrollStart(scrollTimeInMillis, query, clazz, index),
				scrollId -> searchScrollContinue(scrollId, scrollTimeInMillis, clazz), this::searchScrollClear);
	}

	@Override
	public <T> SearchHits<T> search(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(query.getId(), "No document id defined for MoreLikeThisQuery");

		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = requestFactory.moreLikeThisQueryBuilder(query, index);
		return search(new NativeSearchQueryBuilder().withQuery(moreLikeThisQueryBuilder).build(), clazz, index);
	}

	@Override
	public <T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz, IndexCoordinates index) {
		MultiSearchRequest request = new MultiSearchRequest();
		for (Query query : queries) {
			request.add(requestFactory.searchRequest(query, clazz, index));
		}

		MultiSearchResponse.Item[] items = getMultiSearchResult(request);

		List<SearchHits<T>> res = new ArrayList<>(queries.size());
		int c = 0;
		for (Query query : queries) {
			res.add(elasticsearchConverter.read(clazz, SearchDocumentResponse.from(items[c++].getResponse())));
		}
		return res;
	}

	@Override
	public List<SearchHits<?>> multiSearch(List<? extends Query> queries, List<Class<?>> classes,
			IndexCoordinates index) {
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
			res.add(elasticsearchConverter.read(it1.next(), SearchDocumentResponse.from(items[c++].getResponse())));
		}
		return res;
	}

	abstract protected MultiSearchResponse.Item[] getMultiSearchResult(MultiSearchRequest request);
	// endregion

	// region Helper methods
	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return elasticsearchConverter;
	}

	/**
	 * @since 4.0
	 */
	public RequestFactory getRequestFactory() {
		return requestFactory;
	}

	protected static String[] toArray(List<String> values) {
		String[] valuesAsArray = new String[values.size()];
		return values.toArray(valuesAsArray);
	}

	@Override
	public abstract SearchResponse suggest(SuggestBuilder suggestion, IndexCoordinates index);

	/**
	 * @param clazz the entity class
	 * @return the IndexCoordinates defined on the entity.
	 * @since 4.0
	 */
	@Override
	public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return getRequiredPersistentEntity(clazz).getIndexCoordinates();
	}

	protected void checkForBulkOperationFailure(BulkResponse bulkResponse) {

		if (bulkResponse.hasFailures()) {
			Map<String, String> failedDocuments = new HashMap<>();
			for (BulkItemResponse item : bulkResponse.getItems()) {

				if (item.isFailed())
					failedDocuments.put(item.getId(), item.getFailureMessage());
			}
			throw new ElasticsearchException(
					"Bulk operation has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + ']',
					failedDocuments);
		}
	}

	protected void setPersistentEntityId(Object entity, String id) {

		ElasticsearchPersistentEntity<?> persistentEntity = getRequiredPersistentEntity(entity.getClass());
		ElasticsearchPersistentProperty idProperty = persistentEntity.getIdProperty();

		// Only deal with text because ES generated Ids are strings!
		if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
			persistentEntity.getPropertyAccessor(entity).setProperty(idProperty, id);
		}
	}

	ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}
	// endregion
}
