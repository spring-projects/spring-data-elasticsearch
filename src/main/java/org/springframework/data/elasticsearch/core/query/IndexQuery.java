/*
 * Copyright 2013-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import org.springframework.lang.Nullable;

/**
 * IndexQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
public class IndexQuery {

	@Nullable private String id;
	@Nullable private Object object;
	@Nullable private Long version;
	@Nullable private String source;
	@Nullable private String parentId;
	@Nullable private Long seqNo;
	@Nullable private Long primaryTerm;

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Nullable
	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	@Nullable
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Nullable
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @deprecated from 4.0. Elasticsearch 7 does not support the parent id in an index request. parent/child relations
	 *             must be modeled using the join datatype. Setting it here will have no effect.
	 */
	@Nullable
	@Deprecated
	public String getParentId() {
		return parentId;
	}

	/**
	 * @deprecated from 4.0. Elasticsearch 7 does not support the parent id in an index request. parent/child relations
	 *             must be modeled using the join datatype. Setting it here will have no effect.
	 */
	@Deprecated
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	@Nullable
	public Long getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(Long seqNo) {
		this.seqNo = seqNo;
	}

	@Nullable
	public Long getPrimaryTerm() {
		return primaryTerm;
	}

	public void setPrimaryTerm(Long primaryTerm) {
		this.primaryTerm = primaryTerm;
	}
}
