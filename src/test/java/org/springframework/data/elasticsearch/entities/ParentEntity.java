/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.elasticsearch.entities;

import org.springframework.core.style.ToStringCreator;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Parent;

/**
 * ParentEntity
 *
 * @author Philipp Jardas
 * @author Mohsin Husen
 */
@Document(indexName = ParentEntity.INDEX, type = ParentEntity.PARENT_TYPE, shards = 1, replicas = 0, refreshInterval = "-1")
public class ParentEntity {

	public static final String INDEX = "parent-child";
	public static final String PARENT_TYPE = "parent-entity";
	public static final String CHILD_TYPE = "child-entity";

	@Id
	private String id;
	@Field(type = FieldType.Text, store = true)
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

	@Document(indexName = INDEX, type = CHILD_TYPE, shards = 1, replicas = 0, refreshInterval = "-1")
	public static class ChildEntity {

		@Id
		private String id;
		@Field(type = FieldType.Text, store = true)
		@Parent(type = PARENT_TYPE)
		private String parentId;
		@Field(type = FieldType.Text, store = true)
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
