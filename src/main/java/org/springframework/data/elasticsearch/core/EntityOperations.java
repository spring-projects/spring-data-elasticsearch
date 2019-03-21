/*
 * Copyright 2018-2019 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Common operations performed on an entity in the context of it's mapping metadata.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.2
 */
@RequiredArgsConstructor
class EntityOperations {

	private static final String ID_FIELD = "id";

	private final @NonNull MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context;

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
	 * Creates a new {@link AdaptibleEntity} for the given bean and {@link ConversionService}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T> AdaptibleEntity<T> forEntity(T entity, ConversionService conversionService) {

		Assert.notNull(entity, "Bean must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		if (entity instanceof Map) {
			return new SimpleMappedEntity((Map<String, Object>) entity);
		}

		return AdaptibleMappedEntity.of(entity, context, conversionService);
	}

	/**
	 * Determine index name and type name from {@link Entity} with {@code index} and {@code type} overrides. Allows using
	 * preferred values for index and type if provided, otherwise fall back to index and type defined on entity level.
	 *
	 * @param entity the entity to determine the index name. Can be {@literal null} if {@code index} and {@literal type}
	 *          are provided.
	 * @param index index name override can be {@literal null}.
	 * @param type index type override can be {@literal null}.
	 * @return the {@link IndexCoordinates} containing index name and index type.
	 * @see ElasticsearchPersistentEntity#getIndexName()
	 * @see ElasticsearchPersistentEntity#getIndexType()
	 */
	IndexCoordinates determineIndex(Entity<?> entity, @Nullable String index, @Nullable String type) {
		return determineIndex(entity.getPersistentEntity(), index, type);
	}

	/**
	 * Determine index name and type name from {@link ElasticsearchPersistentEntity} with {@code index} and {@code type}
	 * overrides. Allows using preferred values for index and type if provided, otherwise fall back to index and type
	 * defined on entity level.
	 *
	 * @param persistentEntity the entity to determine the index name. Can be {@literal null} if {@code index} and
	 *          {@literal type} are provided.
	 * @param index index name override can be {@literal null}.
	 * @param type index type override can be {@literal null}.
	 * @return the {@link IndexCoordinates} containing index name and index type.
	 * @see ElasticsearchPersistentEntity#getIndexName()
	 * @see ElasticsearchPersistentEntity#getIndexType()
	 */
	IndexCoordinates determineIndex(ElasticsearchPersistentEntity<?> persistentEntity, @Nullable String index,
			@Nullable String type) {
		return new IndexCoordinates(indexName(persistentEntity, index), typeName(persistentEntity, type));
	}

	private static String indexName(@Nullable ElasticsearchPersistentEntity<?> entity, @Nullable String index) {

		if (StringUtils.isEmpty(index)) {
			Assert.notNull(entity, "Cannot determine index name");
			return entity.getIndexName();
		}

		return index;
	}

	private static String typeName(@Nullable ElasticsearchPersistentEntity<?> entity, @Nullable String type) {

		if (StringUtils.isEmpty(type)) {
			Assert.notNull(entity, "Cannot determine index type");
			return entity.getIndexType();
		}

		return type;
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
	interface AdaptibleEntity<T> extends Entity<T> {

		/**
		 * Returns whether the entity has a parent.
		 *
		 * @return {@literal true} if the entity has a parent that has an {@literal id}.
		 */
		boolean hasParent();

		/**
		 * Returns the parent Id. Can be {@literal null}.
		 *
		 * @return can be {@literal null}.
		 */
		@Nullable
		Object getParentId();

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
		@Nullable
		Number getVersion();
	}

	/**
	 * @param <T>
	 * @author Christoph Strobl
	 * @since 3.2
	 */
	@RequiredArgsConstructor
	private static class MapBackedEntity<T extends Map<String, Object>> implements AdaptibleEntity<T> {

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
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#hasParent()
		 */
		@Override
		public boolean hasParent() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#getParentId()
		 */
		@Override
		public Entity<?> getParentId() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#populateIdIfNecessary(java.lang.Object)
		 */
		@Nullable
		@Override
		public T populateIdIfNecessary(@Nullable Object id) {

			map.put(ID_FIELD, id);

			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#initializeVersionProperty()
		 */
		@Override
		public T initializeVersionProperty() {
			return map;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#getVersion()
		 */
		@Override
		@Nullable
		public Number getVersion() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#incrementVersion()
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
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	private static class MappedEntity<T> implements Entity<T> {

		private final ElasticsearchPersistentEntity<?> entity;
		private final IdentifierAccessor idAccessor;
		private final PersistentPropertyAccessor<T> propertyAccessor;

		private static <T> MappedEntity<T> of(T bean,
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context) {

			ElasticsearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
			IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
			PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

			return new MappedEntity<>(entity, identifierAccessor, propertyAccessor);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getId()
		 */
		@Override
		public Object getId() {
			return idAccessor.getIdentifier();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#isVersionedEntity()
		 */
		@Override
		public boolean isVersionedEntity() {
			return entity.hasVersionProperty();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getVersion()
		 */
		@Override
		@Nullable
		public Object getVersion() {
			return propertyAccessor.getProperty(entity.getRequiredVersionProperty());
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getBean()
		 */
		@Override
		public T getBean() {
			return propertyAccessor.getBean();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#isNew()
		 */
		@Override
		public boolean isNew() {
			return entity.isNew(propertyAccessor.getBean());
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.Entity#getPersistentEntity()
		 */
		@Override
		public ElasticsearchPersistentEntity<?> getPersistentEntity() {
			return entity;
		}
	}

	/**
	 * @param <T>
	 * @since 3.2
	 */
	private static class AdaptibleMappedEntity<T> extends MappedEntity<T> implements AdaptibleEntity<T> {

		private final ElasticsearchPersistentEntity<?> entity;
		private final ConvertingPropertyAccessor<T> propertyAccessor;
		private final IdentifierAccessor identifierAccessor;

		private AdaptibleMappedEntity(ElasticsearchPersistentEntity<?> entity, IdentifierAccessor identifierAccessor,
				ConvertingPropertyAccessor<T> propertyAccessor) {

			super(entity, identifierAccessor, propertyAccessor);

			this.entity = entity;
			this.propertyAccessor = propertyAccessor;
			this.identifierAccessor = identifierAccessor;
		}

		static <T> AdaptibleEntity<T> of(T bean,
				MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> context,
				ConversionService conversionService) {

			ElasticsearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(bean.getClass());
			IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(bean);
			PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(bean);

			return new AdaptibleMappedEntity<>(entity, identifierAccessor,
					new ConvertingPropertyAccessor<>(propertyAccessor, conversionService));
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#hasParent()
		 */
		@Override
		public boolean hasParent() {
			return getRequiredPersistentEntity().getParentIdProperty() != null;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#getParentId()
		 */
		@Override
		public Object getParentId() {

			ElasticsearchPersistentProperty parentProperty = getRequiredPersistentEntity().getParentIdProperty();
			return propertyAccessor.getProperty(parentProperty);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#populateIdIfNecessary(java.lang.Object)
		 */
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

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.MappedEntity#getVersion()
		 */
		@Override
		@Nullable
		public Number getVersion() {

			ElasticsearchPersistentProperty versionProperty = entity.getRequiredVersionProperty();

			return propertyAccessor.getProperty(versionProperty, Number.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#initializeVersionProperty()
		 */
		@Override
		public T initializeVersionProperty() {

			if (!entity.hasVersionProperty()) {
				return propertyAccessor.getBean();
			}

			ElasticsearchPersistentProperty versionProperty = entity.getRequiredVersionProperty();

			propertyAccessor.setProperty(versionProperty, versionProperty.getType().isPrimitive() ? 1 : 0);

			return propertyAccessor.getBean();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.elasticsearch.core.EntityOperations.AdaptibleEntity#incrementVersion()
		 */
		@Override
		public T incrementVersion() {

			ElasticsearchPersistentProperty versionProperty = entity.getRequiredVersionProperty();
			Number version = getVersion();
			Number nextVersion = version == null ? 0 : version.longValue() + 1;

			propertyAccessor.setProperty(versionProperty, nextVersion);

			return propertyAccessor.getBean();
		}
	}

	/**
	 * Value object encapsulating index name and index type.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	@Getter
	static class IndexCoordinates {

		private final String indexName;
		private final String typeName;
	}
}
