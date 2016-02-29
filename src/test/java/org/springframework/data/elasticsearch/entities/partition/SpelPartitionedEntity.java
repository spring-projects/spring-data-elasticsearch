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
package org.springframework.data.elasticsearch.entities.partition;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Partitioner;

import java.util.Date;

/**
 *
 * @author Franck Lefebure
 */
@Document(indexName = "index", type = "type", indexStoreType = "memory", shards = 1,
		replicas = 0, refreshInterval = "-1", partitionersFields = {"customerId","creationDate","id"}, partitioners = {Partitioner.fixed_string, Partitioner.date_range, Partitioner.long_range}, partitionersParameters = {"","YYYY","#{@partitionSize}"}, partitionSeparator = "-")
public class SpelPartitionedEntity {

	@Id
	private String id;

	@Field(type = FieldType.String)
	private String label;

	@Field(type = FieldType.Date)
	Date creationDate;

	@Field(type = FieldType.String)
	String customerId;

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
