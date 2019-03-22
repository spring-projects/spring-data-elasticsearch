/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.elasticsearch.entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
@Document(indexName = "book", type = "book", indexStoreType = "memory", shards = 1, replicas = 0, refreshInterval = "-1")
public class Book {

	@Id
	private String id;
	private String name;
	@Field(type = FieldType.Object)
	private Author author;
	@Field(type = FieldType.Nested)
	private Map<Integer, Collection<String>> buckets = new HashMap<Integer, Collection<String>>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public Map<Integer, Collection<String>> getBuckets() {
		return buckets;
	}

	public void setBuckets(Map<Integer, Collection<String>> buckets) {
		this.buckets = buckets;
	}
}
