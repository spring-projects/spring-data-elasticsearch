/*
 * Copyright 2021-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Peter-Josef Meisch
 */
public class BlockHoundTests {

	@Test // #1822
	@DisplayName("should fail if BlockHound is not installed")
	void shouldFailIfBlockHoundIsNotInstalled() {

		assertThatThrownBy(() -> {
			Mono.delay(Duration.ofMillis(1)).doOnNext(it -> {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}).block(); // should throw an exception about Thread.sleep
		}).hasCauseInstanceOf(BlockingOperationError.class);
	}
}
