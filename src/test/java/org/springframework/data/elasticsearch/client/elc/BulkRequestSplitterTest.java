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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.client.elc.BulkRequestSplitter.*;

import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.NdJsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.util.BinaryData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * @author Ralph Ursprung
 */
class BulkRequestSplitterTest {

	private final JsonpMapper jsonpMapper = new JacksonJsonpMapper();
	private final BulkRequestSplitter splitter = new BulkRequestSplitter(jsonpMapper);

	@Test // #3345
	void shouldReturnOneBatchWithoutLimits() {

		final var operations = indexOperations(5, 10);

		final var batches = splitter.split(operations, NO_MAX_OPERATIONS, NO_MAX_SIZE);

		assertThat(batches).containsExactly(operations);
	}

	@Test // #3345
	void shouldReturnOneEmptyBatchForNoOperations() {

		assertThat(splitter.split(List.of(), NO_MAX_OPERATIONS, NO_MAX_SIZE)).containsExactly(List.of());
		assertThat(splitter.split(List.of(), 2, 100L)).containsExactly(List.of());
	}

	@Test // #3345
	void shouldRejectNonPositiveLimits() {

		assertThatIllegalArgumentException().isThrownBy(() -> splitter.split(List.of(), 0, NO_MAX_SIZE));
		assertThatIllegalArgumentException().isThrownBy(() -> splitter.split(List.of(), NO_MAX_OPERATIONS, 0L));
	}

	@Test // #3345
	void shouldSplitByMaxOperations() {

		final var operations = indexOperations(5, 10);

		final var batches = splitter.split(operations, 2, NO_MAX_SIZE);

		assertThat(batches).containsExactly( //
				operations.subList(0, 2), //
				operations.subList(2, 4), //
				operations.subList(4, 5));
	}

	@Test // #3345
	void shouldSplitByMaxSize() {

		final var operations = indexOperations(5, 100);
		final var operationSize = splitter.toSizedOperation(operations.get(0)).size();

		// room for two operations, but not for three
		final var batches = splitter.split(operations, NO_MAX_OPERATIONS, 3 * operationSize - 1);

		assertThat(batches).hasSize(3);
		assertThat(ids(batches.get(0))).containsExactly("0", "1");
		assertThat(ids(batches.get(1))).containsExactly("2", "3");
		assertThat(ids(batches.get(2))).containsExactly("4");
		batches.forEach(batch -> assertThat(requestBodySize(batch)).isLessThanOrEqualTo(3 * operationSize - 1));
	}

	@Test // #3345
	void shouldApplyTheStricterOfBothLimits() {

		final var operations = indexOperations(6, 100);
		final var operationSize = splitter.toSizedOperation(operations.get(0)).size();

		final var batches = splitter.split(operations, 2, 10 * operationSize);

		assertThat(batches).extracting(List::size).containsExactly(2, 2, 2);
	}

	@Test // #3345
	void shouldPutOversizedOperationIntoOwnBatch() {

		final var small = indexOperation("small-1", 10);
		final var large = indexOperation("large", 1000);
		final var small2 = indexOperation("small-2", 10);
		final var small3 = indexOperation("small-3", 10);
		final var maxSize = 3 * splitter.toSizedOperation(small).size();

		final var batches = splitter.split(List.of(small, large, small2, small3), NO_MAX_OPERATIONS, maxSize);

		assertThat(batches).hasSize(3);
		assertThat(ids(batches.get(0))).containsExactly("small-1");
		assertThat(ids(batches.get(1))).containsExactly("large");
		assertThat(ids(batches.get(2))).containsExactly("small-2", "small-3");
	}

	@Test // #3345
	void shouldCalculateTheExactRequestBodySize() {

		final var operations = List.of( //
				indexOperation("1", 50), //
				BulkOperation.of(bo -> bo.create(c -> c.index("test").id("2").routing("r").document(Map.of("a", "b")))), //
				BulkOperation.of(bo -> bo.update(u -> u.index("test").id("3").retryOnConflict(3) //
						.action(a -> a.doc(Map.of("field", "value")).docAsUpsert(true)))));

		final var sizedOperations = operations.stream().map(splitter::toSizedOperation).toList();
		final var calculatedSize = sizedOperations.stream().mapToLong(BulkRequestSplitter.SizedOperation::size).sum();

		assertThat(calculatedSize).isEqualTo(requestBodySize(operations));
		assertThat(requestBodySize(sizedOperations.stream().map(BulkRequestSplitter.SizedOperation::operation).toList()))
				.isEqualTo(calculatedSize);
	}

	@Test // #3345
	void shouldKeepIndexOperationPropertiesWhenReplacingTheDocument() {

		final var operation = BulkOperation.of(bo -> bo.index(i -> i //
				.index("test") //
				.id("42") //
				.routing("routing") //
				.version(7L) //
				.versionType(VersionType.External) //
				.pipeline("pipeline") //
				.requireAlias(true) //
				.document(Map.of("name", "value"))));

		final var sizedOperation = splitter.toSizedOperation(operation);

		assertThat(sizedOperation.operation().index().document()).isInstanceOf(BinaryData.class);
		assertThat(requestBody(List.of(sizedOperation.operation()))).isEqualTo(requestBody(List.of(operation)));
	}

	@Test // #3345
	void shouldKeepUpdateOperationPropertiesWhenReplacingTheAction() {

		final var operation = BulkOperation.of(bo -> bo.update(u -> u //
				.index("test") //
				.id("42") //
				.routing("routing") //
				.ifSeqNo(1L) //
				.ifPrimaryTerm(2L) //
				.retryOnConflict(3) //
				.requireAlias(true) //
				.action(a -> a.doc(Map.of("name", "value")))));

		final var sizedOperation = splitter.toSizedOperation(operation);

		assertThat(sizedOperation.operation().update().binaryAction()).isNotNull();
		assertThat(sizedOperation.operation().update().action()).isNull();
		assertThat(requestBody(List.of(sizedOperation.operation()))).isEqualTo(requestBody(List.of(operation)));
	}

	private static List<BulkOperation> indexOperations(int count, int documentSize) {
		return IntStream.range(0, count).mapToObj(i -> indexOperation(String.valueOf(i), documentSize)).toList();
	}

	private static BulkOperation indexOperation(String id, int documentSize) {
		return BulkOperation
				.of(bo -> bo.index(i -> i.index("test").id(id).document(Map.of("content", "x".repeat(documentSize)))));
	}

	private static List<String> ids(List<BulkOperation> operations) {
		return operations.stream().map(operation -> switch (operation._kind()) {
			case Index -> operation.index().id();
			case Create -> operation.create().id();
			case Update -> operation.update().id();
			case Delete -> operation.delete().id();
		}).toList();
	}

	private long requestBodySize(List<BulkOperation> operations) {
		return requestBody(operations).getBytes(StandardCharsets.UTF_8).length;
	}

	/**
	 * Serializes the operations the same way the Elasticsearch transport does for NDJSON requests.
	 */
	private String requestBody(List<BulkOperation> operations) {

		final var request = BulkRequest.of(b -> b.operations(operations));
		final var out = new ByteArrayOutputStream();
		writeNdJson(out, request);
		return out.toString(StandardCharsets.UTF_8);
	}

	private void writeNdJson(ByteArrayOutputStream out, NdJsonpSerializable value) {

		final var values = value._serializables();
		while (values.hasNext()) {
			final var item = values.next();
			if (item == null) {
				continue;
			}
			if (item instanceof NdJsonpSerializable ndJsonpSerializable && item != value) {
				writeNdJson(out, ndJsonpSerializable);
			} else {
				try {
					BinaryData.of(item, jsonpMapper).writeTo(out);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				out.write('\n');
			}
		}
	}
}
