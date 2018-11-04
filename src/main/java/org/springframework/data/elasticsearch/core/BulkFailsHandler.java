/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import lombok.experimental.UtilityClass;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.springframework.data.elasticsearch.ElasticsearchException;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Kevin Leturc
 * @author Mason Chan
 * @author Young Gu
 * @author Oliver Gierke
 * @author Mark Janssen
 * @author Chris White
 * @author Mark Paluch
 * @author Ilkang Na
 * @author Alen Turkovic
 * @author Sascha Woo
 * @author Ted Liang
 * @author Don Wellington
 * @author Nikita Guchakov
 */
@UtilityClass
public class BulkFailsHandler {

	void checkForBulkUpdateFailure(BulkResponse bulkResponse) {

		if (!bulkResponse.hasFailures()) {
			return;
		}
		Map<String, String> failedDocuments = Arrays.stream(bulkResponse.getItems()).filter(BulkItemResponse::isFailed)
				.collect(toMap(BulkItemResponse::getId, BulkItemResponse::getFailureMessage, (a, b) -> b));
		throw new ElasticsearchException(
				"Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
						+ failedDocuments + "]",
				failedDocuments);
	}
}
