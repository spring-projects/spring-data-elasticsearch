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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperationBase;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.util.BinaryData;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Splits a list of {@link BulkOperation}s into batches that each respect a maximum number of operations and a maximum
 * size in bytes. A new batch is started before an operation would exceed one of the limits. An operation that is larger
 * than the maximum size on its own is put into a batch of its own.
 * <p>
 * To measure the size, the document (or the update action) of an operation is serialized once with the
 * {@link JsonpMapper} and replaced by the resulting {@link BinaryData}, so it is not serialized again when the request
 * is sent. This is the same approach that is used by the {@code BulkIngester} of the Elasticsearch Java client.
 *
 * @author Ralph Ursprung
 * @since 6.2
 */
final class BulkRequestSplitter {

	// the two newlines terminating the action/metadata line and the document line
	private static final int NEWLINES_SIZE = 2;

	private final JsonpMapper jsonpMapper;

	BulkRequestSplitter(JsonpMapper jsonpMapper) {

		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		this.jsonpMapper = jsonpMapper;
	}

	/**
	 * Splits the operations into batches. Without limits, one batch containing all operations is returned, also if
	 * {@code operations} is empty. The order of the operations is preserved.
	 *
	 * @param operations the operations to split
	 * @param maxOperations the maximum number of operations per batch, {@literal null} for no limit
	 * @param maxSize the maximum size in bytes per batch, {@literal null} for no limit
	 * @return the batches, never empty
	 */
	List<List<BulkOperation>> split(List<BulkOperation> operations, @Nullable Integer maxOperations,
			@Nullable Long maxSize) {

		Assert.notNull(operations, "operations must not be null");

		if (maxOperations == null && maxSize == null) {
			return List.of(operations);
		}

		final var batches = new ArrayList<List<BulkOperation>>();
		var currentBatch = new ArrayList<BulkOperation>();
		var currentSize = 0L;

		for (final var operation : operations) {

			var operationToAdd = operation;
			var operationSize = 0L;

			if (maxSize != null) {
				final var sizedOperation = toSizedOperation(operation);
				operationToAdd = sizedOperation.operation();
				operationSize = sizedOperation.size();
			}

			final var exceedsMaxOperations = maxOperations != null && currentBatch.size() >= maxOperations;
			final var exceedsMaxSize = maxSize != null && currentSize + operationSize > maxSize;

			if (!currentBatch.isEmpty() && (exceedsMaxOperations || exceedsMaxSize)) {
				batches.add(currentBatch);
				currentBatch = new ArrayList<>();
				currentSize = 0L;
			}

			currentBatch.add(operationToAdd);
			currentSize += operationSize;
		}

		if (!currentBatch.isEmpty() || batches.isEmpty()) {
			batches.add(currentBatch);
		}

		return batches;
	}

	/**
	 * Replaces the document or update action of the operation by its serialized form and calculates the size the
	 * operation will have in the bulk request body.
	 *
	 * @param operation the operation
	 * @return the operation with the serialized content and its size in bytes
	 */
	SizedOperation toSizedOperation(BulkOperation operation) {

		final BulkOperation binaryOperation;
		final long contentSize;

		switch (operation._kind()) {
			case Index -> {
				final var index = operation.index();
				final var document = toBinaryData(index.document());
				binaryOperation = BulkOperation.of(bo -> bo.index(i -> {
					copyBaseProperties(index, i);
					return i //
							.pipeline(index.pipeline()) //
							.requireAlias(index.requireAlias()) //
							.document(document);
				}));
				contentSize = document.size();
			}
			case Create -> {
				final var create = operation.create();
				final var document = toBinaryData(create.document());
				binaryOperation = BulkOperation.of(bo -> bo.create(c -> {
					copyBaseProperties(create, c);
					return c //
							.pipeline(create.pipeline()) //
							.requireAlias(create.requireAlias()) //
							.document(document);
				}));
				contentSize = document.size();
			}
			case Update -> {
				final var update = operation.update();
				final var action = update.binaryAction() != null ? update.binaryAction()
						: BinaryData.of(update.action(), jsonpMapper);
				binaryOperation = BulkOperation.of(bo -> bo.update(u -> {
					copyBaseProperties(update, u);
					return u //
							.requireAlias(update.requireAlias()) //
							.retryOnConflict(update.retryOnConflict()) //
							.binaryAction(action);
				}));
				contentSize = action.size();
			}
			default -> {
				binaryOperation = operation;
				contentSize = 0L;
			}
		}

		// serializing a BulkOperation only writes the action/metadata line, not the document
		final var metadataSize = BinaryData.of(binaryOperation, jsonpMapper).size();

		return new SizedOperation(binaryOperation, metadataSize + contentSize + NEWLINES_SIZE);
	}

	private BinaryData toBinaryData(Object document) {
		return document instanceof BinaryData binaryData ? binaryData : BinaryData.of(document, jsonpMapper);
	}

	private static void copyBaseProperties(BulkOperationBase source, BulkOperationBase.AbstractBuilder<?> target) {
		target //
				.id(source.id()) //
				.index(source.index()) //
				.ifPrimaryTerm(source.ifPrimaryTerm()) //
				.ifSeqNo(source.ifSeqNo()) //
				.routing(source.routing()) //
				.version(source.version()) //
				.versionType(source.versionType());
	}

	/**
	 * A {@link BulkOperation} together with its size in bytes in the bulk request body.
	 */
	record SizedOperation(BulkOperation operation, long size) {
	}
}
