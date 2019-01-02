/*
 * Copyright 2014-2019 the original author or authors.
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * @author Abdul Waheed
 * @author Mohsin Husen
 */
@Document(indexName = "test-index-2", type = "hetro", replicas = 0, shards = 1)
public class HetroEntity2 {

	@Id
	private String id;
	private String lastName;
	@Version
	private Long version;

	public HetroEntity2(String id, String lastName) {
		this.id = id;
		this.lastName = lastName;
		this.version = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SampleEntity)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		HetroEntity2 rhs = (HetroEntity2) obj;
		return new EqualsBuilder().append(this.id, rhs.id).append(this.lastName, rhs.lastName).append(this.version, rhs.version).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).append(lastName).append(version).toHashCode();
	}
}
