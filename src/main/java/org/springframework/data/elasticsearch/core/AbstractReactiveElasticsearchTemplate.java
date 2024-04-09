/*
 * Copyright 2021-2024 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.elasticsearch.client.UnsupportedClientOperationException;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterLoadCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveBeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.routing.DefaultRoutingResolver;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.data.elasticsearch.support.VersionInfo;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class keeping common code for implementations of the {@link ReactiveElasticsearchOperations} interface
 * independent of the used client.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
abstract public class AbstractReactiveElasticsearchTemplate
		implements ReactiveElasticsearchOperations, ApplicationContextAware {

	protected final ElasticsearchConverter converter;
	protected final SimpleElasticsearchMappingContext mappingContext;
	protected final EntityOperations entityOperations;

	protected @Nullable RefreshPolicy refreshPolicy = RefreshPolicy.NONE;
	protected RoutingResolver routingResolver;

	protected @Nullable ReactiveEntityCallbacks entityCallbacks;

	// region Initialization
	protected AbstractReactiveElasticsearchTemplate(@Nullable ElasticsearchConverter converter) {

		this.converter = converter != null ? converter : createElasticsearchConverter();
		this.mappingContext = (SimpleElasticsearchMappingContext) this.converter.getMappingContext();
		this.entityOperations = new EntityOperations(this.mappingContext);
		this.routingResolver = new DefaultRoutingResolver(this.mappingContext);

		// initialize the VersionInfo class in the initialization phase
		// noinspection ResultOfMethodCallIgnored
		VersionInfo.versionProperties();
	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return converter;
	}

	/**
	 * @return copy of this instance.
	 */
	private AbstractReactiveElasticsearchTemplate copy() {

		AbstractReactiveElasticsearchTemplate copy = doCopy();
		copy.setRefreshPolicy(refreshPolicy);

		if (entityCallbacks != null) {
			copy.setEntityCallbacks(entityCallbacks);
		}

		copy.setRoutingResolver(routingResolver);
		return copy;
	}

	abstract protected AbstractReactiveElasticsearchTemplate doCopy();

	private ElasticsearchConverter createElasticsearchConverter() {
		MappingElasticsearchConverter mappingElasticsearchConverter = new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());
		mappingElasticsearchConverter.afterPropertiesSet();
		return mappingElasticsearchConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		if (entityCallbacks == null) {
			setEntityCallbacks(ReactiveEntityCallbacks.create(applicationContext));
		}
	}

	/**
	 * Set the default {@link RefreshPolicy} to apply when writing to Elasticsearch.
	 *
	 * @param refreshPolicy can be {@literal null}.
	 */
	public void setRefreshPolicy(@Nullable RefreshPolicy refreshPolicy) {
		this.refreshPolicy = refreshPolicy;
	}

	/**
	 * @return the current {@link RefreshPolicy}.
	 */

	@Nullable
	public RefreshPolicy getRefreshPolicy() {
		return refreshPolicy;
	}

	/**
	 * Set the {@link ReactiveEntityCallbacks} instance to use when invoking {@link ReactiveEntityCallbacks callbacks}
	 * like the {@link ReactiveBeforeConvertCallback}. Overrides potentially existing {@link ReactiveEntityCallbacks}.
	 *
	 * @param entityCallbacks must not be {@literal null}.
	 * @throws IllegalArgumentException if the given instance is {@literal null}.
	 * @since 4.0
	 */
	public void setEntityCallbacks(ReactiveEntityCallbacks entityCallbacks) {

		Assert.notNull(entityCallbacks, "EntityCallbacks must not be null!");

		this.entityCallbacks = entityCallbacks;
	}

	/**
	 * logs the versions of the different Elasticsearch components.
	 *
	 * @return a Mono signalling finished execution
	 * @since 4.3
	 */
	@SuppressWarnings("unused")
	public Mono<Void> logVersions() {

		return getVendor()
				.zipWith(getRuntimeLibraryVersion())
				.zipWith(getClusterVersion())
				.doOnNext(objects -> VersionInfo.logVersions(objects.getT1().getT1(), objects.getT1().getT2(), objects.getT2()))
				.then();
	}

	// endregion

	// region customizations
	private void setRoutingResolver(RoutingResolver routingResolver) {

		Assert.notNull(routingResolver, "routingResolver must not be null");

		this.routingResolver = routingResolver;
	}

	@Override
	public ReactiveElasticsearchOperations withRouting(RoutingResolver routingResolver) {

		Assert.notNull(routingResolver, "routingResolver must not be null");

		AbstractReactiveElasticsearchTemplate copy = copy();
		copy.setRoutingResolver(routingResolver);
		return copy;
	}

	@Override
	public ReactiveElasticsearchOperations withRefreshPolicy(@Nullable RefreshPolicy refreshPolicy) {
		AbstractReactiveElasticsearchTemplate copy = copy();
		copy.setRefreshPolicy(refreshPolicy);
		return copy;
	}

	// endregion

	// region DocumentOperations
	@Override
	public <T> Mono<T> save(T entity) {
		return save(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public <T> Flux<T> save(Flux<T> entities, Class<?> clazz, int bulkSize) {
		return save(entities, getIndexCoordinatesFor(clazz), bulkSize);
	}

	@Override
	public <T> Flux<T> save(Flux<T> entities, IndexCoordinates index, int bulkSize) {

		Assert.notNull(entities, "entities must not be null");
		Assert.notNull(index, "index must not be null");
		Assert.isTrue(bulkSize > 0, "bulkSize must be greater than 0");

		return Flux.defer(() -> {
			Sinks.Many<T> sink = Sinks.many().unicast().onBackpressureBuffer();
			// noinspection ReactiveStreamsSubscriberImplementation
			entities
					.bufferTimeout(bulkSize, Duration.ofMillis(200), true)
					.subscribe(new Subscriber<>() {
						@Nullable private Subscription subscription = null;
						private final AtomicBoolean upstreamComplete = new AtomicBoolean(false);

						@Override
						public void onSubscribe(Subscription subscription) {
							this.subscription = subscription;
							subscription.request(1);
						}

						@Override
						public void onNext(List<T> entityList) {
							saveAll(entityList, index)
									.map(sink::tryEmitNext)
									.doOnComplete(() -> {
										if (!upstreamComplete.get()) {
											if (subscription == null) {
												throw new IllegalStateException("no subscription");
											}
											subscription.request(1);
										} else {
											sink.tryEmitComplete();
										}
									}).subscribe();
						}

						@Override
						public void onError(Throwable throwable) {
							if (subscription != null) {
								subscription.cancel();
							}
							sink.tryEmitError(throwable);
						}

						@Override
						public void onComplete() {
							upstreamComplete.set(true);
						}
					});
			return sink.asFlux();
		});

	}

	@Override
	public <T> Flux<T> saveAll(Mono<? extends Collection<? extends T>> entities, Class<T> clazz) {
		return saveAll(entities, getIndexCoordinatesFor(clazz));
	}

	protected IndexQuery getIndexQuery(Object value) {
		EntityOperations.AdaptableEntity<?> entity = entityOperations.forEntity(value, converter.getConversionService(),
				routingResolver);

		Object id = entity.getId();
		IndexQuery query = new IndexQuery();

		if (id != null) {
			query.setId(id.toString());
		}
		query.setObject(value);

		boolean usingSeqNo = false;

		if (entity.hasSeqNoPrimaryTerm()) {
			SeqNoPrimaryTerm seqNoPrimaryTerm = entity.getSeqNoPrimaryTerm();

			if (seqNoPrimaryTerm != null) {
				query.setSeqNo(seqNoPrimaryTerm.sequenceNumber());
				query.setPrimaryTerm(seqNoPrimaryTerm.primaryTerm());
				usingSeqNo = true;
			}
		}

		// seq_no and version are incompatible in the same request
		if (!usingSeqNo && entity.isVersionedEntity()) {

			Number version = entity.getVersion();

			if (version != null) {
				query.setVersion(version.longValue());
			}
		}

		query.setRouting(entity.getRouting());

		return query;
	}

	@Override
	public <T> Flux<MultiGetItem<T>> multiGet(Query query, Class<T> clazz) {
		return multiGet(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public Mono<Boolean> exists(String id, Class<?> entityType) {
		return doExists(id, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Boolean> exists(String id, IndexCoordinates index) {
		return doExists(id, index);
	}

	@Override
	public <T> Mono<T> save(T entity, IndexCoordinates index) {

		Assert.notNull(entity, "Entity must not be null!");
		Assert.notNull(index, "index must not be null");

		return maybeCallbackBeforeConvert(entity, index)
				.flatMap(entityAfterBeforeConversionCallback -> doIndex(entityAfterBeforeConversionCallback, index))
				.map(it -> {
					T savedEntity = it.getT1();
					IndexResponseMetaData indexResponseMetaData = it.getT2();
					return entityOperations.updateIndexedObject(
							savedEntity,
							new IndexedObjectInformation(
									indexResponseMetaData.id(),
									indexResponseMetaData.index(),
									indexResponseMetaData.seqNo(),
									indexResponseMetaData.primaryTerm(),
									indexResponseMetaData.version()),
							converter,
							routingResolver);
				}).flatMap(saved -> maybeCallbackAfterSave(saved, index));
	}

	abstract protected <T> Mono<Tuple2<T, IndexResponseMetaData>> doIndex(T entity, IndexCoordinates index);

	abstract protected Mono<Boolean> doExists(String id, IndexCoordinates index);

	@Override
	public <T> Mono<T> get(String id, Class<T> entityType) {
		return get(id, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<String> delete(Object entity, IndexCoordinates index) {

		EntityOperations.AdaptableEntity<?> elasticsearchEntity = entityOperations.forEntity(entity,
				converter.getConversionService(), routingResolver);

		if (elasticsearchEntity.getId() == null) {
			return Mono.error(new IllegalArgumentException("entity must have an id"));
		}

		return Mono.defer(() -> {
			String id = converter.convertId(elasticsearchEntity.getId());
			String routing = elasticsearchEntity.getRouting();
			return doDeleteById(id, routing, index);
		});
	}

	@Override
	public Mono<String> delete(Object entity) {
		return delete(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public Mono<String> delete(String id, Class<?> entityType) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(entityType, "entityType must not be null");

		return delete(id, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<String> delete(String id, IndexCoordinates index) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		return doDeleteById(id, routingResolver.getRouting(), index);
	}

	abstract protected Mono<String> doDeleteById(String id, @Nullable String routing, IndexCoordinates index);

	@Override
	@Deprecated
	public Mono<ByQueryResponse> delete(Query query, Class<?> entityType) {
		return delete(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<ByQueryResponse> delete(DeleteQuery query, Class<?> entityType) {
		return delete(query, entityType, getIndexCoordinatesFor(entityType));
	}
	// endregion

	// region SearchDocument
	@Override
	public <T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> resultType, IndexCoordinates index) {
		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>(resultType, index);
		return doFind(query, entityType, index).concatMap(callback::toSearchHit);
	}

	@Override
	public <T> Flux<SearchHit<T>> search(Query query, Class<?> entityType, Class<T> returnType) {
		return search(query, entityType, returnType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public <T> Mono<SearchPage<T>> searchForPage(Query query, Class<?> entityType, Class<T> resultType) {
		return searchForPage(query, entityType, resultType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public <T> Mono<SearchPage<T>> searchForPage(Query query, Class<?> entityType, Class<T> resultType,
			IndexCoordinates index) {

		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>(resultType, index);

		return doFindForResponse(query, entityType, index)
				.flatMap(searchDocumentResponse -> Flux.fromIterable(searchDocumentResponse.getSearchDocuments())
						.flatMap(callback::toEntity)
						.collectList()
						.map(entities -> SearchHitMapping.mappingFor(resultType, converter)
								.mapHits(searchDocumentResponse, entities)))
				.map(searchHits -> SearchHitSupport.searchPageFor(searchHits, query.getPageable()));
	}

	@Override
	public <T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<?> entityType, Class<T> resultType) {
		return searchForHits(query, entityType, resultType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public <T> Mono<ReactiveSearchHits<T>> searchForHits(Query query, Class<?> entityType, Class<T> resultType,
			IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(entityType, "entityType must not be null");
		Assert.notNull(resultType, "resultType must not be null");
		Assert.notNull(index, "index must not be null");

		SearchDocumentCallback<T> callback = new ReadSearchDocumentCallback<>(resultType, index);

		return doFindForResponse(query, entityType, index)
				.flatMap(searchDocumentResponse -> Flux.fromIterable(searchDocumentResponse.getSearchDocuments())
						.flatMap(callback::toEntity)
						.collectList()
						.map(entities -> SearchHitMapping.mappingFor(resultType, converter)
								.mapHits(searchDocumentResponse, entities)))
				.map(ReactiveSearchHitSupport::searchHitsFor);
	}

	abstract protected Flux<SearchDocument> doFind(Query query, Class<?> clazz, IndexCoordinates index);

	@SuppressWarnings("unused")
	abstract protected <T> Mono<SearchDocumentResponse> doFindForResponse(Query query, Class<?> clazz,
			IndexCoordinates index);

	@Override
	public Flux<? extends AggregationContainer<?>> aggregate(Query query, Class<?> entityType) {
		return aggregate(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Suggest> suggest(Query query, Class<?> entityType) {
		return suggest(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Suggest> suggest(Query query, Class<?> entityType, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(entityType, "entityType must not be null");
		Assert.notNull(index, "index must not be null");

		return doFindForResponse(query, entityType, index).mapNotNull(searchDocumentResponse -> {
			Suggest suggest = searchDocumentResponse.getSuggest();
			SearchHitMapping.mappingFor(entityType, converter).mapHitsInCompletionSuggestion(suggest);
			return suggest;
		});
	}

	@Override
	public Mono<Long> count(Query query, Class<?> entityType) {
		return count(query, entityType, getIndexCoordinatesFor(entityType));
	}

	@Override
	public Mono<Long> count(Query query, Class<?> entityType, IndexCoordinates index) {
		return doCount(query, entityType, index);
	}

	abstract protected Mono<Long> doCount(Query query, Class<?> entityType, IndexCoordinates index);

	@Override
	public Mono<String> openPointInTime(IndexCoordinates index, Duration keepAlive, Boolean ignoreUnavailable) {
		throw new UnsupportedClientOperationException(getClass(), "openPointInTime");
	}

	@Override
	public Mono<Boolean> closePointInTime(String pit) {
		throw new UnsupportedClientOperationException(getClass(), "closePointInTime");
	}

	// endregion

	// region callbacks

	protected <T> Mono<T> maybeCallbackBeforeConvert(T entity, IndexCoordinates index) {

		if (null != entityCallbacks) {
			return entityCallbacks.callback(ReactiveBeforeConvertCallback.class, entity, index);
		}

		return Mono.just(entity);
	}

	protected <T> Mono<T> maybeCallbackAfterSave(T entity, IndexCoordinates index) {

		if (null != entityCallbacks) {
			return entityCallbacks.callback(ReactiveAfterSaveCallback.class, entity, index);
		}

		return Mono.just(entity);
	}

	protected <T> Mono<T> maybeCallbackAfterConvert(T entity, Document document, IndexCoordinates index) {

		if (null != entityCallbacks) {
			return entityCallbacks.callback(ReactiveAfterConvertCallback.class, entity, document, index);
		}

		return Mono.just(entity);
	}

	protected <T> Mono<Document> maybeCallbackAfterLoad(Document document, Class<T> type, IndexCoordinates index) {
		if (entityCallbacks != null) {
			return entityCallbacks.callback(ReactiveAfterLoadCallback.class, document, type, index);
		}

		return Mono.just(document);
	}

	/**
	 * Callback to convert {@link Document} into an entity of type T
	 *
	 * @param <T> the entity type
	 */
	protected interface DocumentCallback<T> {

		/**
		 * Convert a document into an entity
		 *
		 * @param document the document to convert
		 * @return a Mono of the entity
		 */
		Mono<T> toEntity(@Nullable Document document);
	}

	protected class ReadDocumentCallback<T> implements DocumentCallback<T> {
		private final EntityReader<? super T, Document> reader;
		private final Class<T> type;
		private final IndexCoordinates index;

		public ReadDocumentCallback(EntityReader<? super T, Document> reader, Class<T> type, IndexCoordinates index) {
			Assert.notNull(reader, "reader is null");
			Assert.notNull(type, "type is null");

			this.reader = reader;
			this.type = type;
			this.index = index;
		}

		public Mono<T> toEntity(@Nullable Document document) {
			if (document == null) {
				return Mono.empty();
			}

			return maybeCallbackAfterLoad(document, type, index)
					.flatMap(documentAfterLoad -> {
						// noinspection DuplicatedCode
						T entity = reader.read(type, documentAfterLoad);
						IndexedObjectInformation indexedObjectInformation = new IndexedObjectInformation(
								documentAfterLoad.hasId() ? documentAfterLoad.getId() : null,
								documentAfterLoad.getIndex(),
								documentAfterLoad.hasSeqNo() ? documentAfterLoad.getSeqNo() : null,
								documentAfterLoad.hasPrimaryTerm() ? documentAfterLoad.getPrimaryTerm() : null,
								documentAfterLoad.hasVersion() ? documentAfterLoad.getVersion() : null);
						entity = entityOperations.updateIndexedObject(
								entity,
								indexedObjectInformation,
								converter,
								routingResolver);

						return maybeCallbackAfterConvert(entity, documentAfterLoad, index);
					});
		}
	}

	/**
	 * Callback to convert a {@link SearchDocument} into different other classes
	 *
	 * @param <T> the entity type
	 */
	protected interface SearchDocumentCallback<T> {

		/**
		 * converts a {@link SearchDocument} to an entity
		 *
		 * @param searchDocument the document to convert
		 * @return the entity in a MOno
		 */
		Mono<T> toEntity(SearchDocument searchDocument);

		/**
		 * converts a {@link SearchDocument} into a SearchHit
		 *
		 * @param searchDocument the document to convert
		 * @return the converted SearchHit
		 */
		Mono<SearchHit<T>> toSearchHit(SearchDocument searchDocument);
	}

	protected class ReadSearchDocumentCallback<T> implements SearchDocumentCallback<T> {
		private final DocumentCallback<T> delegate;
		private final Class<T> type;

		public ReadSearchDocumentCallback(Class<T> type, IndexCoordinates index) {
			Assert.notNull(type, "type is null");

			this.delegate = new ReadDocumentCallback<>(converter, type, index);
			this.type = type;
		}

		@Override
		public Mono<T> toEntity(SearchDocument response) {
			return delegate.toEntity(response);
		}

		@Override
		public Mono<SearchHit<T>> toSearchHit(SearchDocument response) {
			return toEntity(response).map(entity -> SearchHitMapping.mappingFor(type, converter).mapHit(response, entity));
		}
	}

	// endregion

	// region script operations
	@Override
	public Mono<Boolean> putScript(Script script) {
		throw new UnsupportedOperationException(
				"putScript() operation not implemented by " + getClass().getCanonicalName());
	}

	@Override
	public Mono<Script> getScript(String name) {
		throw new UnsupportedOperationException(
				"getScript() operation not implemented by " + getClass().getCanonicalName());
	}

	@Override
	public Mono<Boolean> deleteScript(String name) {
		throw new UnsupportedOperationException(
				"deleteScript() operation not implemented by " + getClass().getCanonicalName());
	}
	// endregion

	// region Helper methods
	@Override
	public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(clazz);

		Assert.notNull(persistentEntity, "could not get indexCoordinates for class " + clazz.getName());

		return persistentEntity.getIndexCoordinates();
	}

	@Override
	@Nullable
	public ElasticsearchPersistentEntity<?> getPersistentEntityFor(@Nullable Class<?> type) {
		return type != null ? mappingContext.getPersistentEntity(type) : null;
	}

	/**
	 * @return the vendor name of the used cluster and client library
	 * @since 4.3
	 */
	public abstract Mono<String> getVendor();

	/**
	 * @return the version of the used client runtime library.
	 * @since 4.3
	 */
	public abstract Mono<String> getRuntimeLibraryVersion();

	public abstract Mono<String> getClusterVersion();

	@Nullable
	public String getEntityRouting(Object entity) {
		return entityOperations.forEntity(entity, converter.getConversionService(), routingResolver)
				.getRouting();
	}

	/**
	 * Value class to capture client independent information from a response to an index request.
	 */
	public record IndexResponseMetaData(String id, String index, long seqNo, long primaryTerm, long version) {
	}
	// endregion

	protected class Entities<T> {
		private final List<T> entities;

		public Entities(List<T> entities) {

			Assert.notNull(entities, "entities cannot be null");

			this.entities = entities;
		}

		public boolean isEmpty() {
			return entities.isEmpty();
		}

		public List<IndexQuery> indexQueries() {
			return entities.stream().map(AbstractReactiveElasticsearchTemplate.this::getIndexQuery)
					.collect(Collectors.toList());
		}

		public T entityAt(long index) {
			// it's safe to cast to int because the original indexed collection was fitting in memory
			int intIndex = (int) index;
			return entities.get(intIndex);
		}
	}

}
