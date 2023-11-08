/*
 * Copyright 2018-2023 the original author or authors.
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

import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Common operations performed on an entity in the context of it's mapping metadata.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @since 3.2
 */
public class EntityOperations {

	private static final String ID_FIELD = "id";

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context;

	public EntityOperations(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context) {

		Assert.notNull(context, "context must not be null");

		this.context = context;
	}

	/**
	 * Creates a new {@link Entity} for the given bean.
	 *
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T> Entity<T> forEntity(T entity) {

		Assert.notNull(entity, "Bean must not be null!");

		if (entity instanceof Map) {
			return new SimpleMappedEntity((Map<String, Object>) entity);
		}

		return MappedEntity.of(entity, context);
	}

	/**
	 * Creates a new {@link AdaptableEntity} for the given bean and {@link ConversionService} and {@link RoutingResolver}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 * @param routingResolver the {@link RoutingResolver}, must not be {@literal null}
	 * @return the {@link AdaptableEntity}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> AdaptableEntity<T> forEntity(T entity, ConversionService conversionService,
			RoutingResolver routingResolver) {

		Assert.notNull(entity, "Bean must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		if (entity instanceof Map) {
			return new SimpleMappedEntity((Map<String, Object>) entity);
		}

		return AdaptableMappedEntity.of(entity, context, conversionService, routingResolver);
	}

	/**
	 * Updates an entity after it is stored in Elasticsearch with additional data like id, version, seqno...
	 *
	 * @param <T> the entity class
	 * @param entity the entity to update
	 * @param indexedObjectInformation the update information
	 * @param elasticsearchConverter the converter providing necessary mapping information
	 * @param routingResolver routing resolver to use
	 * @return
	 */
	public <T> T updateIndexedObject(T entity,
			IndexedObjectInformation indexedObjectInformation,
			ElasticsearchConverter elasticsearchConverter,
			RoutingResolver routingResolver) {

		Assert.notNull(entity, "entity must not be null");
		Assert.notNull(indexedObjectInformation, "indexedObjectInformation must not be null");
		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null");

		ElasticsearchPersistentEntity<?> persistentEntity = elasticsearchConverter.getMappingContext()
				.getPersistentEntity(entity.getClass());

		if (persistentEntity != null) {
			PersistentPropertyAccessor<Object> propertyAccessor = persistentEntity.getPropertyAccessor(entity);
			ElasticsearchPersistentProperty idProperty = persistentEntity.getIdProperty();

			// Only deal with text because ES generated Ids are strings!
			if (indexedObjectInformation.id() != null && idProperty != null
					// isReadable from the base class is false in case of records
					&& (idProperty.isReadable() || idProperty.getOwner().getType().isRecord())
					&& idProperty.getType().isAssignableFrom(String.class)) {
				propertyAccessor.setProperty(idProperty, indexedObjectInformation.id());
			}

			if (indexedObjectInformation.seqNo() != null && indexedObjectInformation.primaryTerm() != null
					&& persistentEntity.hasSeqNoPrimaryTermProperty()) {
				ElasticsearchPersistentProperty seqNoPrimaryTermProperty = persistentEntity.getSeqNoPrimaryTermProperty();
				// noinspection ConstantConditions
				propertyAccessor.setProperty(seqNoPrimaryTermProperty,
						new SeqNoPrimaryTerm(indexedObjectInformation.seqNo(), indexedObjectInformation.primaryTerm()));
			}

			if (indexedObjectInformation.version() != null && persistentEntity.hasVersionProperty()) {
				ElasticsearchPersistentProperty versionProperty = persistentEntity.getVersionProperty();
				// noinspection ConstantConditions
				propertyAccessor.setProperty(versionProperty, indexedObjectInformation.version());
			}

			var indexedIndexNameProperty = persistentEntity.getIndexedIndexNameProperty();
			if (indexedIndexNameProperty != null) {
				propertyAccessor.setProperty(indexedIndexNameProperty, indexedObjectInformation.index());
			}

			// noinspection unchecked
			return (T) propertyAccessor.getBean();
		} else {
			EntityOperations.AdaptableEntity<T> adaptableEntity = forEntity(entity,
					elasticsearchConverter.getConversionService(), routingResolver);
			adaptableEntity.populateIdIfNecessary(indexedObjectInformation.id());
		}
		return entity;
	}

	/**
	 * Determine index name and type name from {@link Entity} with {@code index} and {@code type} overrides. Allows using
	 * preferred values for index and type if provided, otherwise fall back to index and type defined on entity level.
	 *
	 * @param entity the entity to determine the index name. Can be {@literal null} if {@code index} and {@literal type}
	 *          are provided.
	 * @param index index name override can be {@literal null}.
	 * @return the {@link IndexCoordinates} containing index name and index type.
	 */
	IndexCoordinates determineIndex(Entity<?> entity, @Nullable String index) {
		return determineIndex(entity.getPersistentEntity(), index);
	}

	/**
	 * Determine index name and type name from {@link ElasticsearchPersistentEntity} with {@code index} and {@code type}
	 * overrides. Allows using preferred values for index and type if provided, otherwise fall back to index and type
	 * defined on entity level.
	 *
	 * @param persistentEntity the entity to determine the index name. Can be {@literal null} if {@code index} and
	 *          {@literal type} are provided.
	 * @param index index name override can be {@literal null}.
	 * @return the {@link IndexCoordinates} containing index name and index type.
	 * @since 4.1
	 */
	IndexCoordinates determineIndex(ElasticsearchPersistentEntity<?> persistentEntity, @Nullable String index) {
		return index != null ? IndexCoordinates.of(index) : persistentEntity.getIndexCoordinates();
	}

	/**
	 * A representation of information about an entity.
	 *
	 * @author Christoph Strobl
	 */
	interface Entity<T> {

		/**
		 * Returns the identifier of the entity.
		 *
		 * @return the ID value, can be {@literal null}.
		 */
		@Nullable
		Object getId();

		/**
		 * Returns whether the entity is versioned, i.e. if it contains a version property.
		 *
		 * @return
		 */
		default boolean isVersionedEntity() {
			return false;
		}

		/**
		 * Returns the value of the version if the entity has a version property, {@literal null} otherwise.
		 *
		 * @return
		 */
		@Nullable
		Object getVersion();

		/**
		 * Returns the underlying bean.
		 *
		 * @return
		 */
		T getBean();

		/**
		 * Returns whether the entity is considered to be new.
		 *
		 * @return
		 */
		boolean isNew();

		/**
		 * Returns the {@link ElasticsearchPersistentEntity} associated with this entity.
		 *
		 * @return can be {@literal null} if this entity is not mapped.
		 */
		@Nullable
		ElasticsearchPersistentEntity<?> getPersistentEntity();

		/**
		 * Returns the required {@link ElasticsearchPersistentEntity}.
		 *
		 * @return
		 * @throws IllegalStateException if no {@link ElasticsearchPersistentEntity} is associated with this entity.
		 */
		default ElasticsearchPersistentEntity<?> getRequiredPersistentEntity() {

			ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity();
			if (persistentEntity == null) {
				throw new IllegalStateException("No ElasticsearchPersistentEntity available for this entity!");
			}

			return persistentEntity;
		}
	}

	/**
	 * Information and commands on an entity.
	 *
	 * @author Mark Paluch
	 * @author Christoph Strobl
	 */
	public interface AdaptableEntity<T> extends Entity<T> {

		/**
		 * Populates the identifier of the backing entity if it has an identifier property and there's no identifier
		 * currently present.
		 *
		 * @param id can be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		T populateIdIfNecessary(@Nullable Object id);

		/**
		 * Initializes the version property of the of the current entity if available.
		 *
		 * @return the entity with the version property updated if available.
		 */
		T initializeVersionProperty();

		/**
		 * Increments the value of the version property if available.
		 *
		 * @return the entity with the version property incremented if available.
		 */
		T incrementVersion();

		/**
		 * Returns the current version value if the entity has a version property.
		 *
		 * @return the current version or {@literal null} in case it's uninitialized or the entity doesn't expose a version
		 *         property.
		 */
		@Override
		@Nullable
		Number getVersion();

		/**
		 * Returns whether there is a property with type SeqNoPrimaryTerm in this entity.
		 *
		 * @return true if there is SeqNoPrimaryTerm property
		 * @since 4.0
		 */
		boolean hasSeqNoPrimaryTerm();

		/**
		 * Returns SeqNoPropertyTerm for this entity.
		 *
		 * @return SeqNoPrimaryTerm, may be {@literal null}
		 * @since 4.0
		 */
		@Nullable
		SeqNoPrimaryTerm getSeqNoPrimaryTerm();

		/**
		 * returns the routing for the entity if it is available
		 *
		 * @return routing if available
		 * @since 4.1
		 */
		@Nullable
		String getRouting();
	}

	/**
	 * @param <T>
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	private static class MapBackedEntity<T extends Map<String, Object>> implements AdaptableEntity<T> {

		public MapBackedEntity(T map) {

			Assert.notNull(map, "map must not be null");

			this.map = map;
		}

		private final T map;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getId()
		 */
		@Override
		public Object getId() {
			return map.get(ID_FIELD);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#populateIdIfNecessary(java.lang.Object)
		 */
		@Nullable
		@Override
		public T populateIdIfNecessary(@Nullable Object id) {

			map.put(ID_FIELD, id);

			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#initializeVersionProperty()
		 */
		@Override
		public T initializeVersionProperty() {
			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#getVersion()
		 */
		@Override
		@Nullable
		public Number getVersion() {
			return null;
		}

		@Override
		public boolean hasSeqNoPrimaryTerm() {
			return false;
		}

		@Override
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptableEntity#incrementVersion()
		 */
		@Override
		public T incrementVersion() {
			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getBean()
		 */
		@Override
		public T getBean() {
			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#isNew()
		 */
		@Override
		public boolean isNew() {
			return map.get(ID_FIELD) != null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getPersistentEntity()
		 */
		@Override
		public ElasticsearchPersistentEntity<?> getPersistentEntity() {
			return null;
		}

		@Override
		public String getRouting() {
			return null;
		}
	}

	/**
	 * Plain entity without applying further mapping.
	 *
	 * @param <T>
	 * @since 3.2
	 */
	private static class UnmappedEntity<T extends Map<String, Object>> extends MapBackedEntity<T> {

		UnmappedEntity(T map) {
			super(map);
		}
	}

	/**
	 * Simple mapped entity without an associated {@link ElasticsearchPersistentEntity}.
	 *
	 * @param <T>
	 * @since 3.2
	 */
	private static class SimpleMappedEntity<T extends Map<String, Object>> extends MapBackedEntity<T> {

		SimpleMappedEntity(T map) {
			super(map);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.UnmappedEntity#getId()
		 */
		@Override
		public Object getId() {
			return getBean().get(ID_FIELD);
		}
	}

	/**
	 * Mapped entity with an associated {@link ElasticsearchPersistentEntity}.
	 *
	 * @param <T>
	 * @since 3.2
	 */
	private static class MappedEntity<T> implements Entity<T> {

		private final ElasticsearchPersistentEntity<?> entity;
		private final IdentifierAccessor idAccessor;
		private final PersistentPropertyAccessor<T> propertyAccessor;

		private MappedEntity(ElasticsearchPersistentEntity<?> entity, IdentifierAccessor idAccessor,
				PersistentPropertyAccessor<T> propertyAccessor) {

			Assert.notNull(entity, "entity must not ne null");
			Assert.notNull(idAccessor, "idAccessor must not ne null");
			Assert.notNull(propertyAccessor, "propertyAccessor must not ne null");

			this.entity = entity;
			this.idAccessor = idAccessor;
			this.propertyAccessor = propertyAccessor;
		}

		private static <T> MappedEntity<T> of(T bean,
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context) {

			ElasticsearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
			IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
			PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

			return new MappedEntity<>(entity, identifierAccessor, propertyAccessor);
		}

		@Override
		public Object getId() {
			return idAccessor.getIdentifier();
		}

		@Override
		public boolean isVersionedEntity() {
			return entity.hasVersionProperty();
		}

		@Override
		@Nullable
		public Object getVersion() {
			return propertyAccessor.getProperty(entity.getVersionProperty());
		}

		@Override
		public T getBean() {
			return propertyAccessor.getBean();
		}

		@Override
		public boolean isNew() {
			return entity.isNew(propertyAccessor.getBean());
		}

		@Override
		public ElasticsearchPersistentEntity<?> getPersistentEntity() {
			return entity;
		}
	}

	/**
	 * @param <T>
	 * @since 3.2
	 */
	private static class AdaptableMappedEntity<T> extends MappedEntity<T> implements AdaptableEntity<T> {

		private final ElasticsearchPersistentEntity<?> entity;
		private final ConvertingPropertyAccessor<T> propertyAccessor;
		private final IdentifierAccessor identifierAccessor;
		private final ConversionService conversionService;
		private final RoutingResolver routingResolver;

		private AdaptableMappedEntity(T bean, ElasticsearchPersistentEntity<?> entity,
				IdentifierAccessor identifierAccessor, ConvertingPropertyAccessor<T> propertyAccessor,
				ConversionService conversionService, RoutingResolver routingResolver) {

			super(entity, identifierAccessor, propertyAccessor);

			this.entity = entity;
			this.propertyAccessor = propertyAccessor;
			this.identifierAccessor = identifierAccessor;
			this.conversionService = conversionService;
			this.routingResolver = routingResolver;
		}

		static <T> AdaptableEntity<T> of(T bean,
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context,
				ConversionService conversionService, RoutingResolver routingResolver) {

			ElasticsearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
			IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
			PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

			return new AdaptableMappedEntity<>(bean, entity, identifierAccessor,
					new ConvertingPropertyAccessor<>(propertyAccessor, conversionService), conversionService, routingResolver);
		}

		@Override
		public T getBean() {
			return propertyAccessor.getBean();
		}

		@Nullable
		@Override
		public T populateIdIfNecessary(@Nullable Object id) {

			if (id == null) {
				return null;
			}

			T bean = propertyAccessor.getBean();
			ElasticsearchPersistentProperty idProperty = entity.getIdProperty();

			if (idProperty == null) {
				return bean;
			}

			if (identifierAccessor.getIdentifier() != null) {
				return bean;
			}

			propertyAccessor.setProperty(idProperty, id);

			return propertyAccessor.getBean();
		}

		@Override
		@Nullable
		public Number getVersion() {

			ElasticsearchPersistentProperty versionProperty = entity.getVersionProperty();
			return versionProperty != null ? propertyAccessor.getProperty(versionProperty, Number.class) : null;
		}

		@Override
		public boolean hasSeqNoPrimaryTerm() {
			return entity.hasSeqNoPrimaryTermProperty();
		}

		@Override
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {

			ElasticsearchPersistentProperty seqNoPrimaryTermProperty = entity.getRequiredSeqNoPrimaryTermProperty();

			return propertyAccessor.getProperty(seqNoPrimaryTermProperty, SeqNoPrimaryTerm.class);
		}

		@Override
		public T initializeVersionProperty() {

			if (!entity.hasVersionProperty()) {
				return propertyAccessor.getBean();
			}

			ElasticsearchPersistentProperty versionProperty = entity.getRequiredVersionProperty();

			propertyAccessor.setProperty(versionProperty, versionProperty.getType().isPrimitive() ? 1 : 0);

			return propertyAccessor.getBean();
		}

		@Override
		public T incrementVersion() {

			ElasticsearchPersistentProperty versionProperty = entity.getRequiredVersionProperty();
			Number version = getVersion();
			Number nextVersion = version == null ? 0 : version.longValue() + 1;

			propertyAccessor.setProperty(versionProperty, nextVersion);

			return propertyAccessor.getBean();
		}

		@Override
		public String getRouting() {

			String routing = routingResolver.getRouting(propertyAccessor.getBean());

			if (routing != null) {
				return routing;
			}

			ElasticsearchPersistentProperty joinFieldProperty = entity.getJoinFieldProperty();

			if (joinFieldProperty != null) {
				JoinField<?> joinField = propertyAccessor.getProperty(joinFieldProperty, JoinField.class);

				if (joinField != null && joinField.getParent() != null) {
					return conversionService.convert(joinField.getParent(), String.class);
				}
			}

			return null;
		}

	}

}
