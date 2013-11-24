package org.springframework.data.elasticsearch;

import org.springframework.core.style.ToStringCreator;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = ParentEntity.INDEX, type = ParentEntity.PARENT_TYPE, indexStoreType = "memory", shards = 1, replicas = 0, refreshInterval = "-1")
public class ParentEntity {
	public static final String INDEX = "parent-child";
	public static final String PARENT_TYPE = "parent-entity";
	public static final String CHILD_TYPE = "child-entity";

	@Id
	private String id;
	@Field(type = FieldType.String, index = FieldIndex.analyzed, store = true)
	private String name;

	public ParentEntity() {
	}

	public ParentEntity(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("id", id).append("name", name).toString();
	}

	@Document(indexName = INDEX, type = CHILD_TYPE, indexStoreType = "memory", shards = 1, replicas = 0, refreshInterval = "-1")
	public static class ChildEntity {
		@Id
		private String id;
		@Field(type = FieldType.String, store = true)
		@Parent(type = PARENT_TYPE)
		private String parentId;
		@Field(type = FieldType.String, index = FieldIndex.analyzed, store = true)
		private String name;

		public ChildEntity() {
		}

		public ChildEntity(String id, String parentId, String name) {
			this.id = id;
			this.parentId = parentId;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getParentId() {
			return parentId;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("id", id).append("parentId", parentId).append("name", name).toString();
		}
	}
}
