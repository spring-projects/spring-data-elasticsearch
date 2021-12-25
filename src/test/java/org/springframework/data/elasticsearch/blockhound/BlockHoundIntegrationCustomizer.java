/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.elasticsearch.blockhound;

import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.blockhound.integration.BlockHoundIntegration;

/**
 * @author Peter-Josef Meisch
 */
public class BlockHoundIntegrationCustomizer implements BlockHoundIntegration {

	@Override
	public void applyTo(BlockHound.Builder builder) {
		// Elasticsearch classes reading from the classpath on initialization, needed for parsing Elasticsearch responses
		builder //
				.allowBlockingCallsInside("org.elasticsearch.common.xcontent.XContentBuilder", "<clinit>") // pre 7.16
				.allowBlockingCallsInside("org.elasticsearch.common.XContentBuilder", "<clinit>") // from 7.16 on
				.allowBlockingCallsInside("org.elasticsearch.xcontent.json.JsonXContent", "contentBuilder") // from 7.16 on
				.allowBlockingCallsInside("org.elasticsearch.Build", "<clinit>");

		builder.blockingMethodCallback(it -> {
			new Error(it.toString()).printStackTrace();
			throw new BlockingOperationError(it);
		});

	}
}
