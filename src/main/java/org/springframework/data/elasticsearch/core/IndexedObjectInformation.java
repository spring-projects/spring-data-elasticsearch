/*
 * Copyright 2020-2023 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Value class capturing information about a newly indexed document in Elasticsearch.
 *
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @since 4.1
 */
public class IndexedObjectInformation {
	@Nullable private final String id;
	@Nullable private final Long seqNo;
	@Nullable private final Long primaryTerm;
	@Nullable private final Long version;

	private IndexedObjectInformation(@Nullable String id, @Nullable Long seqNo, @Nullable Long primaryTerm,
			@Nullable Long version) {
		this.id = id;
		this.seqNo = seqNo;
		this.primaryTerm = primaryTerm;
		this.version = version;
	}

	public static IndexedObjectInformation of(@Nullable String id, @Nullable Long seqNo, @Nullable Long primaryTerm,
			@Nullable Long version) {
		return new IndexedObjectInformation(id, seqNo, primaryTerm, version);
	}

	@Nullable
	public String getId() {
		return id;
	}

	@Nullable
	public Long getSeqNo() {
		return seqNo;
	}

	@Nullable
	public Long getPrimaryTerm() {
		return primaryTerm;
	}

	@Nullable
	public Long getVersion() {
		return version;
	}
}
