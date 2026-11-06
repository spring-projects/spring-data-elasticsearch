/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.elasticsearch.core.bulk;

import org.jspecify.annotations.Nullable;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;

/**
 * The result of a single operation of a bulk request sent by a {@link BulkIngester}.
 *
 * @param operationType the type of the operation
 * @param context the context that was passed when the operation was added, may be {@literal null}
 * @param id the id of the document, may be {@literal null} if Elasticsearch did not return one
 * @param index the name of the index of the document, may be {@literal null} if Elasticsearch did not return one
 * @param status the HTTP status of the operation
 * @param indexedObjectInformation information about the document after a successful operation, {@literal null} if
 *          the operation failed
 * @param failure the failure details if the operation failed, {@literal null} if it was successful
 * @param <C> the type of the context
 * @author Ralph Ursprung
 * @since 6.2
 */
public record BulkOperationResult<C>( //
		BulkOperationType operationType, //
		@Nullable C context, //
		@Nullable String id, //
		@Nullable String index, //
		int status, //
		@Nullable IndexedObjectInformation indexedObjectInformation, //
		BulkFailureException.@Nullable FailureDetails failure //
) {

	/**
	 * @return {@literal true} if the operation was successful
	 */
	public boolean isSuccessful() {
		return failure == null;
	}
}
