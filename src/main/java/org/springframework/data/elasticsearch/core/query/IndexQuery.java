/*
 * Copyright 2013-2024 the original author or authors.
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
 * @author Subhobrata Dey
 */
public class IndexQuery {

	@Nullable private String id;
	@Nullable private Object object;
	@Nullable private Long version;
	@Nullable private String source;
	@Nullable private Long seqNo;
	@Nullable private Long primaryTerm;
	@Nullable private String routing;
	@Nullable private OpType opType;
	@Nullable private String indexName;

	public IndexQuery() {}

	public IndexQuery(@Nullable String id, @Nullable Object object, @Nullable Long version, @Nullable String source,
			@Nullable Long seqNo, @Nullable Long primaryTerm, @Nullable String routing, @Nullable OpType opType,
			@Nullable String indexName) {
		this.id = id;
		this.object = object;
		this.version = version;
		this.source = source;
		this.seqNo = seqNo;
		this.primaryTerm = primaryTerm;
		this.routing = routing;
		this.opType = opType;
		this.indexName = indexName;
	}

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

	@Nullable
	public String getRouting() {
		return routing;
	}

	public void setRouting(@Nullable String routing) {
		this.routing = routing;
	}

	/**
	 * @since 4.2
	 */
	@Nullable
	public OpType getOpType() {
		return opType;
	}

	/**
	 * @since 4.2
	 */
	public void setOpType(OpType opType) {
		this.opType = opType;
	}

	/**
	 * @since 4.4
	 */
	@Nullable
	public String getIndexName() {
		return indexName;
	}

	/**
	 * OpType for the index operation.
	 *
	 * @since 4.2
	 */
	public enum OpType {
		INDEX("index"), CREATE("create");

		private final String esName;

		OpType(String esName) {
			this.esName = esName;
		}

		public String getEsName() {
			return esName;
		}
	}
}
